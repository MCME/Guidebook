package com.mcmiddleearth.guidebook.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    /** The longest WebUI link made. The site was measured to reject URLs somewhere between 4000 and 5000 characters. */
    public static final int MAX_WEBUI_URL_LENGTH = 3500;

    private static final String WEBUI_BASE = "https://webui.advntr.dev/?mode=chat_closed&bg=grass&input=";

    private AreaText() {}

    /** A link that opens the Adventure WebUI pre-filled with a Description. */
    public record WebUiLink(String url, boolean isDescriptionTooLong) {}

    public static Component render(String miniMessage) {
        return MiniMessage.miniMessage().deserialize(miniMessage);
    }

    /** The number of characters a player sees, which is what the Title and Subtitle limits count. */
    public static int visibleLength(String miniMessage) {
        return PlainTextComponentSerializer.plainText()
                .serialize(render(miniMessage))
                .length();
    }

    /**
     * A blank Description opens with {@link #DEFAULT_DESCRIPTION}. So does one too long to fit in a link, and then
     * {@link WebUiLink#isDescriptionTooLong()} is true so the caller can explain.
     */
    public static WebUiLink webUiLink(String description) {
        if (description.isBlank()) {
            return new WebUiLink(webUiUrl(DEFAULT_DESCRIPTION), false);
        }
        String url = webUiUrl(description);
        if (url.length() > MAX_WEBUI_URL_LENGTH) {
            return new WebUiLink(webUiUrl(DEFAULT_DESCRIPTION), true);
        }
        return new WebUiLink(url, false);
    }

    // URLEncoder writes spaces as "+", which only form decoding reads back as a space
    private static String webUiUrl(String description) {
        return WEBUI_BASE
                + URLEncoder.encode(description, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
