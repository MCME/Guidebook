package com.mcmiddleearth.guidebook.data;

/**
 * Something the {@link PlacementCheck} found about a proposed placement, carrying what the Placement dialog needs to
 * word it. Each kind is its own record, so a dialog switching over them must word every kind.
 */
public sealed interface PlacementFinding {

    /** How much a finding matters. A blocking finding stops the placement; the others only tell the staff member. */
    enum Severity {
        BLOCKING,
        WARNING,
        INFORMATION
    }

    Severity severity();

    /** The selection is in a different world from the one the staff member is standing in. */
    record AnotherWorld(String selectionWorld, String staffWorld) implements PlacementFinding {

        @Override
        public Severity severity() {
            return Severity.BLOCKING;
        }
    }

    /** A prism whose minimum and maximum Y are equal, so it is one block tall. */
    record FlatPolygon() implements PlacementFinding {

        @Override
        public Severity severity() {
            return Severity.WARNING;
        }
    }

    /** A side of the Shape is under {@link PlacementCheck#MIN_SIDE} blocks. */
    record TooSmall(AreaGeometry.Sides sides) implements PlacementFinding {

        @Override
        public Severity severity() {
            return Severity.WARNING;
        }
    }

    /** A side of the Shape is over {@link PlacementCheck#MAX_SIDE} blocks. */
    record TooLarge(AreaGeometry.Sides sides) implements PlacementFinding {

        @Override
        public Severity severity() {
            return Severity.WARNING;
        }
    }

    /**
     * The staff member is standing outside the Shape, so the Warp point will be too.
     *
     * @param distance the distance in blocks to the Shape's nearest block, at least 1
     */
    record Outside(int distance) implements PlacementFinding {

        @Override
        public Severity severity() {
            return Severity.INFORMATION;
        }
    }
}
