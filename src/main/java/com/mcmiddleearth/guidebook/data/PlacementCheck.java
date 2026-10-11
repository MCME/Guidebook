package com.mcmiddleearth.guidebook.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Judges a proposed placement of an Area before anything is written, for the Placement dialog. It knows nothing about
 * Paper, WorldEdit or Dialogs, so it works without a running server.
 */
public final class PlacementCheck {

    private PlacementCheck() {}

    /** A Shape proposed for an Area, in the world it would be in. */
    public record Proposal(String world, AreaGeometry geometry) {

        /** Sphere around me: a sphere centred on the staff member, in their world. */
        public static Proposal sphereAround(StaffPosition staff, int radius) {
            return new Proposal(staff.world(), new AreaGeometry.Sphere(staff.x(), staff.y(), staff.z(), radius));
        }
    }

    /** The block the staff member placing the Area stands on, which becomes its Warp point. */
    public record StaffPosition(String world, int x, int y, int z) {}

    /** Checks a placement for a new Area. */
    public static List<PlacementFinding> check(
            Proposal proposal, StaffPosition staff, Collection<? extends MarkableArea> areas) {
        return check(proposal, staff, areas, Optional.empty());
    }

    /**
     * Checks a placement.
     *
     * @param areas the existing Areas
     * @param movedAreaName the Area being moved, if this is a move, which is never compared with itself
     * @return the findings, in the order the dialog shows them
     */
    public static List<PlacementFinding> check(
            Proposal proposal,
            StaffPosition staff,
            Collection<? extends MarkableArea> areas,
            Optional<String> movedAreaName) {
        List<PlacementFinding> findings = new ArrayList<>();
        if (!proposal.world().equals(staff.world())) {
            findings.add(new PlacementFinding.AnotherWorld(proposal.world(), staff.world()));
        }
        return findings;
    }
}
