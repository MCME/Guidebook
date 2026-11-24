package com.mcmiddleearth.guidebook.data;

import com.mcmiddleearth.guidebook.webmap.Marker;
import com.mcmiddleearth.guidebook.webmap.MarkerLayer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** A Web map layer that keeps the markers it was given, by id, so tests can see what is drawn. */
final class RecordingMarkerLayer implements MarkerLayer {

    private final Map<String, Marker> markers = new LinkedHashMap<>();

    @Override
    public void put(Marker marker) {
        markers.put(marker.id(), marker);
    }

    @Override
    public void remove(String markerId) {
        markers.remove(markerId);
    }

    @Override
    public void clear() {
        markers.clear();
    }

    /** The marker drawn for the Area called {@code areaName}, if any. */
    Optional<Marker> markerOf(String areaName) {
        return Optional.ofNullable(markers.get(AreaMarkers.id(areaName)));
    }

    Set<String> ids() {
        return Set.copyOf(markers.keySet());
    }
}
