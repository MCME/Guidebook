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

import com.mcmiddleearth.guidebook.command.GuidebookCommandExecutor;
import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.guidebook.listener.PlayerListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * @author Eriol_Eandur
 */
public class GuidebookPlugin extends JavaPlugin {

    private static final long INITIAL_DELAY_TICKS = 0L;
    private static final long INTERVAL_TICKS = 20L;

    private static GuidebookPlugin pluginInstance;

    public static GuidebookPlugin getPluginInstance() {
        return pluginInstance;
    }

    @Override
    public void onEnable() {
        pluginInstance = this;
        PluginData.loadData();
        this.initializePlayerMoveRunnable();
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getCommand("guidebook").setExecutor(new GuidebookCommandExecutor());
        getLogger().info("Enabled!");
    }

    @Override
    public void onDisable() {
        PluginData.disable();
    }

    public void initializePlayerMoveRunnable() {
        getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            List<InfoArea> enabledAreas = PluginData.getInfoAreas()
                .values()
                .stream()
                .filter(InfoArea::isEnable)
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
        }, INITIAL_DELAY_TICKS, INTERVAL_TICKS);
    }
}


