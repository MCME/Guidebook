package com.mcmiddleearth.guidebook.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mcmiddleearth.guidebook.webmap.Marker;
import com.mcmiddleearth.guidebook.webmap.Outline;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class AreaStoreTest {

    private final InMemoryAreaFiles files = new InMemoryAreaFiles();
    private final RecordingMarkerLayer layer = new RecordingMarkerLayer();
    private final List<LogRecord> logged = new ArrayList<>();
    private final AreaStore store = new AreaStore(files, layer, recordingLogger());

    /** A real Area: a sphere of radius 5 centred on its Warp point at {@code x} in the world "middle-earth". */
    private static InfoArea sphereArea(String name, int x) {
        return InfoArea.create(
                name, new Location(TestWorlds.named("middle-earth"), x, 64, 0), new AreaGeometry.Sphere(x, 64, 0, 5));
    }

    /** Puts an Area's file on disk without the store knowing, as an admin copying files in would. */
    private void fileOf(InfoArea area) throws IOException {
        files.write(area);
    }

    private Set<String> names() {
        return store.all().stream().map(InfoArea::getName).collect(Collectors.toSet());
    }

    @Test
    void reloadReplacesEveryAreaWithWhatTheFilesHoldAndRedrawsTheWebMap() throws IOException {
        fileOf(sphereArea("edoras", 0));
        fileOf(sphereArea("minas", 100));
        store.reload();
        InfoArea minasBefore = store.findExact("minas").orElseThrow();

        files.delete("middle-earth", "edoras");
        fileOf(sphereArea("rohan", 200));
        store.reload();

        assertEquals(Set.of("minas", "rohan"), names());
        assertNotSame(minasBefore, store.findExact("minas").orElseThrow());
        assertEquals(Set.of(AreaMarkers.id("minas"), AreaMarkers.id("rohan")), layer.ids());
    }

    @Test
    void anAreaWhoseWorldIsntLoadedIsErasedFromTheWebMapWithAWarning() throws IOException {
        fileOf(sphereArea("edoras", 0));
        files.holdInUnloadedWorld("old-world", "barad-dur", new AreaGeometry.Sphere(0, 64, 0, 5));
        layer.put(new Marker(AreaMarkers.id("barad-dur"), "old-world", "", "", new Outline.Circle(0, 0, 5), null));

        store.reload();

        assertTrue(store.findExact("barad-dur").isPresent());
        assertEquals(Set.of(AreaMarkers.id("edoras")), layer.ids());
        assertEquals(
                List.of("Area barad-dur isn't drawn on the Web map because its world isn't loaded."),
                messagesAt(Level.WARNING));
    }

    @Test
    void anAreaWhoseWorldIsntLoadedStaysOffTheWebMapWhenAnotherAreaChanges() throws IOException {
        files.holdInUnloadedWorld("old-world", "barad-dur", new AreaGeometry.Sphere(0, 64, 0, 5));
        store.reload();

        store.create(sphereArea("edoras", 0));

        assertEquals(Set.of(AreaMarkers.id("edoras")), layer.ids());
    }

    // ADR 0007
    @Test
    void anAreaHoldingAnothersFootprintIsDrawnAsAnOutline() throws IOException {
        InfoArea city = InfoArea.create(
                "city",
                new Location(TestWorlds.named("middle-earth"), 0, 64, 0),
                new AreaGeometry.Cuboid(-100, 0, -100, 100, 90, 100));
        fileOf(city);
        store.reload();
        assertTrue(fillOf("city") > 0);

        store.create(sphereArea("tower", 50));

        assertEquals(0, fillOf("city"));
        assertTrue(fillOf("tower") > 0);
    }

    @Test
    void reloadLogsTheBackupAndEachLoadProblem() {
        files.reportOnNextLoad(new File("Guidebook-legacy-backup"), List.of("Couldn't read a", "Couldn't read b"));

        store.reload();

        assertEquals(
                List.of("Backed up the Guidebook folder to Guidebook-legacy-backup before converting Guidebook areas"
                        + " from Legacy markup to MiniMessage."),
                messagesAt(Level.INFO));
        assertEquals(List.of("Couldn't read a", "Couldn't read b"), messagesAt(Level.SEVERE));
    }

    private static final AreaDetails NEW_DETAILS =
            new AreaDetails(false, "<gold>Edoras", "Golden Hall", true, "Meduseld stands here.", false);

    @Test
    void anEditChangesTheAreaItsFileAndItsMarker() throws IOException {
        fileOf(sphereArea("edoras", 0));
        store.reload();
        InfoArea edoras = store.findExact("edoras").orElseThrow();

        ChangeOutcome outcome = store.edit(edoras, NEW_DETAILS);

        assertEquals(new ChangeOutcome.Done(), outcome);
        assertSame(edoras, store.findExact("edoras").orElseThrow());
        assertEquals(NEW_DETAILS, AreaDetails.of(edoras));
        YamlConfiguration file = files.contents("middle-earth", "edoras").orElseThrow();
        assertEquals("<gold>Edoras", file.getString("title"));
        assertEquals("Golden Hall", file.getString("subtitle"));
        assertEquals("Meduseld stands here.", file.getString("description"));
        assertFalse(file.getBoolean("showTitle"));
        assertTrue(file.getBoolean("showScoreboard"));
        assertFalse(file.getBoolean("enabled"));
        assertEquals("Edoras (Disabled)", layer.markerOf("edoras").orElseThrow().label());
    }

    @Test
    void anEditWhoseWriteFailsLeavesTheAreaItsFileAndTheWebMapAsTheyWere() throws IOException {
        fileOf(sphereArea("edoras", 0));
        store.reload();
        InfoArea edoras = store.findExact("edoras").orElseThrow();
        AreaDetails before = AreaDetails.of(edoras);
        String fileBefore =
                files.contents("middle-earth", "edoras").orElseThrow().saveToString();
        Marker markerBefore = layer.markerOf("edoras").orElseThrow();

        files.failNextWrite();
        ChangeOutcome outcome = store.edit(edoras, NEW_DETAILS);

        assertInstanceOf(ChangeOutcome.NotWritten.class, outcome);
        assertEquals(before, AreaDetails.of(edoras));
        assertEquals(
                fileBefore,
                files.contents("middle-earth", "edoras").orElseThrow().saveToString());
        assertEquals(markerBefore, layer.markerOf("edoras").orElseThrow());
    }

    @Test
    void anEditOfAnAreaReloadedMeanwhileIsGoneAndWritesNothing() throws IOException {
        fileOf(sphereArea("edoras", 0));
        store.reload();
        InfoArea stale = store.findExact("edoras").orElseThrow();
        store.reload();

        ChangeOutcome outcome = store.edit(stale, NEW_DETAILS);

        assertEquals(new ChangeOutcome.Gone(), outcome);
        assertEquals("", files.contents("middle-earth", "edoras").orElseThrow().getString("title"));
        assertEquals("", store.findExact("edoras").orElseThrow().getTitle());
    }

    @Test
    void anEditOfAnAreaDeletedMeanwhileIsGoneAndWritesNothing() throws IOException {
        fileOf(sphereArea("edoras", 0));
        store.reload();
        InfoArea deleted = store.findExact("edoras").orElseThrow();
        store.delete(deleted);

        ChangeOutcome outcome = store.edit(deleted, NEW_DETAILS);

        assertEquals(new ChangeOutcome.Gone(), outcome);
        assertTrue(files.contents("middle-earth", "edoras").isEmpty());
        assertTrue(layer.markerOf("edoras").isEmpty());
    }

    // ADR 0007
    @Test
    void anEditRedrawsEveryAreaSoOneHoldingAnotherStaysAnOutline() throws IOException {
        InfoArea city = InfoArea.create(
                "city",
                new Location(TestWorlds.named("middle-earth"), 0, 64, 0),
                new AreaGeometry.Cuboid(-100, 0, -100, 100, 90, 100));
        fileOf(city);
        fileOf(sphereArea("tower", 50));
        store.reload();

        store.edit(store.findExact("city").orElseThrow(), NEW_DETAILS);

        Marker cityMarker = layer.markerOf("city").orElseThrow();
        assertEquals(0, cityMarker.style().fillOpacity());
        assertEquals("Edoras (Disabled)", cityMarker.label());
        assertTrue(fillOf("tower") > 0);
    }

    @Test
    void findsAnAreaByNameIgnoringCaseWithAnExactMatchWinning() throws IOException {
        fileOf(sphereArea("minas", 0));
        fileOf(sphereArea("Edoras", 100));
        fileOf(sphereArea("edoras", 200));
        store.reload();

        assertEquals("minas", store.find("MINAS").orElseThrow().getName());
        assertEquals("edoras", store.find("edoras").orElseThrow().getName());
        assertEquals("Edoras", store.find("Edoras").orElseThrow().getName());
        assertTrue(store.find("rohan").isEmpty());
    }

    @Test
    void findsAnAreaByExactNameOnly() throws IOException {
        fileOf(sphereArea("minas", 0));
        store.reload();

        assertEquals("minas", store.findExact("minas").orElseThrow().getName());
        assertTrue(store.findExact("Minas").isEmpty());
    }

    @Test
    void aNewNameIsRefusedWhenInvalidOrTakenIgnoringCase() throws IOException {
        fileOf(sphereArea("minas", 0));
        store.reload();

        assertEquals(Optional.empty(), store.newNameProblem("edoras"));
        assertEquals(Optional.of("Area minas already has that name"), store.newNameProblem("MINAS"));
        assertEquals(
                Optional.of("'helm's-deep' isn't a valid Area name. Use only letters, digits and _ - . +"),
                store.newNameProblem("helm's-deep"));
    }

    @Test
    void suggestsAreasWhoseNameStartsWithTheTypedText() throws IOException {
        fileOf(sphereArea("gondor-minas", 0));
        fileOf(sphereArea("rohan-edoras", 100));
        store.reload();

        assertEquals(
                List.of("gondor-minas"),
                store.suggest("minas").stream()
                        .map(AreaRegistry.Suggestion::name)
                        .toList());
    }

    private double fillOf(String areaName) {
        return layer.markerOf(areaName).orElseThrow().style().fillOpacity();
    }

    private List<String> messagesAt(Level level) {
        return logged.stream()
                .filter(record -> record.getLevel() == level)
                .map(LogRecord::getMessage)
                .toList();
    }

    private Logger recordingLogger() {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(record);
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        });
        return logger;
    }
}
