package com.mcmiddleearth.guidebook.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Which blocks are inside a prism, by the same rule as PluginUtils' {@code PrismoidRegion}, which decides who gets a
 * Welcome. It is copied here, rounding and all, so the Placement check can use it without a running server. The rule
 * casts a ray along each axis from the block, counting crossed edges with each edge's slope rounded to whole blocks, and
 * counts the polygon's corners as inside.
 */
final class PrismInside {

    private final List<Integer> xs;
    private final List<Integer> zs;
    private final List<Double> gradients = new ArrayList<>();
    private final int minX;
    private final int maxX;
    private final int minZ;
    private final int maxZ;
    private final int minY;
    private final int maxY;

    PrismInside(AreaGeometry.Prism prism) {
        List<Integer> xs = prism.xs();
        List<Integer> zs = prism.zs();
        int n = xs.size();
        // PrismoidRegion walks the polygon one way round, reversing it when its turns add up negative. Its turn is
        // asin(edge1 × edge2 / (|edge1| |edge2|)), but Bukkit's crossProduct overwrites edge1 before |edge1| is read,
        // so each term is really asin(cross / (|cross| |edge2|)), and NaN, which never reverses, when cross is 0
        double angle = 0;
        for (int i = 0; i < n; i++) {
            int edge1X = xs.get((i + 1) % n) - xs.get(i);
            int edge1Z = zs.get((i + 1) % n) - zs.get(i);
            int edge2X = xs.get((i + 2) % n) - xs.get((i + 1) % n);
            int edge2Z = zs.get((i + 2) % n) - zs.get((i + 1) % n);
            double cross = (double) edge1Z * edge2X - (double) edge1X * edge2Z;
            double edge2Length = Math.sqrt((double) edge2X * edge2X + (double) edge2Z * edge2Z);
            angle += Math.asin(cross * (1 / (Math.sqrt(cross * cross) * edge2Length)));
        }
        if (angle < 0) {
            xs = xs.reversed();
            zs = zs.reversed();
        }
        this.xs = xs;
        this.zs = zs;
        for (int i = 0; i < n; i++) {
            gradients.add((zs.get((i + 1) % n) - zs.get(i)) / (1.0 * xs.get((i + 1) % n) - xs.get(i)));
        }
        this.minX = xs.stream().mapToInt(Integer::intValue).min().orElse(0);
        this.maxX = xs.stream().mapToInt(Integer::intValue).max().orElse(0);
        this.minZ = zs.stream().mapToInt(Integer::intValue).min().orElse(0);
        this.maxZ = zs.stream().mapToInt(Integer::intValue).max().orElse(0);
        this.minY = prism.minY();
        this.maxY = prism.maxY();
    }

    boolean contains(int x, int y, int z) {
        if (xs.size() < 3 || y < minY || y > maxY || x < minX || x > maxX || z < minZ || z > maxZ) {
            return false;
        }
        return insideZ(x, z) || insideX(x, z) || isCorner(x, z);
    }

    private boolean insideX(int x, int z) {
        boolean inside = false;
        int n = xs.size();
        for (int i = 0; i < n; i++) {
            int x1 = xs.get(i);
            int z1 = zs.get(i);
            int z2 = zs.get((i + 1) % n);
            int borderAddition = z1 > z2 ? 1 : 0;
            if (((z1 < z) != (z2 < z)) && (x < Math.round((z - z1) / gradients.get(i)) + x1 + borderAddition)) {
                inside = !inside;
            }
        }
        return inside;
    }

    private boolean insideZ(int x, int z) {
        boolean inside = false;
        int n = xs.size();
        for (int i = 0; i < n; i++) {
            int x1 = xs.get(i);
            int x2 = xs.get((i + 1) % n);
            int z1 = zs.get(i);
            int borderAddition = x1 > x2 ? -1 : 0;
            if (((x1 < x) != (x2 < x)) && (z > Math.round(gradients.get(i) * (x - x1)) + z1 + borderAddition)) {
                inside = !inside;
            }
        }
        return inside;
    }

    private boolean isCorner(int x, int z) {
        for (int i = 0; i < xs.size(); i++) {
            if (x == xs.get(i) && z == zs.get(i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The horizontal distance from a block to the polygon's outline, measured between block coordinates, or 0 inside
     * it.
     */
    double horizontalDistance(int x, int z) {
        if (contains(x, minY, z)) {
            return 0;
        }
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
