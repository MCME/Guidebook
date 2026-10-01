package com.mcmiddleearth.guidebook.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AreaTextTest {

    private static final String WEBUI_BASE = "https://webui.advntr.dev/?mode=chat_closed&bg=grass&input=";
    private static final String ENCODED_OPENING = "%3Cdark_aqua%3EGuide%3A%20%3Cwhite%3E";

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void theDefaultDescriptionIsTheGuideOpening() {
        assertEquals("<dark_aqua>Guide: <white>", AreaText.DEFAULT_DESCRIPTION);
    }

    @Test
    void rendersTagsAsFormatting() {
        Component rendered = AreaText.render("<gold>Edoras");

        assertEquals("Edoras", plain(rendered));
        assertEquals(Component.text("Edoras", NamedTextColor.GOLD), rendered.compact());
    }

    @Test
    void anUnclosedTagRendersToTheEndOfTheText() {
        Component rendered = AreaText.render("<dark_aqua>Guide: <white>Welcome to Edoras");

        assertEquals("Guide: Welcome to Edoras", plain(rendered));
    }

    @Test
    void anUnknownTagStaysAsLiteralText() {
        assertEquals("<golf>Edoras", plain(AreaText.render("<golf>Edoras")));
    }

    @Test
    void anEmptyStringRendersEmpty() {
        assertEquals("", plain(AreaText.render("")));
    }

    @Test
    void visibleLengthIgnoresTags() {
        assertEquals("Minas Tirith".length(), AreaText.visibleLength("<gold><bold>Minas</bold> <#ff8800>Tirith"));
    }

    @Test
    void visibleLengthCountsAnEscapedAngleBracketAsOneCharacter() {
        assertEquals("a <b".length(), AreaText.visibleLength("a \\<b"));
    }

    @Test
    void visibleLengthCountsAnUnknownTagAsText() {
        assertEquals("<golf>".length(), AreaText.visibleLength("<golf>"));
    }

    @Test
    void visibleLengthCountsEachLineBreakAsOneCharacter() {
        assertEquals("first\nsecond".length(), AreaText.visibleLength("<red>first\n<blue>second"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "<red></red>", "<gold> <bold>\n"})
    void textWithNothingVisibleIsBlank(String miniMessage) {
        assertTrue(AreaText.isVisiblyBlank(miniMessage));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Edoras", "<red>Edoras", "<golf>", "\\<"})
    void textWithSomethingVisibleIsNotBlank(String miniMessage) {
        assertFalse(AreaText.isVisiblyBlank(miniMessage));
    }

    @Test
    void theWebUiLinkCarriesTheEncodedDescription() {
        AreaText.WebUiLink link = AreaText.webUiLink("<gold>Hi & bye\n#1 <white>100%");

        assertEquals(WEBUI_BASE + "%3Cgold%3EHi%20%26%20bye%0A%231%20%3Cwhite%3E100%25", link.url());
        assertFalse(link.isDescriptionTooLong());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  \n "})
    void theWebUiLinkForAnEmptyDescriptionOpensWithTheGuideOpening(String description) {
        AreaText.WebUiLink link = AreaText.webUiLink(description);

        assertEquals(WEBUI_BASE + ENCODED_OPENING, link.url());
        assertFalse(link.isDescriptionTooLong());
    }

    @Test
    void aWebUiLinkAtTheLengthLimitIsKept() {
        String description = "a".repeat(AreaText.MAX_WEBUI_URL_LENGTH - WEBUI_BASE.length());

        AreaText.WebUiLink link = AreaText.webUiLink(description);

        assertEquals(WEBUI_BASE + description, link.url());
        assertFalse(link.isDescriptionTooLong());
    }

    @Test
    void aWebUiLinkOverTheLengthLimitFallsBackToTheGuideOpening() {
        String description = "a".repeat(AreaText.MAX_WEBUI_URL_LENGTH - WEBUI_BASE.length() + 1);

        AreaText.WebUiLink link = AreaText.webUiLink(description);

        assertEquals(WEBUI_BASE + ENCODED_OPENING, link.url());
        assertTrue(link.isDescriptionTooLong());
    }

    @Test
    void theLengthLimitCountsTheEncodedDescription() {
        // Each "<" encodes as three characters, so this fits raw but not encoded
        String description = "<".repeat((AreaText.MAX_WEBUI_URL_LENGTH - WEBUI_BASE.length()) / 2);

        assertTrue(AreaText.webUiLink(description).isDescriptionTooLong());
    }
}
