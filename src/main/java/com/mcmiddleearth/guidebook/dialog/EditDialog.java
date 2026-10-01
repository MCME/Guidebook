package com.mcmiddleearth.guidebook.dialog;

import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.guidebook.util.AreaText;
import com.mcmiddleearth.guidebook.util.GuidebookMessages;
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
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * The Edit dialog: every editable field of an Area on one screen. Text is typed as MiniMessage. Save checks the
 * lengths and saves the Area, then shows its Welcome so staff can spot markup mistakes. Cancel and Esc discard the changes. For an Area {@code set} is creating, the dialog creates it: the Area exists
 * only once Create is pressed, and Esc is turned off so that Cancel can say nothing was created.
 */
public final class EditDialog {

    // The Title and Subtitle limits count visible characters, so their inputs leave room for markup
    private static final int TITLE_MAX = 32;
    private static final int SUBTITLE_MAX = 64;
    private static final int TITLE_SUBTITLE_INPUT_MAX = 256;
    private static final int DESCRIPTION_MAX = 4096;

    private static final String SHOW_TITLE = "show_title";
    private static final String TITLE = "title";
    private static final String SUBTITLE = "subtitle";
    private static final String SHOW_BOSS_BAR = "show_boss_bar";
    private static final String DESCRIPTION = "description";
    private static final String ENABLED = "enabled";

    private EditDialog() {}

    /** The dialog's fields, as typed and stored: MiniMessage, with real line breaks in the Description. */
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
                    area.getTitle(),
                    area.getSubtitle(),
                    area.isShowScoreboard(),
                    area.getDescription(),
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

        /**
         * A new Area's dialog starts with no Title (not the stored placeholder), Show title ticked and the
         * Description's usual opening.
         */
        Values forNewArea() {
            return new Values(
                    true,
                    "",
                    subtitle,
                    showBossBar,
                    description.isEmpty() ? AreaText.DEFAULT_DESCRIPTION : description,
                    enabled);
        }
    }

    public static void open(Player player, InfoArea area) {
        show(player, area, Mode.EDIT, Values.of(area), null);
    }

    /**
     * Opens the dialog that creates {@code area}, which {@code set} has built but not added. Nothing is added or written
     * until Create is pressed.
     */
    public static void openToCreate(Player player, InfoArea area) {
        show(player, area, Mode.CREATE, Values.of(area).forNewArea(), null);
    }

    /** Whether the dialog changes a stored Area, or creates one that {@code set} has built. */
    private enum Mode {
        EDIT("Edit", "✔ Save", "saved"),
        CREATE("Create", "✔ Create", "created");

        private final String titleVerb;
        private final String button;
        private final String done;

        Mode(String titleVerb, String button, String done) {
            this.titleVerb = titleVerb;
            this.button = button;
            this.done = done;
        }
    }

    /** Shows the dialog filled with {@code values}, with {@code error} at the top if it isn't null. */
    private static void show(Player player, InfoArea area, Mode mode, Values values, Component error) {
        List<DialogBody> body = new ArrayList<>();
        if (error != null) {
            body.add(DialogBody.plainMessage(error.color(NamedTextColor.RED)));
        }

        // The Dialog API puts body text above every input, so the Description's hint goes in its label
        List<DialogInput> inputs = List.of(
                DialogInput.text(TITLE, Component.text("Title"))
                        .width(300)
                        .initial(values.title())
                        .maxLength(TITLE_SUBTITLE_INPUT_MAX)
                        .build(),
                DialogInput.text(SUBTITLE, Component.text("Subtitle"))
                        .width(300)
                        .initial(values.subtitle())
                        .maxLength(TITLE_SUBTITLE_INPUT_MAX)
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
                                                "(MiniMessage; each line is sent as one line in chat)",
                                                NamedTextColor.GRAY)))
                        .width(400)
                        .initial(values.description())
                        .maxLength(DESCRIPTION_MAX)
                        .multiline(TextDialogInput.MultilineOptions.create(null, 150))
                        .build());

        // Each dialog gets its own single-use callbacks, holding the Area it edits or creates
        ClickCallback.Options once = ClickCallback.Options.builder().uses(1).build();
        ActionButton save = ActionButton.builder(Component.text(mode.button, NamedTextColor.GREEN))
                .action(DialogAction.customClick(
                        (response, audience) -> save(audience, area, mode, Values.of(response)), once))
                .build();
        // Esc just closes when editing. Creating turns Esc off, so Cancel is the only way out and always says that
        // nothing was created
        ActionButton cancel = ActionButton.builder(Component.text("✘ Cancel", NamedTextColor.WHITE))
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            audience.closeDialog();
                            if (mode == Mode.CREATE && audience instanceof Player canceller) {
                                PluginData.getMessageUtil()
                                        .sendInfoMessage(canceller, "No Guidebook area was created.");
                            }
                        },
                        once))
                .build();

        player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(mode.titleVerb + " Guidebook area \"")
                                .append(Component.text(area.getName(), GuidebookMessages.STRESSED))
                                .append(Component.text("\"")))
                        .canCloseWithEscape(mode == Mode.EDIT)
                        // The dialog stays open after the Preview button, so that typed text isn't lost. Save and
                        // Cancel close it themselves. Minecraft only allows that for a dialog that doesn't pause the
                        // game, and pausing only ever applies in single-player
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .pause(false)
                        .body(body)
                        .inputs(inputs)
                        .build())
                .type(DialogType.multiAction(List.of(save, webUiButton(values.description()), cancel), null, 3))));
    }

    /** Opens the Adventure WebUI with {@code description}, so staff can preview it while writing. */
    private static ActionButton webUiButton(String description) {
        AreaText.WebUiLink link = AreaText.webUiLink(description);
        String hover = "Opens the Adventure WebUI to preview the Description.";
        if (link.isDescriptionTooLong()) {
            hover += " The Description is too long to fit in the link, so it opens with the default opening, "
                    + AreaText.DEFAULT_DESCRIPTION + ", instead.";
        }
        return ActionButton.builder(Component.text("Preview in WebUI", GuidebookMessages.INFO))
                .tooltip(Component.text(hover))
                .action(DialogAction.staticAction(ClickEvent.openUrl(link.url())))
                .build();
    }

    private static void save(Audience audience, InfoArea area, Mode mode, Values values) {
        if (!(audience instanceof Player player)) {
            return;
        }
        String areaName = area.getName();
        // A rename keeps the same Area, but a delete or reload while the dialog was open drops it from the store
        if (mode == Mode.EDIT && PluginData.getInfoAreaExact(areaName) != area) {
            PluginData.getMessageUtil()
                    .sendErrorMessage(
                            player,
                            "Guidebook area " + areaName
                                    + " was deleted or reloaded while you edited it. Your changes were NOT saved.");
            player.closeDialog();
            return;
        }
        Optional<String> problem = problem(values);
        if (problem.isPresent()) {
            show(player, area, mode, values, Component.text(problem.get() + " Nothing was " + mode.done + "."));
            return;
        }
        player.closeDialog();
        // The name was free when set ran, but it isn't reserved while the dialog is open
        if (mode == Mode.CREATE && PluginData.newAreaNameProblem(areaName).isPresent()) {
            PluginData.getMessageUtil()
                    .sendErrorMessage(
                            player,
                            "Guidebook area " + areaName
                                    + " was created by someone else while you edited it. Nothing was created.");
            return;
        }

        area.setShowTitle(values.showTitle());
        area.setTitle(values.title());
        area.setSubtitle(values.subtitle());
        area.setShowScoreboard(values.showBossBar());
        area.setDescription(values.description().stripTrailing());
        if (values.enabled()) {
            area.statusOn();
        } else {
            area.statusOff();
        }
        try {
            if (mode == Mode.CREATE) {
                PluginData.createInfoArea(area);
            } else {
                PluginData.saveArea(area);
            }
        } catch (IOException ex) {
            Logger.getLogger(EditDialog.class.getName()).log(Level.SEVERE, null, ex);
            PluginData.getMessageUtil()
                    .sendErrorMessage(
                            player, "There was an error. Guidebook area " + areaName + " was NOT " + mode.done + ".");
            return;
        }
        PluginData.getMessageUtil()
                .sendInfoMessage(player, "Guidebook area " + areaName + " was " + mode.done + ". This is its Welcome:");
        area.previewWelcome(player);
    }

    /**
     * @return why the values can't be saved, or empty if they can. The Title and Subtitle limits count visible
     *     characters, which the dialog can't enforce. It enforces the Description's length, but a modified client
     *     could send longer text.
     */
    private static Optional<String> problem(Values values) {
        if (AreaText.isVisiblyBlank(values.title()) && (values.showTitle() || values.showBossBar())) {
            return Optional.of("Give the area a Title, or untick Show title and Show boss bar.");
        }
        int titleLength = AreaText.visibleLength(values.title());
        if (titleLength > TITLE_MAX) {
            return Optional.of(
                    "The Title has " + titleLength + " visible characters, more than the " + TITLE_MAX + " allowed.");
        }
        int subtitleLength = AreaText.visibleLength(values.subtitle());
        if (subtitleLength > SUBTITLE_MAX) {
            return Optional.of("The Subtitle has " + subtitleLength + " visible characters, more than the "
                    + SUBTITLE_MAX + " allowed.");
        }
        if (values.description().length() > DESCRIPTION_MAX) {
            return Optional.of("The Description is longer than " + DESCRIPTION_MAX + " characters.");
        }
        return Optional.empty();
    }
}
