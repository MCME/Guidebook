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

    /** The opening a new Area's Description starts with. */
    public static final String DEFAULT_DESCRIPTION = "<guide>";

    // Guidebook's own tags, on top of the standard ones. They are permanent once stored text uses them (ADR 0004).
    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
            .tags(TagResolver.resolver(
                    TagResolver.standard(),
                    // The house opening. The white is left open, so it colours the rest of the text.
                    TagResolver.resolver("guide", Tag.preProcessParsed("<dark_aqua>Guide: </dark_aqua><white>")),
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
        String meaning = arguments.popOr("A term needs its meaning").value();
        return Tag.styling(
                NamedTextColor.GOLD,
                TextDecoration.UNDERLINED,
                HoverEvent.showText(Component.text(meaning, NamedTextColor.GRAY)));
    }

    // <wiki:page>, where the page is the title as written on Tolkien Gateway
    private static Tag wiki(ArgumentQueue arguments, Context context) {
        String page = arguments.popOr("A wiki link needs its page").value();
        String url = WIKI_URL + URLEncoder.encode(page.replace(' ', '_'), StandardCharsets.UTF_8);
        return Tag.styling(
                NamedTextColor.AQUA,
                TextDecoration.UNDERLINED,
                ClickEvent.openUrl(url),
                HoverEvent.showText(Component.text("Click to open Tolkien Gateway")));
    }

    // <warp:name>, run as the player's own /warp command
    private static Tag warp(ArgumentQueue arguments, Context context) {
        String name = arguments.popOr("A warp needs its name").value();
        if (!WARP_NAME.matcher(name).matches()) {
            throw context.newException("A warp name may contain only letters, digits and _ - .", arguments);
        }
        return Tag.styling(
                NamedTextColor.GREEN,
                TextDecoration.UNDERLINED,
                ClickEvent.runCommand("/warp " + name),
                HoverEvent.showText(Component.text("Click to warp to " + name)));
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
