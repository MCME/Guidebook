package com.mcmiddleearth.guidebook.webmap.dynmap;

import com.mcmiddleearth.guidebook.webmap.Marker;
import com.mcmiddleearth.guidebook.webmap.MarkerLayer;
import com.mcmiddleearth.guidebook.webmap.MarkerStyle;
import com.mcmiddleearth.guidebook.webmap.Outline;
import com.mcmiddleearth.guidebook.webmap.WebMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.CircleMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

/**
 * Draws on Dynmap. Each layer is a marker set. Markers aren't persistent, so Dynmap never keeps a marker its owner has
 * forgotten. Instead, every layer remembers its markers and draws them again whenever Dynmap's API (re)appears, such as
 * after {@code /dynmap reload}.
 */
public final class DynmapWebMap extends DynmapCommonAPIListener implements WebMap {

    // Dynmap's marker heights are ignored on its flat maps, so every marker sits at sea level
    private static final double FLAT_Y = 64;

    private final Map<String, Layer> layers = new LinkedHashMap<>();

    // Null while Dynmap is disabled or reloading
    private MarkerAPI markerApi;

    /**
     * Only call this when Dynmap is installed. It returns a plain {@link WebMap} so callers never name this class,
     * whose Dynmap superclass doesn't exist on servers without Dynmap.
     */
    public static WebMap create() {
        return new DynmapWebMap();
    }

    private DynmapWebMap() {
        // Calls apiEnabled straight away if Dynmap is already enabled
        DynmapCommonAPIListener.register(this);
    }

    @Override
    public void apiEnabled(DynmapCommonAPI api) {
        markerApi = api.getMarkerAPI();
        layers.values().forEach(Layer::drawAll);
    }

    @Override
    public void apiDisabled(DynmapCommonAPI api) {
        markerApi = null;
        layers.values().forEach(Layer::forget);
    }

    @Override
    public MarkerLayer layer(String id, String label, boolean hiddenByDefault) {
        return layers.computeIfAbsent(id, key -> {
            Layer layer = new Layer(id, label, hiddenByDefault);
            layer.drawAll();
            return layer;
        });
    }

    @Override
    public void close() {
        // The markers aren't persistent, so they go when the server stops without being deleted here
        DynmapCommonAPIListener.unregister(this);
    }

    private final class Layer implements MarkerLayer {

        private final String id;
        private final String label;
        private final boolean hiddenByDefault;
        private final Map<String, Marker> markers = new LinkedHashMap<>();

        // Null until Dynmap's API is available
        private MarkerSet markerSet;

        private Layer(String id, String label, boolean hiddenByDefault) {
            this.id = id;
            this.label = label;
            this.hiddenByDefault = hiddenByDefault;
        }

        @Override
        public void put(Marker marker) {
            markers.put(marker.id(), marker);
            if (markerSet != null) {
                draw(marker);
            }
        }

        @Override
        public void remove(String markerId) {
            markers.remove(markerId);
            if (markerSet != null) {
                erase(markerId);
            }
        }

        @Override
        public void clear() {
            if (markerSet != null) {
                markers.keySet().forEach(this::erase);
            }
            markers.clear();
        }

        private void drawAll() {
            if (markerApi == null) {
                return;
            }
            markerSet = markerApi.getMarkerSet(id);
            if (markerSet == null) {
                markerSet = markerApi.createMarkerSet(id, label, null, false);
            }
            markerSet.setHideByDefault(hiddenByDefault);
            markers.values().forEach(this::draw);
        }

        // Dynmap drops its marker sets when it's disabled, so they're looked up again when it comes back
        private void forget() {
            markerSet = null;
        }

        // A marker that changed outline type can't be updated in place, so it's always drawn afresh
        private void draw(Marker marker) {
            erase(marker.id());
            String popup = toHtml(marker.popup());
            MarkerStyle style = marker.style();
            switch (marker.outline()) {
                case Outline.Circle circle -> {
                    CircleMarker drawn = markerSet.createCircleMarker(
                            marker.id(),
                            marker.label(),
                            false,
                            marker.world(),
                            circle.x(),
                            FLAT_Y,
                            circle.z(),
                            circle.radius(),
                            circle.radius(),
                            false);
                    drawn.setDescription(popup);
                    drawn.setLineStyle(style.lineWeight(), 1, style.lineRgb());
                    drawn.setFillStyle(style.fillOpacity(), style.fillRgb());
                }
                case Outline.Polygon polygon -> {
                    AreaMarker drawn = markerSet.createAreaMarker(
                            marker.id(),
                            marker.label(),
                            false,
                            marker.world(),
                            polygon.points().stream()
                                    .mapToDouble(Outline.Point::x)
                                    .toArray(),
                            polygon.points().stream()
                                    .mapToDouble(Outline.Point::z)
                                    .toArray(),
                            false);
                    // A Y range would make LiveAtlas draw the polygon as a hollow 3D shape, which from above is only
                    // an outline that can't be hovered or clicked inside, so it's drawn flat instead
                    drawn.setRangeY(FLAT_Y, FLAT_Y);
                    drawn.setDescription(popup);
                    drawn.setLineStyle(style.lineWeight(), 1, style.lineRgb());
                    drawn.setFillStyle(style.fillOpacity(), style.fillRgb());
                }
            }
        }

        private void erase(String markerId) {
            CircleMarker circle = markerSet.findCircleMarker(markerId);
            if (circle != null) {
                circle.deleteMarker();
            }
            AreaMarker area = markerSet.findAreaMarker(markerId);
            if (area != null) {
                area.deleteMarker();
            }
        }
    }

    // Dynmap shows a description as HTML
    private static String toHtml(String plainText) {
        return plainText
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\n", "<br>");
    }
}
