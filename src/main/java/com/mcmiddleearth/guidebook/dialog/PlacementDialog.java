package com.mcmiddleearth.guidebook.dialog;

import com.mcmiddleearth.guidebook.data.AreaGeometry;
import com.mcmiddleearth.guidebook.data.CuboidInfoArea;
import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PlacementCheck;
import com.mcmiddleearth.guidebook.data.PlacementFinding;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.guidebook.data.PrismoidInfoArea;
import com.mcmiddleearth.guidebook.data.SphericalInfoArea;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntPredicate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * The Placement dialog: where an Area will go, checked before anything is written. It shows the staff member's
 * WorldEdit selection and the {@link PlacementCheck}'s findings, and offers to place the Area from the selection or as
 * a sphere around them. Every button runs the check again, since other staff may have changed Areas meanwhile.
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

        String sphereAroundMe();

        String cancelled();

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
            return "Use selection";
        }

        @Override
        public String sphereAroundMe() {
            return "Sphere around me";
        }

        @Override
        public String cancelled() {
            return "No Guidebook area was created.";
        }

        // Built from where the player is now, but only added once they press Create in the Edit dialog
        @Override
        public void place(Player player, AreaGeometry geometry, Location warpPoint) {
            InfoArea area =
                    switch (geometry) {
                        case AreaGeometry.Sphere sphere -> new SphericalInfoArea(areaName, warpPoint, sphere.radius());
                        case AreaGeometry.Cuboid cuboid -> new CuboidInfoArea(areaName, warpPoint, cuboid);
                        case AreaGeometry.Prism prism -> new PrismoidInfoArea(areaName, warpPoint, prism);
                    };
            EditDialog.openToCreate(player, area);
        }
    }

    /** Opens the dialog that places the new Area {@code name}. The name was free when the command ran. */
    public static void openToCreate(Player player, String name) {
        show(player, new Create(name), List.of());
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
        List<PlacementFinding> findings = proposal.map(p -> check(player, p)).orElse(List.of());

        List<DialogBody> body = new ArrayList<>();
        if (!refused.isEmpty()) {
            body.add(DialogBody.plainMessage(Component.text("Nothing was placed:", NamedTextColor.RED)));
            refused.forEach(finding -> body.add(DialogBody.plainMessage(line(finding))));
        }
        body.add(DialogBody.plainMessage(selectionLine(selection)));
        // A refused finding that still applies is already shown above
        findings.stream()
                .filter(finding -> !refused.contains(finding))
                .forEach(finding -> body.add(DialogBody.plainMessage(line(finding))));

        List<DialogInput> inputs =
                List.of(DialogInput.numberRange(RADIUS, Component.text("Sphere radius"), RADIUS_MIN, RADIUS_MAX)
                        .step(1f)
                        .initial((float) RADIUS_DEFAULT)
                        .width(300)
                        .build());

        // Each dialog gets its own single-use callbacks, holding what it places
        ClickCallback.Options once = ClickCallback.Options.builder().uses(1).build();
        List<ActionButton> buttons = new ArrayList<>();
        if (proposal.isPresent() && !hasBlocking(findings)) {
            PlacementCheck.Proposal fromSelection = proposal.get();
            buttons.add(ActionButton.builder(Component.text("✔ " + purpose.useSelection(), NamedTextColor.GREEN))
                    .tooltip(Component.text(
                            "Places the Area in your WorldEdit selection, with its Warp point where you stand."))
                    .action(DialogAction.customClick(
                            (response, audience) -> {
                                if (audience instanceof Player placer) {
                                    place(placer, purpose, fromSelection);
                                }
                            },
                            once))
                    .build());
        }
        buttons.add(ActionButton.builder(Component.text("● " + purpose.sphereAroundMe(), NamedTextColor.GREEN))
                .tooltip(Component.text("Places the Area as a sphere centred on you, with the radius above."))
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
                .build());
        buttons.add(ActionButton.builder(Component.text("✘ Cancel", NamedTextColor.WHITE))
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            audience.closeDialog();
                            if (audience instanceof Player canceller) {
                                GuidebookMessages.sendInfo(canceller, purpose.cancelled());
                            }
                        },
                        once))
                .build());

        player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(
                                Component.text(purpose.title()).append(GuidebookMessages.stressed(purpose.areaName())))
                        // Cancel is the only way out, so it always says that nothing changed
                        .canCloseWithEscape(false)
                        // Stays open after a button until the server replaces or closes it, as the Edit dialog does
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .pause(false)
                        .body(body)
                        .inputs(inputs)
                        .build())
                .type(DialogType.multiAction(buttons, null, 1))));
    }

    /** Checks the proposal again against the current Areas, and places it unless something now blocks it. */
    private static void place(Player player, Purpose purpose, PlacementCheck.Proposal proposal) {
        List<PlacementFinding> blocking = check(player, proposal).stream()
                .filter(finding -> finding.severity() == PlacementFinding.Severity.BLOCKING)
                .toList();
        if (!blocking.isEmpty()) {
            show(player, purpose, blocking);
            return;
        }
        purpose.place(player, proposal.geometry(), player.getLocation().clone());
    }

    private static List<PlacementFinding> check(Player player, PlacementCheck.Proposal proposal) {
        return PlacementCheck.check(
                proposal, positionOf(player), PluginData.getInfoAreas().values());
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

    /** The selection's Shape and dimensions, or why it can't be used. */
    private static Component selectionLine(WorldEditSelections.Selection selection) {
        Component label = Component.text("Your WorldEdit selection: ", NamedTextColor.GRAY);
        return switch (selection) {
            case WorldEditSelections.Usable usable -> label.append(Component.text(describe(usable.geometry())));
            case WorldEditSelections.NoSelection none ->
                label.append(Component.text(
                        "none. Select a cuboid or polygon with WorldEdit to use it.", NamedTextColor.GOLD));
            case WorldEditSelections.Incomplete incomplete ->
                label.append(Component.text(
                        "an incomplete " + incomplete.typeName() + ". Finish it with WorldEdit to use it.",
                        NamedTextColor.GOLD));
            case WorldEditSelections.Unsupported unsupported ->
                label.append(Component.text(
                        "a " + unsupported.typeName() + ", which Guidebook can't use. Select a cuboid or polygon, or"
                                + " use Sphere around me.",
                        NamedTextColor.GOLD));
        };
    }

    /** A Shape and its block-inclusive dimensions, such as "cuboid, 21 × 10 × 30 blocks (x × y × z)". */
    static String describe(AreaGeometry geometry) {
        AreaGeometry.Sides sides = geometry.sides();
        String dimensions = sides.x() + " × " + sides.y() + " × " + sides.z() + " blocks (x × y × z)";
        return switch (geometry) {
            case AreaGeometry.Sphere sphere -> "sphere, radius " + sphere.radius();
            case AreaGeometry.Cuboid cuboid -> "cuboid, " + dimensions;
            case AreaGeometry.Prism prism -> "prism with " + prism.xs().size() + " corners, " + dimensions;
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
        return axes.size() == 1
                ? axes.getFirst()
                : String.join(", ", axes.subList(0, axes.size() - 1)) + " and " + axes.getLast();
    }

    /** One finding on its own line: blocking ones in red, warnings marked ⚠. */
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
            case PlacementFinding.Outside outside ->
                Component.text(
                        "You're standing " + outside.distance() + (outside.distance() == 1 ? " block" : " blocks")
                                + " outside it, and the Warp point will be where you stand. Move inside it first if"
                                + " the Warp point should be.",
                        NamedTextColor.GRAY);
            case PlacementFinding.Overlap overlap ->
                warning("This overlaps " + namesUpToFive(overlap.areaNames())
                        + ". Players standing in both get both Welcomes.");
        };
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
