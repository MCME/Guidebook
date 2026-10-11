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
            case Prism prism ->
                new Sides(
                        prism.maxX() - prism.minX() + 1,
                        prism.maxY() - prism.minY() + 1,
                        prism.maxZ() - prism.minZ() + 1);
        };
    }

    /** The Shape and its block-inclusive dimensions, such as "cuboid, 21 × 10 × 30 blocks (x × y × z)". */
    default String describe() {
        Sides sides = sides();
        String dimensions = sides.x() + " × " + sides.y() + " × " + sides.z() + " blocks (x × y × z)";
        return switch (this) {
            case Sphere sphere -> "sphere, radius " + sphere.radius();
            case Cuboid cuboid -> "cuboid, " + dimensions;
            case Prism prism -> "prism with " + prism.xs().size() + " corners, " + dimensions;
        };
    }

    record Sphere(int centerX, int centerY, int centerZ, int radius) implements AreaGeometry {}

    record Cuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) implements AreaGeometry {}

    /** A polygon of {@code xs} and {@code zs} corners, extruded between two heights. */
    record Prism(List<Integer> xs, List<Integer> zs, int minY, int maxY) implements AreaGeometry {

        /** The lowest corner X, or 0 for a polygon with no corners. */
        public int minX() {
            return xs.stream().mapToInt(Integer::intValue).min().orElse(0);
        }

        /** The highest corner X, or 0 for a polygon with no corners. */
        public int maxX() {
            return xs.stream().mapToInt(Integer::intValue).max().orElse(0);
        }

        /** The lowest corner Z, or 0 for a polygon with no corners. */
        public int minZ() {
            return zs.stream().mapToInt(Integer::intValue).min().orElse(0);
        }

        /** The highest corner Z, or 0 for a polygon with no corners. */
        public int maxZ() {
            return zs.stream().mapToInt(Integer::intValue).max().orElse(0);
        }
    }
}
