package com.mcmiddleearth.guidebook.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The Overlap finding of the {@link PlacementCheck}: which existing Areas share at least one block with a placement.
 */
class PlacementCheckOverlapTest {

    private static final String WORLD = "middle_earth";

    private static InfoArea area(String name, AreaGeometry geometry) {
        return TestAreas.area(name, WORLD, geometry);
    }

    private static AreaGeometry cuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return new AreaGeometry.Cuboid(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static AreaGeometry sphere(int x, int y, int z, int radius) {
        return new AreaGeometry.Sphere(x, y, z, radius);
    }

    /** The names the check reports overlapping when {@code proposed} is placed among {@code areas}, or none. */
    private static List<String> overlapping(AreaGeometry proposed, InfoArea... areas) {
        // The staff member stands in the proposal's world, so Another world never applies
        PlacementCheck.StaffPosition staff = new PlacementCheck.StaffPosition(WORLD, 0, 64, 0);
        return PlacementCheck.check(new PlacementCheck.Proposal(WORLD, proposed), staff, List.of(areas)).stream()
                .filter(PlacementFinding.Overlap.class::isInstance)
                .map(PlacementFinding.Overlap.class::cast)
                .findFirst()
                .map(PlacementFinding.Overlap::areaNames)
                .orElse(List.of());
    }

    // Cuboid–cuboid

    @Test
    void cuboidsThatClearlyOverlapAreReported() {
        assertEquals(
                List.of("bree"),
                overlapping(cuboid(0, 60, 0, 20, 80, 20), area("bree", cuboid(10, 70, 10, 30, 90, 30))));
    }

    @Test
    void cuboidsSharingExactlyOneCornerBlockOverlap() {
        assertEquals(
                List.of("bree"),
                overlapping(cuboid(0, 60, 0, 20, 80, 20), area("bree", cuboid(20, 80, 20, 30, 90, 30))));
    }

    @Test
    void cuboidsWhoseFacesTouchDoNotOverlap() {
        assertEquals(List.of(), overlapping(cuboid(0, 60, 0, 20, 80, 20), area("bree", cuboid(21, 60, 0, 40, 80, 20))));
    }

    @Test
    void cuboidsFarApartDoNotOverlap() {
        assertEquals(
                List.of(), overlapping(cuboid(0, 60, 0, 20, 80, 20), area("bree", cuboid(500, 60, 500, 520, 80, 520))));
    }

    // Sphere–sphere

    @Test
    void spheresThatClearlyOverlapAreReported() {
        assertEquals(List.of("weathertop"), overlapping(sphere(0, 64, 0, 5), area("weathertop", sphere(6, 64, 0, 5))));
    }

    // (5, 64, 0) is 5 from both centres
    @Test
    void spheresSharingExactlyOneBlockOverlap() {
        assertEquals(List.of("weathertop"), overlapping(sphere(0, 64, 0, 5), area("weathertop", sphere(10, 64, 0, 5))));
    }

    // One ends at x = 5 and the other starts at x = 6
    @Test
    void spheresWhoseFacesTouchDoNotOverlap() {
        assertEquals(List.of(), overlapping(sphere(0, 64, 0, 5), area("weathertop", sphere(11, 64, 0, 5))));
    }

    // The centres are √3 apart, less than the radii's sum of 2, but no block is within 1 of both
    @Test
    void spheresWhoseSurfacesCrossBetweenBlocksDoNotOverlap() {
        assertEquals(List.of(), overlapping(sphere(0, 64, 0, 1), area("weathertop", sphere(1, 65, 1, 1))));
    }

    @Test
    void spheresFarApartDoNotOverlap() {
        assertEquals(List.of(), overlapping(sphere(0, 64, 0, 5), area("weathertop", sphere(300, 64, 0, 5))));
    }

    // Sphere–cuboid

    @Test
    void aSphereAndCuboidThatClearlyOverlapAreReported() {
        assertEquals(List.of("bree"), overlapping(sphere(0, 64, 0, 5), area("bree", cuboid(3, 60, -2, 10, 70, 2))));
    }

    // Of the cuboid's blocks only (5, 64, 0) is within 5 of the centre
    @Test
    void aSphereAndCuboidSharingExactlyOneBlockOverlap() {
        assertEquals(List.of("bree"), overlapping(sphere(0, 64, 0, 5), area("bree", cuboid(5, 64, 0, 10, 70, 5))));
    }

    @Test
    void aSphereAndCuboidWhoseFacesTouchDoNotOverlap() {
        assertEquals(List.of(), overlapping(sphere(0, 64, 0, 5), area("bree", cuboid(6, 60, -5, 10, 70, 5))));
    }

    @Test
    void aSphereAndCuboidFarApartDoNotOverlap() {
        assertEquals(List.of(), overlapping(sphere(0, 64, 0, 5), area("bree", cuboid(100, 60, 100, 110, 70, 110))));
    }

    // A square prism from (0, 0) to (20, 20), 60 to 80 high; its edges are inside
    private static final AreaGeometry SQUARE = prism(60, 80, 0, 0, 20, 0, 20, 20, 0, 20);

    // Sphere–prism

    @Test
    void aSphereAndPrismThatClearlyOverlapAreReported() {
        assertEquals(List.of("chetwood"), overlapping(sphere(20, 70, 10, 5), area("chetwood", SQUARE)));
    }

    // Of the sphere's blocks only (20, 70, 10) is on the prism's edge
    @Test
    void aSphereAndPrismSharingExactlyOneBlockOverlap() {
        assertEquals(List.of("chetwood"), overlapping(sphere(25, 70, 10, 5), area("chetwood", SQUARE)));
    }

    @Test
    void aSphereAndPrismWhoseFacesTouchDoNotOverlap() {
        assertEquals(List.of(), overlapping(sphere(26, 70, 10, 5), area("chetwood", SQUARE)));
    }

    @Test
    void aSphereAndPrismFarApartDoNotOverlap() {
        assertEquals(List.of(), overlapping(sphere(200, 70, 10, 5), area("chetwood", SQUARE)));
    }

    // The triangle's long edge runs from (20, 0) to (0, 20); the sphere is inside its bounding box but beyond that edge
    @Test
    void aSphereNearAPrismsCornerButOutsideItsPolygonDoesNotOverlap() {
        AreaGeometry triangle = prism(60, 80, 0, 0, 20, 0, 0, 20);

        assertEquals(List.of(), overlapping(sphere(15, 70, 15, 3), area("chetwood", triangle)));
    }

    // Cuboid–prism

    @Test
    void aCuboidAndPrismThatClearlyOverlapAreReported() {
        assertEquals(List.of("chetwood"), overlapping(cuboid(10, 70, 10, 30, 90, 30), area("chetwood", SQUARE)));
    }

    @Test
    void aCuboidAndPrismSharingExactlyOneCornerBlockOverlap() {
        assertEquals(List.of("chetwood"), overlapping(cuboid(20, 80, 20, 30, 90, 30), area("chetwood", SQUARE)));
    }

    @Test
    void aCuboidAndPrismWhoseFacesTouchDoNotOverlap() {
        assertEquals(List.of(), overlapping(cuboid(21, 60, 0, 40, 80, 20), area("chetwood", SQUARE)));
    }

    @Test
    void aCuboidAndPrismFarApartDoNotOverlap() {
        assertEquals(List.of(), overlapping(cuboid(500, 60, 500, 520, 80, 520), area("chetwood", SQUARE)));
    }

    // Prism–prism

    @Test
    void prismsThatClearlyOverlapAreReported() {
        AreaGeometry triangle = prism(70, 90, 10, 10, 40, 10, 10, 40);

        assertEquals(List.of("chetwood"), overlapping(triangle, area("chetwood", SQUARE)));
    }

    // The triangles meet only at the corner (20, 20)
    @Test
    void prismsSharingExactlyOneCornerBlockOverlap() {
        AreaGeometry first = prism(60, 80, 0, 0, 20, 0, 20, 20);
        AreaGeometry second = prism(60, 80, 20, 20, 40, 20, 40, 40);

        assertEquals(List.of("chetwood"), overlapping(first, area("chetwood", second)));
    }

    @Test
    void prismsWhoseSidesTouchDoNotOverlap() {
        AreaGeometry first = prism(60, 80, 0, 0, 20, 0, 20, 20);
        AreaGeometry second = prism(60, 80, 21, 20, 41, 20, 41, 40);

        assertEquals(List.of(), overlapping(first, area("chetwood", second)));
    }

    @Test
    void prismsStackedOnTopOfEachOtherDoNotOverlap() {
        AreaGeometry above = prism(81, 90, 0, 0, 20, 0, 20, 20, 0, 20);

        assertEquals(List.of(), overlapping(above, area("chetwood", SQUARE)));
    }

    @Test
    void prismsFarApartDoNotOverlap() {
        AreaGeometry far = prism(60, 80, 500, 500, 520, 500, 520, 520);

        assertEquals(List.of(), overlapping(far, area("chetwood", SQUARE)));
    }

    // A U open to the north, its arms 10 to 20 wide, around a cuboid and a prism standing in its gap
    @Test
    void aConcavePrismWrappedAroundAnotherAreaDoesNotOverlapIt() {
        AreaGeometry u = prism(60, 80, 0, 0, 30, 0, 30, 30, 20, 30, 20, 10, 10, 10, 10, 30, 0, 30);

        assertEquals(
                List.of(),
                overlapping(
                        u,
                        area("inn", cuboid(11, 60, 11, 19, 80, 40)),
                        area("yard", prism(60, 80, 11, 11, 19, 11, 19, 40, 11, 40))));
    }

    private static AreaGeometry prism(int minY, int maxY, int... corners) {
        List<Integer> xs = new ArrayList<>();
        List<Integer> zs = new ArrayList<>();
        for (int i = 0; i < corners.length; i += 2) {
            xs.add(corners[i]);
            zs.add(corners[i + 1]);
        }
        return new AreaGeometry.Prism(xs, zs, minY, maxY);
    }

    // One rule: a single block overlaps a prism exactly when a player standing in it is inside the prism's Area

    @Test
    void aBlockOverlapsAPrismExactlyWhereThePrismsAreaHasAPlayerInside() {
        List<AreaGeometry> prisms = List.of(
                // A sloped triangle
                prism(60, 62, 0, 0, 20, 7, 5, 18),
                // A concave arrowhead, notched at (1, 3)
                prism(60, 62, 0, 0, 4, 14, 7, 0, 1, 3));
        for (AreaGeometry prism : prisms) {
            InfoArea bree = area("bree", prism);
            for (int x = -2; x <= 22; x++) {
                for (int z = -2; z <= 20; z++) {
                    for (int y = 59; y <= 63; y++) {
                        boolean inside = bree.isInside(new WorldPoint(WORLD, x + 0.5, y, z + 0.5));
                        assertEquals(
                                inside ? List.of("bree") : List.of(),
                                overlapping(cuboid(x, y, z, x, y, z), bree),
                                "block " + x + " " + y + " " + z + " of " + prism);
                    }
                }
            }
        }
    }

    // Worlds, moves and order

    @Test
    void anAreaInAnotherWorldNeverOverlaps() {
        InfoArea elsewhere = TestAreas.area("moria-gate", "moria", cuboid(0, 60, 0, 20, 80, 20));

        assertEquals(List.of(), overlapping(cuboid(0, 60, 0, 20, 80, 20), elsewhere));
    }

    @Test
    void overlappingAreasAreNamedInSortedOrder() {
        AreaGeometry everywhere = cuboid(-100, 0, -100, 100, 100, 100);

        assertEquals(
                List.of("archet", "bree", "combe", "staddle"),
                overlapping(
                        cuboid(0, 60, 0, 20, 80, 20),
                        area("staddle", everywhere),
                        area("bree", everywhere),
                        area("combe", everywhere),
                        area("archet", everywhere)));
    }
}
