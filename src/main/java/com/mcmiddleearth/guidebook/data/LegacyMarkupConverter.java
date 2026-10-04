package com.mcmiddleearth.guidebook.data;

import com.mcmiddleearth.pluginutil.message.FancyMessage;
import com.mcmiddleearth.pluginutil.message.MessageType;
import com.mcmiddleearth.pluginutil.message.MessageUtil;
import com.mcmiddleearth.pluginutil.message.config.FancyMessageConfigUtil;
import com.mcmiddleearth.pluginutil.message.config.MessageParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Converts an Area file's Title, Subtitle and Description from Legacy markup to MiniMessage, keeping exactly what
 * players saw (ADR 0003). This is the only class that uses PluginUtils' Legacy markup parser.
 */
public final class LegacyMarkupConverter {

    /** The key marking an Area file whose text is MiniMessage. Without it, the text is Legacy markup. */
    public static final String MARKER = "minimessage";

    private static final String TITLE = "title";
    private static final String SUBTITLE = "subtitle";
    private static final String DESCRIPTION = "description";

    /** Stands for the house opening, {@code Guide: } in dark aqua and then white text (ADR 0004). */
    private static final String GUIDE_TAG = "<guide>";

    /** The two characters that mark a line break in a Legacy Description. */
    private static final String LINE_BREAK = "\\n";

    // Only its hover formatting is used, which doesn't depend on the plugin's name
    private static final MessageUtil MESSAGE_UTIL = new MessageUtil();

    private LegacyMarkupConverter() {}

    public sealed interface Result {}

    public record Converted() implements Result {}

    public record AlreadyMiniMessage() implements Result {}

    public record Failed(String problem) implements Result {}

    public static boolean isMiniMessage(ConfigurationSection area) {
        return area.getBoolean(MARKER);
    }

    /** Converts the section in place and marks it, unless it's already marked. A failure leaves it unchanged. */
    public static Result convert(ConfigurationSection area) {
        if (isMiniMessage(area)) {
            return new AlreadyMiniMessage();
        }
        String description;
        try {
            description = convertDescription(storedDescription(area));
        } catch (MessageParseException | RuntimeException e) {
            return new Failed(e.getMessage() != null ? e.getMessage() : e.toString());
        }
        convertColourCodes(area, TITLE);
        convertColourCodes(area, SUBTITLE);
        area.set(DESCRIPTION, description);
        area.set(MARKER, true);
        return new Converted();
    }

    /**
     * Renders a Legacy Description as players saw it. The stored lines are joined with a space, as the old parser
     * joined them, and the space it left at the end is trimmed.
     */
    static Component renderDescription(List<String> lines) throws MessageParseException {
        if (lines.isEmpty()) {
            return Component.empty();
        }
        FancyMessage message = FancyMessageConfigUtil.addFromStringList(
                        new FancyMessage(MessageType.WHITE, MESSAGE_UTIL), List.of(String.join(" ", lines)))
                .setRunDirect();
        List<String[]> parts = message.getData();
        if (!parts.isEmpty() && parts.getLast()[0].endsWith(" ")) {
            String[] last = parts.getLast();
            last[0] = last[0].substring(0, last[0].length() - 1);
        }
        return toComponent(message);
    }

    /**
     * Each old {@code §3Guide: } opening becomes the {@code <guide>} tag, so the house opening is stored one way in
     * every Area. The text between openings is written separately.
     */
    private static String convertDescription(List<String> lines) throws MessageParseException {
        List<Component> parts = renderDescription(lines).children();
        StringBuilder description = new StringBuilder();
        List<Component> section = new ArrayList<>();
        boolean afterGuide = false;
        for (int i = 0; i < parts.size(); i++) {
            Component part = parts.get(i);
            if (isGuideOpening(parts, i)) {
                description.append(convertParts(section)).append(GUIDE_TAG);
                section = new ArrayList<>();
                afterGuide = true;
            } else {
                // <guide> leaves white open, so white text after it needn't say so
                section.add(afterGuide && NamedTextColor.WHITE.equals(part.color()) ? part.color(null) : part);
            }
        }
        return description.append(convertParts(section)).toString();
    }

    private static String convertParts(List<Component> parts) {
        return serialize(cleanUp(groupByColour(parts))) + trailingColourTag(parts);
    }

    /**
     * Whether {@code parts[i]} is the old opening, {@code Guide: } in dark aqua and then a colour code, where swapping
     * it for {@code <guide>} looks the same. The tag makes the text after it white, and doesn't close a format, click
     * or hover the serialiser leaves open before it.
     */
    private static boolean isGuideOpening(List<Component> parts, int i) {
        Component part = parts.get(i);
        boolean opening = part instanceof TextComponent text
                && text.content().equals("Guide: ")
                && NamedTextColor.DARK_AQUA.equals(part.color())
                && isPlain(part);
        boolean colourChangesAfter = i + 1 == parts.size()
                || !NamedTextColor.DARK_AQUA.equals(parts.get(i + 1).color());
        boolean nothingOpenBefore = i == 0 || isPlain(parts.get(i - 1));
        return opening && colourChangesAfter && nothingOpenBefore;
    }

    private static boolean isPlain(Component part) {
        return part.clickEvent() == null
                && part.hoverEvent() == null
                && part.decorations().values().stream().noneMatch(state -> state == TextDecoration.State.TRUE);
    }

    // Every part carries its own colour, so neighbouring parts of one colour are grouped under it to write it once
    private static Component groupByColour(List<Component> parts) {
        TextComponent.Builder description = Component.text();
        List<Component> run = new ArrayList<>();
        TextColor runColour = null;
        for (Component part : parts) {
            if (!run.isEmpty() && !Objects.equals(part.color(), runColour)) {
                description.append(Component.text().color(runColour).append(run));
                run = new ArrayList<>();
            }
            runColour = part.color();
            run.add(part.color(null));
        }
        if (!run.isEmpty()) {
            description.append(Component.text().color(runColour).append(run));
        }
        return description.build();
    }

    /**
     * A colour code at the very end has no text, so serialising drops it. It's kept as a tag so that text typed after
     * it, such as after the old {@code §3Guide: §f} opening, still takes that colour.
     */
    private static String trailingColourTag(List<Component> parts) {
        if (parts.isEmpty()
                || !(parts.getLast() instanceof TextComponent last)
                || !last.content().isEmpty()) {
            return "";
        }
        TextColor colour = last.color();
        Component lastVisible = parts.reversed().stream()
                .filter(part ->
                        !(part instanceof TextComponent text) || !text.content().isEmpty())
                .findFirst()
                .orElse(Component.empty());
        // The serialiser leaves a click or hover open at the end, and typed text mustn't inherit it
        boolean endsInsideEvent = lastVisible.clickEvent() != null || lastVisible.hoverEvent() != null;
        if (colour == null || colour.equals(lastVisible.color()) || endsInsideEvent) {
            return "";
        }
        return colour instanceof NamedTextColor named ? "<" + named + ">" : "<" + colour.asHexString() + ">";
    }

    // An older file holds a single string, or no Description at all
    private static List<String> storedDescription(ConfigurationSection area) {
        if (area.isList(DESCRIPTION)) {
            return area.getStringList(DESCRIPTION);
        }
        String description = area.getString(DESCRIPTION);
        return description == null ? List.of() : List.of(description);
    }

    private static void convertColourCodes(ConfigurationSection area, String key) {
        String legacy = area.getString(key);
        if (legacy != null) {
            area.set(
                    key,
                    serialize(cleanUp(LegacyComponentSerializer.legacySection().deserialize(legacy))));
        }
    }

    private static String serialize(Component component) {
        return MiniMessage.miniMessage().serialize(component);
    }

    // The root of a chat message or title has no decorations, so a decoration set false looks the same as one not set
    private static Component cleanUp(Component component) {
        Component cleaned = component;
        for (TextDecoration decoration : TextDecoration.values()) {
            if (cleaned.decoration(decoration) == TextDecoration.State.FALSE) {
                cleaned = cleaned.decoration(decoration, TextDecoration.State.NOT_SET);
            }
        }
        return cleaned.children(cleaned.children().stream()
                        .map(LegacyMarkupConverter::cleanUp)
                        .toList())
                .compact();
    }

    // Each part of a FancyMessage is {text, click command, hover text, colour, format JSON}
    private static Component toComponent(FancyMessage message) {
        TextComponent.Builder line = Component.text();
        for (String[] part : message.getData()) {
            Component text = Component.text(lineBreaks(part[0]), color(part[3]));
            for (TextDecoration decoration : TextDecoration.values()) {
                text = text.decoration(decoration, decorationState(part[4], decoration));
            }
            if (part[1] != null) {
                text = text.clickEvent(clickEvent(part[1]));
            }
            if (part[2] != null) {
                text = text.hoverEvent(HoverEvent.showText(
                        LegacyComponentSerializer.legacySection().deserialize(lineBreaks(part[2]))));
            }
            line.append(text);
        }
        return line.build();
    }

    // FancyMessage wrote the text into /tellraw JSON unescaped, so the \n typed as a line break worked as one
    private static String lineBreaks(String text) {
        return text.replace(LINE_BREAK, "\n");
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

    // Welcomes have always run commands directly
    private static ClickEvent clickEvent(String command) {
        return command.startsWith("http") ? ClickEvent.openUrl(command) : ClickEvent.runCommand(command);
    }
}
