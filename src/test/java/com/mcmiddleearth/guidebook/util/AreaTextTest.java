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
}
