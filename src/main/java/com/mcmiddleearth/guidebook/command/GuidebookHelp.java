package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.guidebook.util.GuidebookMessages;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.strokkur.commands.CustomSuggestion;
import org.bukkit.command.CommandSender;

/**
 * {@code help [command]}, and bare {@code /guidebook}: the help table for every {@code /guidebook} command. Each row
 * is shown only to senders with its permission, matching the Brigadier requirements in {@link GuidebookRoot}.
 */
final class GuidebookHelp {

    // GuidebookRoot's @Permission requirements use these too, so the table and the command tree agree
    static final String USER = "guidebook.user";
    static final String STAFF = "guidebook.staff";

    private static final String MANUAL = "https://www.mcmiddleearth.com/resources/guidebook-plugin-manual.107/";

    /**
     * One command in the help table.
     *
     * @param usages each form of the command, after {@code /guidebook}
     */
    private record Entry(String name, String permission, List<String> usages, String description) {

        Entry(String name, String permission, String usage, String description) {
            this(name, permission, List.of(usage), description);
        }
    }

    private static final List<Entry> COMMANDS = List.of(
            new Entry("on", USER, "on", "Receive welcome messages when you enter a Guidebook area."),
            new Entry("off", USER, "off", "Stop receiving welcome messages when you enter a Guidebook area."),
            new Entry("help", USER, "help [command]", "List the commands you can use, or show one command in detail."),
            new Entry("show", STAFF, "show <area>", "Show an area's description as players see it."),
            new Entry("details", STAFF, "details <area>", "Show an area's shape and location."),
            new Entry("warp", STAFF, "warp <area>", "Teleport to an area."),
            new Entry("enable", STAFF, "enable <area>", "Turn an area's welcome messages on."),
            new Entry("disable", STAFF, "disable <area>", "Turn an area's welcome messages off."),
            new Entry(
                    "set",
                    STAFF,
                    List.of("set <area>", "set <area> sphere <radius>"),
                    "Create an area from your WorldEdit selection, or as a sphere around you. If the area exists,"
                            + " move it there after you confirm."),
            new Entry(
                    "size",
                    STAFF,
                    List.of(
                            "size <area>",
                            "size <area> radius <radius>",
                            "size <area> corners <pos1> <pos2>",
                            "size <area> height <minY> <maxY>"),
                    "Resize an area: radius for a sphere, corners for a cuboid, height for a prism. With only"
                            + " <area>, show its size and the form that fits its shape."),
            new Entry("title", STAFF, "title <area>", "Edit an area's title, subtitle and boss bar."),
            new Entry(
                    "description",
                    STAFF,
                    List.of("description <area>", "description <area> getbook", "description <area> save"),
                    "Edit an area's description in chat, or get it as a book to edit and save the book in your"
                            + " hand."),
            new Entry("rename", STAFF, "rename <area> <new name>", "Rename an area."),
            new Entry("delete", STAFF, "delete <area>", "Delete an area after you confirm."),
            new Entry(
                    "list",
                    STAFF,
                    List.of("list [page]", "list <filter> [page]"),
                    "List the areas, or those whose names contain <filter>."),
            new Entry("reload", STAFF, "reload", "Reload every area from its file."),
            new Entry(
                    "dev",
                    STAFF,
                    List.of("dev", "dev console <true|false>", "dev level <level>", "dev watch", "dev unwatch"),
                    "Show or change the debug output."));

    private GuidebookHelp() {}

    /** Marks {@code help}'s command argument, which suggests the commands the sender may run. */
    @CustomSuggestion
    @interface CommandSuggestions {}

    @CommandSuggestions
    static CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        CommandSender sender = ctx.getSource().getSender();
        String typed = builder.getRemainingLowerCase();
        visibleTo(sender)
                .map(Entry::name)
                .filter(name -> name.startsWith(typed))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    /** Lists the commands the sender may run. Clicking one shows its help. */
    static void sendAll(CommandSender sender) {
        GuidebookMessages.send(
                sender, GuidebookMessages.info(Component.text("Guidebook commands (click one for details):")));
        visibleTo(sender)
                .forEach(entry -> GuidebookMessages.send(
                        sender,
                        GuidebookMessages.infoShortIndented(
                                GuidebookMessages.runsCommand(
                                        GuidebookMessages.stressed("/guidebook " + entry.name()),
                                        "/guidebook help " + entry.name(),
                                        "Click for details."),
                                Component.text(": " + entry.description()))));
        sendManual(sender);
    }

    /** Shows each form of one command and what it does, if the sender may run it. */
    static void sendOne(CommandSender sender, String name) {
        Optional<Entry> found = visibleTo(sender)
                .filter(entry -> entry.name().equals(name.toLowerCase(Locale.ROOT)))
                .findFirst();
        if (found.isEmpty()) {
            PluginData.getMessageUtil()
                    .sendErrorMessage(sender, "There is no Guidebook command '" + name + "' you can use.");
            return;
        }
        Entry entry = found.get();
        GuidebookMessages.send(sender, GuidebookMessages.info(Component.text("Help for /guidebook " + entry.name())));
        for (String usage : entry.usages()) {
            GuidebookMessages.send(
                    sender,
                    GuidebookMessages.infoShortIndented(GuidebookMessages.suggestsCommand(
                            GuidebookMessages.stressed("/guidebook " + usage),
                            "/guidebook " + entry.name() + " ",
                            "Click to fill in the command.")));
        }
        GuidebookMessages.send(sender, GuidebookMessages.infoShortIndented(Component.text(entry.description())));
    }

    private static Stream<Entry> visibleTo(CommandSender sender) {
        return COMMANDS.stream().filter(entry -> sender.hasPermission(entry.permission()));
    }

    private static void sendManual(CommandSender sender) {
        GuidebookMessages.send(
                sender,
                GuidebookMessages.infoShortIndented(
                        Component.text("Manual: "),
                        GuidebookMessages.opensUrl(Component.text(MANUAL), MANUAL, "Click to open the manual.")));
    }
}
