package com.mcmiddleearth.guidebook.data;

import com.mcmiddleearth.guidebook.util.AreaText;
import com.mcmiddleearth.guidebook.webmap.Marker;
import com.mcmiddleearth.guidebook.webmap.MarkerStyle;
import com.mcmiddleearth.guidebook.webmap.Outline;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Turns an Area into its Area marker on the Web map. */
public final class AreaMarkers {

    /** Every Area marker is in one layer, hidden by default because it's for staff. */
    public static final String LAYER_ID = "guidebook";

    public static final String LAYER_LABEL = "Guidebook";

    // Okabe–Ito blue and vermillion, which stay distinct for the common colour blindnesses
    private static final MarkerStyle ENABLED_STYLE = new MarkerStyle(0x0072B2, 2, 0x0072B2, 0.2);
    private static final MarkerStyle DISABLED_STYLE = new MarkerStyle(0xD55E00, 2, 0xD55E00, 0.2);

    private AreaMarkers() {}

    /** @return the Area's Area marker */
    public static Marker toMarker(MarkableArea area) {
        return new Marker(
                id(area.getName()),
                area.getWorldName(),
                label(area),
                popup(area),
                outline(area.getGeometry()),
                style(area));
    }

    private static String label(MarkableArea area) {
        // An older file can hold an empty Title, and a marker with no label can't be told apart on hover
        String title = AreaText.isVisiblyBlank(area.getTitle()) ? area.getName() : AreaText.plainText(area.getTitle());
        // Said in words as well as colour, for staff who can't tell the colours apart
        return area.isEnabled() ? title : title + " (Disabled)";
    }

    // <guide> is kept as "Guide: ", so staff can spot Descriptions missing the house opening
    private static String popup(MarkableArea area) {
        StringBuilder popup = new StringBuilder()
                .append(area.getName())
                .append(" (")
                .append(area.getShape().displayName())
                .append(")\n");
        // An empty Title or Subtitle would only leave a gap
        for (String line : List.of(area.getTitle(), area.getSubtitle())) {
            if (!AreaText.isVisiblyBlank(line)) {
                popup.append(AreaText.plainText(line)).append('\n');
            }
        }
        return popup.append('\n')
                .append(AreaText.plainText(area.getDescription()))
                .toString();
    }

    private static MarkerStyle style(MarkableArea area) {
        return area.isEnabled() ? ENABLED_STYLE : DISABLED_STYLE;
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
