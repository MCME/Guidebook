package com.mcmiddleearth.guidebook.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * The rules for an Area's Title, Subtitle and Description, which are stored and typed as MiniMessage. Text is parsed
 * leniently: an unclosed tag carries on to the end of the text, and an unknown tag stays as literal text.
 */
public final class AreaText {

    /** The opening a new Area's Description starts with. */
    public static final String DEFAULT_DESCRIPTION = "<dark_aqua>Guide: <white>";

    private AreaText() {}

    public static Component render(String miniMessage) {
        return MiniMessage.miniMessage().deserialize(miniMessage);
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
