package com.mcmiddleearth.guidebook.welcome;

import org.bukkit.entity.Player;

/**
 * A way of showing a Welcome to a player, such as in chat or with Typewriter. It's only called for players who should
 * see the Welcome, so it shows it without checking anything itself.
 *
 * <p>It's called on the main thread. If a new Welcome arrives while the last one is still on screen, the Display
 * decides what happens. Register one with {@link DisplaySwitchboard#register} (ADR 0008).
 */
@FunctionalInterface
public interface Display {

    /**
     * Shows {@code welcome} to {@code player}. Throwing {@link Unavailable} hands it to the Chat display quietly; any
     * other exception does too, but is logged as a failure.
     */
    void show(Player player, Welcome welcome);

    /**
     * A Display can't show a Welcome right now, for an expected reason such as the player being busy, so the Chat
     * display shows it instead. Expected in normal play, so it's not logged as a failure.
     */
    final class Unavailable extends RuntimeException {

        /** @param reason why, for the server log's fine-grained level */
        public Unavailable(String reason) {
            super(reason);
        }
    }
}
