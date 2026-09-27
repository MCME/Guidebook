package com.mcmiddleearth.guidebook.data;

/**
 * The parts of an Area that the {@link AreaRegistry} needs, so the registry works without a running server.
 */
public interface AreaView {

    String getName();

    String getTitle();

    boolean isEnabled();
}
