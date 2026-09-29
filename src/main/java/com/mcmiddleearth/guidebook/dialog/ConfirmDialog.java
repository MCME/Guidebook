package com.mcmiddleearth.guidebook.dialog;

import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.guidebook.util.GuidebookMessages;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

/**
 * A confirmation dialog for an action on one Area, such as deleting or moving it. It shows the question, the Area's
 * name and its Title. Esc runs the Cancel button's action, so closing the dialog counts as Cancel.
 */
public final class ConfirmDialog {

    private ConfirmDialog() {}

    /**
     * What Confirm does to the Area, with the dialog's wording and Confirm's colour. Deleting can't be undone, so its
     * Confirm is red.
     */
    private enum Mode {
        DELETE("Delete", "deleted", "Do you really want to delete this Guidebook area?", NamedTextColor.RED),
        MOVE(
                "Move",
                "moved",
                "This Guidebook area already exists. Do you want to move it to your location and selection?",
                NamedTextColor.GREEN);

        private final String verb;
        private final String done;
        private final String question;
        private final TextColor confirmColor;

        Mode(String verb, String done, String question, TextColor confirmColor) {
            this.verb = verb;
            this.done = done;
            this.question = question;
            this.confirmColor = confirmColor;
        }
    }

    /** Asks {@code player} to confirm deleting {@code area}. {@code delete} runs on Confirm. */
    public static void openToDelete(Player player, InfoArea area, Consumer<Player> delete) {
        open(player, area, Mode.DELETE, delete);
    }

    /** Asks {@code player} to confirm moving {@code area} to their location and selection. {@code move} runs on Confirm. */
    public static void openToMove(Player player, InfoArea area, Consumer<Player> move) {
        open(player, area, Mode.MOVE, move);
    }

    /**
     * Each dialog holds its own Area and actions, so two players confirming at once can't act on each other's Area.
     *
     * @param confirmed runs on Confirm, if {@code area} is still the Area registered under its name
     */
    private static void open(Player player, InfoArea area, Mode mode, Consumer<Player> confirmed) {
        String areaName = area.getName();
        String title = area.getTitle();
        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text(mode.question)),
                DialogBody.plainMessage(Component.text("Area: ").append(GuidebookMessages.stressed(areaName))),
                DialogBody.plainMessage(Component.text("Title: ")
                        .append(
                                title == null || title.isEmpty()
                                        ? Component.text("(none)", NamedTextColor.GRAY)
                                        : GuidebookMessages.title(title))));

        // Each dialog gets its own single-use callbacks, holding the Area it acts on
        ClickCallback.Options once = ClickCallback.Options.builder().uses(1).build();
        ActionButton confirm = ActionButton.builder(Component.text("✔ " + mode.verb, mode.confirmColor))
                .action(DialogAction.customClick(
                        (response, audience) -> confirm(audience, area, mode, confirmed), once))
                .build();
        ActionButton cancel = ActionButton.builder(Component.text("✘ Cancel", NamedTextColor.WHITE))
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (audience instanceof Player canceller) {
                                PluginData.getMessageUtil()
                                        .sendInfoMessage(
                                                canceller,
                                                "Guidebook area " + area.getName() + " was not " + mode.done + ".");
                            }
                        },
                        once))
                .build();

        player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(mode.verb + " Guidebook area \"")
                                .append(GuidebookMessages.stressed(areaName))
                                .append(Component.text("\"?")))
                        .body(body)
                        .build())
                .type(DialogType.confirmation(confirm, cancel))));
    }

    private static void confirm(Audience audience, InfoArea area, Mode mode, Consumer<Player> confirmed) {
        if (!(audience instanceof Player player)) {
            return;
        }
        // A rename keeps the same Area, but a delete or reload while the dialog was open drops it from the store
        if (PluginData.getInfoAreaExact(area.getName()) != area) {
            PluginData.getMessageUtil()
                    .sendErrorMessage(
                            player,
                            "Guidebook area " + area.getName()
                                    + " was deleted or reloaded while you decided. It was NOT " + mode.done + ".");
            return;
        }
        confirmed.accept(player);
    }
}
