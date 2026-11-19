package com.mcmiddleearth.guidebook.welcome;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerLoadEvent;

/** Tells the Watchman about players leaving the server, and checks the chosen Display once every plugin has enabled. */
public final class WatchmanListener implements Listener {

    private final Watchman watchman;
    private final DisplaySwitchboard switchboard;

    public WatchmanListener(Watchman watchman, DisplaySwitchboard switchboard) {
        this.watchman = watchman;
        this.switchboard = switchboard;
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        watchman.quit(event.getPlayer());
    }

    @EventHandler
    public void serverLoaded(ServerLoadEvent event) {
        // A reload fires this again, by which time a Display's plugin has had the chance to register again
        switchboard.warnIfChosenIsMissing();
    }
}
