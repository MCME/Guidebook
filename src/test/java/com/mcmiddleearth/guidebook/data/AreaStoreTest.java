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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AreaStoreTest {

    private final InMemoryAreaFiles files = new InMemoryAreaFiles();
    private final RecordingMarkerLayer layer = new RecordingMarkerLayer();
    private final List<LogRecord> logged = new ArrayList<>();
    private final AreaStore store =
            new AreaStore(files, layer, worldName -> !worldName.equals(UNLOADED_WORLD), recordingLogger());

    /** The one world the store is told isn't loaded. */
    private static final String UNLOADED_WORLD = "old-world";

    /** An Area in {@link #UNLOADED_WORLD}: a sphere of radius 5 centred on its Warp point at (0, 64, 0). */
    private static InfoArea inUnloadedWorld(String name) {
        return InfoArea.create(
                name, UNLOADED_WORLD, new WarpPoint(0, 64, 0, 0, 0), new AreaGeometry.Sphere(0, 64, 0, 5));
    }

    /** A real Area: a sphere of radius 5 centred on its Warp point at {@code x} in the world "middle-earth". */
    private static InfoArea sphereArea(String name, int x) {
        return InfoArea.create(
                name, "middle-earth", new WarpPoint(x, 64, 0, 0, 0), new AreaGeometry.Sphere(x, 64, 0, 5));
    }

    /** The cuboid (-100, 0, -100) to (100, 90, 100) in "middle-earth", big enough to hold other Areas. */
    private static InfoArea city() {
        return InfoArea.create(
                "city",
                "middle-earth",
                new WarpPoint(0, 64, 0, 0, 0),
                new AreaGeometry.Cuboid(-100, 0, -100, 100, 90, 100));
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
        InfoArea minasBefore = store.find("minas").orElseThrow();

        files.delete("middle-earth", "edoras");
        fileOf(sphereArea("rohan", 200));
        store.reload();

        assertEquals(Set.of("minas", "rohan"), names());
        assertNotSame(minasBefore, store.find("minas").orElseThrow());
        assertEquals(Set.of(AreaMarkers.id("minas"), AreaMarkers.id("rohan")), layer.ids());
    }

    @Test
    void anAreaWhoseWorldIsntLoadedIsLoadedFullyButErasedFromTheWebMapWithAWarning() throws IOException {
        fileOf(sphereArea("edoras", 0));
        fileOf(inUnloadedWorld("barad-dur"));
        layer.put(new Marker(AreaMarkers.id("barad-dur"), "old-world", "", "", new Outline.Circle(0, 0, 5), null));

        store.reload();

        InfoArea baradDur = store.find("barad-dur").orElseThrow();
        assertEquals(new AreaGeometry.Sphere(0, 64, 0, 5), baradDur.getGeometry());
        assertEquals(UNLOADED_WORLD, baradDur.getWorldName());
        assertEquals(Set.of(AreaMarkers.id("edoras")), layer.ids());
        assertEquals(
                List.of("Area barad-dur isn't drawn on the Web map because its world isn't loaded."),
                messagesAt(Level.WARNING));
    }

    @Test
    void anAreaWhoseWorldIsntLoadedStaysOffTheWebMapWhenAnotherAreaChanges() throws IOException {
        fileOf(inUnloadedWorld("barad-dur"));
        store.reload();

        store.create(sphereArea("edoras", 0), NEW_DETAILS);

        assertEquals(Set.of(AreaMarkers.id("edoras")), layer.ids());
    }

    // ADR 0007
    @Test
    void anAreaHoldingAnothersFootprintIsDrawnAsAnOutline() throws IOException {
        fileOf(city());
        store.reload();
        assertTrue(fillOf("city") > 0);

        store.create(sphereArea("tower", 50), NEW_DETAILS);

        assertEquals(0, fillOf("city"));
        assertTrue(fillOf("tower") > 0);
    }

    // ADR 0007
    @Test
    void aMoveIntoAnotherAreasFootprintRedrawsThatAreaAsAnOutline() throws IOException {
        fileOf(city());
        fileOf(sphereArea("tower", 500));
        store.reload();
        assertTrue(fillOf("city") > 0);

        store.move(
                store.find("tower").orElseThrow(),
                "middle-earth",
                new WarpPoint(50, 64, 0, 0, 0),
                new AreaGeometry.Sphere(50, 64, 0, 5));

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
        InfoArea edoras = store.find("edoras").orElseThrow();

        ChangeOutcome outcome = store.edit(edoras, NEW_DETAILS);

        assertEquals(new ChangeOutcome.Done(), outcome);
        assertSame(edoras, store.find("edoras").orElseThrow());
        assertEquals(NEW_DETAILS, edoras.details());
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
        InfoArea edoras = store.find("edoras").orElseThrow();
        AreaDetails before = edoras.details();
        String fileBefore =
                files.contents("middle-earth", "edoras").orElseThrow().saveToString();
        Marker markerBefore = layer.markerOf("edoras").orElseThrow();

        files.failNextWrite();
        ChangeOutcome outcome = store.edit(edoras, NEW_DETAILS);

        assertInstanceOf(ChangeOutcome.NotWritten.class, outcome);
        assertEquals(before, edoras.details());
        assertEquals(
                fileBefore,
                files.contents("middle-earth", "edoras").orElseThrow().saveToString());
        assertEquals(markerBefore, layer.markerOf("edoras").orElseThrow());
    }

    @Test
    void anEditOfAnAreaReloadedMeanwhileIsGoneAndWritesNothing() throws IOException {
        fileOf(sphereArea("edoras", 0));
        store.reload();
        InfoArea stale = store.find("edoras").orElseThrow();
        store.reload();

        ChangeOutcome outcome = store.edit(stale, NEW_DETAILS);

        assertEquals(new ChangeOutcome.Gone(), outcome);
        assertEquals("", files.contents("middle-earth", "edoras").orElseThrow().getString("title"));
        assertEquals("", store.find("edoras").orElseThrow().details().title());
    }

    @Test
    void anEditOfAnAreaDeletedMeanwhileIsGoneAndWritesNothing() throws IOException {
        fileOf(sphereArea("edoras", 0));
        store.reload();
        InfoArea deleted = store.find("edoras").orElseThrow();
        store.delete(deleted);

        ChangeOutcome outcome = store.edit(deleted, NEW_DETAILS);

        assertEquals(new ChangeOutcome.Gone(), outcome);
        assertTrue(files.contents("middle-earth", "edoras").isEmpty());
        assertTrue(layer.markerOf("edoras").isEmpty());
    }

    @Test
    void aCreateAddsTheAreaWithItsDetailsWritesItsFileAndDrawsItsMarker() {
        store.reload();
        InfoArea edoras = sphereArea("edoras", 0);

        ChangeOutcome outcome = store.create(edoras, NEW_DETAILS);

        assertEquals(new ChangeOutcome.Done(), outcome);
        assertSame(edoras, store.find("edoras").orElseThrow());
        assertEquals(NEW_DETAILS, edoras.details());
        YamlConfiguration file = files.contents("middle-earth", "edoras").orElseThrow();
        assertEquals("<gold>Edoras", file.getString("title"));
        assertFalse(file.getBoolean("enabled"));
        assertEquals("Edoras (Disabled)", layer.markerOf("edoras").orElseThrow().label());
    }

    @Test
    void aCreateWhoseWriteFailsAddsNothingAndDrawsNothing() {
        store.reload();

        files.failNextWrite();
        ChangeOutcome outcome = store.create(sphereArea("edoras", 0), NEW_DETAILS);

        assertInstanceOf(ChangeOutcome.NotWritten.class, outcome);
        assertTrue(store.find("edoras").isEmpty());
        assertTrue(files.contents("middle-earth", "edoras").isEmpty());
        assertTrue(layer.ids().isEmpty());
    }

    @Test
    void aCreateWhoseNameWasTakenMeanwhileIgnoringCaseWritesNothing() {
        store.reload();
        InfoArea mine = sphereArea("edoras", 0);
        store.create(sphereArea("Edoras", 100), sphereArea("Edoras", 100).details());

        ChangeOutcome outcome = store.create(mine, NEW_DETAILS);

        assertEquals(new ChangeOutcome.NameTaken(), outcome);
        assertEquals(Set.of("Edoras"), names());
        assertTrue(files.contents("middle-earth", "edoras").isEmpty());
        assertEquals("", files.contents("middle-earth", "Edoras").orElseThrow().getString("title"));
        assertEquals(Set.of(AreaMarkers.id("Edoras")), layer.ids());
    }

    /** Far from every test Area, and centred on {@link #MOVED_WARP_POINT}, as a sphere is. */
    private static final AreaGeometry.Sphere MOVED_TO = new AreaGeometry.Sphere(500, 70, 0, 8);

    private static final WarpPoint MOVED_WARP_POINT = new WarpPoint(500, 70, 0, 0, 0);

    /** Where a staff member stands, part way into a block; the world doesn't matter to a Warp point. */
    private static final Location STANDING = new Location(null, 500.7, 70.2, -0.4, 45.5f, -10.25f);

    @Test
    void aCreateRoundsTheWarpPointToWholeBlocksKeepingYawAndPitch() {
        store.reload();
        InfoArea edoras = InfoArea.create(
                "edoras", "middle-earth", WarpPoint.of(STANDING), new AreaGeometry.Cuboid(490, 60, -10, 510, 80, 10));

        store.create(edoras, NEW_DETAILS);

        assertEquals(new WarpPoint(500, 70, -1, 45.5f, -10.25f), edoras.getWarpPoint());
        assertEquals(-1, files.contents("middle-earth", "edoras").orElseThrow().getInt("locationZ"));
    }

    @Test
    void aMoveRoundsTheWarpPointToWholeBlocksKeepingYawAndPitch() throws IOException {
        fileOf(sphereArea("edoras", 0));
        store.reload();
        InfoArea edoras = store.find("edoras").orElseThrow();

        store.move(edoras, "middle-earth", WarpPoint.of(STANDING), new AreaGeometry.Cuboid(490, 60, -10, 510, 80, 10));

        assertEquals(new WarpPoint(500, 70, -1, 45.5f, -10.25f), edoras.getWarpPoint());
    }

    @Test
    void aSphereMovedToANewWarpPointIsCentredOnIt() throws IOException {
        fileOf(sphereArea("edoras", 0));
        store.reload();
        InfoArea edoras = store.find("edoras").orElseThrow();

        store.move(edoras, "middle-earth", WarpPoint.of(STANDING), new AreaGeometry.Sphere(0, 64, 0, 8));

        assertEquals(new AreaGeometry.Sphere(500, 70, -1, 8), edoras.getGeometry());
        assertEquals(
                new Outline.Circle(500, -1, 8),
                layer.markerOf("edoras").orElseThrow().outline());
    }

    @Test
    void aMoveGivesTheAreaItsNewShapeAndWarpPointAndUpdatesItsFileAndMarker() throws IOException {
        fileOf(sphereArea("edoras", 0));
        store.reload();
        InfoArea edoras = store.find("edoras").orElseThrow();

        ChangeOutcome outcome = store.move(edoras, "middle-earth", MOVED_WARP_POINT, MOVED_TO);

        assertEquals(new ChangeOutcome.Done(), outcome);
        assertSame(edoras, store.find("edoras").orElseThrow());
        assertEquals(MOVED_TO, edoras.getGeometry());
        assertEquals(MOVED_WARP_POINT, edoras.getWarpPoint());
        YamlConfiguration file = files.contents("middle-earth", "edoras").orElseThrow();
        assertEquals(500, file.getDouble("locationX"));
        assertEquals(70, file.getDouble("locationY"));
        assertEquals(
                new Outline.Circle(500, 0, 8),
                layer.markerOf("edoras").orElseThrow().outline());
    }

    @Test
    void aMoveWhoseWriteFailsLeavesTheAreaItsFileAndTheWebMapAsTheyWere() throws IOException {
        fileOf(sphereArea("edoras", 0));
        store.reload();
        InfoArea edoras = store.find("edoras").orElseThrow();
        String fileBefore =
                files.contents("middle-earth", "edoras").orElseThrow().saveToString();
        Marker markerBefore = layer.markerOf("edoras").orElseThrow();

        files.failNextWrite();
        ChangeOutcome outcome = store.move(edoras, "middle-earth", MOVED_WARP_POINT, MOVED_TO);

        assertInstanceOf(ChangeOutcome.NotWritten.class, outcome);
        assertEquals(new AreaGeometry.Sphere(0, 64, 0, 5), edoras.getGeometry());
        assertEquals(new WarpPoint(0, 64, 0, 0, 0), edoras.getWarpPoint());
        assertEquals(
                fileBefore,
                files.contents("middle-earth", "edoras").orElseThrow().saveToString());
        assertEquals(markerBefore, layer.markerOf("edoras").orElseThrow());
    }

    @Test
    void aMoveOfAnAreaReloadedMeanwhileIsGoneAndWritesNothing() throws IOException {
        fileOf(sphereArea("edoras", 0));
        store.reload();
        InfoArea stale = store.find("edoras").orElseThrow();
        store.reload();

        ChangeOutcome outcome = store.move(stale, "middle-earth", MOVED_WARP_POINT, MOVED_TO);

        assertEquals(new ChangeOutcome.Gone(), outcome);
        assertEquals(0, files.contents("middle-earth", "edoras").orElseThrow().getDouble("locationX"));
        assertEquals(
                new Outline.Circle(0, 0, 5),
                layer.markerOf("edoras").orElseThrow().outline());
    }

    @Test
    void aMoveOfAnAreaDeletedMeanwhileIsGoneAndWritesNothing() throws IOException {
        fileOf(sphereArea("edoras", 0));
        store.reload();
        InfoArea deleted = store.find("edoras").orElseThrow();
        store.delete(deleted);

        ChangeOutcome outcome = store.move(deleted, "middle-earth", MOVED_WARP_POINT, MOVED_TO);

        assertEquals(new ChangeOutcome.Gone(), outcome);
        assertTrue(files.contents("middle-earth", "edoras").isEmpty());
        assertTrue(layer.markerOf("edoras").isEmpty());
    }

    @Test
    void aMoveToAnotherWorldWhoseOldFileCantBeDeletedIsDoneWithThatFileLeftOver() throws IOException {
        fileOf(sphereArea("edoras", 0));
        store.reload();
        InfoArea edoras = store.find("edoras").orElseThrow();
        files.failNextDelete();
        ChangeOutcome outcome = store.move(edoras, "arda", MOVED_WARP_POINT, MOVED_TO);

        assertEquals(new ChangeOutcome.DoneButOldFileLeft("middle-earth", "edoras"), outcome);
        assertEquals("arda", edoras.getWorldName());
        assertTrue(files.contents("arda", "edoras").isPresent());
        assertTrue(files.contents("middle-earth", "edoras").isPresent());
        assertEquals("arda", layer.markerOf("edoras").orElseThrow().world());
    }

    @Test
    void aMoveToAnotherWorldDeletesTheOldWorldsFile() throws IOException {
        fileOf(sphereArea("edoras", 0));
        store.reload();

        ChangeOutcome outcome = store.move(store.find("edoras").orElseThrow(), "arda", MOVED_WARP_POINT, MOVED_TO);

        assertEquals(new ChangeOutcome.Done(), outcome);
        assertTrue(files.contents("arda", "edoras").isPresent());
        assertTrue(files.contents("middle-earth", "edoras").isEmpty());
    }

    @Test
    void aRenameWritesTheNewFileDeletesTheOldOneAndRedrawsTheMarkerUnderTheNewName() throws IOException {
        fileOf(sphereArea("edoras", 0));
        store.reload();
        InfoArea edoras = store.find("edoras").orElseThrow();

        ChangeOutcome outcome = store.rename(edoras, "meduseld");

        assertEquals(new ChangeOutcome.Done(), outcome);
        assertEquals("meduseld", edoras.getName());
        assertTrue(files.contents("middle-earth", "meduseld").isPresent());
        assertTrue(files.contents("middle-earth", "edoras").isEmpty());
        assertTrue(layer.markerOf("meduseld").isPresent());
        assertTrue(layer.markerOf("edoras").isEmpty());
    }

    @Test
    void aRenameWhoseWriteFailsKeepsTheOldNameFileAndMarker() throws IOException {
        fileOf(sphereArea("edoras", 0));
        store.reload();
        InfoArea edoras = store.find("edoras").orElseThrow();
        String fileBefore =
                files.contents("middle-earth", "edoras").orElseThrow().saveToString();
        Marker markerBefore = layer.markerOf("edoras").orElseThrow();

        files.failNextWrite();
        ChangeOutcome outcome = store.rename(edoras, "meduseld");

        assertInstanceOf(ChangeOutcome.NotWritten.class, outcome);
        assertEquals("edoras", edoras.getName());
        assertSame(edoras, store.find("edoras").orElseThrow());
        assertTrue(store.find("meduseld").isEmpty());
        assertEquals(
                fileBefore,
                files.contents("middle-earth", "edoras").orElseThrow().saveToString());
        assertTrue(files.contents("middle-earth", "meduseld").isEmpty());
        assertEquals(markerBefore, layer.markerOf("edoras").orElseThrow());
        assertTrue(layer.markerOf("meduseld").isEmpty());
    }

    @Test
    void aRenameToANameTakenIgnoringCaseChangesNothing() throws IOException {
        fileOf(sphereArea("edoras", 0));
        fileOf(sphereArea("Meduseld", 100));
        store.reload();
        InfoArea edoras = store.find("edoras").orElseThrow();

        ChangeOutcome outcome = store.rename(edoras, "meduseld");

        assertEquals(new ChangeOutcome.NameTaken(), outcome);
        assertEquals("edoras", edoras.getName());
        assertEquals(Set.of("edoras", "Meduseld"), names());
        assertTrue(files.contents("middle-earth", "edoras").isPresent());
        assertTrue(files.contents("middle-earth", "meduseld").isEmpty());
        assertTrue(layer.markerOf("edoras").isPresent());
    }

    @Test
    void aRenameWhoseOldFileCantBeDeletedIsDoneWithThatFileLeftOver() throws IOException {
        fileOf(sphereArea("edoras", 0));
        store.reload();
        InfoArea edoras = store.find("edoras").orElseThrow();

        files.failNextDelete();
        ChangeOutcome outcome = store.rename(edoras, "meduseld");

        assertEquals(new ChangeOutcome.DoneButOldFileLeft("middle-earth", "edoras"), outcome);
        assertEquals("meduseld", edoras.getName());
        assertTrue(files.contents("middle-earth", "meduseld").isPresent());
        assertTrue(files.contents("middle-earth", "edoras").isPresent());
        assertTrue(layer.markerOf("edoras").isEmpty());
    }

    @Test
    void aRenameOfAnAreaReloadedMeanwhileIsGoneAndWritesNothing() throws IOException {
        fileOf(sphereArea("edoras", 0));
        store.reload();
        InfoArea stale = store.find("edoras").orElseThrow();
        store.reload();
        InfoArea current = store.find("edoras").orElseThrow();

        ChangeOutcome outcome = store.rename(stale, "meduseld");

        assertEquals(new ChangeOutcome.Gone(), outcome);
        assertEquals("edoras", stale.getName());
        assertSame(current, store.find("edoras").orElseThrow());
        assertEquals(Set.of("edoras"), names());
        assertTrue(files.contents("middle-earth", "edoras").isPresent());
        assertTrue(files.contents("middle-earth", "meduseld").isEmpty());
        assertTrue(layer.markerOf("edoras").isPresent());
        assertTrue(layer.markerOf("meduseld").isEmpty());
    }

    @Test
    void aRenamedAreaIsTheSameAreaFoundUnderItsNewName() throws IOException {
        fileOf(sphereArea("edoras", 0));
        store.reload();
        InfoArea edoras = store.find("edoras").orElseThrow();

        store.rename(edoras, "meduseld");

        assertSame(edoras, store.find("meduseld").orElseThrow());
        assertSame(edoras, store.find("MEDUSELD").orElseThrow());
        assertTrue(store.find("edoras").isEmpty());
        assertEquals(Set.of("meduseld"), names());
    }

    // ADR 0007
    @Test
    void aDeleteRemovesTheAreaItsFileAndItsMarkerAndRedrawsAnAreaThatHeldIt() throws IOException {
        fileOf(city());
        fileOf(sphereArea("tower", 50));
        store.reload();
        assertEquals(0, fillOf("city"));

        ChangeOutcome outcome = store.delete(store.find("tower").orElseThrow());

        assertEquals(new ChangeOutcome.Done(), outcome);
        assertEquals(Set.of("city"), names());
        assertTrue(files.contents("middle-earth", "tower").isEmpty());
        assertTrue(layer.markerOf("tower").isEmpty());
        assertTrue(fillOf("city") > 0);
    }

    @Test
    void aDeleteWhoseFileCantBeDeletedKeepsTheAreaAndItsMarker() throws IOException {
        fileOf(sphereArea("edoras", 0));
        store.reload();
        InfoArea edoras = store.find("edoras").orElseThrow();
        Marker markerBefore = layer.markerOf("edoras").orElseThrow();

        files.failNextDelete();
        ChangeOutcome outcome = store.delete(edoras);

        assertInstanceOf(ChangeOutcome.NotWritten.class, outcome);
        assertSame(edoras, store.find("edoras").orElseThrow());
        assertTrue(files.contents("middle-earth", "edoras").isPresent());
        assertEquals(markerBefore, layer.markerOf("edoras").orElseThrow());
    }

    @Test
    void aDeleteOfAnAreaReloadedMeanwhileIsGoneAndDeletesNoFile() throws IOException {
        fileOf(sphereArea("edoras", 0));
        store.reload();
        InfoArea stale = store.find("edoras").orElseThrow();
        store.reload();

        ChangeOutcome outcome = store.delete(stale);

        assertEquals(new ChangeOutcome.Gone(), outcome);
        assertTrue(files.contents("middle-earth", "edoras").isPresent());
        assertTrue(store.find("edoras").isPresent());
        assertTrue(layer.markerOf("edoras").isPresent());
    }

    @Test
    void aDeleteOfAnAreaDeletedMeanwhileIsGoneAndLeavesANewAreaOfThatNameAlone() throws IOException {
        fileOf(sphereArea("edoras", 0));
        store.reload();
        InfoArea deleted = store.find("edoras").orElseThrow();
        store.delete(deleted);
        store.create(sphereArea("edoras", 100), NEW_DETAILS);

        ChangeOutcome outcome = store.delete(deleted);

        assertEquals(new ChangeOutcome.Gone(), outcome);
        assertTrue(files.contents("middle-earth", "edoras").isPresent());
        assertTrue(layer.markerOf("edoras").isPresent());
    }

    // ADR 0007
    @Test
    void anEditRedrawsEveryAreaSoOneHoldingAnotherStaysAnOutline() throws IOException {
        fileOf(city());
        fileOf(sphereArea("tower", 50));
        store.reload();

        store.edit(store.find("city").orElseThrow(), NEW_DETAILS);

        Marker cityMarker = layer.markerOf("city").orElseThrow();
        assertEquals(0, cityMarker.style().fillOpacity());
        assertEquals("Edoras (Disabled)", cityMarker.label());
        assertTrue(fillOf("tower") > 0);
    }

    /** Holds exactly these Areas, as loaded from their files. */
    private void holding(InfoArea... areas) throws IOException {
        for (InfoArea area : areas) {
            fileOf(area);
        }
        store.reload();
    }

    /** An Area in "middle-earth" with {@code title} and {@code shape}, Enabled or not. */
    private static InfoArea titled(String name, String title, Shape shape, boolean enabled) {
        AreaGeometry geometry =
                switch (shape) {
                    case SPHERE -> new AreaGeometry.Sphere(0, 64, 0, 10);
                    case CUBOID -> new AreaGeometry.Cuboid(0, 60, 0, 10, 70, 10);
                    case PRISM -> new AreaGeometry.Prism(List.of(0, 10, 0), List.of(0, 0, 10), 60, 70);
                };
        return TestAreas.area(name, "middle-earth", geometry, new AreaDetails(false, title, "", false, "", enabled));
    }

    private Set<String> suggestedNames(String typed) {
        return store.suggest(typed).stream().map(AreaStore.Suggestion::name).collect(Collectors.toSet());
    }

    @Test
    void findsAnAreaByNameIgnoringCaseWithAnExactMatchWinning() throws IOException {
        holding(sphereArea("minas", 0), sphereArea("Edoras", 100), sphereArea("edoras", 200));

        assertEquals("minas", store.find("MINAS").orElseThrow().getName());
        assertEquals("edoras", store.find("edoras").orElseThrow().getName());
        assertEquals("Edoras", store.find("Edoras").orElseThrow().getName());
        assertTrue(store.find("rohan").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"helms-deep", "Minas_Tirith", "v1.2+old", "0"})
    void aNewNameMayHoldLettersDigitsAndUnderscoreHyphenDotPlus(String name) {
        assertEquals(Optional.empty(), store.newNameProblem(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {"helm's-deep", "helms deep", "", "minas/tirith", "café"})
    void aNewNameWithOtherCharactersIsRefusedNamingTheAllowedCharacters(String name) {
        assertEquals(
                Optional.of("'" + name + "' isn't a valid Area name. Use only letters, digits and _ - . +"),
                store.newNameProblem(name));
    }

    @Test
    void aNewNameTakenIgnoringCaseIsRefusedNamingTheAreaThatHasIt() throws IOException {
        holding(sphereArea("minas", 0));

        assertEquals(Optional.of("Area minas already has that name"), store.newNameProblem("MINAS"));
        assertEquals(Optional.empty(), store.newNameProblem("minas-tirith"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "world", "world-gondor-minas", "gondor", "gondor-mi", "minas", "MINAS", "m"})
    void suggestsANameWhenAnyHyphenSegmentStartsWithTheTypedText(String typed) throws IOException {
        holding(sphereArea("world-gondor-minas", 0));

        assertEquals(Set.of("world-gondor-minas"), suggestedNames(typed));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ondor", "inas", "gondor-minas-x", "rohan"})
    void doesNotSuggestANameWhenTheTypedTextStartsMidSegment(String typed) throws IOException {
        holding(sphereArea("world-gondor-minas", 0));

        assertEquals(Set.of(), suggestedNames(typed));
    }

    @Test
    void suggestsOnlyTheMatchingNames() throws IOException {
        holding(
                sphereArea("world-gondor-minas", 0),
                sphereArea("world-rohan-edoras", 100),
                sphereArea("world-gondor-osgiliath", 200));

        assertEquals(Set.of("world-gondor-minas", "world-gondor-osgiliath"), suggestedNames("gondor"));
    }

    @Test
    void aSuggestionCarriesTheAreasTitleAndShape() throws IOException {
        holding(titled("world-gondor-minas", "Minas Tirith", Shape.PRISM, true));

        assertEquals(
                List.of(new AreaStore.Suggestion("world-gondor-minas", "Minas Tirith", Shape.PRISM, false)),
                store.suggest("minas"));
    }

    @Test
    void aDisabledAreaIsStillSuggestedAndMarkedDisabled() throws IOException {
        holding(titled("world-gondor-minas", "Minas Tirith", Shape.SPHERE, false));

        assertEquals(
                List.of(new AreaStore.Suggestion("world-gondor-minas", "Minas Tirith", Shape.SPHERE, true)),
                store.suggest("minas"));
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
