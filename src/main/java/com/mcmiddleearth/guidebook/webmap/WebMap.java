package com.mcmiddleearth.guidebook.webmap;

/**
 * A browser map of the server's worlds, such as Dynmap, that plugins draw markers on. It knows nothing about any
 * plugin's own concepts, so it can move into a shared library (ADR 0005).
 */
public interface WebMap {

    /** The layer with this id, created on first use. Markers are grouped into layers a viewer can turn on and off. */
    MarkerLayer layer(String id, String label, boolean hiddenByDefault);

    /** Stops listening to the map, so a plugin that has stopped is never called back. */
    void close();
}
