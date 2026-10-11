package com.mcmiddleearth.guidebook.dialog;

import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.guidebook.util.AreaText;
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
import org.bukkit.entity.Player;

/**
 * The confirmation dialog for deleting an Area. It shows the question, the Area's name and its Title. Deleting can't be
 * undone, so its Confirm is red. Esc runs the Cancel button's action, so closing the dialog counts as Cancel.
 */
public final class ConfirmDialog {

    private ConfirmDialog() {}

    /**
     * Asks {@code player} to confirm deleting {@code area}. Confirm deletes it, and Cancel says it was not deleted.
     */
    public static void openToDelete(Player player, InfoArea area) {
        openToDelete(
                player,
                area,
                canceller -> GuidebookMessages.sendInfo(
                        canceller, GuidebookMessages.area(area.getName()), " was not deleted."));
    }

    /**
     * Asks {@code player} to confirm deleting {@code area}. Confirm deletes it, and {@code cancelled} runs on Cancel
     * or Esc, so a dialog that opened the confirmation can reopen itself.
     */
    public static void openToDelete(Player player, InfoArea area, Consumer<Player> cancelled) {
        String areaName = area.getName();
        String title = area.getTitle();
        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("Do you really want to delete this Guidebook area?")),
                DialogBody.plainMessage(Component.text("Area: ").append(GuidebookMessages.stressed(areaName))),
                DialogBody.plainMessage(Component.text("Title: ")
                        .append(
                                title == null || title.isEmpty()
                                        ? Component.text("(none)", NamedTextColor.GRAY)
                                        : AreaText.render(title))));

        // Each dialog gets its own single-use callbacks, holding the Area it acts on
        ClickCallback.Options once = ClickCallback.Options.builder().uses(1).build();
        ActionButton confirm = ActionButton.builder(Component.text("✔ Delete", NamedTextColor.RED))
                .action(DialogAction.customClick((response, audience) -> delete(audience, area), once))
                .build();
        ActionButton cancel = ActionButton.builder(Component.text("✘ Cancel", NamedTextColor.WHITE))
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (audience instanceof Player canceller) {
                                cancelled.accept(canceller);
                            }
                        },
                        once))
                .build();

        player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Delete Guidebook area ")
                                .append(GuidebookMessages.stressed(areaName))
                                .append(Component.text("?")))
                        .body(body)
                        .build())
                .type(DialogType.confirmation(confirm, cancel))));
    }

    /** Deletes {@code area}, if it is still the Area registered under its name. */
    private static void delete(Audience audience, InfoArea area) {
        if (!(audience instanceof Player player)) {
            return;
        }
        // A rename keeps the same Area, but a delete or reload while the dialog was open drops it from the store
        if (PluginData.getInfoAreaExact(area.getName()) != area) {
            GuidebookMessages.sendError(
                    player,
                    GuidebookMessages.area(area.getName()),
                    " was deleted or reloaded while you decided. It was NOT deleted.");
            return;
        }
        if (PluginData.deleteInfoArea(area)) {
            GuidebookMessages.sendInfo(player, GuidebookMessages.area(area.getName()), " was deleted.");
        } else {
            GuidebookMessages.sendError(player, "There was an error while deleting the data file from disk.");
        }
    }
}
