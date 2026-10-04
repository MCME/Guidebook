package com.mcmiddleearth.guidebook.webmap;

/** A group of markers a viewer can turn on and off together. */
public interface MarkerLayer {

    /** Draws the marker, replacing any marker with the same id. */
    void put(Marker marker);

    void remove(String markerId);

    void clear();
}
