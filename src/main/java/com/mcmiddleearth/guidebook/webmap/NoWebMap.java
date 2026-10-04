package com.mcmiddleearth.guidebook.webmap;

/** Used when the server has no Web map, so callers needn't check for one. */
public final class NoWebMap implements WebMap {

    private static final MarkerLayer NO_LAYER = new MarkerLayer() {
        @Override
        public void put(Marker marker) {}

        @Override
        public void remove(String markerId) {}

        @Override
        public void clear() {}
    };

    @Override
    public MarkerLayer layer(String id, String label, boolean hiddenByDefault) {
        return NO_LAYER;
    }

    @Override
    public void close() {}
}
