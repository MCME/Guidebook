package com.mcmiddleearth.guidebook.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** How far Sphere around me can reach before it overlaps another Area, shown before the staff member picks a radius. */
class PlacementCheckSphereClearanceTest {

    private static final String WORLD = "middle_earth";
    private static final PlacementCheck.StaffPosition STAFF = new PlacementCheck.StaffPosition(WORLD, 0, 64, 0);

    record TestArea(String getName, String getWorldName, AreaGeometry getGeometry) implements MarkableArea {

        @Override
        public String getTitle() {
            return getName;
        }

        @Override
        public String getSubtitle() {
            return "";
        }

        @Override
        public String getDescription() {
            return "";
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    }

    // A one-block cuboid on the x axis, d blocks east of the staff member
    private static TestArea blockEastBy(String name, int d) {
        return new TestArea(name, WORLD, new AreaGeometry.Cuboid(d, 64, 0, d, 64, 0));
    }

    private static PlacementCheck.SphereClearance clearance(List<TestArea> areas) {
        return clearance(areas, Optional.empty());
    }

    private static PlacementCheck.SphereClearance clearance(List<TestArea> areas, Optional<String> moved) {
        return PlacementCheck.sphereClearance(STAFF, areas, moved, 1, 200);
    }

    @Test
    void withNoAreasNearbyEveryRadiusIsClear() {
        assertEquals(new PlacementCheck.SphereClearance(200, List.of()), clearance(List.of()));
    }

    @Test
    void theLargestClearRadiusStopsJustShortOfTheNearestArea() {
        assertEquals(
                new PlacementCheck.SphereClearance(29, List.of("gate")),
                clearance(List.of(blockEastBy("gate", 30), blockEastBy("hall", 50))));
    }

    @Test
    void everyAreaReachedAtTheFirstOverlappingRadiusIsNamedInOrder() {
        TestArea west = new TestArea("west", WORLD, new AreaGeometry.Cuboid(-30, 64, 0, -30, 64, 0));
        assertEquals(
                new PlacementCheck.SphereClearance(29, List.of("east", "west")),
                clearance(List.of(west, blockEastBy("east", 30))));
    }

    @Test
    void standingInAnotherAreaLeavesNoClearRadius() {
        TestArea around = new TestArea("town", WORLD, new AreaGeometry.Cuboid(-5, 60, -5, 5, 70, 5));
        assertEquals(new PlacementCheck.SphereClearance(0, List.of("town")), clearance(List.of(around)));
    }

    @Test
    void anAreaJustBeyondTheLargestRadiusLeavesItClear() {
        assertEquals(new PlacementCheck.SphereClearance(200, List.of()), clearance(List.of(blockEastBy("far", 201))));
    }

    @Test
    void theAreaBeingMovedIsIgnored() {
        assertEquals(
                new PlacementCheck.SphereClearance(200, List.of()),
                clearance(List.of(blockEastBy("gate", 30)), Optional.of("gate")));
    }
}
