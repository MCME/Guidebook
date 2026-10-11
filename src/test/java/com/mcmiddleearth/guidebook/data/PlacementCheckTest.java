package com.mcmiddleearth.guidebook.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PlacementCheckTest {

    private static final AreaGeometry CUBOID = new AreaGeometry.Cuboid(0, 60, 0, 20, 80, 20);

    // Standing inside the cuboid, so only the world can be wrong
    private static PlacementCheck.StaffPosition standingIn(String world) {
        return new PlacementCheck.StaffPosition(world, 10, 70, 10);
    }

    @Test
    void aSelectionInAnotherWorldIsBlocking() {
        List<PlacementFinding> findings = PlacementCheck.check(
                new PlacementCheck.Proposal("moria", CUBOID), standingIn("middle_earth"), List.of());

        assertEquals(List.of(new PlacementFinding.AnotherWorld("moria", "middle_earth")), findings);
        assertEquals(PlacementFinding.Severity.BLOCKING, findings.getFirst().severity());
    }

    @Test
    void aSelectionInTheStaffMembersWorldHasNoFindings() {
        List<PlacementFinding> findings = PlacementCheck.check(
                new PlacementCheck.Proposal("middle_earth", CUBOID), standingIn("middle_earth"), List.of());

        assertEquals(List.of(), findings);
    }

    // Sphere around me is centred on the staff member, in their world
    @Test
    void aSphereAroundTheStaffMemberIsNeverInAnotherWorld() {
        PlacementCheck.StaffPosition staff = new PlacementCheck.StaffPosition("middle_earth", -40, 72, 315);

        List<PlacementFinding> findings =
                PlacementCheck.check(PlacementCheck.Proposal.sphereAround(staff, 20), staff, List.of());

        assertEquals(List.of(), findings);
    }

    private static final PlacementCheck.StaffPosition AT_ORIGIN =
            new PlacementCheck.StaffPosition("middle_earth", 0, 64, 0);

    private static List<PlacementFinding> checkHere(AreaGeometry geometry, PlacementCheck.StaffPosition staff) {
        return PlacementCheck.check(new PlacementCheck.Proposal("middle_earth", geometry), staff, List.of());
    }

    // A 10 × 10 square around the origin, so AT_ORIGIN stands inside it at any height in range
    private static AreaGeometry.Prism square(int minY, int maxY) {
        return new AreaGeometry.Prism(List.of(-5, 5, 5, -5), List.of(-5, -5, 5, 5), minY, maxY);
    }

    @Test
    void aPrismOneBlockTallIsFlat() {
        assertTrue(checkHere(square(64, 64), AT_ORIGIN).contains(new PlacementFinding.FlatPolygon()));
    }

    @Test
    void aPrismTwoBlocksTallIsNotFlat() {
        assertFalse(checkHere(square(64, 65), AT_ORIGIN).contains(new PlacementFinding.FlatPolygon()));
    }

    @Test
    void aFlatPolygonIsAWarning() {
        assertEquals(PlacementFinding.Severity.WARNING, new PlacementFinding.FlatPolygon().severity());
    }

    // Only the size findings, so a test needn't stand inside the Shape
    private static List<PlacementFinding> sizeFindings(AreaGeometry geometry) {
        return checkHere(geometry, AT_ORIGIN).stream()
                .filter(finding ->
                        finding instanceof PlacementFinding.TooSmall || finding instanceof PlacementFinding.TooLarge)
                .toList();
    }

    private static AreaGeometry.Prism prism(int xExtent, int zExtent, int minY, int maxY) {
        int maxX = xExtent - 1;
        int maxZ = zExtent - 1;
        return new AreaGeometry.Prism(List.of(0, maxX, maxX, 0), List.of(0, 0, maxZ, maxZ), minY, maxY);
    }

    @Test
    void aCuboidTwoBlocksWideIsTooSmall() {
        assertEquals(
                List.of(new PlacementFinding.TooSmall(new AreaGeometry.Sides(2, 21, 30))),
                sizeFindings(new AreaGeometry.Cuboid(0, 60, 0, 1, 80, 29)));
    }

    @Test
    void aCuboidThreeBlocksWideIsBigEnough() {
        assertEquals(List.of(), sizeFindings(new AreaGeometry.Cuboid(0, 60, 0, 2, 62, 2)));
    }

    @Test
    void aCuboidAThousandBlocksAcrossIsSmallEnough() {
        assertEquals(List.of(), sizeFindings(new AreaGeometry.Cuboid(-500, 0, -500, 499, 999, 499)));
    }

    @Test
    void aCuboidAThousandAndOneBlocksDeepIsTooLarge() {
        assertEquals(
                List.of(new PlacementFinding.TooLarge(new AreaGeometry.Sides(10, 10, 1001))),
                sizeFindings(new AreaGeometry.Cuboid(0, 60, 0, 9, 69, 1000)));
    }

    @Test
    void aPrismTwoBlocksWideIsTooSmall() {
        assertEquals(
                List.of(new PlacementFinding.TooSmall(new AreaGeometry.Sides(2, 11, 40))),
                sizeFindings(prism(2, 40, 60, 70)));
    }

    @Test
    void aPrismThreeBlocksWideIsBigEnough() {
        assertEquals(List.of(), sizeFindings(prism(3, 3, 60, 62)));
    }

    @Test
    void aPrismTwoBlocksTallIsTooSmall() {
        assertEquals(
                List.of(new PlacementFinding.TooSmall(new AreaGeometry.Sides(11, 2, 11))),
                sizeFindings(square(64, 65)));
    }

    @Test
    void aFlatPrismIsAlsoTooSmall() {
        assertEquals(
                List.of(new PlacementFinding.TooSmall(new AreaGeometry.Sides(11, 1, 11))),
                sizeFindings(square(64, 64)));
    }

    @Test
    void aPrismAThousandBlocksAcrossIsSmallEnough() {
        assertEquals(List.of(), sizeFindings(prism(1000, 1000, 0, 999)));
    }

    @Test
    void aPrismAThousandAndOneBlocksTallIsTooLarge() {
        assertEquals(
                List.of(new PlacementFinding.TooLarge(new AreaGeometry.Sides(20, 1001, 20))),
                sizeFindings(prism(20, 20, -64, 936)));
    }

    // A sphere's sides are 2 × radius + 1, always odd, so its boundaries are 1 and 3, 999 and 1001
    @Test
    void aSphereOfRadiusZeroIsTooSmall() {
        assertEquals(
                List.of(new PlacementFinding.TooSmall(new AreaGeometry.Sides(1, 1, 1))),
                sizeFindings(new AreaGeometry.Sphere(0, 64, 0, 0)));
    }

    @Test
    void aSphereOfRadiusOneIsBigEnough() {
        assertEquals(List.of(), sizeFindings(new AreaGeometry.Sphere(0, 64, 0, 1)));
    }

    @Test
    void aSphereOfRadius499IsSmallEnough() {
        assertEquals(List.of(), sizeFindings(new AreaGeometry.Sphere(0, 64, 0, 499)));
    }

    @Test
    void aSphereOfRadius500IsTooLarge() {
        assertEquals(
                List.of(new PlacementFinding.TooLarge(new AreaGeometry.Sides(1001, 1001, 1001))),
                sizeFindings(new AreaGeometry.Sphere(0, 64, 0, 500)));
    }

    @Test
    void sizeFindingsAreWarnings() {
        AreaGeometry.Sides sides = new AreaGeometry.Sides(2, 2, 2);
        assertEquals(PlacementFinding.Severity.WARNING, new PlacementFinding.TooSmall(sides).severity());
        assertEquals(PlacementFinding.Severity.WARNING, new PlacementFinding.TooLarge(sides).severity());
    }
}
