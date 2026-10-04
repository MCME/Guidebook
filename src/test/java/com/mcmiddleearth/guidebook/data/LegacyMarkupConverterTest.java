package com.mcmiddleearth.guidebook.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mcmiddleearth.guidebook.util.AreaText;
import com.mcmiddleearth.guidebook.util.StyledCharacters;
import java.util.List;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class LegacyMarkupConverterTest {

    private static YamlConfiguration yaml(String text) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(text);
        } catch (InvalidConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
        return config;
    }

    /** An Area file holding a Description of these stored lines, written as YAML so quoting is realistic. */
    private static YamlConfiguration areaWithDescription(String... lines) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("title", "§6Edoras");
        config.set("description", List.of(lines));
        return yaml(config.saveToString());
    }

    private static String convertedDescription(String... lines) {
        YamlConfiguration area = areaWithDescription(lines);
        assertInstanceOf(LegacyMarkupConverter.Converted.class, LegacyMarkupConverter.convert(area));
        return area.getString("description");
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static Stream<List<String>> samples() {
        return Stream.of(
                List.of("§3Guide: §fWelcome to Edoras."),
                List.of("§3Guide: §fThe hall of §6§lMeduseld§f stands on the hill."),
                List.of("§cRed §lbold§r plain"),
                List.of("§3Guide: #ff8800orange text"),
                List.of("§3Guide: §fSee [Click=\"/warp edoras\"]§bthe warp[/Click] here."),
                List.of("§3Guide: §fRead [Click=\"https://mcme.co/edoras\"]the wiki[/Click]."),
                List.of("§3Guide: §f[Hover=\"The capital of Rohan\"]Edoras[/Hover] awaits."),
                List.of("[Hover=\"Click to warp\"][Click=\"/warp edoras\"]§aWarp[/Click][/Hover] now"),
                List.of("§3Guide: §ffirst line\\n", "\\n", "§esecond paragraph"),
                List.of("§3Guide: §f1 < 2 and <red> is not a tag"),
                List.of("§3Guide: §f"),
                List.of("§3Guide: §7Ruins"),
                List.of("§3Guide: §fThe tower.\\n", "§3Guide: §fThe hill."),
                List.of("§3Guide: §3§lWeathertop"),
                List.of("§lBold §3Guide: §fafter bold"),
                List.of("[Click=\"/warp edoras\"]§bwarp[/Click]§3Guide: §fafter a click"),
                List.of("§3Guide: §f[Click=\"/warp edoras\"]warp[/Click]§7"),
                List.of("Welcome to ", "Edoras."));
    }

    @Test
    void colourAndFormatCodesBecomeTags() {
        assertEquals(
                "<white>The hall of </white><bold><gold>Meduseld</gold></bold><white> stands.",
                convertedDescription("§fThe hall of §6§lMeduseld§f stands."));
    }

    @Test
    void aFormatCodeLastsOnlyUntilTheNextCode_asTheOldParserRenderedIt() {
        assertEquals("<red>Red <bold>bold</bold> plain", convertedDescription("§cRed §lbold§c plain"));
    }

    @Test
    void aResetCodeGoesBackToWhite() {
        assertEquals("<red>Red </red><white>plain", convertedDescription("§cRed §rplain"));
    }

    @Test
    void hexColoursBecomeHexTags() {
        assertEquals("<guide><#FF8800>orange", convertedDescription("§3Guide: #ff8800orange"));
    }

    @Test
    void aCommandClickRunsTheCommand() {
        assertEquals(
                "<white>See </white><aqua><click:run_command:'/warp edoras'>the warp</click></aqua><white> here.",
                convertedDescription("§fSee [Click=\"/warp edoras\"]§bthe warp[/Click] here."));
    }

    @Test
    void aUrlClickOpensTheUrl() {
        Component rendered =
                AreaText.render(convertedDescription("Read [Click=\"https://mcme.co/edoras\"]the wiki[/Click]."));

        assertTrue(rendered.toString()
                .contains(ClickEvent.openUrl("https://mcme.co/edoras").toString()));
    }

    @Test
    void hoverTextKeepsPluginUtilsWrappingAndHighlightColour() {
        String converted = convertedDescription("[Hover=\"The capital of Rohan\"]Edoras[/Hover] awaits.");

        assertEquals("<white><hover:show_text:'<yellow>The capital of Rohan'>Edoras</hover> awaits.", converted);
    }

    @Test
    void longHoverTextKeepsPluginUtilsLineBreaks() {
        String converted = convertedDescription(
                "[Hover=\"Edoras is the capital of Rohan and the seat of its kings in Meduseld\"]Edoras[/Hover]");
        Component hover =
                (Component) AreaText.render(converted).compact().hoverEvent().value();

        assertTrue(plain(hover).contains("\n"), plain(hover));
    }

    @Test
    void aColourCodeAfterAClickIsNotKept_soTypedTextDoesNotJoinTheClick() {
        String converted = convertedDescription("§3Guide: [Click=\"/warp\"]warp[/Click]§f");

        assertFalse(converted.endsWith("<white>"), converted);
    }

    @Test
    void aHoverAroundAClickKeepsBoth() {
        String converted =
                convertedDescription("[Hover=\"Click to warp\"][Click=\"/warp edoras\"]§aWarp[/Click][/Hover] now");
        Component warp = AreaText.render(converted).compact().children().getFirst();

        assertEquals(ClickEvent.runCommand("/warp edoras"), warp.clickEvent());
        assertEquals(HoverEvent.Action.SHOW_TEXT, warp.hoverEvent().action());
    }

    @Test
    void lineBreakMarkersBecomeRealLineBreaks_andStoredLinesAreJoinedWithASpace() {
        assertEquals("<white>first line\n \n second", convertedDescription("§ffirst line\\n", "\\n", "§fsecond"));
    }

    @Test
    void aLiteralAngleBracketIsEscaped() {
        assertEquals("<white>1 \\< 2 and \\<red>", convertedDescription("1 < 2 and <red>"));
    }

    @Test
    void theOldDefaultBecomesTheGuideOpening() {
        assertEquals(AreaText.DEFAULT_DESCRIPTION, convertedDescription("§3Guide: §f"));
    }

    @Test
    void theGuideOpeningBecomesTheGuideTag() {
        assertEquals("<guide>Welcome to Edoras.", convertedDescription("§3Guide: §fWelcome to Edoras."));
    }

    @Test
    void aColourOtherThanWhiteAfterTheGuideOpeningIsKept() {
        assertEquals("<guide><gray>Ruins of Amon Sûl", convertedDescription("§3Guide: §7Ruins of Amon Sûl"));
    }

    @Test
    void everyGuideOpeningBecomesTheGuideTag() {
        assertEquals(
                "<guide>The tower.\n <guide>The hill.",
                convertedDescription("§3Guide: §fThe tower.\\n", "§3Guide: §fThe hill."));
    }

    @Test
    void aGuideOpeningThatTheTextAfterItContinuesInDarkAquaIsKeptAsColour() {
        assertEquals("<dark_aqua>Guide: <bold>Weathertop", convertedDescription("§3Guide: §3§lWeathertop"));
    }

    @Test
    void titleAndSubtitleConvertFromColourCodes() {
        YamlConfiguration area = yaml("""
                title: §6Edoras
                subtitle: §7The §ogolden§r hall
                description: []
                """);

        LegacyMarkupConverter.convert(area);

        assertEquals("<gold>Edoras", area.getString("title"));
        assertEquals("<gray>The <italic>golden</italic></gray> hall", area.getString("subtitle"));
    }

    @Test
    void aSingleStringDescriptionIsOneLine() {
        YamlConfiguration area = yaml("""
                title: Edoras
                description: "§3Guide: §fWelcome"
                """);

        LegacyMarkupConverter.convert(area);

        assertEquals("<guide>Welcome", area.getString("description"));
    }

    @Test
    void aMissingDescriptionBecomesEmpty() {
        YamlConfiguration area = yaml("title: Edoras\n");

        LegacyMarkupConverter.convert(area);

        assertEquals("", area.getString("description"));
    }

    @Test
    void aMissingTitleAndSubtitleStayMissing() {
        YamlConfiguration area = yaml("description:\n- \"§3Guide: §fWelcome\"\n");

        assertInstanceOf(LegacyMarkupConverter.Converted.class, LegacyMarkupConverter.convert(area));

        assertFalse(area.contains("title"));
        assertFalse(area.contains("subtitle"));
        assertEquals("<guide>Welcome", area.getString("description"));
    }

    @Test
    void aConvertedSectionIsMarked() {
        YamlConfiguration area = areaWithDescription("§3Guide: §f");

        LegacyMarkupConverter.convert(area);

        assertTrue(LegacyMarkupConverter.isMiniMessage(area));
    }

    @Test
    void aMarkedSectionIsLeftUnchanged() {
        String stored = """
                title: <gold>Edoras
                description: "<dark_aqua>Guide: <white>§l is literal here"
                minimessage: true
                """;
        YamlConfiguration area = yaml(stored);

        assertInstanceOf(LegacyMarkupConverter.AlreadyMiniMessage.class, LegacyMarkupConverter.convert(area));

        assertEquals(yaml(stored).saveToString(), area.saveToString());
    }

    @Test
    void unparseableMarkupIsReportedAndTheSectionLeftUnchanged() {
        YamlConfiguration area = areaWithDescription("§3Guide: [Click=\"/warp\"][Click=\"/again\"]text");
        String before = area.saveToString();

        LegacyMarkupConverter.Result result = LegacyMarkupConverter.convert(area);

        LegacyMarkupConverter.Failed failed = assertInstanceOf(LegacyMarkupConverter.Failed.class, result);
        assertFalse(failed.problem().isBlank());
        assertEquals(before, area.saveToString());
        assertFalse(LegacyMarkupConverter.isMiniMessage(area));
    }

    @Test
    void aHashTooShortToBeAHexColourIsReported_ratherThanThrown() {
        YamlConfiguration area = areaWithDescription("§3Guide: we are #1");

        assertInstanceOf(LegacyMarkupConverter.Failed.class, LegacyMarkupConverter.convert(area));
    }

    @ParameterizedTest
    @MethodSource("samples")
    void theConvertedDescriptionShowsTheSamePlainTextAsTheOldRenderer(List<String> lines) throws Exception {
        assertEquals(
                plain(LegacyMarkupConverter.renderDescription(lines)),
                plain(AreaText.render(convertedDescription(lines.toArray(String[]::new)))));
    }

    @ParameterizedTest
    @MethodSource("samples")
    void theConvertedDescriptionShowsTheSameColoursFormatsAndActionsAsTheOldRenderer(List<String> lines)
            throws Exception {
        assertEquals(
                StyledCharacters.of(LegacyMarkupConverter.renderDescription(lines)),
                StyledCharacters.of(AreaText.render(convertedDescription(lines.toArray(String[]::new)))));
    }

    @ParameterizedTest
    @MethodSource("samples")
    void theConvertedDescriptionHasNoRedundantTags(List<String> lines) {
        String converted = convertedDescription(lines.toArray(String[]::new));

        assertFalse(converted.contains("<!"), converted);
        // <guide> isn't a colour, so it's written once per opening
        assertFalse(converted.matches("(?s).*<(?!guide>)(\\w+)>[^<]*<\\1>.*"), converted);
    }
}
