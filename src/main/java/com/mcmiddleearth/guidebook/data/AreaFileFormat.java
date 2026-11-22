package com.mcmiddleearth.guidebook.data;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

/**
 * The keys of an Area file: its Shape, its world, its Warp point and its text. Older Guidebook versions read the same
 * keys, so they don't change. A cuboid written before cuboids kept their corners holds a centre and its sizes instead,
 * and is read as the same cuboid with its Warp point at that centre.
 */
final class AreaFileFormat {

    private AreaFileFormat() {}

    /**
     * Reads the Area {@code areaName} from its file's contents.
     *
     * @param worlds looks a world up by its name, or gives null if it isn't loaded. The Area keeps its world's name
     *     either way.
     * @return the Area, or empty if the file holds no Shape Guidebook knows
     */
    static Optional<InfoArea> read(String areaName, ConfigurationSection file, Function<String, World> worlds) {
        // An old cuboid file keeps its world under its center
        String worldName = file.getString("world", file.getString("center.world"));
        World world = worldName == null ? null : worlds.apply(worldName);
        Location warpPoint = new Location(
                world,
                file.getInt("locationX"),
                file.getInt("locationY"),
                file.getInt("locationZ"),
                (float) file.getDouble("locationYaw"),
                (float) file.getDouble("locationPitch"));
        AreaGeometry geometry;
        if (file.contains("radius")) {
            geometry = new AreaGeometry.Sphere(
                    warpPoint.getBlockX(), warpPoint.getBlockY(), warpPoint.getBlockZ(), file.getInt("radius"));
        } else if (file.contains("xPoints")) {
            geometry = new AreaGeometry.Prism(
                    file.getIntegerList("xPoints"),
                    file.getIntegerList("zPoints"),
                    file.getInt("minY"),
                    file.getInt("maxY"));
        } else if (!file.contains("minCornerX") && !file.contains("xSize")) {
            return Optional.empty();
        } else if (file.contains("center")) {
            // The old format: a centre and the sizes along each axis
            warpPoint = new Location(
                    world, file.getDouble("center.x"), file.getDouble("center.y"), file.getDouble("center.z"));
            geometry = oldCuboid(warpPoint, file.getInt("xSize"), file.getInt("ySize"), file.getInt("zSize"));
        } else {
            geometry = new AreaGeometry.Cuboid(
                    file.getInt("minCornerX", 0),
                    file.getInt("minCornerY", 0),
                    file.getInt("minCornerZ", 0),
                    file.getInt("maxCornerX", 0),
                    file.getInt("maxCornerY", 0),
                    file.getInt("maxCornerZ", 0));
        }
        return Optional.of(InfoArea.loaded(areaName, worldName, warpPoint, geometry, details(file)));
    }

    private static AreaGeometry.Cuboid oldCuboid(Location center, int sizeX, int sizeY, int sizeZ) {
        return new AreaGeometry.Cuboid(
                center.getBlockX() - sizeX / 2,
                center.getBlockY() - sizeY / 2,
                center.getBlockZ() - sizeZ / 2,
                center.getBlockX() + sizeX / 2,
                center.getBlockY() + sizeY / 2,
                center.getBlockZ() + sizeZ / 2);
    }

    private static AreaDetails details(ConfigurationSection file) {
        String description = file.getString("description", "");
        if (!file.contains("title")) {
            return new AreaDetails(false, "", "", false, description, true);
        }
        return new AreaDetails(
                file.getBoolean("showTitle"),
                file.getString("title"),
                file.getString("subtitle"),
                // Stored under its old name, from when it was a scoreboard
                file.getBoolean("showScoreboard"),
                description,
                file.getBoolean("enabled", true));
    }

    /** Writes the Area into {@code file}, with the Warp point in whole blocks. */
    static void write(InfoArea area, ConfigurationSection file) {
        Location warpPoint = area.getLocation();
        file.set("world", area.getWorldName());
        file.set("locationX", warpPoint.getBlockX());
        file.set("locationY", warpPoint.getBlockY());
        file.set("locationZ", warpPoint.getBlockZ());
        file.set("locationYaw", warpPoint.getYaw());
        file.set("locationPitch", warpPoint.getPitch());
        switch (area.getGeometry()) {
            case AreaGeometry.Sphere sphere -> file.set("radius", sphere.radius());
            case AreaGeometry.Cuboid cuboid -> {
                file.set("minCornerX", cuboid.minX());
                file.set("minCornerY", cuboid.minY());
                file.set("minCornerZ", cuboid.minZ());
                file.set("maxCornerX", cuboid.maxX());
                file.set("maxCornerY", cuboid.maxY());
                file.set("maxCornerZ", cuboid.maxZ());
            }
            case AreaGeometry.Prism prism -> {
                file.set("xPoints", List.copyOf(prism.xs()));
                file.set("zPoints", List.copyOf(prism.zs()));
                file.set("minY", prism.minY());
                file.set("maxY", prism.maxY());
            }
        }
        file.set("description", area.getDescription());
        file.set("title", area.getTitle());
        file.set("subtitle", area.getSubtitle());
        file.set("showScoreboard", area.isShowBossBar());
        file.set("showTitle", area.isShowTitle());
        file.set("enabled", area.isEnabled());
        file.set(LegacyMarkupConverter.MARKER, true);
    }
}
