package com.mcmiddleearth.guidebook.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mcmiddleearth.pluginutil.region.PrismoidRegion;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
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

    private static List<PlacementFinding> outsideFindings(AreaGeometry geometry, int x, int y, int z) {
        return checkHere(geometry, new PlacementCheck.StaffPosition("middle_earth", x, y, z)).stream()
                .filter(finding -> finding instanceof PlacementFinding.Outside)
                .toList();
    }

    @Test
    void standingInsideACuboidIsNotOutside() {
        assertEquals(List.of(), outsideFindings(CUBOID, 10, 70, 10));
    }

    // The cuboid's own blocks are inside it, up to and including its corners
    @Test
    void standingOnACuboidsCornerIsNotOutside() {
        assertEquals(List.of(), outsideFindings(CUBOID, 20, 80, 20));
    }

    @Test
    void standingBesideACuboidGivesTheDistanceToItsNearestBlock() {
        assertEquals(List.of(new PlacementFinding.Outside(5)), outsideFindings(CUBOID, 25, 70, 10));
    }

    // 3 blocks past the edge in x and 4 above the top: the nearest block is 5 away
    @Test
    void standingDiagonallyOffACuboidGivesTheStraightLineDistance() {
        assertEquals(List.of(new PlacementFinding.Outside(5)), outsideFindings(CUBOID, 23, 84, 10));
    }

    @Test
    void standingNextToACuboidIsOneBlockOutside() {
        assertEquals(List.of(new PlacementFinding.Outside(1)), outsideFindings(CUBOID, 21, 81, 10));
    }

    @Test
    void outsideIsInformation() {
        assertEquals(PlacementFinding.Severity.INFORMATION, new PlacementFinding.Outside(5).severity());
    }

    // An L, with its notch at x 10 to 20, z 10 to 20
    private static final AreaGeometry.Prism L_SHAPE =
            new AreaGeometry.Prism(List.of(0, 20, 20, 10, 10, 0), List.of(0, 0, 10, 10, 20, 20), 60, 70);

    @Test
    void standingInsideAConcavePrismIsNotOutside() {
        assertEquals(List.of(), outsideFindings(L_SHAPE, 5, 65, 15));
    }

    // The nearest block is on the notch's edge at z 10
    @Test
    void standingInAConcavePrismsNotchGivesTheDistanceToItsNearestBlock() {
        assertEquals(List.of(new PlacementFinding.Outside(4)), outsideFindings(L_SHAPE, 16, 65, 14));
    }

    @Test
    void standingAboveAPrismGivesTheDistanceDownToIt() {
        assertEquals(List.of(new PlacementFinding.Outside(7)), outsideFindings(L_SHAPE, 5, 77, 5));
    }

    // 3 blocks west of the L and 4 below it
    @Test
    void standingDiagonallyOffAPrismGivesTheStraightLineDistance() {
        assertEquals(List.of(new PlacementFinding.Outside(5)), outsideFindings(L_SHAPE, -3, 56, 5));
    }

    /**
     * Outside must agree with the inside rule that decides who gets a Welcome, PluginUtils' {@link PrismoidRegion},
     * for every block in and around the polygon, including its slanted and concave edges.
     */
    @Test
    void outsideAPrismAgreesWithWhoGetsAWelcome() {
        List<AreaGeometry.Prism> prisms = List.of(
                L_SHAPE,
                // A slanted, concave arrowhead, drawn clockwise
                new AreaGeometry.Prism(List.of(0, 7, 15, 7, 2), List.of(0, 13, 1, 5, 9), 60, 62),
                // A triangle drawn the other way round
                new AreaGeometry.Prism(List.of(0, 3, 17), List.of(0, 11, 4), 60, 62));
        World world = world("middle_earth");
        for (AreaGeometry.Prism prism : prisms) {
            PrismoidRegion region = new PrismoidRegion(
                    new Location(world, 0, 61, 0),
                    new ArrayList<>(prism.xs()),
                    new ArrayList<>(prism.zs()),
                    prism.minY(),
                    prism.maxY());
            for (int x = -3; x <= 23; x++) {
                for (int y = 58; y <= 64; y++) {
                    for (int z = -3; z <= 23; z++) {
                        boolean welcomed = region.isInside(new Location(world, x + 0.5, y, z + 0.5));
                        boolean outside = !outsideFindings(prism, x, y, z).isEmpty();
                        assertEquals(
                                !welcomed,
                                outside,
                                "block " + x + " " + y + " " + z + " of " + prism.xs() + " " + prism.zs());
                    }
                }
            }
        }
    }

    // A world that only has a name, enough for a region to compare Locations
    private static World world(String name) {
        return (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[] {World.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> name;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "World " + name;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    @Test
    void standingAnywhereIsNeverOutsideASphere() {
        assertEquals(List.of(), outsideFindings(new AreaGeometry.Sphere(0, 64, 0, 20), 100, 64, 100));
    }

    @Test
    void aSelectionInAnotherWorldIsNotAlsoOutside() {
        List<PlacementFinding> findings = PlacementCheck.check(
                new PlacementCheck.Proposal("moria", CUBOID),
                new PlacementCheck.StaffPosition("middle_earth", 500, 70, 500),
                List.of());

        assertEquals(List.of(new PlacementFinding.AnotherWorld("moria", "middle_earth")), findings);
    }
}
