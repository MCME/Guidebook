package com.mcmiddleearth.guidebook.welcome;

import org.bukkit.Location;

/**
 * The parts of an Area that the Watchman needs.
 * A real Area's Shape needs a loaded world, so this lets the Watchman's tests use simple fake Areas instead.
 */
public interface WelcomeArea {

    String getName();

    String getTitle();

    String getSubtitle();

    String getDescription();

    boolean isShowTitle();

    /** Whether the Area shows its Boss bar. */
    boolean isShowBossBar();

    boolean isEnabled();

    /** Whether a player here enters the Area. */
    boolean isInside(Location location);

    /** Whether a player already inside the Area is still in it here, which reaches a little beyond its Shape. */
    boolean isNear(Location location);
}
