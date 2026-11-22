package com.mcmiddleearth.guidebook.welcome;

import com.mcmiddleearth.guidebook.data.WorldPoint;

/** The parts of an Area that the Watchman needs. */
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
    boolean isInside(WorldPoint point);

    /** Whether a player already inside the Area is still in it here, which reaches a little beyond its Shape. */
    boolean isNear(WorldPoint point);
}
