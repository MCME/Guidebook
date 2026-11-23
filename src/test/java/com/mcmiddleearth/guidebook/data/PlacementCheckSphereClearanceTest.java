package com.mcmiddleearth.guidebook.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** How far Sphere around me can reach before it overlaps another Area, shown before the staff member picks a radius. */
class PlacementCheckSphereClearanceTest {

    private static final String WORLD = "middle_earth";
    private static final PlacementCheck.StaffPosition STAFF = new PlacementCheck.StaffPosition(WORLD, 0, 64, 0);

    // A one-block cuboid on the x axis, distance blocks east of the staff member
    private static InfoArea blockEastBy(String name, int distance) {
        return TestAreas.area(name, WORLD, new AreaGeometry.Cuboid(distance, 64, 0, distance, 64, 0));
    }

    private static PlacementCheck.SphereClearance clearance(List<InfoArea> areas) {
        return PlacementCheck.sphereClearance(STAFF, areas, 1, 200);
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
        InfoArea west = TestAreas.area("west", WORLD, new AreaGeometry.Cuboid(-30, 64, 0, -30, 64, 0));
        assertEquals(
                new PlacementCheck.SphereClearance(29, List.of("east", "west")),
                clearance(List.of(west, blockEastBy("east", 30))));
    }

    @Test
    void standingInAnotherAreaLeavesNoClearRadius() {
        InfoArea around = TestAreas.area("town", WORLD, new AreaGeometry.Cuboid(-5, 60, -5, 5, 70, 5));
        assertEquals(new PlacementCheck.SphereClearance(0, List.of("town")), clearance(List.of(around)));
    }

    @Test
    void anAreaJustBeyondTheLargestRadiusLeavesItClear() {
        assertEquals(new PlacementCheck.SphereClearance(200, List.of()), clearance(List.of(blockEastBy("far", 201))));
    }
}
