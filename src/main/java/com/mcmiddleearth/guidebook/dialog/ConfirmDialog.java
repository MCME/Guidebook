package com.mcmiddleearth.guidebook.dialog;

import com.mcmiddleearth.guidebook.GuidebookPlugin;
import com.mcmiddleearth.guidebook.data.ChangeOutcome;
import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.util.AreaText;
import com.mcmiddleearth.guidebook.util.GuidebookMessages;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * The confirmation dialog for deleting an Area. It shows the question, the Area's name and its Title. Deleting can't be
 * undone, so "delete" in the title and question, and its Confirm, are red. Esc runs the Cancel button's action, so
 * closing the dialog counts as Cancel.
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
                DialogBody.plainMessage(Component.text("Do you really want to ")
                        .append(Component.text("delete", NamedTextColor.RED))
                        .append(Component.text(" this Guidebook area?"))),
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
                // Appended text takes its parent's colour, so the red "Delete" is a sibling rather than the parent
                .base(DialogBase.builder(Component.text()
                                .append(Component.text("Delete", NamedTextColor.RED))
                                .append(Component.text(" Guidebook area "))
                                .append(GuidebookMessages.stressed(areaName))
                                .append(Component.text("?"))
                                .build())
                        .body(body)
                        .build())
                .type(DialogType.confirmation(confirm, cancel))));
    }

    /** Deletes {@code area} through the store, which refuses if it was deleted or reloaded meanwhile. */
    private static void delete(Audience audience, InfoArea area) {
        if (!(audience instanceof Player player)) {
            return;
        }
        String name = area.getName();
        ChangeOutcome outcome =
                GuidebookPlugin.getPluginInstance().getAreaStore().delete(area);
        switch (outcome) {
            case ChangeOutcome.Done() ->
                GuidebookMessages.sendInfo(player, GuidebookMessages.area(name), " was deleted.");
            case ChangeOutcome.Gone() -> GuidebookMessages.sendGoneMeanwhile(player, name, "decided", "deleted");
            case ChangeOutcome.NotWritten(IOException cause) -> {
                Logger.getLogger(ConfirmDialog.class.getName()).severe(cause.getMessage());
                GuidebookMessages.sendNotDone(player, name, "deleted");
            }
            case ChangeOutcome.NameTaken _, ChangeOutcome.DoneButOldFileLeft _ -> {}
        }
    }
}
