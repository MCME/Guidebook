package com.mcmiddleearth.guidebook.worldedit;

import com.mcmiddleearth.guidebook.data.AreaGeometry;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.Polygonal2DRegionSelector;
import com.sk89q.worldedit.regions.selector.SphereRegionSelector;
import com.sk89q.worldedit.world.World;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;

/**
 * Guidebook's view of a staff member's WorldEdit selection. It reads the session directly, so a missing or unfinished
 * selection is an ordinary result and not an exception.
 */
public final class WorldEditSelections {

    private WorldEditSelections() {}

    /**
     * A staff member's WorldEdit selection, as far as Guidebook can use it.
     * A sum type: it's exactly one of the records below, so a {@code switch} over it must handle every case.
     */
    public sealed interface Selection {}

    /** A cuboid or polygon selection, as an Area's Shape in the selection's own world. */
    public record Usable(String world, AreaGeometry geometry) implements Selection {}

    /** The staff member hasn't selected anything. */
    public record NoSelection() implements Selection {}

    /** The selection was started but not finished, such as a cuboid with only one position set. */
    public record Incomplete(String typeName) implements Selection {}

    /** A selection of a type Guidebook can't make an Area from, such as a sphere, ellipsoid or cylinder. */
    public record Unsupported(String typeName) implements Selection {}

    /** Reads the player's WorldEdit selection, in whichever world it was made. */
    public static Selection read(Player player) {
        LocalSession session = WorldEdit.getInstance().getSessionManager().getIfPresent(BukkitAdapter.adapt(player));
        if (session == null) {
            return new NoSelection();
        }
        World world = session.getSelectionWorld();
        if (world == null) {
            return new NoSelection();
        }

        RegionSelector selector = session.getRegionSelector(world);
        if (!selector.isDefined()) {
            return hasStarted(selector) ? new Incomplete(selector.getTypeName()) : new NoSelection();
        }
        Region region;
        try {
            region = selector.getRegion();
        } catch (IncompleteRegionException ex) {
            return new Incomplete(selector.getTypeName());
        }

        return switch (region) {
            case CuboidRegion cuboid -> new Usable(world.getName(), toGeometry(cuboid));
            case Polygonal2DRegion polygon -> new Usable(world.getName(), toGeometry(polygon));
            default -> new Unsupported(selector.getTypeName());
        };
    }

    // Only the cuboid and polygon selectors (and their extending variants) reliably refuse a primary position before
    // anything is selected. A cleared sphere or ellipsoid selector still reports a centre, so after //sel those would
    // read as incomplete. Guidebook can't use them anyway, so an unfinished one counts as no selection
    private static boolean hasStarted(RegionSelector selector) {
        if (!(selector instanceof CuboidRegionSelector || selector instanceof Polygonal2DRegionSelector)) {
            return false;
        }
        try {
            return selector.getPrimaryPosition() != null;
        } catch (IncompleteRegionException ex) {
            return false;
        }
    }

    private static AreaGeometry toGeometry(CuboidRegion cuboid) {
        BlockVector3 min = cuboid.getMinimumPoint();
        BlockVector3 max = cuboid.getMaximumPoint();
        return new AreaGeometry.Cuboid(min.x(), min.y(), min.z(), max.x(), max.y(), max.z());
    }

    private static AreaGeometry toGeometry(Polygonal2DRegion polygon) {
        List<BlockVector2> points = polygon.getPoints();
        return new AreaGeometry.Prism(
                points.stream().map(BlockVector2::x).toList(),
                points.stream().map(BlockVector2::z).toList(),
                polygon.getMinimumY(),
                polygon.getMaximumY());
    }

    /**
     * Replaces the player's WorldEdit selection, and its selection mode, with {@code geometry} in {@code world}: a
     * cuboid, a polygon with the prism's heights, or a sphere.
     */
    public static void select(Player player, org.bukkit.World world, AreaGeometry geometry) {
        World weWorld = BukkitAdapter.adapt(world);
        RegionSelector selector =
                switch (geometry) {
                    case AreaGeometry.Sphere sphere ->
                        new SphereRegionSelector(
                                weWorld,
                                BlockVector3.at(sphere.centerX(), sphere.centerY(), sphere.centerZ()),
                                sphere.radius());
                    case AreaGeometry.Cuboid cuboid ->
                        new CuboidRegionSelector(
                                weWorld,
                                BlockVector3.at(cuboid.minX(), cuboid.minY(), cuboid.minZ()),
                                BlockVector3.at(cuboid.maxX(), cuboid.maxY(), cuboid.maxZ()));
                    case AreaGeometry.Prism prism -> {
                        List<BlockVector2> points = new ArrayList<>();
                        for (int i = 0; i < prism.xs().size(); i++) {
                            points.add(BlockVector2.at(
                                    prism.xs().get(i), prism.zs().get(i)));
                        }
                        yield new Polygonal2DRegionSelector(weWorld, points, prism.minY(), prism.maxY());
                    }
                };
        WorldEdit.getInstance()
                .getSessionManager()
                .get(BukkitAdapter.adapt(player))
                .setRegionSelector(weWorld, selector);
    }
}
