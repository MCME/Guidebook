package com.mcmiddleearth.guidebook.area;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Reads Area files back through the Area file adapter, from the sample files of each Shape and era. */
class AreaFileReadTest {

    private static final String WORLD = "middle-earth";
    private static final AreaDetails EDORAS_TEXT =
            new AreaDetails(true, "<gold>Edoras", "<gray>Capital of Rohan", true, "Welcome", true);
    private static final AreaGeometry.Cuboid OLD_CUBOID = new AreaGeometry.Cuboid(80, 44, -230, 120, 84, -170);

    @TempDir
    Path dataFolder;

    private static final WarpPoint EDORAS_WARP_POINT = new WarpPoint(100, 64, -200, 90.5f, -12.25f);
    private static final WarpPoint OLD_CUBOID_CENTRE = new WarpPoint(100, 64, -200, 0, 0);

    private void putSample(String sample) throws IOException {
        Path worldFolder = dataFolder.resolve(WORLD);
        Files.createDirectories(worldFolder);
        try (InputStream in = getClass().getResourceAsStream("/area-files/" + sample + ".yml")) {
            Files.copy(in, worldFolder.resolve("edoras.yml"));
        }
    }

    private Area onlyArea(AreaFiles.Loaded loaded) {
        assertEquals(List.of(), loaded.problems());
        assertEquals(1, loaded.areas().size());
        return loaded.areas().getFirst();
    }

    private Area loadSample(String sample) throws IOException {
        putSample(sample);
        return onlyArea(new AreaFolder(dataFolder.toFile()).loadAll());
    }

    private static void assertLoaded(AreaGeometry geometry, WarpPoint warpPoint, Area area) {
        assertEquals("edoras", area.name());
        assertEquals(geometry, area.geometry());
        assertEquals(WORLD, area.worldName());
        assertEquals(warpPoint, area.warpPoint());
        assertEquals(EDORAS_TEXT, area.details());
    }

    @Test
    void aSphereFileLoadsAsTheSphereAroundItsWarpPoint() throws IOException {
        assertLoaded(new AreaGeometry.Sphere(100, 64, -200, 30), EDORAS_WARP_POINT, loadSample("sphere"));
    }

    @Test
    void aCuboidFileLoadsAsTheCuboidBetweenItsCorners() throws IOException {
        assertLoaded(new AreaGeometry.Cuboid(80, 50, -230, 120, 90, -170), EDORAS_WARP_POINT, loadSample("cuboid"));
    }

    @Test
    void aPrismFileLoadsAsThePrismOnItsCorners() throws IOException {
        assertLoaded(
                new AreaGeometry.Prism(List.of(95, 120, 120, 80), List.of(-180, -170, -230, -230), 50, 90),
                EDORAS_WARP_POINT,
                loadSample("prism"));
    }

    @Test
    void anOldCentreAndSizeCuboidFileLoadsAsTheSameCuboidWithItsWarpPointAtTheCentre() throws IOException {
        assertLoaded(OLD_CUBOID, OLD_CUBOID_CENTRE, loadSample("old-centre-and-size-cuboid"));
    }

    @Test
    void anOldCuboidFileWithItsWorldOnlyUnderItsCentreLoadsInThatWorld() throws IOException {
        assertLoaded(OLD_CUBOID, OLD_CUBOID_CENTRE, loadSample("old-cuboid-world-under-centre"));
    }

    @Test
    void anAreaWrittenThenReadBackIsTheSame() throws IOException {
        List<AreaGeometry> shapes = List.of(
                new AreaGeometry.Sphere(10, 64, -4, 8),
                new AreaGeometry.Cuboid(80, 50, -230, 120, 90, -170),
                new AreaGeometry.Prism(List.of(95, 120, 120, 80), List.of(-180, -170, -230, -230), 50, 90));
        for (AreaGeometry shape : shapes) {
            Path folder = Files.createTempDirectory(dataFolder, "round-trip");
            Area written = Area.create("fangorn", WORLD, new WarpPoint(10, 64, -4, -135.75f, 45.5f), shape);
            written.setDetails(EDORAS_TEXT);
            AreaFolder files = new AreaFolder(folder.toFile());
            files.write(written);

            Area read = onlyArea(files.loadAll());

            assertEquals("fangorn", read.name());
            assertEquals(written.geometry(), read.geometry(), shape.toString());
            assertEquals(WORLD, read.worldName());
            assertEquals(new WarpPoint(10, 64, -4, -135.75f, 45.5f), read.warpPoint());
            assertEquals(EDORAS_TEXT, read.details());
        }
    }

    @Test
    void anAreaReadFromItsFileIsWrittenBackAsTheSameFile() throws IOException {
        for (String sample : List.of("sphere", "cuboid", "prism")) {
            Path folder = Files.createTempDirectory(dataFolder, sample);
            Files.createDirectories(folder.resolve(WORLD));
            try (InputStream in = getClass().getResourceAsStream("/area-files/" + sample + ".yml")) {
                Files.copy(in, folder.resolve(WORLD).resolve("edoras.yml"));
            }
            AreaFolder files = new AreaFolder(folder.toFile());

            Area area = onlyArea(files.loadAll());
            assertEquals(WORLD, area.worldName(), sample);
            Path rewritten = Files.createTempDirectory(dataFolder, sample + "-rewritten");
            new AreaFolder(rewritten.toFile()).write(area);

            Path file = rewritten.resolve(WORLD).resolve("edoras.yml");
            assertTrue(Files.isRegularFile(file), "no file at " + file);
            assertEquals(
                    sampleValues(sample),
                    YamlConfiguration.loadConfiguration(file.toFile()).getValues(true),
                    sample);
        }
    }

    @Test
    void aFileWithNoShapeGuidebookKnowsIsReportedAsALoadProblem() throws IOException {
        Path worldFolder = dataFolder.resolve(WORLD);
        Files.createDirectories(worldFolder);
        Files.writeString(worldFolder.resolve("edoras.yml"), """
                world: middle-earth
                locationX: 100
                locationY: 64
                locationZ: -200
                title: Edoras
                minimessage: true
                """);

        AreaFiles.Loaded loaded = new AreaFolder(dataFolder.toFile()).loadAll();

        assertEquals(List.of(), loaded.areas());
        assertEquals(1, loaded.problems().size());
        assertTrue(
                loaded.problems().getFirst().contains("edoras"),
                loaded.problems().getFirst());
    }

    @Test
    void aFileWithATitleButNoSubtitleLoadsWithAnEmptySubtitle() throws IOException {
        Path worldFolder = dataFolder.resolve(WORLD);
        Files.createDirectories(worldFolder);
        Files.writeString(worldFolder.resolve("edoras.yml"), """
                world: middle-earth
                locationX: 100
                locationY: 64
                locationZ: -200
                radius: 10
                title: Edoras
                minimessage: true
                """);

        Area area = onlyArea(new AreaFolder(dataFolder.toFile()).loadAll());

        assertEquals("Edoras", area.details().title());
        assertEquals("", area.details().subtitle());
    }

    @Test
    void aTitleBlankedByHandLoadsEmptyAndKeepsTheRestOfTheText() throws IOException {
        Path worldFolder = dataFolder.resolve(WORLD);
        Files.createDirectories(worldFolder);
        Files.writeString(worldFolder.resolve("edoras.yml"), """
                world: middle-earth
                locationX: 100
                locationY: 64
                locationZ: -200
                radius: 10
                title:
                subtitle: Capital of Rohan
                showTitle: true
                showScoreboard: true
                description: Welcome
                enabled: false
                minimessage: true
                """);

        Area area = onlyArea(new AreaFolder(dataFolder.toFile()).loadAll());

        assertEquals(new AreaDetails(true, "", "Capital of Rohan", true, "Welcome", false), area.details());
    }

    @Test
    void aFileWithNoWorldIsReportedAsALoadProblem() throws IOException {
        Path worldFolder = dataFolder.resolve(WORLD);
        Files.createDirectories(worldFolder);
        Files.writeString(worldFolder.resolve("edoras.yml"), """
                locationX: 100
                locationY: 64
                locationZ: -200
                radius: 10
                title: Edoras
                minimessage: true
                """);

        AreaFiles.Loaded loaded = new AreaFolder(dataFolder.toFile()).loadAll();

        assertEquals(List.of(), loaded.areas());
        assertEquals(1, loaded.problems().size());
        assertTrue(
                loaded.problems().getFirst().endsWith("names no world."),
                loaded.problems().getFirst());
    }

    private Map<String, Object> sampleValues(String sample) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/area-files/" + sample + ".yml")) {
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getValues(true);
        }
    }
}
