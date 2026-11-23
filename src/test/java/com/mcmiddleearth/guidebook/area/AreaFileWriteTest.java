package com.mcmiddleearth.guidebook.area;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pins what an Area's file holds for each Shape, as written today, before Areas become plain geometry. Reading the
 * files back is {@link AreaFileReadTest}'s.
 */
class AreaFileWriteTest {

    private static final String WORLD = "middle-earth";
    private static final Set<String> WARP_POINT_KEYS =
            Set.of("world", "locationX", "locationY", "locationZ", "locationYaw", "locationPitch");
    private static final Set<String> TEXT_KEYS =
            Set.of("description", "title", "subtitle", "showScoreboard", "showTitle", "enabled", "minimessage");

    @TempDir
    Path dataFolder;

    /** The Warp point of a staff member standing here, as creating an Area takes it. */
    private static WarpPoint warpPoint(double x, double y, double z, float yaw, float pitch) {
        return WarpPoint.of(new Location(null, x, y, z, yaw, pitch));
    }

    private static Area edoras(AreaGeometry geometry) {
        Area area = Area.create("edoras", WORLD, warpPoint(100, 64, -200, 90.5f, -12.25f), geometry);
        area.setDetails(new AreaDetails(true, "<gold>Edoras", "<gray>Capital of Rohan", true, "Welcome", false));
        return area;
    }

    private YamlConfiguration written(Area area) throws IOException {
        new AreaFolder(dataFolder.toFile()).write(area);
        File file = dataFolder.resolve(WORLD).resolve(area.name() + ".yml").toFile();
        assertTrue(file.isFile(), "no file at " + file);
        return YamlConfiguration.loadConfiguration(file);
    }

    private static void assertKeys(Set<String> shapeKeys, YamlConfiguration file) {
        Set<String> expected = new HashSet<>(WARP_POINT_KEYS);
        expected.addAll(TEXT_KEYS);
        expected.addAll(shapeKeys);
        assertEquals(expected, file.getKeys(false));
    }

    private static void assertWarpPoint(YamlConfiguration file) {
        assertEquals(WORLD, file.get("world"));
        assertEquals(100, file.get("locationX"));
        assertEquals(64, file.get("locationY"));
        assertEquals(-200, file.get("locationZ"));
        assertEquals(90.5, file.getDouble("locationYaw"));
        assertEquals(-12.25, file.getDouble("locationPitch"));
    }

    private static void assertText(YamlConfiguration file) {
        assertEquals("Welcome", file.get("description"));
        assertEquals("<gold>Edoras", file.get("title"));
        assertEquals("<gray>Capital of Rohan", file.get("subtitle"));
        assertEquals(true, file.get("showScoreboard"));
        assertEquals(true, file.get("showTitle"));
        assertEquals(false, file.get("enabled"));
        assertEquals(true, file.get("minimessage"));
    }

    @Test
    void aSphereWritesItsWarpPointRadiusAndText() throws IOException {
        YamlConfiguration file = written(edoras(new AreaGeometry.Sphere(100, 64, -200, 30)));

        assertKeys(Set.of("radius"), file);
        assertWarpPoint(file);
        assertEquals(30, file.get("radius"));
        assertText(file);
    }

    @Test
    void aCuboidWritesItsWarpPointCornersAndText() throws IOException {
        YamlConfiguration file = written(edoras(new AreaGeometry.Cuboid(80, 50, -230, 120, 90, -170)));

        assertKeys(Set.of("minCornerX", "minCornerY", "minCornerZ", "maxCornerX", "maxCornerY", "maxCornerZ"), file);
        assertWarpPoint(file);
        assertEquals(80, file.get("minCornerX"));
        assertEquals(50, file.get("minCornerY"));
        assertEquals(-230, file.get("minCornerZ"));
        assertEquals(120, file.get("maxCornerX"));
        assertEquals(90, file.get("maxCornerY"));
        assertEquals(-170, file.get("maxCornerZ"));
        assertText(file);
    }

    @Test
    void aPrismWritesItsWarpPointCornersHeightAndText() throws IOException {
        YamlConfiguration file = written(
                edoras(new AreaGeometry.Prism(List.of(95, 120, 120, 80), List.of(-180, -170, -230, -230), 50, 90)));

        assertKeys(Set.of("xPoints", "zPoints", "minY", "maxY"), file);
        assertWarpPoint(file);
        assertEquals(List.of(95, 120, 120, 80), file.getList("xPoints"));
        assertEquals(List.of(-180, -170, -230, -230), file.getList("zPoints"));
        assertEquals(50, file.get("minY"));
        assertEquals(90, file.get("maxY"));
        assertText(file);
    }

    /**
     * PluginUtils' {@code PrismoidRegion} turned a prism's corners round when they went the other way. The polygon is
     * the same either way.
     */
    @Test
    void aPrismWhoseCornersGoTheOtherWayIsWrittenWithThemInTheOrderGiven() throws IOException {
        YamlConfiguration file = written(
                edoras(new AreaGeometry.Prism(List.of(80, 120, 120, 95), List.of(-230, -230, -170, -180), 50, 90)));

        assertEquals(List.of(80, 120, 120, 95), file.getList("xPoints"));
        assertEquals(List.of(-230, -230, -170, -180), file.getList("zPoints"));
    }

    @Test
    void aFractionalWarpPointIsWrittenAsWholeBlocksKeepingYawAndPitch() throws IOException {
        Area area = Area.create(
                "fangorn", WORLD, warpPoint(10.7, 64.5, -3.2, -135.75f, 45.5f), new AreaGeometry.Sphere(10, 64, -4, 8));

        YamlConfiguration file = written(area);

        assertEquals(10, file.get("locationX"));
        assertEquals(64, file.get("locationY"));
        assertEquals(-4, file.get("locationZ"));
        assertEquals(-135.75, file.getDouble("locationYaw"));
        assertEquals(45.5, file.getDouble("locationPitch"));
    }

    @Test
    void theSampleFilesHoldWhatGuidebookWritesToday() throws IOException {
        assertSampleIsWritten("sphere", new AreaGeometry.Sphere(100, 64, -200, 30));
        assertSampleIsWritten("cuboid", new AreaGeometry.Cuboid(80, 50, -230, 120, 90, -170));
        assertSampleIsWritten(
                "prism", new AreaGeometry.Prism(List.of(95, 120, 120, 80), List.of(-180, -170, -230, -230), 50, 90));
    }

    private void assertSampleIsWritten(String sample, AreaGeometry geometry) throws IOException {
        Area area = edoras(geometry);
        area.setDetails(new AreaDetails(true, "<gold>Edoras", "<gray>Capital of Rohan", true, "Welcome", true));
        YamlConfiguration file = written(area);
        YamlConfiguration expected;
        try (InputStream in = getClass().getResourceAsStream("/area-files/" + sample + ".yml")) {
            expected = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        assertEquals(expected.getValues(true), file.getValues(true), sample);
    }
}
