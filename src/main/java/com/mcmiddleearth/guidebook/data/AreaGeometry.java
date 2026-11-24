package com.mcmiddleearth.guidebook.data;

import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import java.util.ArrayList;
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

    /** Which kind of Shape this is. */
    default Shape shape() {
        return switch (this) {
            case Sphere ignored -> Shape.SPHERE;
            case Cuboid ignored -> Shape.CUBOID;
            case Prism ignored -> Shape.PRISM;
        };
    }

    /**
     * The Shape, its block-inclusive footprint and the heights it spans, such as "cuboid, 21 × 30 blocks, from y 60 to
     * 69". The height range is where selection mistakes usually are: one block tall, or at the wrong level.
     */
    default String describe() {
        Sides sides = sides();
        String footprint = sides.x() + " × " + sides.z() + " blocks";
        String shape = shape().displayName();
        return switch (this) {
            case Sphere sphere ->
                shape + ", radius " + sphere.radius()
                        + heights(sphere.centerY() - sphere.radius(), sphere.centerY() + sphere.radius());
            case Cuboid cuboid -> shape + ", " + footprint + heights(cuboid.minY(), cuboid.maxY());
            case Prism prism ->
                shape + " with " + prism.xs().size() + " corners, " + footprint + heights(prism.minY(), prism.maxY());
        };
    }

    /**
     * Whether a point is inside the Shape. A sphere measures from the point's exact position: it's inside when its
     * distance from the centre is at most the radius. A cuboid or prism uses the block the point is in, edges
     * included, and a prism asks {@link Prism#containsBlock(int, int, int)}.
     */
    default boolean contains(double x, double y, double z) {
        return switch (this) {
            case Sphere sphere -> sphere.distanceFromCentre(x, y, z) <= sphere.radius();
            case Cuboid c -> blockBetween(x, y, z, c.minX(), c.minY(), c.minZ(), c.maxX(), c.maxY(), c.maxZ(), 0);
            case Prism prism -> prism.containsBlock((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        };
    }

    /**
     * Whether a point is within {@code distance} blocks of the Shape. A sphere measures from the point's exact
     * position, up to its radius plus the distance. A cuboid uses the block the point is in, widened by the distance
     * on every side; a prism does the same with its bounding box, not its polygon.
     */
    default boolean near(double x, double y, double z, int distance) {
        return switch (this) {
            case Sphere sphere -> sphere.distanceFromCentre(x, y, z) <= sphere.radius() + distance;
            case Cuboid c ->
                blockBetween(x, y, z, c.minX(), c.minY(), c.minZ(), c.maxX(), c.maxY(), c.maxZ(), distance);
            case Prism p -> blockBetween(x, y, z, p.minX(), p.minY(), p.minZ(), p.maxX(), p.maxY(), p.maxZ(), distance);
        };
    }

    /**
     * Whether the block a point is in lies between two corners widened by {@code distance} on every side, edges
     * included.
     */
    private static boolean blockBetween(
            double x, double y, double z, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int distance) {
        return between(x, minX, maxX, distance) && between(y, minY, maxY, distance) && between(z, minZ, maxZ, distance);
    }

    private static boolean between(double coordinate, int min, int max, int distance) {
        double block = Math.floor(coordinate);
        return block >= min - distance && block <= max + distance;
    }

    private static String heights(int minY, int maxY) {
        return ", from y " + minY + " to " + maxY;
    }

    record Sphere(int centerX, int centerY, int centerZ, int radius) implements AreaGeometry {

        private double distanceFromCentre(double x, double y, double z) {
            return Math.sqrt(square(x - centerX) + square(y - centerY) + square(z - centerZ));
        }

        private static double square(double d) {
            return d * d;
        }
    }

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

        /**
         * Whether a block is inside, by WorldEdit's own rule for a polygon selection, so the prism holds exactly the
         * blocks staff selected.
         */
        boolean containsBlock(int x, int y, int z) {
            return containsBlock(corners(), minY, maxY, x, y, z);
        }

        /**
         * {@link #containsBlock(int, int, int)} for a caller that asks about many blocks and keeps the {@link
         * #corners()}.
         */
        static boolean containsBlock(List<BlockVector2> corners, int minY, int maxY, int x, int y, int z) {
            return Polygonal2DRegion.contains(corners, minY, maxY, BlockVector3.at(x, y, z));
        }

        /** The polygon's corners as WorldEdit takes them. */
        List<BlockVector2> corners() {
            List<BlockVector2> corners = new ArrayList<>(xs.size());
            for (int i = 0; i < xs.size(); i++) {
                corners.add(BlockVector2.at(xs.get(i), zs.get(i)));
            }
            return corners;
        }
    }
}
