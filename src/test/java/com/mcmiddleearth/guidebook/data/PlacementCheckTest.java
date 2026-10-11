package com.mcmiddleearth.guidebook.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
