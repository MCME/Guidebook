package com.mcmiddleearth.guidebook.data;

/** The parts of an Area that its Area marker shows, so an Area can be turned into a marker without a running server. */
public interface MarkableArea extends AreaView {

    String getSubtitle();

    String getDescription();

    /** The name of the Area's world, which must be loaded. */
    String getWorldName();

    AreaGeometry getGeometry();
}
