package com.mcmiddleearth.guidebook.worldedit;

import com.mcmiddleearth.guidebook.data.AreaGeometry;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.Polygonal2DRegionSelector;
import com.sk89q.worldedit.regions.selector.SphereRegionSelector;
import com.sk89q.worldedit.world.World;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;

/** Guidebook's view of a staff member's WorldEdit selection. Untested: it needs a running WorldEdit. */
public final class WorldEditSelections {

    private WorldEditSelections() {}

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
