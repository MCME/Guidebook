package com.mcmiddleearth.guidebook.data;

import com.mcmiddleearth.guidebook.text.AreaHtml;
import com.mcmiddleearth.guidebook.text.AreaText;
import com.mcmiddleearth.guidebook.webmap.Marker;
import com.mcmiddleearth.guidebook.webmap.MarkerStyle;
import com.mcmiddleearth.guidebook.webmap.Outline;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Turns an Area into its Area marker on the Web map. */
public final class AreaMarkers {

    /** Every Area marker is in one layer, hidden by default, placed in the debug layer group */
    public static final String LAYER_ID = "debug.guidebook";

    public static final String LAYER_LABEL = "Guidebook";

    // Okabe–Ito blue and vermillion, which stay distinct for the common colour blindnesses
    private static final int ENABLED_RGB = 0x0072B2;
    private static final int DISABLED_RGB = 0xD55E00;

    private static final MarkerStyle ENABLED_STYLE = new MarkerStyle(ENABLED_RGB, 2, ENABLED_RGB, 0.2);
    private static final MarkerStyle DISABLED_STYLE = new MarkerStyle(DISABLED_RGB, 2, DISABLED_RGB, 0.2);

    // With no fill, LiveAtlas draws only the outline, so the Areas inside it can be clicked. The outline is all of it
    // that can be hovered or clicked, and LiveAtlas counts a click within half the line's width, so the line is thick
    private static final MarkerStyle ENABLED_CONTAINER_STYLE = new MarkerStyle(ENABLED_RGB, 3, ENABLED_RGB, 0);
    private static final MarkerStyle DISABLED_CONTAINER_STYLE = new MarkerStyle(DISABLED_RGB, 3, DISABLED_RGB, 0);

    // The popup brings its own background, LiveAtlas's popup grey, so Area text in Minecraft's colours (made for a dark
    // chat box) stays readable on a Web map with light popups too. Unstyled text is white, as it is in game
    private static final String POPUP_CSS = "background:#222222;color:#ffffff;padding:6px 8px 6px 10px";
    private static final String MUTED_HEX = "#8a8a8a";

    private AreaMarkers() {}

    /**
     * @param drawn every Area drawn on the Web map, which may include {@code area}. An Area holding another is drawn
     *     unfilled, so the one inside can still be clicked
     * @return the Area's Area marker
     */
    public static Marker toMarker(InfoArea area, Collection<InfoArea> drawn) {
        return new Marker(
                id(area.getName()),
                area.getWorldName(),
                label(area),
                popup(area),
                outline(area.getGeometry()),
                style(area, holdsAnother(area, drawn)));
    }

    private static String label(InfoArea area) {
        // An older file can hold an empty Title, and a marker with no label can't be told apart on hover
        String title = AreaText.isVisiblyBlank(area.details().title())
                ? area.getName()
                : AreaText.plainText(area.details().title());
        // Said in words as well as colour, for staff who can't tell the colours apart
        return area.isEnabled() ? title : title + " (Disabled)";
    }

    /**
     * A compact card, since staff scan many Areas for problems and check their wording. The text is in the colours
     * players see, and has no labels: a label read the same as the text it labelled. Missing text is spelled out
     * instead, so a gap stands out.
     */
    private static String popup(InfoArea area) {
        // Nearly every Area is Enabled, so only a Disabled one says so, where it stands out
        String disabledMark = area.isEnabled()
                ? ""
                : "<div style=\"font-size:11px;font-weight:bold;text-transform:uppercase\">"
                        + styled("color:" + hex(DISABLED_RGB), "Disabled") + "</div>";
        // The name is a block of its own, so a triple-click selects just it to copy into a command
        String header = "<div style=\"display:flex;justify-content:space-between;align-items:baseline;gap:8px\">"
                + "<div style=\"font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;font-weight:bold\">"
                + AreaHtml.escape(area.getName()) + "</div>"
                + disabledMark
                + "</div>";
        AreaDetails details = area.details();
        String title = orMissing(details.title(), "no Title", "font-size:14px");
        String subtitle = orMissing(details.subtitle(), "no Subtitle", "");
        String titleLine = "<div style=\"margin:6px 0 8px\">" + title + " " + styled("color:" + MUTED_HEX, "—") + " "
                + subtitle + "</div>";
        String description = "<div>" + orMissing(details.description(), "no Description", "") + "</div>";
        String edgeHex = hex(area.isEnabled() ? ENABLED_RGB : DISABLED_RGB);
        return "<div style=\"" + POPUP_CSS + ";border-left:4px solid " + edgeHex + "\">" + header + titleLine
                + description + "</div>";
    }

    private static String orMissing(String miniMessage, String missing, String css) {
        if (AreaText.isVisiblyBlank(miniMessage)) {
            return styled(
                    "border:1px dashed " + MUTED_HEX + ";color:" + MUTED_HEX
                            + ";border-radius:4px;padding:0 5px;font-size:11px",
                    missing);
        }
        return AreaHtml.span(css, AreaHtml.of(miniMessage));
    }

    private static String styled(String css, String plainText) {
        return AreaHtml.span(css, AreaHtml.escape(plainText));
    }

    private static String hex(int rgb) {
        return String.format("#%06x", rgb);
    }

    private static MarkerStyle style(InfoArea area, boolean holdsAnother) {
        if (holdsAnother) {
            return area.isEnabled() ? ENABLED_CONTAINER_STYLE : DISABLED_CONTAINER_STYLE;
        }
        return area.isEnabled() ? ENABLED_STYLE : DISABLED_STYLE;
    }

    /**
     * Whether another Area's footprint box lies within this Area's. Boxes are looser than Shapes, so a circle or prism
     * can count as holding an Area just outside it, which only costs it its fill.
     */
    private static boolean holdsAnother(InfoArea area, Collection<InfoArea> drawn) {
        Footprint footprint = Footprint.of(area.getGeometry());
        return drawn.stream()
                .filter(other -> other.getWorldName().equals(area.getWorldName()))
                .filter(other -> !other.getName().equalsIgnoreCase(area.getName()))
                .anyMatch(other -> {
                    Footprint inner = Footprint.of(other.getGeometry());
                    // Of two Areas with the same box, one is left filled so it can be clicked anywhere inside
                    return footprint.equals(inner)
                            ? area.getName().compareToIgnoreCase(other.getName()) < 0
                            : footprint.holds(inner);
                });
    }

    /** The blocks a Shape's bounding box covers seen from above, both corners included. */
    private record Footprint(int minX, int minZ, int maxX, int maxZ) {

        static Footprint of(AreaGeometry geometry) {
            return switch (geometry) {
                case AreaGeometry.Sphere sphere -> {
                    // A hand-edited file can hold a negative radius, which would turn the box inside out
                    int radius = Math.max(sphere.radius(), 0);
                    yield new Footprint(
                            sphere.centerX() - radius,
                            sphere.centerZ() - radius,
                            sphere.centerX() + radius,
                            sphere.centerZ() + radius);
                }
                case AreaGeometry.Cuboid cuboid ->
                    new Footprint(cuboid.minX(), cuboid.minZ(), cuboid.maxX(), cuboid.maxZ());
                case AreaGeometry.Prism prism -> new Footprint(prism.minX(), prism.minZ(), prism.maxX(), prism.maxZ());
            };
        }

        boolean holds(Footprint inner) {
            return minX <= inner.minX && minZ <= inner.minZ && inner.maxX <= maxX && inner.maxZ <= maxZ;
        }
    }

    private static Outline outline(AreaGeometry geometry) {
        return switch (geometry) {
            case AreaGeometry.Sphere sphere ->
                // Seen from above, a sphere is a circle as wide as it is
                new Outline.Circle(sphere.centerX(), sphere.centerZ(), sphere.radius());
            case AreaGeometry.Cuboid cuboid -> {
                // The max corner is a block inside the cuboid, so the outline goes on to that block's far edge
                int farX = cuboid.maxX() + 1;
                int farZ = cuboid.maxZ() + 1;
                yield new Outline.Polygon(
                        List.of(
                                new Outline.Point(cuboid.minX(), cuboid.minZ()),
                                new Outline.Point(farX, cuboid.minZ()),
                                new Outline.Point(farX, farZ),
                                new Outline.Point(cuboid.minX(), farZ)),
                        heights(cuboid.minY(), cuboid.maxY()));
            }
            case AreaGeometry.Prism prism -> {
                List<Outline.Point> points = new ArrayList<>();
                for (int i = 0; i < prism.xs().size(); i++) {
                    points.add(new Outline.Point(prism.xs().get(i), prism.zs().get(i)));
                }
                yield new Outline.Polygon(points, heights(prism.minY(), prism.maxY()));
            }
        };
    }

    // The top layer of blocks is inside the Area, so the range goes on to its top face
    private static Optional<Outline.YRange> heights(int minY, int maxY) {
        return Optional.of(new Outline.YRange(minY, maxY + 1));
    }

    /** The id of an Area's marker. Area names are unique ignoring case, so the lowercase name is unique too. */
    public static String id(String areaName) {
        return "guidebook." + areaName.toLowerCase(Locale.ROOT);
    }
}
