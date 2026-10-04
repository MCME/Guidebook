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
import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.guidebook.listener.PlayerListener;
import com.mcmiddleearth.guidebook.webmap.NoWebMap;
import com.mcmiddleearth.guidebook.webmap.WebMap;
import com.mcmiddleearth.guidebook.webmap.dynmap.DynmapWebMap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Eriol_Eandur
 */
public class GuidebookPlugin extends JavaPlugin {

    private static final long INITIAL_DELAY_TICKS = 0L;
    private static final long INTERVAL_TICKS = 20L;

    private static GuidebookPlugin pluginInstance;

    private WebMap webMap = new NoWebMap();

    public static GuidebookPlugin getPluginInstance() {
        return pluginInstance;
    }

    @Override
    public void onEnable() {
        pluginInstance = this;
        // Before loading, so loading draws the Areas once
        useWebMap();
        PluginData.loadData();

        this.initializePlayerMoveRunnable();
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);

        // Register the command with the description and aliases declared as annotations
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            GuidebookRootBrigadier.register(event.registrar());
        });

        getLogger().info("Enabled!");
    }

    @Override
    public void onDisable() {
        PluginData.disable();
        webMap.close();
    }

    private void useWebMap() {
        if (getServer().getPluginManager().isPluginEnabled("dynmap")) {
            webMap = DynmapWebMap.create();
            getLogger().info("Drawing Guidebook areas on Dynmap.");
        } else {
            getLogger().info("Dynmap isn't running, so Guidebook areas aren't drawn on a Web map.");
        }
        PluginData.useWebMap(webMap);
    }

    public void initializePlayerMoveRunnable() {
        getServer()
                .getScheduler()
                .scheduleSyncRepeatingTask(
                        this,
                        () -> {
                            List<InfoArea> enabledAreas = PluginData.getInfoAreas().values().stream()
                                    .filter(InfoArea::isEnabled)
                                    .toList();

                            for (Player player : Bukkit.getOnlinePlayers()) {
                                Location playerLocation = player.getLocation();

                                for (InfoArea area : enabledAreas) {
                                    boolean isInside = area.containsPlayer(player);

                                    if (!isInside) {
                                        if (area.isInside(playerLocation)) {
                                            area.onRegionEnter(player);
                                        }
                                        continue;
                                    }

                                    if (!area.isNear(playerLocation)) {
                                        area.onRegionLeave(player);
                                    }
                                }
                            }
                        },
                        INITIAL_DELAY_TICKS,
                        INTERVAL_TICKS);
    }
}
