package com.mcmiddleearth.guidebook.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * When a point is inside a Shape, and when it's near one, with no world. The cases and answers are {@link
 * AreaInsideTest}'s, which pin today's {@link InfoArea} in its own world, so an Area can switch over to these.
 */
class AreaGeometryInsideTest {

    private static final int NEAR = 10;

    // Sphere: radius 5 centred on (100, 64, 200)

    private static final AreaGeometry SPHERE = new AreaGeometry.Sphere(100, 64, 200, 5);

    @Test
    void aPointExactlyARadiusFromASpheresCentreIsInsideIt() {
        assertTrue(SPHERE.contains(103, 64, 204)); // 3-4-5: exactly 5 away
        assertTrue(SPHERE.contains(100, 64, 200));
    }

    @Test
    void aPointAFractionPastASpheresRadiusIsOutsideIt() {
        assertFalse(SPHERE.contains(103, 64, 204.1));
        // Measured from the exact position, not the block: block 105 is 5 blocks away, but the point is 5.4
        assertFalse(SPHERE.contains(105.4, 64, 200));
    }

    @Test
    void aPointTenPastASpheresRadiusIsNearItAndAFractionMoreIsNot() {
        assertTrue(SPHERE.near(109, 64, 212, NEAR)); // 9-12-15: exactly 15 away
        assertFalse(SPHERE.near(109, 64, 212.1, NEAR));
    }

    // Cuboid: blocks (0, 60, 0) to (10, 70, 10)

    private static final AreaGeometry CUBOID = new AreaGeometry.Cuboid(0, 60, 0, 10, 70, 10);

    @Test
    void aPointOnACuboidsEdgeBlocksIsInsideIt() {
        assertTrue(CUBOID.contains(0, 60, 0));
        assertTrue(CUBOID.contains(10.9, 70.9, 10.9));
        assertTrue(CUBOID.contains(5, 65, 5));
    }

    @Test
    void aPointOneBlockPastACuboidsEdgeIsOutsideIt() {
        assertFalse(CUBOID.contains(11, 65, 5));
        assertFalse(CUBOID.contains(-0.1, 65, 5));
        assertFalse(CUBOID.contains(5, 71, 5));
        assertFalse(CUBOID.contains(5, 59.9, 5));
        assertFalse(CUBOID.contains(5, 65, 11));
    }

    @Test
    void aPointTenBlocksPastACuboidIsNearItAndElevenIsNot() {
        assertTrue(CUBOID.near(20.9, 65, 5, NEAR));
        assertFalse(CUBOID.near(21, 65, 5, NEAR));
        assertTrue(CUBOID.near(-10, 65, 5, NEAR));
        assertFalse(CUBOID.near(-10.1, 65, 5, NEAR));
        assertTrue(CUBOID.near(20, 80, 20, NEAR));
        assertFalse(CUBOID.near(5, 81, 5, NEAR));
        assertFalse(CUBOID.near(5, 49, 5, NEAR));
    }

    // Prism with straight edges: the square (0, 0), (10, 0), (10, 10), (0, 10), from y 60 to 70

    private static final AreaGeometry.Prism SQUARE =
            new AreaGeometry.Prism(List.of(0, 10, 10, 0), List.of(0, 0, 10, 10), 60, 70);

    @Test
    void aStraightEdgedPrismHoldsEveryBlockOfItsSelectionEdgesAndCornersIncluded() {
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 10; z++) {
                assertTrue(SQUARE.contains(x, 64, z), "(" + x + ", " + z + ")");
            }
        }
        assertTrue(SQUARE.contains(10.9, 70.9, 10.9));
        assertTrue(SQUARE.contains(0, 60, 0));
    }

    @Test
    void aPointOneBlockPastAStraightEdgedPrismIsOutsideIt() {
        for (int i = -1; i <= 11; i++) {
            assertFalse(SQUARE.contains(-1, 64, i), "west at " + i);
            assertFalse(SQUARE.contains(11, 64, i), "east at " + i);
            assertFalse(SQUARE.contains(i, 64, -1), "north at " + i);
            assertFalse(SQUARE.contains(i, 64, 11), "south at " + i);
        }
        assertFalse(SQUARE.contains(5, 71, 5));
        assertFalse(SQUARE.contains(5, 59, 5));
    }

    // Prism with sloped edges: the triangle (0, 0), (12, 4), (3, 15), from y 60 to 70

    private static final AreaGeometry.Prism TRIANGLE =
            new AreaGeometry.Prism(List.of(0, 12, 3), List.of(0, 4, 15), 60, 70);

    @Test
    void aSlopedPrismHoldsEachOfItsCornerBlocks() {
        assertTrue(TRIANGLE.contains(0, 64, 0));
        assertTrue(TRIANGLE.contains(12, 64, 4));
        assertTrue(TRIANGLE.contains(3, 64, 15));
    }

    @Test
    void aSlopedPrismsEdgesFallOnTheBlocksPrismoidRegionChoosesToday() {
        // Each row of z, the first and last x inside, as AreaInsideTest pins them
        int[][] rows = {
            {0, 0, 1}, {1, 0, 4}, {2, 0, 7}, {3, 1, 10}, {4, 1, 12}, {5, 1, 11}, {6, 1, 10}, {7, 1, 10},
            {8, 2, 9}, {9, 2, 8}, {10, 2, 7}, {11, 2, 6}, {12, 2, 5}, {13, 3, 5}, {14, 3, 4}, {15, 3, 3}
        };
        for (int[] row : rows) {
            int z = row[0];
            for (int x = -2; x <= 14; x++) {
                boolean expected = x >= row[1] && x <= row[2];
                assertEquals(expected, TRIANGLE.contains(x, 64, z), "(" + x + ", " + z + ")");
            }
        }
        for (int x = -2; x <= 14; x++) {
            assertFalse(TRIANGLE.contains(x, 64, -1));
            assertFalse(TRIANGLE.contains(x, 64, 16));
        }
    }

    @Test
    void aSlopedPrismHoldsNothingAboveOrBelowItsHeights() {
        assertTrue(TRIANGLE.contains(5, 60, 5));
        assertTrue(TRIANGLE.contains(5, 70, 5));
        assertFalse(TRIANGLE.contains(5, 59, 5));
        assertFalse(TRIANGLE.contains(5, 71, 5));
        assertFalse(TRIANGLE.contains(0, 71, 0));
    }

    @Test
    void aPrismHoldsAPointExactlyWherePrismInsideHoldsItsBlock() {
        for (AreaGeometry.Prism prism : List.of(SQUARE, TRIANGLE)) {
            PrismInside rule = new PrismInside(prism);
            for (int x = -3; x <= 15; x++) {
                for (int y = 58; y <= 72; y++) {
                    for (int z = -3; z <= 18; z++) {
                        assertEquals(
                                rule.contains(x, y, z),
                                prism.contains(x + 0.5, y + 0.5, z + 0.5),
                                "(" + x + ", " + y + ", " + z + ")");
                    }
                }
            }
        }
    }

    @Test
    void aPrismIsNearUpToTenBlocksBeyondItsBoundingBoxNotItsPolygon() {
        // The triangle's bounding box is x 0..12, y 60..70, z 0..15
        assertTrue(TRIANGLE.near(22, 80, 25, NEAR)); // over 20 blocks from the triangle itself
        assertTrue(TRIANGLE.near(22.9, 80.9, 25.9, NEAR));
        assertFalse(TRIANGLE.near(23, 64, 5, NEAR));
        assertFalse(TRIANGLE.near(5, 64, 26, NEAR));
        assertFalse(TRIANGLE.near(5, 81, 5, NEAR));
        assertTrue(TRIANGLE.near(-10, 50, -10, NEAR));
        assertFalse(TRIANGLE.near(-11, 64, 5, NEAR));
        assertFalse(TRIANGLE.near(5, 49, 5, NEAR));
        assertFalse(TRIANGLE.near(5, 64, -10.1, NEAR));
    }
}
