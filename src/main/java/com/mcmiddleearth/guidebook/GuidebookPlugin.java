/*
 * Copyright (C) 2016 MCME
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mcmiddleearth.guidebook;

import com.mcmiddleearth.guidebook.command.GuidebookRootBrigadier;
import com.mcmiddleearth.guidebook.data.AreaFolder;
import com.mcmiddleearth.guidebook.data.AreaMarkers;
import com.mcmiddleearth.guidebook.data.AreaStore;
import com.mcmiddleearth.guidebook.events.GuidebookSendEvent;
import com.mcmiddleearth.guidebook.webmap.NoWebMap;
import com.mcmiddleearth.guidebook.webmap.WebMap;
import com.mcmiddleearth.guidebook.webmap.dynmap.DynmapWebMap;
import com.mcmiddleearth.guidebook.welcome.ChatDisplay;
import com.mcmiddleearth.guidebook.welcome.ChosenDisplayNotice;
import com.mcmiddleearth.guidebook.welcome.DisplaySwitchboard;
import com.mcmiddleearth.guidebook.welcome.Watchman;
import com.mcmiddleearth.guidebook.welcome.WatchmanListener;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.time.InstantSource;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Eriol_Eandur
 */
public class GuidebookPlugin extends JavaPlugin {

    private static final long INITIAL_DELAY_TICKS = 0L;
    private static final long INTERVAL_TICKS = 20L;

    private static final String DISPLAY_KEY = "display";
    private static final String OPTED_OUT_KEY = "excludedPlayers";

    private static GuidebookPlugin pluginInstance;

    private WebMap webMap = new NoWebMap();

    private AreaStore areaStore;

    private DisplaySwitchboard displaySwitchboard;
    private Watchman watchman;

    public static GuidebookPlugin getPluginInstance() {
        return pluginInstance;
    }

    @Override
    public void onEnable() {
        pluginInstance = this;
        useWebMap();
        areaStore = new AreaStore(
                new AreaFolder(getDataFolder()),
                webMap.layer(AreaMarkers.LAYER_ID, AreaMarkers.LAYER_LABEL, true),
                getLogger());
        areaStore.reload();

        startWatchman();

        // Register the command with the description and aliases declared as annotations
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            GuidebookRootBrigadier.register(event.registrar());
        });

        getLogger().info("Enabled!");
    }

    @Override
    public void onDisable() {
        if (watchman != null) {
            watchman.close();
            getConfig()
                    .set(
                            OPTED_OUT_KEY,
                            watchman.optedOut().stream()
                                    .map(UUID::toString)
                                    .sorted()
                                    .toList());
            saveConfig();
        }
        webMap.close();
    }

    /** Where another plugin registers its own Display (ADR 0008). */
    public DisplaySwitchboard getDisplaySwitchboard() {
        return displaySwitchboard;
    }

    /** Holds every Area, keeping its file and its marker on the Web map up to date. */
    public AreaStore getAreaStore() {
        return areaStore;
    }

    /** Notices players entering Areas and gives them their Welcomes. */
    public Watchman getWatchman() {
        return watchman;
    }

    private void useWebMap() {
        if (getServer().getPluginManager().isPluginEnabled("dynmap")) {
            webMap = DynmapWebMap.create();
            getLogger().info("Drawing Guidebook areas on Dynmap.");
        } else {
            getLogger().info("Dynmap isn't running, so Guidebook areas aren't drawn on a Web map.");
        }
    }

    private void startWatchman() {
        // Written to config.yml on the next save, so staff can see where to choose another Display
        getConfig().addDefault(DISPLAY_KEY, DisplaySwitchboard.CHAT);
        getConfig().options().copyDefaults(true);
        displaySwitchboard = new DisplaySwitchboard(
                getConfig().getString(DISPLAY_KEY, DisplaySwitchboard.CHAT), new ChatDisplay(this), getLogger());

        List<UUID> optedOut = getConfig().getStringList(OPTED_OUT_KEY).stream()
                .map(UUID::fromString)
                .toList();
        watchman = new Watchman(
                displaySwitchboard,
                (player, areaName) -> {
                    GuidebookSendEvent event = new GuidebookSendEvent(player, areaName);
                    event.callEvent();
                    return event.isCancelled();
                },
                InstantSource.system(),
                (ticks, task) -> getServer().getScheduler().runTaskLater(this, task, ticks),
                optedOut);

        getServer().getPluginManager().registerEvents(new WatchmanListener(watchman), this);
        getServer().getPluginManager().registerEvents(new ChosenDisplayNotice(displaySwitchboard, this), this);
        getServer()
                .getScheduler()
                .scheduleSyncRepeatingTask(
                        this,
                        () -> watchman.tick(areaStore.all(), Bukkit.getOnlinePlayers()),
                        INITIAL_DELAY_TICKS,
                        INTERVAL_TICKS);
    }
}
