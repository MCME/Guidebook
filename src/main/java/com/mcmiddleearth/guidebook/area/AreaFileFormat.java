package com.mcmiddleearth.guidebook.area;

import java.util.List;
import org.bukkit.configuration.ConfigurationSection;

/**
 * The keys of an Area file: its Shape, its world, its Warp point and its text. Older Guidebook versions read the same
 * keys, so they don't change. A cuboid written before cuboids kept their corners holds a centre and its sizes instead,
 * and is read as the same cuboid with its Warp point at that centre.
 */
final class AreaFileFormat {

    private AreaFileFormat() {}

    /** What an Area file holds: an Area, or why it holds none that Guidebook can load. */
    sealed interface Read {

        record Found(Area area) implements Read {}

        /** @param why ends a sentence about the file, such as "names no world" */
        record Unreadable(String why) implements Read {}
    }

    /** Reads the Area {@code areaName} from its file's contents. Its world needn't be loaded. */
    static Read read(String areaName, ConfigurationSection file) {
        // An old cuboid file keeps its world under its center
        String worldName = file.getString("world", file.getString("center.world"));
        if (worldName == null) {
            return new Read.Unreadable("names no world");
        }

        WarpPoint warpPoint = new WarpPoint(
                file.getInt("locationX"),
                file.getInt("locationY"),
                file.getInt("locationZ"),
                (float) file.getDouble("locationYaw"),
                (float) file.getDouble("locationPitch"));
        AreaGeometry geometry;
        if (file.contains("radius")) {
            // Area#place says where a sphere's centre is
            geometry = new AreaGeometry.Sphere(warpPoint.x(), warpPoint.y(), warpPoint.z(), file.getInt("radius"));
        } else if (file.contains("xPoints")) {
            geometry = new AreaGeometry.Prism(
                    file.getIntegerList("xPoints"),
                    file.getIntegerList("zPoints"),
                    file.getInt("minY"),
                    file.getInt("maxY"));
        } else if (!file.contains("minCornerX") && !file.contains("xSize")) {
            return new Read.Unreadable("holds no Shape Guidebook knows");
        } else if (file.contains("center")) {
            // The old format: a centre and the sizes along each axis
            warpPoint = new WarpPoint(
                    blockOf(file.getDouble("center.x")),
                    blockOf(file.getDouble("center.y")),
                    blockOf(file.getDouble("center.z")),
                    0,
                    0);
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

        return new Read.Found(Area.loaded(areaName, worldName, warpPoint, geometry, details(file)));
    }

    /** The block a coordinate is in, as Bukkit's {@code Location.getBlockX} gives it. */
    private static int blockOf(double coordinate) {
        return (int) Math.floor(coordinate);
    }

    private static AreaGeometry.Cuboid oldCuboid(WarpPoint center, int sizeX, int sizeY, int sizeZ) {
        return new AreaGeometry.Cuboid(
                center.x() - sizeX / 2,
                center.y() - sizeY / 2,
                center.z() - sizeZ / 2,
                center.x() + sizeX / 2,
                center.y() + sizeY / 2,
                center.z() + sizeZ / 2);
    }

    // Each key with its own default, since a file can lack any of them: the oldest hold only a Description, and a
    // value blanked by hand reads as no key at all
    private static AreaDetails details(ConfigurationSection file) {
        return new AreaDetails(
                file.getBoolean("showTitle"),
                file.getString("title", ""),
                file.getString("subtitle", ""),
                // Stored under its old name, from when it was a scoreboard
                file.getBoolean("showScoreboard"),
                file.getString("description", ""),
                file.getBoolean("enabled", true));
    }

    /** Writes the Area into {@code file}. */
    static void write(Area area, ConfigurationSection file) {
        WarpPoint warpPoint = area.warpPoint();
        file.set("world", area.worldName());
        file.set("locationX", warpPoint.x());
        file.set("locationY", warpPoint.y());
        file.set("locationZ", warpPoint.z());
        file.set("locationYaw", warpPoint.yaw());
        file.set("locationPitch", warpPoint.pitch());

        switch (area.geometry()) {
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

        AreaDetails details = area.details();
        file.set("description", details.description());
        file.set("title", details.title());
        file.set("subtitle", details.subtitle());
        file.set("showScoreboard", details.showBossBar());
        file.set("showTitle", details.showTitle());
        file.set("enabled", details.enabled());
        file.set(LegacyMarkupConverter.MARKER, true);
    }
}
