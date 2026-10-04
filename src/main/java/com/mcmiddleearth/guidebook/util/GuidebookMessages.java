package com.mcmiddleearth.guidebook.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Builds and sends every Guidebook chat message as Adventure components. Text passed in is never parsed as markup.
 * Each line starts with the {@code [Guidebook]} prefix in its own colour, and the body colour shows whether it's info
 * or an error.
 */
public final class GuidebookMessages {

    private static final TextColor PREFIX_COLOR = NamedTextColor.DARK_AQUA;
    public static final TextColor INFO = NamedTextColor.AQUA;
    public static final TextColor STRESSED = NamedTextColor.GREEN;
    private static final TextColor ERROR = NamedTextColor.RED;
    private static final TextColor ERROR_STRESSED = NamedTextColor.DARK_RED;

    private static final String PLUGIN_NAME = "Guidebook";
    private static final String PREFIX = "[" + PLUGIN_NAME + "] ";
    // Indented lines line up after the prefix's name and brackets
    private static final String INDENT = " ".repeat(PLUGIN_NAME.length() + 2);
    private static final String SHORT_INDENT = "  ";

    private GuidebookMessages() {}

    /** A line starting with the {@code [Guidebook]} prefix, in the info colour. */
    public static Component info(ComponentLike... parts) {
        return prefixed(INFO, parts);
    }

    /** A line starting with the {@code [Guidebook]} prefix, in the error colour. */
    public static Component error(ComponentLike... parts) {
        return prefixed(ERROR, parts);
    }

    public static void sendInfo(CommandSender recipient, String text) {
        send(recipient, info(Component.text(text)));
    }

    public static void sendInfo(CommandSender recipient, ComponentLike... parts) {
        send(recipient, info(parts));
    }

    public static void sendError(CommandSender recipient, String text) {
        send(recipient, error(Component.text(text)));
    }

    public static void sendError(CommandSender recipient, ComponentLike... parts) {
        send(recipient, error(parts));
    }

    /** A line indented to follow an {@link #info} line, in the info colour. */
    public static Component infoIndented(ComponentLike... parts) {
        return line(INDENT, INFO, parts);
    }

    /** A line slightly indented to follow an {@link #info} line, for long lines that would wrap under the full indent. */
    public static Component infoShortIndented(ComponentLike... parts) {
        return line(SHORT_INDENT, INFO, parts);
    }

    public static Component stressed(String text) {
        return Component.text(text, STRESSED);
    }

    public static Component errorStressed(String text) {
        return Component.text(text, ERROR_STRESSED);
    }

    /** {@code Area <name>} for an {@link #info} line, with the name stressed. Names never need quotes, as they have no spaces. */
    public static Component area(String name) {
        return Component.text("Area ").append(stressed(name));
    }

    /** {@code Area <name>} for an {@link #error} line, with the name stressed. */
    public static Component errorArea(String name) {
        return Component.text("Area ").append(errorStressed(name));
    }

    /** Clicking the text fills in {@code command} in the chat box, and hovering shows {@code hover}. */
    public static Component suggestsCommand(Component text, String command, String hover) {
        return withHover(text.clickEvent(ClickEvent.suggestCommand(command)), hover);
    }

    /** Clicking the text runs {@code command}, and hovering shows {@code hover}. */
    public static Component runsCommand(Component text, String command, String hover) {
        return withHover(text.clickEvent(ClickEvent.runCommand(command)), hover);
    }

    /** Clicking the text opens {@code url}, and hovering shows {@code hover}. */
    public static Component opensUrl(Component text, String url, String hover) {
        return withHover(text.clickEvent(ClickEvent.openUrl(url)), hover);
    }

    public static Component withHover(Component text, String hover) {
        return text.hoverEvent(HoverEvent.showText(Component.text(hover)));
    }

    /**
     * Sends a blank line before a multi-line block, so it stands apart from the messages above it. Wrapped lines start
     * at the left edge, so indentation alone can't show where a block starts.
     */
    public static void startBlock(CommandSender recipient) {
        send(recipient, Component.empty());
    }

    /** Players get the component; senders without a chat screen, such as the console, get its plain text. */
    public static void send(CommandSender recipient, Component message) {
        if (recipient instanceof Player) {
            recipient.sendMessage(message);
        } else {
            recipient.sendMessage(PlainTextComponentSerializer.plainText().serialize(message));
        }
    }

    private static Component prefixed(TextColor color, ComponentLike... parts) {
        return Component.text()
                .color(color)
                .append(Component.text(PREFIX, PREFIX_COLOR))
                .append(parts)
                .build();
    }

    private static Component line(String start, TextColor color, ComponentLike... parts) {
        return Component.text().content(start).color(color).append(parts).build();
    }
}
