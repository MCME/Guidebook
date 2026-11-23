package com.mcmiddleearth.guidebook.area;

import java.util.List;

/** How far a block is from a prism's polygon, across the ground, for the Placement check's "N blocks outside". */
final class PrismHorizontalDistance {

    private PrismHorizontalDistance() {}

    /**
     * The horizontal distance from a block to the polygon's outline, measured between block coordinates, or 0 when
     * its column is inside the prism.
     */
    static double of(AreaGeometry.Prism prism, int x, int z) {
        if (prism.containsBlock(x, prism.minY(), z)) {
            return 0;
        }
        List<Integer> xs = prism.xs();
        List<Integer> zs = prism.zs();
        double nearest = Double.MAX_VALUE;
        int n = xs.size();
        for (int i = 0; i < n; i++) {
            nearest = Math.min(
                    nearest, distanceToEdge(x, z, xs.get(i), zs.get(i), xs.get((i + 1) % n), zs.get((i + 1) % n)));
        }
        return nearest;
    }

    private static double distanceToEdge(int x, int z, int x1, int z1, int x2, int z2) {
        double edgeX = x2 - x1;
        double edgeZ = z2 - z1;
        double lengthSquared = edgeX * edgeX + edgeZ * edgeZ;
        double t = lengthSquared == 0 ? 0 : Math.clamp(((x - x1) * edgeX + (z - z1) * edgeZ) / lengthSquared, 0, 1);
        return Math.hypot(x - (x1 + t * edgeX), z - (z1 + t * edgeZ));
    }
}
