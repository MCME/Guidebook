package com.mcmiddleearth.guidebook.dialog;

import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.guidebook.util.DescriptionText;
import com.mcmiddleearth.guidebook.util.GuidebookMessages;
import com.mcmiddleearth.guidebook.util.InputUtil;
import com.mcmiddleearth.pluginutil.message.FancyMessage;
import com.mcmiddleearth.pluginutil.message.config.FancyMessageConfigUtil;
import com.mcmiddleearth.pluginutil.message.config.MessageParseException;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * The Edit dialog: every editable field of an Area on one screen. Save checks the Description's markup and saves the
 * Area. Cancel and Esc discard the changes.
 */
public final class EditDialog {

    private static final int TITLE_MAX = 32;
    private static final int SUBTITLE_MAX = 64;
    private static final int DESCRIPTION_MAX = 4096;

    private static final String SHOW_TITLE = "show_title";
    private static final String TITLE = "title";
    private static final String SUBTITLE = "subtitle";
    private static final String SHOW_BOSS_BAR = "show_boss_bar";
    private static final String DESCRIPTION = "description";
    private static final String ENABLED = "enabled";

    private EditDialog() {}

    /**
     * The dialog's fields, as typed: colour codes are written {@code #}, and the Description is one string with real
     * line breaks.
     */
    private record Values(
            boolean showTitle,
            String title,
            String subtitle,
            boolean showBossBar,
            String description,
            boolean enabled) {

        static Values of(InfoArea area) {
            return new Values(
                    area.isShowTitle(),
                    typed(area.getTitle()),
                    typed(area.getSubtitle()),
                    area.isShowScoreboard(),
                    DescriptionText.toTyped(area.getDescription()),
                    area.isEnabled());
        }

        static Values of(DialogResponseView response) {
            return new Values(
                    Boolean.TRUE.equals(response.getBoolean(SHOW_TITLE)),
                    Objects.requireNonNullElse(response.getText(TITLE), ""),
                    Objects.requireNonNullElse(response.getText(SUBTITLE), ""),
                    Boolean.TRUE.equals(response.getBoolean(SHOW_BOSS_BAR)),
                    Objects.requireNonNullElse(response.getText(DESCRIPTION), ""),
                    Boolean.TRUE.equals(response.getBoolean(ENABLED)));
        }

        List<String> descriptionLines() {
            return DescriptionText.toStored(description);
        }

        private static String typed(String stored) {
            return stored == null ? "" : InputUtil.replaceColorCodeWithAltCode(stored);
        }
    }

    public static void open(Player player, InfoArea area) {
        show(player, area, Values.of(area), null);
    }

    /** Shows the dialog filled with {@code values}, with {@code error} at the top if it isn't null. */
    private static void show(Player player, InfoArea area, Values values, Component error) {
        List<DialogBody> body = new ArrayList<>();
        if (error != null) {
            body.add(DialogBody.plainMessage(error.color(NamedTextColor.RED)));
        }

        // The Dialog API puts body text above every input, so the Description's hint goes in its label
        List<DialogInput> inputs = List.of(
                DialogInput.text(TITLE, Component.text("Title"))
                        .width(300)
                        .initial(values.title())
                        .maxLength(TITLE_MAX)
                        .build(),
                DialogInput.text(SUBTITLE, Component.text("Subtitle"))
                        .width(300)
                        .initial(values.subtitle())
                        .maxLength(SUBTITLE_MAX)
                        .build(),
                DialogInput.bool(SHOW_TITLE, Component.text("Show title"))
                        .initial(values.showTitle())
                        .build(),
                DialogInput.bool(SHOW_BOSS_BAR, Component.text("Show boss bar"))
                        .initial(values.showBossBar())
                        .build(),
                DialogInput.bool(ENABLED, Component.text("Enabled"))
                        .initial(values.enabled())
                        .build(),
                DialogInput.text(
                                DESCRIPTION,
                                Component.text("Description ")
                                        .append(Component.text(
                                                "(type # for a colour code; each line is sent as one line in chat)",
                                                NamedTextColor.GRAY)))
                        .width(400)
                        .initial(values.description())
                        .maxLength(DESCRIPTION_MAX)
                        .multiline(TextDialogInput.MultilineOptions.create(null, 150))
                        .build());

        // Each dialog gets its own single-use callback, holding the Area it edits
        ActionButton save = ActionButton.builder(Component.text("✔ Save", NamedTextColor.GREEN))
                .action(DialogAction.customClick(
                        (response, audience) -> save(audience, area, Values.of(response)),
                        ClickCallback.Options.builder().uses(1).build()))
                .build();
        ActionButton cancel = ActionButton.builder(Component.text("✘ Cancel", NamedTextColor.RED))
                .build();

        player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Edit Guidebook area \"")
                                .append(Component.text(area.getName(), GuidebookMessages.STRESSED))
                                .append(Component.text("\"")))
                        .canCloseWithEscape(true)
                        .body(body)
                        .inputs(inputs)
                        .build())
                .type(DialogType.confirmation(save, cancel))));
    }

    private static void save(Audience audience, InfoArea area, Values values) {
        if (!(audience instanceof Player player)) {
            return;
        }
        String areaName = area.getName();
        // A rename keeps the same Area, but a delete or reload while the dialog was open drops it from the store
        if (PluginData.getInfoAreaExact(areaName) != area) {
            PluginData.getMessageUtil()
                    .sendErrorMessage(
                            player,
                            "Guidebook area " + areaName
                                    + " was deleted or reloaded while you edited it. Your changes were NOT saved.");
            return;
        }
        Optional<String> problem = problem(values);
        if (problem.isPresent()) {
            show(player, area, values, Component.text(problem.get() + " Nothing was saved."));
            return;
        }
        area.setShowTitle(values.showTitle());
        area.setTitle(InputUtil.replaceAltColorCode(values.title()));
        area.setSubtitle(InputUtil.replaceAltColorCode(values.subtitle()));
        area.setShowScoreboard(values.showBossBar());
        area.setDescription(new ArrayList<>(values.descriptionLines()));
        if (values.enabled()) {
            area.statusOn();
        } else {
            area.statusOff();
        }
        try {
            PluginData.saveArea(area);
        } catch (IOException ex) {
            Logger.getLogger(EditDialog.class.getName()).log(Level.SEVERE, null, ex);
            PluginData.getMessageUtil()
                    .sendErrorMessage(player, "There was an error. Guidebook area " + areaName + " was NOT saved.");
            return;
        }
        PluginData.getMessageUtil()
                .sendInfoMessage(player, "Guidebook area " + areaName + " was saved. This is its Welcome:");
        area.previewWelcome(player);
    }

    /**
     * @return why the values can't be saved, or empty if they can. The dialog enforces the lengths, but a modified
     *     client could send longer text.
     */
    private static Optional<String> problem(Values values) {
        if (values.title().isBlank() && (values.showTitle() || values.showBossBar())) {
            return Optional.of("Give the area a Title, or untick Show title and Show boss bar.");
        }
        if (values.title().length() > TITLE_MAX) {
            return Optional.of("The Title is longer than " + TITLE_MAX + " characters.");
        }
        if (values.subtitle().length() > SUBTITLE_MAX) {
            return Optional.of("The Subtitle is longer than " + SUBTITLE_MAX + " characters.");
        }
        if (values.description().length() > DESCRIPTION_MAX) {
            return Optional.of("The Description is longer than " + DESCRIPTION_MAX + " characters.");
        }
        try {
            FancyMessageConfigUtil.addFromStringList(
                    new FancyMessage(PluginData.getMessageUtil()), values.descriptionLines());
        } catch (MessageParseException ex) {
            return Optional.of("The Description has a markup error: " + ex.getMessage());
        }
        return Optional.empty();
    }
}
