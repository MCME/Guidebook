package com.mcmiddleearth.guidebook.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mcmiddleearth.guidebook.webmap.Marker;
import com.mcmiddleearth.guidebook.webmap.MarkerStyle;
import com.mcmiddleearth.guidebook.webmap.Outline;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AreaMarkersTest {

    private static InfoArea area(
            String name,
            String title,
            String subtitle,
            String description,
            boolean enabled,
            String worldName,
            AreaGeometry geometry) {
        return TestAreas.area(
                name, worldName, geometry, new AreaDetails(false, title, subtitle, false, description, enabled));
    }

    private static InfoArea sphere(String name) {
        return area(
                name, "Minas Tirith", "", "<guide>The city", true, "world", new AreaGeometry.Sphere(10, 64, 20, 30));
    }

    private static Marker markerOf(InfoArea area) {
        return AreaMarkers.toMarker(area, List.of(area));
    }

    @Test
    void theIdIsTheLowercaseAreaNamePrefixedWithGuidebook() {
        assertEquals(
                "guidebook.world-gondor-minas",
                markerOf(sphere("World-Gondor-Minas")).id());
    }

    @Test
    void aSphereIsACircleWithItsCentreAndRadiusInItsWorld() {
        InfoArea area =
                area("minas", "Minas Tirith", "", "", true, "middle_earth", new AreaGeometry.Sphere(-120, 70, 340, 45));

        Marker marker = markerOf(area);

        assertEquals("middle_earth", marker.world());
        assertEquals(new Outline.Circle(-120, 340, 45), marker.outline());
    }

    private static InfoArea shaped(AreaGeometry geometry) {
        return area("minas", "Minas Tirith", "", "", true, "world", geometry);
    }

    // A cuboid's max corner is a block inside it, so the outline reaches that block's far edge
    @Test
    void aCuboidIsARectangleCoveringAllOfItsBlocksWithItsHeights() {
        Marker marker = markerOf(shaped(new AreaGeometry.Cuboid(-10, 40, 20, 30, 90, 50)));

        assertEquals(
                new Outline.Polygon(
                        List.of(
                                new Outline.Point(-10, 20),
                                new Outline.Point(31, 20),
                                new Outline.Point(31, 51),
                                new Outline.Point(-10, 51)),
                        Optional.of(new Outline.YRange(40, 91))),
                marker.outline());
    }

    @Test
    void aPrismIsItsPolygonWithItsHeights() {
        Marker marker = markerOf(shaped(new AreaGeometry.Prism(List.of(0, 40, 25), List.of(-5, 10, 60), 12, 80)));

        assertEquals(
                new Outline.Polygon(
                        List.of(new Outline.Point(0, -5), new Outline.Point(40, 10), new Outline.Point(25, 60)),
                        Optional.of(new Outline.YRange(12, 81))),
                marker.outline());
    }

    private static InfoArea titled(String title, boolean enabled) {
        return area("world-gondor-minas", title, "", "", enabled, "world", new AreaGeometry.Sphere(0, 64, 0, 10));
    }

    @Test
    void theLabelIsTheTitleAsPlainText() {
        assertEquals(
                "Minas Tirith",
                markerOf(titled("<gold><bold>Minas</bold> Tirith", true)).label());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "<gold></gold>"})
    void anAreaWithNoVisibleTitleIsLabelledWithItsName(String title) {
        assertEquals("world-gondor-minas", markerOf(titled(title, true)).label());
    }

    @Test
    void aDisabledAreasLabelSaysSo() {
        assertEquals(
                "Minas Tirith (Disabled)",
                markerOf(titled("Minas Tirith", false)).label());
    }

    // Okabe–Ito blue and vermillion stay distinct for the common colour blindnesses
    @Test
    void anEnabledAreaIsBlue() {
        MarkerStyle style = markerOf(titled("Minas Tirith", true)).style();

        assertEquals(0x0072B2, style.lineRgb());
        assertEquals(0x0072B2, style.fillRgb());
    }

    @Test
    void aDisabledAreaIsVermillion() {
        MarkerStyle style = markerOf(titled("Minas Tirith", false)).style();

        assertEquals(0xD55E00, style.lineRgb());
        assertEquals(0xD55E00, style.fillRgb());
    }

    private static String popupOf(String title, String subtitle, String description) {
        return popupOf(title, subtitle, description, true);
    }

    private static String popupOf(String title, String subtitle, String description, boolean enabled) {
        InfoArea area = area(
                "world-gondor-minas",
                title,
                subtitle,
                description,
                enabled,
                "world",
                new AreaGeometry.Sphere(0, 64, 0, 10));
        return markerOf(area).popup();
    }

    // What a browser shows of the popup, one block or line break per line
    private static String visibleText(String html) {
        return html.replace("<br>", "\n")
                .replace("</div>", "\n")
                .replaceAll("<[^>]+>", "")
                .replaceAll("\n+", "\n")
                .strip()
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&amp;", "&");
    }

    @Test
    void thePopupShowsTheNameThenTheTitleAndSubtitleThenTheDescription() {
        String popup =
                popupOf("<gold>Minas Tirith", "<i>The Tower of Guard", "<guide>The <term:'a city'>great city</term>");

        assertEquals(
                "world-gondor-minas\nMinas Tirith — The Tower of Guard\nGuide: The great city", visibleText(popup));
    }

    @Test
    void aDisabledAreasPopupSaysSo() {
        assertEquals(
                "world-gondor-minas\nDisabled\nMinas Tirith — The Tower of Guard\nGuide:",
                visibleText(popupOf("Minas Tirith", "The Tower of Guard", "<guide>", false)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "<gold></gold>"})
    void aMissingTitleAndSubtitleAreSpelledOut(String missing) {
        assertEquals(
                "world-gondor-minas\nno Title — no Subtitle\nGuide:",
                visibleText(popupOf(missing, missing, "<guide>")));
    }

    @Test
    void aMissingDescriptionIsSpelledOut() {
        assertEquals(
                "world-gondor-minas\nMinas Tirith — no Subtitle\nno Description",
                visibleText(popupOf("Minas Tirith", "", " ")));
    }

    @Test
    void theDescriptionKeepsItsLineBreaks() {
        assertEquals(
                "world-gondor-minas\nMinas Tirith — no Subtitle\n" + "Guide: The city\n▸ The citadel\n▸ The gate",
                visibleText(
                        popupOf("Minas Tirith", "", "<guide>The city\n<bullet>The citadel<newline><bullet>The gate")));
    }

    @Test
    void theTextIsShownInItsColours() {
        String popup = popupOf("<gold>Minas Tirith", "", "<date>T.A. 1900</date>");

        assertTrue(popup.contains("<span style=\"color:#FFAA00\">Minas Tirith</span>"), popup);
        assertTrue(popup.contains("<span style=\"color:#FFFF55\">T.A. 1900</span>"), popup);
    }

    // Area text is escaped, so it can't break the Web map's page
    @Test
    void charactersSpecialToHtmlAreShownAsTheyAre() {
        String popup = popupOf("Fish & Chips", "", "a < b > c & d <script>");

        assertEquals("world-gondor-minas\nFish & Chips — no Subtitle\na < b > c & d <script>", visibleText(popup));
        assertFalse(popup.contains("<script>"), popup);
    }

    private static InfoArea placed(String name, String world, AreaGeometry geometry) {
        return area(name, "Title", "", "", true, world, geometry);
    }

    private static double fillOf(InfoArea area, InfoArea... drawn) {
        return AreaMarkers.toMarker(area, List.of(drawn)).style().fillOpacity();
    }

    private static final InfoArea CITY = placed("city", "world", new AreaGeometry.Cuboid(0, 0, 0, 100, 90, 100));

    private static final InfoArea TOWER = placed("tower", "world", new AreaGeometry.Cuboid(40, 60, 40, 50, 90, 50));

    // Unfilled, LiveAtlas draws only the outline, so the Area inside can be clicked
    @Test
    void anAreaHoldingAnotherIsUnfilledAndTheOneInsideIsFilled() {
        assertEquals(0, fillOf(CITY, CITY, TOWER));
        assertTrue(fillOf(TOWER, CITY, TOWER) > 0);
    }

    @Test
    void anUnfilledAreaKeepsItsColourAndHasAThickerLine() {
        MarkerStyle style = AreaMarkers.toMarker(CITY, List.of(CITY, TOWER)).style();

        assertEquals(0x0072B2, style.lineRgb());
        assertTrue(style.lineWeight() > markerOf(TOWER).style().lineWeight());
    }

    // Each still has a part of its own to click, so neither gives up its fill
    @Test
    void areasThatOnlyPartlyOverlapAreBothFilled() {
        InfoArea neighbour = placed("neighbour", "world", new AreaGeometry.Cuboid(50, 0, 50, 150, 90, 150));

        assertTrue(fillOf(CITY, CITY, neighbour) > 0);
        assertTrue(fillOf(neighbour, CITY, neighbour) > 0);
    }

    // Heights are ignored because the Web map is flat
    @Test
    void anAreaAboveOrBelowAnotherStillCountsAsInsideIt() {
        InfoArea cellar = placed("cellar", "world", new AreaGeometry.Cuboid(10, -40, 10, 20, -30, 20));

        assertEquals(0, fillOf(CITY, CITY, cellar));
    }

    @Test
    void anAreaInAnotherWorldDoesntCount() {
        InfoArea elsewhere = placed("elsewhere", "nether", new AreaGeometry.Cuboid(40, 60, 40, 50, 90, 50));

        assertTrue(fillOf(CITY, CITY, elsewhere) > 0);
    }

    @Test
    void aCircleAndAPrismHoldWhatIsInsideTheirBoxes() {
        InfoArea circle = placed("circle", "world", new AreaGeometry.Sphere(0, 64, 0, 30));
        InfoArea prism = placed("prism", "world", new AreaGeometry.Prism(List.of(0, 60, 30), List.of(0, 0, 60), 0, 90));
        InfoArea small = placed("small", "world", new AreaGeometry.Cuboid(10, 0, 10, 20, 90, 20));

        assertEquals(0, fillOf(circle, circle, small));
        assertEquals(0, fillOf(prism, prism, small));
    }

    // Otherwise each would leave the other unclickable, or neither would be filled
    @Test
    void ofTwoAreasWithTheSameBoxOnlyTheFirstByNameIsUnfilled() {
        InfoArea first = placed("Alpha", "world", new AreaGeometry.Cuboid(0, 0, 0, 10, 10, 10));
        InfoArea second = placed("beta", "world", new AreaGeometry.Sphere(5, 5, 5, 5));

        assertEquals(0, fillOf(first, first, second));
        assertTrue(fillOf(second, first, second) > 0);
    }

    // Unclamped, the radius would turn the sphere's box inside out, reaching back across the city
    @Test
    void aSphereWithANegativeRadiusIsHeldOnlyWhereItsCentreIs() {
        InfoArea broken = placed("broken", "world", new AreaGeometry.Sphere(200, 64, 50, -150));

        assertTrue(fillOf(CITY, CITY, broken) > 0);
    }
}
