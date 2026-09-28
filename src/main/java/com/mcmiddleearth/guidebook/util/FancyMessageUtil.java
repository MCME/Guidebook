package com.mcmiddleearth.guidebook.util;

import com.mcmiddleearth.pluginutil.message.FancyMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public final class FancyMessageUtil {

    private FancyMessageUtil() {}

    /**
     * Sends a {@link FancyMessage} as an Adventure component. {@link FancyMessage#send} writes {@code /tellraw} JSON
     * with the {@code clickEvent}/{@code hoverEvent} keys that Minecraft 1.21.5 renamed, so its click and hover
     * actions are silently dropped. Senders without a chat screen, such as the console, just get the text.
     */
    public static void send(CommandSender recipient, FancyMessage message) {
        recipient.sendMessage(toComponent(message));
    }

    // Each part of a FancyMessage is {text, click command, hover text, colour, format JSON}
    private static Component toComponent(FancyMessage message) {
        TextComponent.Builder line = Component.text();
        for (String[] part : message.getData()) {
            Component text = Component.text(part[0], color(part[3]));
            for (TextDecoration decoration : TextDecoration.values()) {
                text = text.decoration(decoration, decorationState(part[4], decoration));
            }
            if (part[1] != null) {
                text = text.clickEvent(clickEvent(message, part[1]));
            }
            if (part[2] != null) {
                text = text.hoverEvent(HoverEvent.showText(
                        LegacyComponentSerializer.legacySection().deserialize(part[2])));
            }
            line.append(text);
        }
        return line.build();
    }

    private static TextColor color(String color) {
        return color.startsWith("#") ? TextColor.fromHexString(color) : NamedTextColor.NAMES.value(color);
    }

    // The format JSON is e.g. `, "bold" : true`, or every decoration set false after a reset code
    private static TextDecoration.State decorationState(String format, TextDecoration decoration) {
        String key = "\"" + TextDecoration.NAMES.key(decoration) + "\" : ";
        if (format.contains(key + "true")) {
            return TextDecoration.State.TRUE;
        }
        if (format.contains(key + "false")) {
            return TextDecoration.State.FALSE;
        }
        return TextDecoration.State.NOT_SET;
    }

    private static ClickEvent clickEvent(FancyMessage message, String command) {
        if (message.isCopyToClipboard()) {
            return ClickEvent.copyToClipboard(command);
        }
        if (command.startsWith("http")) {
            return ClickEvent.openUrl(command);
        }
        return message.isRunDirect() ? ClickEvent.runCommand(command) : ClickEvent.suggestCommand(command);
    }
}
