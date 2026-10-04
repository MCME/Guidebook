package com.mcmiddleearth.guidebook.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mcmiddleearth.guidebook.webmap.Marker;
import com.mcmiddleearth.guidebook.webmap.MarkerStyle;
import com.mcmiddleearth.guidebook.webmap.Outline;
import java.util.List;
import java.util.Optional;
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
        return AreaMarkers.toMarker(area);
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

    private static TestArea shaped(AreaGeometry geometry) {
        return new TestArea("minas", "Minas Tirith", "", "", true, "world", geometry);
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

    @Test
    void thePopupNamesACuboidsAndAPrismsShape() {
        Marker cuboid = markerOf(shaped(new AreaGeometry.Cuboid(0, 0, 0, 1, 1, 1)));
        Marker prism = markerOf(shaped(new AreaGeometry.Prism(List.of(0, 1, 1), List.of(0, 0, 1), 0, 1)));

        assertEquals("Shape: cuboid", cuboid.popup().lines().skip(1).findFirst().orElseThrow());
        assertEquals("Shape: prism", prism.popup().lines().skip(1).findFirst().orElseThrow());
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

    // The name is alone on its line so it can be copied straight into a command
    @Test
    void thePopupShowsTheNameThenTheShapeAndTextAsPlainText() {
        String popup =
                popupOf("<gold>Minas Tirith", "<i>The Tower of Guard", "<guide>The <term:'a city'>great city</term>");

        assertEquals(
                "world-gondor-minas\nShape: sphere\nTitle: Minas Tirith\nSubtitle: The Tower of Guard\n\n"
                        + "Guide: The great city",
                popup);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "<gold></gold>"})
    void aMissingTitleAndSubtitleAreShownAsNone(String missing) {
        assertEquals(
                "world-gondor-minas\nShape: sphere\nTitle: (none)\nSubtitle: (none)\n\nGuide: ",
                popupOf(missing, missing, "<guide>"));
    }

    @Test
    void aMissingDescriptionIsShownAsNone() {
        assertEquals(
                "world-gondor-minas\nShape: sphere\nTitle: Minas Tirith\nSubtitle: (none)\n\n(no Description)",
                popupOf("Minas Tirith", "", " "));
    }

    @Test
    void theDescriptionKeepsItsLineBreaks() {
        assertEquals(
                "world-gondor-minas\nShape: sphere\nTitle: Minas Tirith\nSubtitle: (none)\n\n"
                        + "Guide: The city\n▸ The citadel\n▸ The gate",
                popupOf("Minas Tirith", "", "<guide>The city\n<bullet>The citadel<newline><bullet>The gate"));
    }

    // Escaping for the Web map's own format is each Web map's job
    @Test
    void charactersSpecialToHtmlAreLeftAsTheyAre() {
        assertEquals(
                "world-gondor-minas\nShape: sphere\nTitle: Fish & Chips\nSubtitle: (none)\n\na < b > c & d",
                popupOf("Fish & Chips", "", "a < b > c & d"));
    }
}
