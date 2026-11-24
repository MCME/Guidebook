package com.mcmiddleearth.guidebook.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * When a player is inside an Area, and when they're near enough not to have Left it, for each Shape. These pinned the
 * rules PluginUtils' regions had, so the move to plain geometry kept them, except that a prism now compares the world
 * as the other Shapes do, and holds exactly the blocks staff selected in WorldEdit.
 */
class AreaInsideTest {

    private static final String WORLD = "middle-earth";
    private static final String OTHER_WORLD = "arda";

    private static WorldPoint at(double x, double y, double z) {
        return new WorldPoint(WORLD, x, y, z);
    }

    private static WorldPoint inOtherWorld(double x, double y, double z) {
        return new WorldPoint(OTHER_WORLD, x, y, z);
    }

    private static InfoArea area(AreaGeometry geometry) {
        return InfoArea.create("bree", WORLD, new WarpPoint(0, 64, 0, 0, 0), geometry);
    }

    // Sphere: radius 5 centred on its Warp point at (100, 64, 200)

    private static InfoArea sphere() {
        return InfoArea.create(
                "bree", WORLD, new WarpPoint(100, 64, 200, 0, 0), new AreaGeometry.Sphere(100, 64, 200, 5));
    }

    @Test
    void aPlayerExactlyARadiusFromASpheresWarpPointIsInsideIt() {
        assertTrue(sphere().isInside(at(103, 64, 204))); // 3-4-5: exactly 5 away
        assertTrue(sphere().isInside(at(100, 64, 200)));
    }

    @Test
    void aPlayerAFractionPastASpheresRadiusIsOutsideIt() {
        assertFalse(sphere().isInside(at(103, 64, 204.1)));
        // Measured from the exact position, not the block: block 105 is 5 blocks away, but the player is 5.4
        assertFalse(sphere().isInside(at(105.4, 64, 200)));
    }

    @Test
    void aPlayerTenPastASpheresRadiusIsNearItAndAFractionMoreIsNot() {
        assertTrue(sphere().isNear(at(109, 64, 212))); // 9-12-15: exactly 15 away
        assertFalse(sphere().isNear(at(109, 64, 212.1)));
    }

    @Test
    void aSphereInOtherWorldsIsNeitherInsideNorNear() {
        assertFalse(sphere().isInside(inOtherWorld(100, 64, 200)));
        assertFalse(sphere().isNear(inOtherWorld(100, 64, 200)));
    }

    // Cuboid: blocks (0, 60, 0) to (10, 70, 10)

    private static InfoArea cuboid() {
        return area(new AreaGeometry.Cuboid(0, 60, 0, 10, 70, 10));
    }

    @Test
    void aPlayerOnACuboidsEdgeBlocksIsInsideIt() {
        assertTrue(cuboid().isInside(at(0, 60, 0)));
        assertTrue(cuboid().isInside(at(10.9, 70.9, 10.9)));
        assertTrue(cuboid().isInside(at(5, 65, 5)));
    }

    @Test
    void aPlayerOneBlockPastACuboidsEdgeIsOutsideIt() {
        assertFalse(cuboid().isInside(at(11, 65, 5)));
        assertFalse(cuboid().isInside(at(-0.1, 65, 5)));
        assertFalse(cuboid().isInside(at(5, 71, 5)));
        assertFalse(cuboid().isInside(at(5, 59.9, 5)));
        assertFalse(cuboid().isInside(at(5, 65, 11)));
    }

    @Test
    void aPlayerTenBlocksPastACuboidIsNearItAndElevenHasLeft() {
        assertTrue(cuboid().isNear(at(20.9, 65, 5)));
        assertFalse(cuboid().isNear(at(21, 65, 5)));
        assertTrue(cuboid().isNear(at(-10, 65, 5)));
        assertFalse(cuboid().isNear(at(-10.1, 65, 5)));
        assertTrue(cuboid().isNear(at(20, 80, 20)));
        assertFalse(cuboid().isNear(at(5, 81, 5)));
        assertFalse(cuboid().isNear(at(5, 49, 5)));
    }

    @Test
    void aCuboidInOtherWorldsIsNeitherInsideNorNear() {
        assertFalse(cuboid().isInside(inOtherWorld(5, 65, 5)));
        assertFalse(cuboid().isNear(inOtherWorld(5, 65, 5)));
    }

    // Prism with straight edges: the square (0, 0), (10, 0), (10, 10), (0, 10), from y 60 to 70

    private static InfoArea square() {
        return area(new AreaGeometry.Prism(List.of(0, 10, 10, 0), List.of(0, 0, 10, 10), 60, 70));
    }

    @Test
    void aStraightEdgedPrismHoldsEveryBlockOfItsSelectionEdgesAndCornersIncluded() {
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 10; z++) {
                assertTrue(square().isInside(at(x, 64, z)), "(" + x + ", " + z + ")");
            }
        }
        assertTrue(square().isInside(at(10.9, 70.9, 10.9)));
        assertTrue(square().isInside(at(0, 60, 0)));
    }

    @Test
    void aPlayerOneBlockPastAStraightEdgedPrismIsOutsideIt() {
        for (int i = -1; i <= 11; i++) {
            assertFalse(square().isInside(at(-1, 64, i)), "west at " + i);
            assertFalse(square().isInside(at(11, 64, i)), "east at " + i);
            assertFalse(square().isInside(at(i, 64, -1)), "north at " + i);
            assertFalse(square().isInside(at(i, 64, 11)), "south at " + i);
        }
        assertFalse(square().isInside(at(5, 71, 5)));
        assertFalse(square().isInside(at(5, 59, 5)));
    }

    // Prism with sloped edges: the triangle (0, 0), (12, 4), (3, 15), from y 60 to 70

    private static final List<Integer> TRIANGLE_XS = List.of(0, 12, 3);
    private static final List<Integer> TRIANGLE_ZS = List.of(0, 4, 15);

    private static InfoArea triangle() {
        return area(new AreaGeometry.Prism(TRIANGLE_XS, TRIANGLE_ZS, 60, 70));
    }

    @Test
    void aSlopedPrismHoldsEachOfItsCornerBlocks() {
        assertTrue(triangle().isInside(at(0, 64, 0)));
        assertTrue(triangle().isInside(at(12, 64, 4)));
        assertTrue(triangle().isInside(at(3, 64, 15)));
    }

    @Test
    void aSlopedPrismsEdgesFallOnTheBlocksWorldEditSelects() {
        // Each row of z, the first and last x inside, as WorldEdit selects them
        int[][] rows = {
            {0, 0, 0}, {1, 1, 3}, {2, 1, 6}, {3, 1, 9}, {4, 1, 12}, {5, 1, 11}, {6, 2, 10}, {7, 2, 9},
            {8, 2, 8}, {9, 2, 7}, {10, 2, 7}, {11, 3, 6}, {12, 3, 5}, {13, 3, 4}, {14, 3, 3}, {15, 3, 3}
        };
        for (int[] row : rows) {
            int z = row[0];
            for (int x = -2; x <= 14; x++) {
                boolean expected = x >= row[1] && x <= row[2];
                assertEquals(expected, triangle().isInside(at(x, 64, z)), "(" + x + ", " + z + ")");
            }
        }
        for (int x = -2; x <= 14; x++) {
            assertFalse(triangle().isInside(at(x, 64, -1)));
            assertFalse(triangle().isInside(at(x, 64, 16)));
        }
    }

    @Test
    void aSlopedPrismHoldsNothingAboveOrBelowItsHeights() {
        assertTrue(triangle().isInside(at(5, 60, 5)));
        assertTrue(triangle().isInside(at(5, 70, 5)));
        assertFalse(triangle().isInside(at(5, 59, 5)));
        assertFalse(triangle().isInside(at(5, 71, 5)));
        assertFalse(triangle().isInside(at(0, 71, 0)));
    }

    @Test
    void aPrismIsNearUpToTenBlocksBeyondItsBoundingBoxNotItsPolygon() {
        // The triangle's bounding box is x 0..12, y 60..70, z 0..15
        assertTrue(triangle().isNear(at(22, 80, 25))); // over 20 blocks from the triangle itself
        assertFalse(triangle().isNear(at(23, 64, 5)));
        assertFalse(triangle().isNear(at(5, 64, 26)));
        assertFalse(triangle().isNear(at(5, 81, 5)));
        assertTrue(triangle().isNear(at(-10, 50, -10)));
        assertFalse(triangle().isNear(at(-11, 64, 5)));
        assertFalse(triangle().isNear(at(5, 49, 5)));
        assertFalse(triangle().isNear(at(5, 64, -10.1)));
    }

    @Test
    void aPrismHoldsNoBlockAwayFromItsCornersInOtherWorlds() {
        assertFalse(triangle().isInside(inOtherWorld(5, 64, 5)));
        assertFalse(square().isInside(inOtherWorld(5, 64, 5)));
    }

    // PluginUtils' prism ignored the world for near and for its corners

    @Test
    void aPrismIsNotNearAPlayerInAnotherWorld() {
        assertFalse(triangle().isNear(inOtherWorld(5, 64, 5)));
        assertFalse(square().isNear(inOtherWorld(5, 64, 5)));
    }

    @Test
    void aPrismsCornerBlocksAreNotInsideForAPlayerInAnotherWorld() {
        assertFalse(triangle().isInside(inOtherWorld(0, 64, 0)));
        assertFalse(triangle().isInside(inOtherWorld(12, 64, 4)));
        assertFalse(triangle().isInside(inOtherWorld(3, 64, 15)));
        assertFalse(square().isInside(inOtherWorld(10, 64, 10)));
    }

    @Test
    void anAreaComparesItsWorldsNameExactly() {
        assertFalse(cuboid().isInside(new WorldPoint("Middle-Earth", 5, 65, 5)));
        assertFalse(cuboid().isNear(new WorldPoint("Middle-Earth", 5, 65, 5)));
    }
}
