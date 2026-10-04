package com.mcmiddleearth.guidebook.util;

import java.util.Arrays;
import java.util.List;
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

    /**
     * A line starting with the {@code [Guidebook]} prefix, in the info colour. Each part is a {@code String} or a
     * {@link ComponentLike}, and {@link #stressed} parts are green.
     */
    public static Component info(Object... parts) {
        return prefixed(INFO, STRESSED, parts);
    }

    /** A line starting with the {@code [Guidebook]} prefix, in the error colour, with {@link #stressed} parts in dark red. */
    public static Component error(Object... parts) {
        return prefixed(ERROR, ERROR_STRESSED, parts);
    }

    public static void sendInfo(CommandSender recipient, Object... parts) {
        send(recipient, info(parts));
    }

    public static void sendError(CommandSender recipient, Object... parts) {
        send(recipient, error(parts));
    }

    /** Says that saving an Area failed, so the change to it was NOT {@code done}, e.g. "saved" or "renamed". */
    public static void sendNotDone(CommandSender recipient, String areaName, String done) {
        sendError(recipient, "There was an error. ", area(areaName), " was NOT " + done + ".");
    }

    /** A line indented to follow an {@link #info} line, in the info colour. */
    public static Component infoIndented(Object... parts) {
        return line(INDENT, INFO, STRESSED, parts);
    }

    /** A line slightly indented to follow an {@link #info} line, for long lines that would wrap under the full indent. */
    public static Component infoShortIndented(Object... parts) {
        return line(SHORT_INDENT, INFO, STRESSED, parts);
    }

    /**
     * Text in the stressed colour of the line it's a part of: green in an info line, dark red in an error line.
     * Anywhere else, such as inside a clickable component or a dialog, it's green.
     */
    public static Stressed stressed(String text) {
        return new Stressed("", text);
    }

    /** {@code Area <name>}, with the name {@link #stressed}. Names never need quotes, as they have no spaces. */
    public static Stressed area(String name) {
        return new Stressed("Area ", name);
    }

    /** {@code plain}, then {@code text} in the stressed colour of the line it's a part of. */
    public record Stressed(String plain, String text) implements ComponentLike {

        @Override
        public Component asComponent() {
            return render(STRESSED);
        }

        private Component render(TextColor color) {
            Component stressed = Component.text(text, color);
            return plain.isEmpty() ? stressed : Component.text(plain).append(stressed);
        }
    }

    /** Clicking the text fills in {@code command} in the chat box, and hovering shows {@code hover}. */
    public static Component suggestsCommand(ComponentLike text, String command, String hover) {
        return withHover(text.asComponent().clickEvent(ClickEvent.suggestCommand(command)), hover);
    }

    /** Clicking the text runs {@code command}, and hovering shows {@code hover}. */
    public static Component runsCommand(ComponentLike text, String command, String hover) {
        return withHover(text.asComponent().clickEvent(ClickEvent.runCommand(command)), hover);
    }

    /** Clicking the text opens {@code url}, and hovering shows {@code hover}. */
    public static Component opensUrl(ComponentLike text, String url, String hover) {
        return withHover(text.asComponent().clickEvent(ClickEvent.openUrl(url)), hover);
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

    private static Component prefixed(TextColor color, TextColor stressedColor, Object... parts) {
        return Component.text()
                .color(color)
                .append(Component.text(PREFIX, PREFIX_COLOR))
                .append(components(stressedColor, parts))
                .build();
    }

    private static Component line(String start, TextColor color, TextColor stressedColor, Object... parts) {
        return Component.text()
                .content(start)
                .color(color)
                .append(components(stressedColor, parts))
                .build();
    }

    private static List<Component> components(TextColor stressedColor, Object... parts) {
        return Arrays.stream(parts)
                .map(part -> switch (part) {
                    case String text -> Component.text(text);
                    case Stressed stressed -> stressed.render(stressedColor);
                    case ComponentLike component -> component.asComponent();
                    default ->
                        throw new IllegalArgumentException(
                                "A message part must be a String or a ComponentLike, not " + part.getClass());
                })
                .toList();
    }
}
