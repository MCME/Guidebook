package com.mcmiddleearth.guidebook.welcome;

import org.bukkit.Location;

/** The parts of an Area that the Watchman needs, so the Watchman works without a running server. */
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
