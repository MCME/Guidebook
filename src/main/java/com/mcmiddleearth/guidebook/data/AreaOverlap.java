package com.mcmiddleearth.guidebook.data;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Finds the existing Areas a placement overlaps: those in the same world sharing at least one block with it.
 *
 * <p>A block is inside a Shape by the same rule that decides who gets a Welcome, so the check is exact on blocks. Each
 * Shape is seen as columns of blocks: at each X and Z it fills one unbroken range of Y, or nothing. Two Shapes overlap
 * when, in some column inside both their bounding boxes, their Y ranges meet. Bounding boxes only narrow the search.
 */
final class AreaOverlap {

    private AreaOverlap() {}

    /** The Overlap finding for {@code proposal}, or empty when it overlaps nothing. The moved Area is skipped. */
    static Optional<PlacementFinding> overlapping(
            PlacementCheck.Proposal proposal,
            Collection<? extends MarkableArea> areas,
            Optional<String> movedAreaName) {
        List<String> names = areas.stream()
                .filter(area -> area.getWorldName().equals(proposal.world()))
                .filter(area -> movedAreaName
                        .map(moved -> !moved.equalsIgnoreCase(area.getName()))
                        .orElse(true))
                .filter(area -> overlap(proposal.geometry(), area.getGeometry()))
                .map(MarkableArea::getName)
                .sorted()
                .toList();
        return names.isEmpty() ? Optional.empty() : Optional.of(new PlacementFinding.Overlap(names));
    }

    /** Whether at least one block is inside both Shapes. */
    static boolean overlap(AreaGeometry a, AreaGeometry b) {
        Columns first = columnsOf(a);
        Columns second = columnsOf(b);
        int minX = Math.max(first.minX(), second.minX());
        int maxX = Math.min(first.maxX(), second.maxX());
        int minZ = Math.max(first.minZ(), second.minZ());
        int maxZ = Math.min(first.maxZ(), second.maxZ());
        if (minX > maxX || minZ > maxZ) {
            return false;
        }
        long[] firstRange = new long[2];
        long[] secondRange = new long[2];
        for (int z = minZ; z <= maxZ; z++) {
            first.startRow(z);
            second.startRow(z);
            for (int x = minX; x <= maxX; x++) {
                if (first.range(x, z, firstRange)
                        && second.range(x, z, secondRange)
                        && Math.max(firstRange[0], secondRange[0]) <= Math.min(firstRange[1], secondRange[1])) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Columns columnsOf(AreaGeometry geometry) {
        return switch (geometry) {
            case AreaGeometry.Sphere sphere -> new SphereColumns(sphere);
            case AreaGeometry.Cuboid cuboid -> new CuboidColumns(cuboid);
            case AreaGeometry.Prism prism -> new PrismColumns(prism);
        };
    }

    /** A Shape as columns of blocks, within a bounding box on X and Z. */
    private interface Columns {

        int minX();

        int maxX();

        int minZ();

        int maxZ();

        /** Called before the columns of row {@code z} are asked for, so a Shape can prepare the row once. */
        default void startRow(int z) {}

        /**
         * Puts the lowest and highest Y the Shape fills at {@code x}, {@code z} into {@code range}.
         *
         * @return false when the Shape fills nothing there
         */
        boolean range(int x, int z, long[] range);
    }

    /**
     * A block is inside when its distance from the centre block is at most the radius, as {@code SphericalRegion}
     * measures it. The region measures from the staff member's exact position, which an Area's geometry rounds to its
     * block.
     */
    private record SphereColumns(int centerX, int centerY, int centerZ, int radius) implements Columns {

        SphereColumns(AreaGeometry.Sphere sphere) {
            this(sphere.centerX(), sphere.centerY(), sphere.centerZ(), sphere.radius());
        }

        @Override
        public int minX() {
            return centerX - Math.max(radius, 0);
        }

        @Override
        public int maxX() {
            return centerX + Math.max(radius, 0);
        }

        @Override
        public int minZ() {
            return centerZ - Math.max(radius, 0);
        }

        @Override
        public int maxZ() {
            return centerZ + Math.max(radius, 0);
        }

        @Override
        public boolean range(int x, int z, long[] range) {
            long dx = x - centerX;
            long dz = z - centerZ;
            long left = (long) radius * radius - dx * dx - dz * dz;
            if (radius < 0 || left < 0) {
                return false;
            }
            long height = floorSqrt(left);
            range[0] = centerY - height;
            range[1] = centerY + height;
            return true;
        }

        private static long floorSqrt(long value) {
            long root = (long) Math.sqrt((double) value);
            while (root * root > value) {
                root--;
            }
            while ((root + 1) * (root + 1) <= value) {
                root++;
            }
            return root;
        }
    }

    /** A block is inside when each coordinate is between the corners, both included, as {@code CuboidRegion} has it. */
    private record CuboidColumns(AreaGeometry.Cuboid cuboid) implements Columns {

        @Override
        public int minX() {
            return cuboid.minX();
        }

        @Override
        public int maxX() {
            return cuboid.maxX();
        }

        @Override
        public int minZ() {
            return cuboid.minZ();
        }

        @Override
        public int maxZ() {
            return cuboid.maxZ();
        }

        @Override
        public boolean range(int x, int z, long[] range) {
            range[0] = cuboid.minY();
            range[1] = cuboid.maxY();
            return range[0] <= range[1];
        }
    }

    /** A block is inside by {@link PrismInside}'s copy of {@code PrismoidRegion}'s rule, edges and rounding included. */
    private static final class PrismColumns implements Columns {

        private final PrismInside inside;
        private final int minY;
        private final int maxY;
        private final int minX;
        private final int maxX;
        private final int minZ;
        private final int maxZ;

        PrismColumns(AreaGeometry.Prism prism) {
            inside = new PrismInside(prism);
            minY = prism.minY();
            maxY = prism.maxY();
            minX = prism.minX();
            maxX = prism.maxX();
            minZ = prism.minZ();
            maxZ = prism.maxZ();
        }

        @Override
        public int minX() {
            return minX;
        }

        @Override
        public int maxX() {
            return maxX;
        }

        @Override
        public int minZ() {
            return minZ;
        }

        @Override
        public int maxZ() {
            return maxZ;
        }

        // The rule doesn't depend on Y within the prism's height, so one block decides the whole column
        @Override
        public boolean range(int x, int z, long[] range) {
            if (!inside.contains(x, minY, z)) {
                return false;
            }
            range[0] = minY;
            range[1] = maxY;
            return true;
        }
    }
}
