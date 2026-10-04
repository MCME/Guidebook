package com.mcmiddleearth.guidebook.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * The rules for an Area's Title, Subtitle and Description, which are stored and typed as MiniMessage. Text is parsed
 * leniently: an unclosed tag carries on to the end of the text, and an unknown tag stays as literal text.
 */
public final class AreaText {

    /** The house opening: {@code Guide: } in dark aqua, then the text after it in white. */
    public static final String GUIDE_TAG = "<guide>";

    /** The opening a new Area's Description starts with. */
    public static final String DEFAULT_DESCRIPTION = GUIDE_TAG;

    // Guidebook's own tags, on top of the standard ones. They are permanent once stored text uses them (ADR 0004).
    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
            .tags(TagResolver.resolver(
                    TagResolver.standard(),
                    // The text inside the tag becomes the white part's children, up to </guide> or the end
                    TagResolver.resolver(
                            "guide",
                            Tag.inserting(Component.text()
                                    .color(NamedTextColor.WHITE)
                                    .append(Component.text("Guide: ", NamedTextColor.DARK_AQUA))
                                    .build())),
                    TagResolver.resolver("date", Tag.styling(NamedTextColor.YELLOW)),
                    TagResolver.resolver("term", AreaText::term),
                    TagResolver.resolver("wiki", AreaText::wiki),
                    TagResolver.resolver("warp", AreaText::warp),
                    TagResolver.resolver(
                            "bullet", Tag.selfClosingInserting(Component.text("▸ ", NamedTextColor.GRAY)))))
            .build();

    private static final String WIKI_URL = "https://tolkiengateway.net/wiki/";

    private static final Pattern WARP_NAME = Pattern.compile("[A-Za-z0-9_.-]+");

    private AreaText() {}

    // <term:'meaning'>
    private static Tag term(ArgumentQueue arguments, Context context) {
        String meaning = onlyArgument(arguments, context, "A term needs its meaning");
        return Tag.styling(
                NamedTextColor.GOLD,
                TextDecoration.UNDERLINED,
                HoverEvent.showText(Component.text(meaning, NamedTextColor.GRAY)));
    }

    // <wiki:page>, where the page is the title as written on Tolkien Gateway
    private static Tag wiki(ArgumentQueue arguments, Context context) {
        String page = onlyArgument(arguments, context, "A wiki link needs its page");
        // Namespaced pages and subpages, like Category:Cities, keep their : and / as Tolkien Gateway writes them
        String url = WIKI_URL
                + URLEncoder.encode(page.replace(' ', '_'), StandardCharsets.UTF_8)
                        .replace("%3A", ":")
                        .replace("%2F", "/");
        return Tag.styling(
                NamedTextColor.AQUA,
                TextDecoration.UNDERLINED,
                ClickEvent.openUrl(url),
                HoverEvent.showText(Component.text("Click to open Tolkien Gateway")));
    }

    // <warp:name>, run as the player's own /warp command
    private static Tag warp(ArgumentQueue arguments, Context context) {
        String name = onlyArgument(arguments, context, "A warp needs its name");
        if (!WARP_NAME.matcher(name).matches()) {
            throw context.newException("A warp name may contain only letters, digits and _ - .", arguments);
        }
        return Tag.styling(
                NamedTextColor.GREEN,
                TextDecoration.UNDERLINED,
                ClickEvent.runCommand("/warp " + name),
                HoverEvent.showText(Component.text("Click to warp to " + name)));
    }

    // A missing, empty or extra argument is a typo, so the tag stays as literal text
    private static String onlyArgument(ArgumentQueue arguments, Context context, String problem) {
        String argument = arguments.popOr(problem).value();
        if (argument.isEmpty() || arguments.hasNext()) {
            throw context.newException(problem, arguments);
        }
        return argument;
    }

    public static Component render(String miniMessage) {
        return MINI_MESSAGE.deserialize(miniMessage);
    }

    /** The number of characters a player sees, which is what the Title and Subtitle limits count. */
    public static int visibleLength(String miniMessage) {
        return visibleText(miniMessage).length();
    }

    /** Whether a player would see nothing, such as a Title that is only tags and spaces. */
    public static boolean isVisiblyBlank(String miniMessage) {
        return visibleText(miniMessage).isBlank();
    }

    private static String visibleText(String miniMessage) {
        return PlainTextComponentSerializer.plainText().serialize(render(miniMessage));
    }
}
