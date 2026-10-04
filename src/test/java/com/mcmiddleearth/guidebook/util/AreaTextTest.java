package com.mcmiddleearth.guidebook.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class AreaTextTest {

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void theDefaultDescriptionIsTheGuideOpening() {
        assertEquals("<guide>", AreaText.DEFAULT_DESCRIPTION);
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
    void aDateIsYellow() {
        Component rendered = AreaText.render("On <date>6 October T.A. 3018</date> Frodo");

        assertEquals(
                Component.text()
                        .append(Component.text("On "))
                        .append(Component.text("6 October T.A. 3018", NamedTextColor.YELLOW))
                        .append(Component.text(" Frodo"))
                        .build()
                        .compact(),
                rendered.compact());
    }

    @Test
    void theGuideOpeningIsDarkAquaAndTheTextAfterItWhite() {
        Component rendered = AreaText.render("<guide>You stand upon Weathertop.");

        assertEquals(
                StyledCharacters.of(Component.text()
                        .append(Component.text("Guide: ", NamedTextColor.DARK_AQUA))
                        .append(Component.text("You stand upon Weathertop.", NamedTextColor.WHITE))
                        .build()),
                StyledCharacters.of(rendered));
    }

    @Test
    void aColourAfterTheGuideOpeningOverridesTheWhite() {
        Component rendered = AreaText.render("<guide><gray>Ruins");

        assertEquals(
                StyledCharacters.of(Component.text()
                        .append(Component.text("Guide: ", NamedTextColor.DARK_AQUA))
                        .append(Component.text("Ruins", NamedTextColor.GRAY))
                        .build()),
                StyledCharacters.of(rendered));
    }

    @Test
    void aTermIsGoldAndUnderlinedAndHoverShowsItsMeaningInGrey() {
        Component rendered = AreaText.render("<term:'Hill of the Wind'>Amon Sûl</term>");

        assertEquals(
                Component.text("Amon Sûl", NamedTextColor.GOLD)
                        .decorate(TextDecoration.UNDERLINED)
                        .hoverEvent(HoverEvent.showText(Component.text("Hill of the Wind", NamedTextColor.GRAY))),
                rendered.compact());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "<term>Amon Sûl</term>",
                "<wiki>Weathertop</wiki>",
                "<term:''>Amon Sûl</term>",
                "<wiki:''>Weathertop</wiki>",
                "<term:'Hill':'Wind'>Amon Sûl</term>",
                "<wiki:Weathertop:Bree>Weathertop</wiki>"
            })
    void aTermOrWikiLinkWithAMissingOrExtraArgumentStaysAsLiteralText(String miniMessage) {
        assertEquals(miniMessage, plain(AreaText.render(miniMessage)));
    }

    @Test
    void aWikiLinkIsAquaAndUnderlinedAndOpensTolkienGateway() {
        Component rendered = AreaText.render("<wiki:Weathertop>Tolkien Gateway</wiki>");

        assertEquals(
                Component.text("Tolkien Gateway", NamedTextColor.AQUA)
                        .decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl("https://tolkiengateway.net/wiki/Weathertop"))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to open Tolkien Gateway"))),
                rendered.compact());
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "<wiki:Minas Tirith>|https://tolkiengateway.net/wiki/Minas_Tirith",
                "<wiki:'Minas Tirith'>|https://tolkiengateway.net/wiki/Minas_Tirith",
                "<wiki:Barad-dûr>|https://tolkiengateway.net/wiki/Barad-d%C3%BBr",
                "<wiki:'Bree?x=1&y'>|https://tolkiengateway.net/wiki/Bree%3Fx%3D1%26y",
                "<wiki:'Category:Cities'>|https://tolkiengateway.net/wiki/Category:Cities",
                "<wiki:'Gondor/History'>|https://tolkiengateway.net/wiki/Gondor/History"
            })
    void aWikiPageIsUrlEncodedWithSpacesAsUnderscores(String tag, String url) {
        Component rendered = AreaText.render(tag + "the page</wiki>");

        assertEquals(ClickEvent.openUrl(url), rendered.compact().clickEvent());
    }

    @Test
    void aWarpIsGreenAndUnderlinedAndRunsTheWarpCommand() {
        Component rendered = AreaText.render("<warp:helmsdeep>Helm's Deep</warp>");

        assertEquals(
                Component.text("Helm's Deep", NamedTextColor.GREEN)
                        .decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.runCommand("/warp helmsdeep"))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to warp to helmsdeep"))),
                rendered.compact());
    }

    @Test
    void aWarpNameMayContainLettersDigitsAndUnderscoresHyphensAndFullStops() {
        Component rendered = AreaText.render("<warp:Minas_Tirith-2.0>there</warp>");

        assertEquals(
                ClickEvent.runCommand("/warp Minas_Tirith-2.0"),
                rendered.compact().clickEvent());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "<warp:helms deep>there</warp>",
                "<warp:a/b>there</warp>",
                "<warp>there</warp>",
                "<warp:a:b>there</warp>"
            })
    void aWarpWithAnInvalidOrMissingNameStaysAsLiteralText(String miniMessage) {
        assertEquals(miniMessage, plain(AreaText.render(miniMessage)));
    }

    @Test
    void aBulletIsAGreyListMarkerBeforeTheText() {
        Component rendered = AreaText.render("<bullet>Elendil raised the tower.");

        assertEquals(
                Component.text()
                        .append(Component.text("▸ ", NamedTextColor.GRAY))
                        .append(Component.text("Elendil raised the tower."))
                        .build()
                        .compact(),
                rendered.compact());
    }

    @Test
    void visibleLengthCountsWhatGuidebookTagsRender() {
        assertEquals("▸ Bree".length(), AreaText.visibleLength("<bullet>Bree"));
        assertEquals("Guide: Bree".length(), AreaText.visibleLength("<guide>Bree"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"guide", "date", "term", "wiki", "warp", "bullet"})
    void noGuidebookTagShadowsAStandardTag(String name) {
        assertFalse(TagResolver.standard().has(name));
    }

    @Test
    void aClosingGuideTagEndsTheWhite() {
        Component rendered = AreaText.render("<gray>Ruins <guide>Weathertop</guide> remain");

        assertEquals(
                StyledCharacters.of(Component.text()
                        .append(Component.text("Ruins ", NamedTextColor.GRAY))
                        .append(Component.text("Guide: ", NamedTextColor.DARK_AQUA))
                        .append(Component.text("Weathertop", NamedTextColor.WHITE))
                        .append(Component.text(" remain", NamedTextColor.GRAY))
                        .build()),
                StyledCharacters.of(rendered));
    }
}
