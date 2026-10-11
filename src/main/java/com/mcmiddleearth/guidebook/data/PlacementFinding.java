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
}
