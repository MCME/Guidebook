package com.mcmiddleearth.guidebook.data;

/**
 * The parts of an Area that its Area marker shows.
 * A real Area's Shape needs a loaded world, so this lets the Area marker tests use simple fake Areas instead.
 */
public interface MarkableArea extends AreaView {

    String getSubtitle();

    String getDescription();

    /** The name of the Area's world. */
    String getWorldName();

    AreaGeometry getGeometry();

    // The Shape follows the geometry, since a move can give an Area another Shape
    @Override
    default Shape getShape() {
        return getGeometry().shape();
    }
}
