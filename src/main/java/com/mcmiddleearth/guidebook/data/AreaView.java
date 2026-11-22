package com.mcmiddleearth.guidebook.data;

/**
 * The parts of an Area that the {@link AreaRegistry} needs.
 * A real Area's Shape needs a loaded world, so this lets the registry's tests use simple fake Areas instead.
 */
public interface AreaView {

    String getName();

    String getTitle();

    boolean isEnabled();

    Shape getShape();
}
