package com.mcmiddleearth.guidebook.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Judges a proposed placement of an Area before anything is written, for the Placement dialog. It knows nothing about
 * Paper, WorldEdit or Dialogs, so it works without a running server.
 */
public final class PlacementCheck {

    /** A side under this many blocks is too small. */
    public static final int MIN_SIDE = 3;

    /** A side over this many blocks is too large. */
    public static final int MAX_SIDE = 1000;

    private PlacementCheck() {}

    /** A Shape proposed for an Area, in the world it would be in. */
    public record Proposal(String world, AreaGeometry geometry) {

        /** Sphere around me: a sphere centred on the staff member, in their world. */
        public static Proposal sphereAround(StaffPosition staff, int radius) {
            return new Proposal(staff.world(), new AreaGeometry.Sphere(staff.x(), staff.y(), staff.z(), radius));
        }
    }

    /** The block the staff member placing the Area stands on, which becomes its Warp point. */
    public record StaffPosition(String world, int x, int y, int z) {}

    /** Checks a placement for a new Area. */
    public static List<PlacementFinding> check(
            Proposal proposal, StaffPosition staff, Collection<? extends MarkableArea> areas) {
        return check(proposal, staff, areas, Optional.empty());
    }

    /**
     * Checks a placement.
     *
     * @param areas the existing Areas
     * @param movedAreaName the Area being moved, if this is a move, which is never compared with itself
     * @return the findings, in the order the dialog shows them
     */
    public static List<PlacementFinding> check(
            Proposal proposal,
            StaffPosition staff,
            Collection<? extends MarkableArea> areas,
            Optional<String> movedAreaName) {
        List<PlacementFinding> findings = new ArrayList<>();
        if (!proposal.world().equals(staff.world())) {
            findings.add(new PlacementFinding.AnotherWorld(proposal.world(), staff.world()));
        }
        findings.addAll(shapeFindings(proposal.geometry()));
        outside(proposal, staff).ifPresent(findings::add);
        AreaOverlap.overlapping(proposal, areas, movedAreaName).ifPresent(findings::add);
        return findings;
    }

    /**
     * How far Sphere around me reaches before it overlaps another Area.
     *
     * @param largestClearRadius the largest radius, from the smallest offered up to the largest, that overlaps no other
     *     Area, or 0 when even the smallest does
     * @param overlappedNext the Areas the next radius up overlaps, sorted, or none when every radius offered is clear
     */
    public record SphereClearance(int largestClearRadius, List<String> overlappedNext) {}

    /**
     * Works out the {@link SphereClearance} of a sphere centred on the staff member, for radii {@code minRadius} to
     * {@code maxRadius}. A larger sphere holds every block of a smaller one, so once a radius overlaps, every larger one
     * does too.
     *
     * @param movedAreaName the Area being moved, if this is a move, which is never compared with itself
     */
    public static SphereClearance sphereClearance(
            StaffPosition staff,
            Collection<? extends MarkableArea> areas,
            Optional<String> movedAreaName,
            int minRadius,
            int maxRadius) {
        if (overlapped(staff, areas, movedAreaName, maxRadius).isEmpty()) {
            return new SphereClearance(maxRadius, List.of());
        }
        // Binary search for the smallest overlapping radius, which is somewhere in low to high
        int low = minRadius;
        int high = maxRadius;
        while (low < high) {
            int middle = (low + high) / 2;
            if (overlapped(staff, areas, movedAreaName, middle).isEmpty()) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        int largestClear = low == minRadius ? 0 : low - 1;
        return new SphereClearance(largestClear, overlapped(staff, areas, movedAreaName, low));
    }

    private static List<String> overlapped(
            StaffPosition staff, Collection<? extends MarkableArea> areas, Optional<String> movedAreaName, int radius) {
        return AreaOverlap.overlapping(Proposal.sphereAround(staff, radius), areas, movedAreaName)
                .map(PlacementFinding.Overlap::areaNames)
                .orElse(List.of());
    }

    /** What is wrong with the Shape on its own: a flat polygon, or a side too small or too large. */
    private static List<PlacementFinding> shapeFindings(AreaGeometry geometry) {
        List<PlacementFinding> findings = new ArrayList<>();
        if (geometry instanceof AreaGeometry.Prism prism && prism.minY() == prism.maxY()) {
            findings.add(new PlacementFinding.FlatPolygon());
        }
        AreaGeometry.Sides sides = geometry.sides();
        if (sides.smallest() < MIN_SIDE) {
            findings.add(new PlacementFinding.TooSmall(sides));
        }
        if (sides.largest() > MAX_SIDE) {
            findings.add(new PlacementFinding.TooLarge(sides));
        }
        return findings;
    }

    /**
     * Whether the staff member, whose position becomes the Warp point, stands outside the Shape. A sphere is centred on
     * them, and in another world there is no distance to give.
     */
    private static Optional<PlacementFinding> outside(Proposal proposal, StaffPosition staff) {
        if (!proposal.world().equals(staff.world())) {
            return Optional.empty();
        }
        return switch (proposal.geometry()) {
            case AreaGeometry.Sphere sphere -> Optional.empty();
            case AreaGeometry.Cuboid cuboid -> {
                int dx = gap(staff.x(), cuboid.minX(), cuboid.maxX());
                int dy = gap(staff.y(), cuboid.minY(), cuboid.maxY());
                int dz = gap(staff.z(), cuboid.minZ(), cuboid.maxZ());
                yield dx == 0 && dy == 0 && dz == 0
                        ? Optional.empty()
                        : Optional.of(outsideBy(Math.sqrt((double) dx * dx + (double) dy * dy + (double) dz * dz)));
            }
            case AreaGeometry.Prism prism -> {
                PrismInside inside = new PrismInside(prism);
                if (inside.contains(staff.x(), staff.y(), staff.z())) {
                    yield Optional.empty();
                }
                double horizontal = inside.horizontalDistance(staff.x(), staff.z());
                int dy = gap(staff.y(), prism.minY(), prism.maxY());
                yield Optional.of(outsideBy(Math.hypot(horizontal, dy)));
            }
        };
    }

    /** How far {@code value} lies outside {@code min} to {@code max}, or 0 within it. */
    private static int gap(int value, int min, int max) {
        return Math.max(0, Math.max(min - value, value - max));
    }

    // Standing outside is always at least a block away, even where rounding says less
    private static PlacementFinding.Outside outsideBy(double distance) {
        return new PlacementFinding.Outside((int) Math.max(1, Math.round(distance)));
    }
}
