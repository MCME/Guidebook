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
import com.sk89q.worldedit.world.World;
import java.util.List;
import org.bukkit.entity.Player;

/**
 * Guidebook's view of staff members' WorldEdit selections. It reads the session directly, so an incomplete selection
 * is an ordinary result rather than a logged stack trace.
 */
public final class WorldEditSelections {

    private WorldEditSelections() {}

    /** A staff member's WorldEdit selection, as far as Guidebook can use it. */
    public sealed interface Selection {}

    /** A cuboid or polygon selection, as an Area's Shape in the selection's own world. */
    public record Usable(String world, AreaGeometry geometry) implements Selection {}

    /** The staff member hasn't selected anything. */
    public record NoSelection() implements Selection {}

    /** The selection was started but not finished, such as a cuboid with only one position set. */
    public record Incomplete(String typeName) implements Selection {}

    /** A selection of a type Guidebook can't make an Area from, such as a sphere, ellipsoid or cylinder. */
    public record Unsupported(String typeName) implements Selection {}

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

    // Every selector has a primary position once anything has been selected
    private static boolean hasStarted(RegionSelector selector) {
        try {
            selector.getPrimaryPosition();
            return true;
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
}
