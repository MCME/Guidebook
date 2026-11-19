package com.mcmiddleearth.guidebook.welcome;

import org.bukkit.entity.Player;

/**
 * A way of showing a Welcome to a player, such as in chat or with Typewriter. It's only called for players who should
 * see the Welcome, so it shows it without checking anything itself.
 *
 * <p>It's called on the main thread. If a new Welcome arrives while the last one is still on screen, the Display decides
 * what happens. Register one with {@link DisplaySwitchboard#register} (ADR 0008).
 */
@FunctionalInterface
public interface Display {

    void show(Player player, Welcome welcome);
}
