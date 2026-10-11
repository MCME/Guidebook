package com.mcmiddleearth.guidebook.data;

import java.util.List;

/** Where an Area's Shape is in its world, in block coordinates. */
public sealed interface AreaGeometry {

    /** The block-inclusive extents of a Shape along X, Y and Z. */
    record Sides(int x, int y, int z) {

        public int smallest() {
            return Math.min(x, Math.min(y, z));
        }

        public int largest() {
            return Math.max(x, Math.max(y, z));
        }
    }

    /**
     * The Shape's sides: max − min + 1 for a cuboid, the polygon's X and Z extents plus its height for a prism, and 2 ×
     * radius + 1 for a sphere.
     */
    default Sides sides() {
        return switch (this) {
            case Sphere sphere -> {
                int diameter = 2 * sphere.radius() + 1;
                yield new Sides(diameter, diameter, diameter);
            }
            case Cuboid cuboid ->
                new Sides(
                        cuboid.maxX() - cuboid.minX() + 1,
                        cuboid.maxY() - cuboid.minY() + 1,
                        cuboid.maxZ() - cuboid.minZ() + 1);
            case Prism prism -> new Sides(extent(prism.xs()), prism.maxY() - prism.minY() + 1, extent(prism.zs()));
        };
    }

    private static int extent(List<Integer> coordinates) {
        int min = coordinates.stream().mapToInt(Integer::intValue).min().orElse(0);
        int max = coordinates.stream().mapToInt(Integer::intValue).max().orElse(0);
        return max - min + 1;
    }

    record Sphere(int centerX, int centerY, int centerZ, int radius) implements AreaGeometry {}

    record Cuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) implements AreaGeometry {}

    /** A polygon of {@code xs} and {@code zs} corners, extruded between two heights. */
    record Prism(List<Integer> xs, List<Integer> zs, int minY, int maxY) implements AreaGeometry {}
}
