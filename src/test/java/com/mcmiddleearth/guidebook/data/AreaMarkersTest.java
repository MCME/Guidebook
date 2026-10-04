package com.mcmiddleearth.guidebook.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mcmiddleearth.guidebook.webmap.Marker;
import com.mcmiddleearth.guidebook.webmap.MarkerStyle;
import com.mcmiddleearth.guidebook.webmap.Outline;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AreaMarkersTest {

    record TestArea(
            String getName,
            String getTitle,
            String getSubtitle,
            String getDescription,
            boolean isEnabled,
            String getWorldName,
            AreaGeometry getGeometry)
            implements MarkableArea {

        @Override
        public Shape getShape() {
            return switch (getGeometry) {
                case AreaGeometry.Sphere sphere -> Shape.SPHERE;
                case AreaGeometry.Cuboid cuboid -> Shape.CUBOID;
                case AreaGeometry.Prism prism -> Shape.PRISM;
            };
        }
    }

    private static TestArea sphere(String name) {
        return new TestArea(
                name, "Minas Tirith", "", "<guide>The city", true, "world", new AreaGeometry.Sphere(10, 64, 20, 30));
    }

    private static Marker markerOf(TestArea area) {
        return AreaMarkers.toMarker(area).orElseThrow();
    }

    @Test
    void theIdIsTheLowercaseAreaNamePrefixedWithGuidebook() {
        assertEquals(
                "guidebook.world-gondor-minas",
                markerOf(sphere("World-Gondor-Minas")).id());
    }

    @Test
    void aSphereIsACircleWithItsCentreAndRadiusInItsWorld() {
        TestArea area = new TestArea(
                "minas", "Minas Tirith", "", "", true, "middle_earth", new AreaGeometry.Sphere(-120, 70, 340, 45));

        Marker marker = markerOf(area);

        assertEquals("middle_earth", marker.world());
        assertEquals(new Outline.Circle(-120, 340, 45), marker.outline());
    }

    private static TestArea titled(String title, boolean enabled) {
        return new TestArea(
                "world-gondor-minas", title, "", "", enabled, "world", new AreaGeometry.Sphere(0, 64, 0, 10));
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
        TestArea area = new TestArea(
                "world-gondor-minas",
                title,
                subtitle,
                description,
                true,
                "world",
                new AreaGeometry.Sphere(0, 64, 0, 10));
        return markerOf(area).popup();
    }

    @Test
    void thePopupShowsTheNameAndShapeThenTheTextAsPlainText() {
        String popup =
                popupOf("<gold>Minas Tirith", "<i>The Tower of Guard", "<guide>The <term:'a city'>great city</term>");

        assertEquals("world-gondor-minas (sphere)\nMinas Tirith\nThe Tower of Guard\n\nGuide: The great city", popup);
    }

    @Test
    void anEmptySubtitleIsLeftOutOfThePopup() {
        assertEquals("world-gondor-minas (sphere)\nMinas Tirith\n\nGuide: ", popupOf("Minas Tirith", "", "<guide>"));
    }

    @Test
    void anEmptyTitleIsLeftOutOfThePopup() {
        assertEquals(
                "world-gondor-minas (sphere)\nThe Tower of Guard\n\nGuide: ",
                popupOf(" ", "The Tower of Guard", "<guide>"));
    }

    @Test
    void theDescriptionKeepsItsLineBreaks() {
        assertEquals(
                "world-gondor-minas (sphere)\nMinas Tirith\n\nGuide: The city\n▸ The citadel\n▸ The gate",
                popupOf("Minas Tirith", "", "<guide>The city\n<bullet>The citadel<newline><bullet>The gate"));
    }

    // Escaping for the Web map's own format is each Web map's job
    @Test
    void charactersSpecialToHtmlAreLeftAsTheyAre() {
        assertEquals(
                "world-gondor-minas (sphere)\nFish & Chips\n\na < b > c & d",
                popupOf("Fish & Chips", "", "a < b > c & d"));
    }
}
