package com.mcmiddleearth.guidebook.welcome;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/** Tells the Watchman about players leaving the server. */
public final class WatchmanListener implements Listener {

    private final Watchman watchman;

    public WatchmanListener(Watchman watchman) {
        this.watchman = watchman;
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        watchman.quit(event.getPlayer());
    }
}
