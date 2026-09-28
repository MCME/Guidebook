package com.mcmiddleearth.guidebook.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Builds Guidebook's chat messages as Adventure components, with the same prefix and colours as PluginUtils'
 * {@code MessageUtil}. Text passed in is never parsed as markup. Plain one-line messages still use {@code MessageUtil}.
 */
public final class GuidebookMessages {

    public static final TextColor INFO = NamedTextColor.AQUA;
    public static final TextColor STRESSED = NamedTextColor.GREEN;

    private static final String PLUGIN_NAME = "Guidebook";
    private static final String PREFIX = "[" + PLUGIN_NAME + "] ";
    // MessageUtil indents by the width of the prefix's name and brackets
    private static final String INDENT = " ".repeat(PLUGIN_NAME.length() + 2);

    private GuidebookMessages() {}

    /** A line starting with the {@code [Guidebook]} prefix, in the info colour. */
    public static Component info(ComponentLike... parts) {
        return line(PREFIX, INFO, parts);
    }

    /** A line indented to follow an {@link #info} line, in the info colour. */
    public static Component infoIndented(ComponentLike... parts) {
        return line(INDENT, INFO, parts);
    }

    public static Component stressed(String text) {
        return Component.text(text, STRESSED);
    }

    /** A Title, whose formatting is the {@code §} colour codes stored when a {@code #} is typed into it. */
    public static Component title(String title) {
        return LegacyComponentSerializer.legacySection().deserialize(title);
    }

    /** Clicking the text fills in {@code command} in the chat box, and hovering shows {@code hover}. */
    public static Component suggestsCommand(Component text, String command, String hover) {
        return withHover(text.clickEvent(ClickEvent.suggestCommand(command)), hover);
    }

    public static Component withHover(Component text, String hover) {
        return text.hoverEvent(HoverEvent.showText(Component.text(hover)));
    }

    /** Players get the component; senders without a chat screen, such as the console, get its plain text. */
    public static void send(CommandSender recipient, Component message) {
        if (recipient instanceof Player) {
            recipient.sendMessage(message);
        } else {
            recipient.sendMessage(PlainTextComponentSerializer.plainText().serialize(message));
        }
    }

    private static Component line(String start, TextColor color, ComponentLike... parts) {
        return Component.text().content(start).color(color).append(parts).build();
    }
}
