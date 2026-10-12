package com.mcmiddleearth.guidebook.dialog;

import com.mcmiddleearth.guidebook.data.AreaGeometry;
import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PlacementCheck;
import com.mcmiddleearth.guidebook.data.PlacementFinding;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.guidebook.util.GuidebookMessages;
import com.mcmiddleearth.guidebook.worldedit.WorldEditSelections;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntPredicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * The Placement dialog: where an Area will go, checked before anything is written. It shows the staff member's
 * WorldEdit selection and the {@link PlacementCheck}'s findings, and offers to place the Area from the selection or as
 * a sphere around them. When the selection can't be used, its button is greyed out, with the reason on hover. Sphere
 * around me opens a sphere step with a radius slider, which shows up front how far the sphere can reach before it
 * overlaps another Area. For {@code create} it continues into the Edit dialog; for {@code move} it also shows where
 * the Area is now, and its Move buttons are the confirmation. Every placing button runs the check again, since other
 * staff may have changed Areas meanwhile.
 *
 * <p>There is no Refresh button: the selection can't change while the dialog is open, so staff cancel, fix it and run
 * the command again.
 */
public final class PlacementDialog {

    private static final String RADIUS = "radius";
    private static final int RADIUS_MIN = 1;
    private static final int RADIUS_MAX = 200;
    private static final int RADIUS_DEFAULT = 20;

    private PlacementDialog() {}

    /** What the placement is for, which decides the dialog's wording and what a placement does. */
    private sealed interface Purpose {

        /** The Area being placed. */
        String areaName();

        String title();

        String useSelection();

        String useSelectionTooltip();

        String sphereAroundMe();

        String sphereAroundMeTooltip();

        /** The sphere step's button that places the sphere. */
        String placeSphere();

        String placeSphereTooltip();

        /** Tells the player that Cancel changed nothing. */
        void sendCancelled(Player player);

        /** For a move, the Area being moved, which the overlap check skips. */
        default Optional<String> movedAreaName() {
            return Optional.empty();
        }

        /** For a move, where the Area is now, shown above the new placement. */
        default Optional<Component> currentPlacement() {
            return Optional.empty();
        }

        /** Places the Area, once the check has found nothing blocking. */
        void place(Player player, AreaGeometry geometry, Location warpPoint);
    }

    /** Creating the Area {@code areaName}, which continues into the Edit dialog's create mode. */
    private record Create(String areaName) implements Purpose {

        @Override
        public String title() {
            return "Placing new Guidebook area ";
        }

        @Override
        public String useSelection() {
            return "Use WorldEdit selection";
        }

        @Override
        public String useSelectionTooltip() {
            return "Creates the area from your WorldEdit selection, with its Warp point where you stand.";
        }

        @Override
        public String sphereAroundMe() {
            return "Sphere around me";
        }

        @Override
        public String sphereAroundMeTooltip() {
            return "Creates the area as a sphere centred on you. Next you choose its radius, and see how far it can"
                    + " reach without overlapping another area.";
        }

        @Override
        public String placeSphere() {
            return "Create sphere";
        }

        @Override
        public String placeSphereTooltip() {
            return "Creates the area as a sphere centred on you, with the radius above, and goes on to its Title and"
                    + " Description.";
        }

        @Override
        public void sendCancelled(Player player) {
            GuidebookMessages.sendInfo(player, "No Guidebook area was created.");
        }

        // Built from where the player is now, but only added once they press Create in the Edit dialog
        @Override
        public void place(Player player, AreaGeometry geometry, Location warpPoint) {
            EditDialog.openToCreate(player, InfoArea.create(areaName, warpPoint, geometry));
        }
    }

    /**
     * Moving {@code area}. Pressing a Move button is the confirmation: the Area takes the new Shape, and its Warp point
     * becomes where the player stands.
     */
    private record Move(InfoArea area) implements Purpose {

        // Read when needed, so a rename while the dialog is open is followed
        @Override
        public String areaName() {
            return area.getName();
        }

        @Override
        public String title() {
            return "Moving Guidebook area ";
        }

        @Override
        public String useSelection() {
            return "Move to WorldEdit selection";
        }

        @Override
        public String useSelectionTooltip() {
            return "Moves the area to your WorldEdit selection, with its Warp point where you stand.";
        }

        @Override
        public String sphereAroundMe() {
            return "Move to sphere around me";
        }

        @Override
        public String sphereAroundMeTooltip() {
            return "Moves the area to a sphere centred on you. Next you choose its radius, and see how far it can"
                    + " reach without overlapping another area.";
        }

        @Override
        public String placeSphere() {
            return "Move to sphere";
        }

        @Override
        public String placeSphereTooltip() {
            return "Moves the area to a sphere centred on you, with the radius above. Its Warp point becomes where you"
                    + " stand.";
        }

        @Override
        public void sendCancelled(Player player) {
            GuidebookMessages.sendInfo(player, GuidebookMessages.area(areaName()), " was not moved.");
        }

        @Override
        public Optional<String> movedAreaName() {
            return Optional.of(areaName());
        }

        @Override
        public Optional<Component> currentPlacement() {
            return Optional.of(Component.text("Now: ")
                    .append(Component.text(
                            area.getWorldName() + ", " + area.getGeometry().describe())));
        }

        @Override
        public void place(Player player, AreaGeometry geometry, Location warpPoint) {
            // A rename keeps the same Area, but a delete or reload while the dialog was open drops it from the store
            if (PluginData.getInfoAreaExact(area.getName()) != area) {
                player.closeDialog();
                GuidebookMessages.sendError(
                        player,
                        GuidebookMessages.area(area.getName()),
                        " was deleted or reloaded while you decided. It was NOT moved.");
                return;
            }
            String oldWorld = area.getWorldName();
            boolean oldFileDeleted;
            try {
                oldFileDeleted = PluginData.moveInfoArea(area, geometry, warpPoint);
            } catch (IOException ex) {
                Logger.getLogger(PlacementDialog.class.getName()).log(Level.SEVERE, null, ex);
                player.closeDialog();
                GuidebookMessages.sendNotDone(player, area.getName(), "moved");
                return;
            }
            player.closeDialog();
            GuidebookMessages.sendInfo(
                    player, GuidebookMessages.area(area.getName()), " was moved to your location and selection.");
            if (!oldFileDeleted) {
                GuidebookMessages.sendError(
                        player,
                        "The old data file " + oldWorld + "/" + area.getName()
                                + ".yml couldn't be deleted. Delete it before reloading, or the area will load twice.");
            }
        }
    }

    /** Opens the dialog that places the new Area {@code name}. The name was free when the command ran. */
    public static void openToCreate(Player player, String name) {
        show(player, new Create(name), List.of());
    }

    /** Opens the dialog that moves {@code area}, showing where it is now above the new placement. */
    public static void openToMove(Player player, InfoArea area) {
        show(player, new Move(area), List.of());
    }

    /**
     * Shows the dialog for the player's current selection.
     *
     * @param refused blocking findings that stopped a button press, shown at the top
     */
    private static void show(Player player, Purpose purpose, List<PlacementFinding> refused) {
        WorldEditSelections.Selection selection = WorldEditSelections.read(player);
        Optional<PlacementCheck.Proposal> proposal = selection instanceof WorldEditSelections.Usable usable
                ? Optional.of(new PlacementCheck.Proposal(usable.world(), usable.geometry()))
                : Optional.empty();
        List<PlacementFinding> findings =
                proposal.map(p -> check(player, purpose, p)).orElse(List.of());

        List<DialogBody> body = new ArrayList<>();
        if (!refused.isEmpty()) {
            body.add(DialogBody.plainMessage(Component.text("Nothing was placed:", NamedTextColor.RED)));
            refused.forEach(finding -> body.add(DialogBody.plainMessage(line(finding))));
        }
        purpose.currentPlacement().ifPresent(current -> body.add(DialogBody.plainMessage(current)));
        selectionLines(selection).forEach(line -> body.add(DialogBody.plainMessage(line)));
        // A refused finding that still applies is already shown above
        findings.stream()
                .filter(finding -> !refused.contains(finding))
                .forEach(finding -> body.add(DialogBody.plainMessage(line(finding))));

        // Each dialog gets its own single-use callbacks, holding what it places
        ClickCallback.Options once = ClickCallback.Options.builder().uses(1).build();
        List<ActionButton> buttons = new ArrayList<>();
        if (proposal.isPresent() && !hasBlocking(findings)) {
            PlacementCheck.Proposal fromSelection = proposal.get();
            buttons.add(ActionButton.builder(Component.text("✔ " + purpose.useSelection(), NamedTextColor.GREEN))
                    .tooltip(Component.text(purpose.useSelectionTooltip()))
                    .action(DialogAction.customClick(
                            (response, audience) -> {
                                if (audience instanceof Player placer) {
                                    place(placer, purpose, fromSelection);
                                }
                            },
                            once))
                    .build());
        } else {
            // Greyed out with no action, so staff can see the option exists and hover to learn why it's unavailable.
            // Light grey with a red cross, as dark grey on the grey button was hard to read
            buttons.add(ActionButton.builder(Component.text("✘ ", NamedTextColor.RED)
                            .append(Component.text(purpose.useSelection(), NamedTextColor.GRAY)))
                    .tooltip(Component.text(unusableReason(selection)))
                    .build());
        }
        buttons.add(ActionButton.builder(Component.text("● " + purpose.sphereAroundMe(), NamedTextColor.WHITE))
                .tooltip(Component.text(purpose.sphereAroundMeTooltip()))
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (audience instanceof Player placer) {
                                showSphere(placer, purpose);
                            }
                        },
                        once))
                .build());
        // Cancel is the exit action, so it sits on its own row below the others and Esc runs it too
        ActionButton cancel = ActionButton.builder(Component.text("✘ Cancel", NamedTextColor.WHITE))
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            audience.closeDialog();
                            if (audience instanceof Player canceller) {
                                purpose.sendCancelled(canceller);
                            }
                        },
                        once))
                .build();

        player.showDialog(Dialog.create(builder ->
                builder.empty().base(base(purpose, body, List.of())).type(DialogType.multiAction(buttons, cancel, 2))));
    }

    /**
     * The sphere step: a radius slider, with how far a sphere centred on the player can reach before it overlaps
     * another Area worked out up front, so the radius is chosen knowing it. Back and Esc return to the Placement
     * dialog.
     */
    private static void showSphere(Player player, Purpose purpose) {
        PlacementCheck.StaffPosition staff = positionOf(player);
        PlacementCheck.SphereClearance clearance = PlacementCheck.sphereClearance(
                staff, PluginData.getInfoAreas().values(), purpose.movedAreaName(), RADIUS_MIN, RADIUS_MAX);
        int clear = clearance.largestClearRadius();

        List<DialogBody> body = new ArrayList<>();
        purpose.currentPlacement().ifPresent(current -> body.add(DialogBody.plainMessage(current)));
        body.add(DialogBody.plainMessage(Component.text("Sphere around you at " + staff.x() + " " + staff.y() + " "
                + staff.z() + ", with its Warp point" + " where you stand.")));
        if (clearance.overlappedNext().isEmpty()) {
            body.add(DialogBody.plainMessage(
                    Component.text("No other area is within radius " + RADIUS_MAX + " of you.", NamedTextColor.GREEN)));
        } else if (clear == 0) {
            body.add(DialogBody.plainMessage(warning("Even radius " + RADIUS_MIN + " overlaps "
                    + overlappedAreas(clearance.overlappedNext()) + ". You're standing in or next to it.")));
        } else {
            body.add(DialogBody.plainMessage(
                    Component.text("Up to radius " + clear + " overlaps no other area.", NamedTextColor.GREEN)));
            body.add(DialogBody.plainMessage(warning("Radius " + (clear + 1) + " or more overlaps "
                    + overlappedAreas(clearance.overlappedNext()) + ". Players standing in both get both Welcomes.")));
        }

        // Starts at the default radius, or the largest clear one when that's smaller
        int initialRadius = clear == 0 ? RADIUS_DEFAULT : Math.min(RADIUS_DEFAULT, clear);
        List<DialogInput> inputs =
                List.of(DialogInput.numberRange(RADIUS, Component.text("Sphere radius"), RADIUS_MIN, RADIUS_MAX)
                        .step(1f)
                        .initial((float) initialRadius)
                        .width(300)
                        .build());

        ClickCallback.Options once = ClickCallback.Options.builder().uses(1).build();
        ActionButton placeSphere = ActionButton.builder(
                        Component.text("✔ " + purpose.placeSphere(), NamedTextColor.GREEN))
                .tooltip(Component.text(purpose.placeSphereTooltip()))
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (audience instanceof Player placer) {
                                place(
                                        placer,
                                        purpose,
                                        PlacementCheck.Proposal.sphereAround(positionOf(placer), radius(response)));
                            }
                        },
                        once))
                .build();
        ActionButton back = ActionButton.builder(Component.text("← Back", NamedTextColor.WHITE))
                .tooltip(Component.text("Back to choosing between your WorldEdit selection and a sphere."))
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (audience instanceof Player backer) {
                                show(backer, purpose, List.of());
                            }
                        },
                        once))
                .build();

        player.showDialog(Dialog.create(builder -> builder.empty()
                .base(base(purpose, body, inputs))
                .type(DialogType.multiAction(List.of(placeSphere), back, 1))));
    }

    private static DialogBase base(Purpose purpose, List<DialogBody> body, List<DialogInput> inputs) {
        return DialogBase.builder(
                        Component.text(purpose.title()).append(GuidebookMessages.stressed(purpose.areaName())))
                // Stays open after a button until the server replaces or closes it, as the Edit dialog does
                .afterAction(DialogBase.DialogAfterAction.NONE)
                .pause(false)
                .body(body)
                .inputs(inputs)
                .build();
    }

    /** Checks the proposal again against the current Areas, and places it unless something now blocks it. */
    private static void place(Player player, Purpose purpose, PlacementCheck.Proposal proposal) {
        List<PlacementFinding> blocking = check(player, purpose, proposal).stream()
                .filter(finding -> finding.severity() == PlacementFinding.Severity.BLOCKING)
                .toList();
        if (!blocking.isEmpty()) {
            show(player, purpose, blocking);
            return;
        }
        purpose.place(player, proposal.geometry(), player.getLocation().clone());
    }

    private static List<PlacementFinding> check(Player player, Purpose purpose, PlacementCheck.Proposal proposal) {
        return PlacementCheck.check(
                proposal, positionOf(player), PluginData.getInfoAreas().values(), purpose.movedAreaName());
    }

    private static boolean hasBlocking(List<PlacementFinding> findings) {
        return findings.stream().anyMatch(finding -> finding.severity() == PlacementFinding.Severity.BLOCKING);
    }

    private static PlacementCheck.StaffPosition positionOf(Player player) {
        Location location = player.getLocation();
        return new PlacementCheck.StaffPosition(
                location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private static int radius(DialogResponseView response) {
        Float radius = response.getFloat(RADIUS);
        if (radius == null) {
            return RADIUS_DEFAULT;
        }
        return Math.clamp(Math.round(radius), RADIUS_MIN, RADIUS_MAX);
    }

    /**
     * The selection's Shape and dimensions, or what's wrong with it in gold, with what to do about it on a line of its
     * own.
     */
    private static List<Component> selectionLines(WorldEditSelections.Selection selection) {
        Component label = Component.text("Your WorldEdit selection: ");
        String again = " then run this command again.";
        return switch (selection) {
            case WorldEditSelections.Usable usable ->
                List.of(label.append(Component.text(usable.geometry().describe())));
            case WorldEditSelections.NoSelection none ->
                List.of(
                        label.append(Component.text("none", NamedTextColor.GOLD)),
                        Component.text("To use one, select a cuboid or polygon with WorldEdit," + again
                                + " Or use Sphere around me."));
            case WorldEditSelections.Incomplete incomplete ->
                List.of(
                        label.append(Component.text("an unfinished " + incomplete.typeName(), NamedTextColor.GOLD)),
                        Component.text("Set its remaining points with WorldEdit," + again));
            case WorldEditSelections.Unsupported unsupported ->
                List.of(
                        label.append(Component.text("a " + unsupported.typeName(), NamedTextColor.GOLD)),
                        Component.text("Guidebook can only use a cuboid or polygon. Switch with //sel cuboid or //sel"
                                + " poly, select it," + again + " Or use Sphere around me."));
        };
    }

    /** Why the selection button is greyed out, for its tooltip. */
    private static String unusableReason(WorldEditSelections.Selection selection) {
        String again = " then run this command again.";
        return switch (selection) {
            case WorldEditSelections.Usable usable -> "Your selection can't be used here. The reason is shown above.";
            case WorldEditSelections.NoSelection none ->
                "You have no WorldEdit selection. Select a cuboid or polygon," + again;
            case WorldEditSelections.Incomplete incomplete ->
                "Your WorldEdit selection isn't finished. Set its remaining points," + again;
            case WorldEditSelections.Unsupported unsupported ->
                "Guidebook can't use a " + unsupported.typeName() + " selection. Select a cuboid or polygon (//sel"
                        + " cuboid or //sel poly)," + again;
        };
    }

    /** The axes along which a test holds, such as "x and z". */
    private static String axes(AreaGeometry.Sides sides, IntPredicate test) {
        List<String> axes = new ArrayList<>();
        if (test.test(sides.x())) {
            axes.add("x");
        }
        if (test.test(sides.y())) {
            axes.add("y");
        }
        if (test.test(sides.z())) {
            axes.add("z");
        }
        return joinWithAnd(axes);
    }

    /**
     * One finding on its own line: blocking ones in red, warnings marked ⚠ in gold, and information in the default white,
     * since grey is hard to read over a bright sky behind the dialog.
     */
    private static Component line(PlacementFinding finding) {
        return switch (finding) {
            case PlacementFinding.AnotherWorld anotherWorld ->
                Component.text(
                        "✘ Your selection is in " + anotherWorld.selectionWorld() + ", but you're in "
                                + anotherWorld.staffWorld() + ". Go to " + anotherWorld.selectionWorld()
                                + ", or make a selection here.",
                        NamedTextColor.RED);
            case PlacementFinding.FlatPolygon flat ->
                warning("Your polygon is only one block tall. Set its height with //expand <blocks> up, or"
                        + " //expand vert for the full height of the world.");
            case PlacementFinding.TooSmall tooSmall ->
                warning("It's under " + PlacementCheck.MIN_SIDE + " blocks along "
                        + axes(tooSmall.sides(), side -> side < PlacementCheck.MIN_SIDE)
                        + ", so players can cross it without being welcomed. Make it bigger.");
            case PlacementFinding.TooLarge tooLarge ->
                warning("It's over " + PlacementCheck.MAX_SIDE + " blocks along "
                        + axes(tooLarge.sides(), side -> side > PlacementCheck.MAX_SIDE)
                        + ". Check your selection is what you meant.");
            case PlacementFinding.Outside outside -> outsideLine(outside);
            case PlacementFinding.Overlap overlap ->
                warning("This overlaps " + overlappedAreas(overlap.areaNames())
                        + ". Players standing in both get both Welcomes.");
        };
    }

    /**
     * Where the staff member stands, which becomes the Warp point: plain information just outside, a warning further
     * out, and in red once it blocks.
     */
    private static Component outsideLine(PlacementFinding.Outside outside) {
        String blocks = outside.distance() == 1 ? " block" : " blocks";
        Component distance = Component.text(outside.distance());
        return switch (outside.severity()) {
            // Only the number stands out: gold here, and bold in the warning's and blocker's own colours
            case INFORMATION ->
                Component.text()
                        .append(Component.text("You're standing "))
                        .append(distance.color(NamedTextColor.GOLD))
                        .append(Component.text(blocks + " outside it, so the Warp point will be outside the area too."))
                        .build();
            case WARNING ->
                Component.text()
                        .color(NamedTextColor.GOLD)
                        .append(Component.text("⚠ You're standing "))
                        .append(distance.decorate(TextDecoration.BOLD))
                        .append(Component.text(
                                blocks + " outside it, the Warp point will be far from the area, maybe move closer."))
                        .build();
            case BLOCKING ->
                Component.text()
                        .color(NamedTextColor.RED)
                        .append(Component.text("✘ You're standing "))
                        .append(distance.decorate(TextDecoration.BOLD))
                        .append(Component.text(blocks + " outside it. The Warp point is placed where you stand, move"
                                + " to at least within " + PlacementFinding.Outside.BLOCKING_DISTANCE
                                + " blocks of your selection, then run this command again."))
                        .build();
        };
    }

    /** Such as "the Guidebook area 'a'", or "the Guidebook areas 'a', 'b' and 3 more". */
    private static String overlappedAreas(List<String> names) {
        return "the Guidebook " + (names.size() == 1 ? "area " : "areas ")
                + namesUpToFive(names.stream().map(name -> "'" + name + "'").toList());
    }

    private static Component warning(String text) {
        return Component.text("⚠ " + text, NamedTextColor.GOLD);
    }

    /** Up to five names, then "and N more" for the rest. */
    private static String namesUpToFive(List<String> names) {
        if (names.size() <= 5) {
            return joinWithAnd(names);
        }
        List<String> shown = new ArrayList<>(names.subList(0, 5));
        shown.add((names.size() - 5) + " more");
        return joinWithAnd(shown);
    }

    private static String joinWithAnd(List<String> parts) {
        if (parts.size() == 1) {
            return parts.getFirst();
        }
        return String.join(", ", parts.subList(0, parts.size() - 1)) + " and " + parts.getLast();
    }
}
