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
package com.mcmiddleearth.guidebook.data;

import com.mcmiddleearth.guidebook.GuidebookPlugin;
import com.mcmiddleearth.guidebook.util.DevUtil;
import com.mcmiddleearth.pluginutil.FileUtil;
import com.mcmiddleearth.pluginutil.message.MessageUtil;
import com.mcmiddleearth.pluginutil.region.CuboidRegion;
import com.mcmiddleearth.pluginutil.region.PrismoidRegion;
import com.mcmiddleearth.pluginutil.region.SphericalRegion;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 *
 * @author Eriol_Eandur
 */
public class PluginData {

    @Getter
    private static final MessageUtil messageUtil = new MessageUtil();
    
    @Getter
    private final static Map<String, InfoArea> infoAreas = new HashMap<>();
    
    private final static Set<UUID> excludedPlayers = new HashSet<>();
    
    @Getter
    private static final File dataFolder = GuidebookPlugin.getPluginInstance().getDataFolder();
    
    static {
        if(!GuidebookPlugin.getPluginInstance().getDataFolder().exists()) {
            GuidebookPlugin.getPluginInstance().getDataFolder().mkdirs();
        }
        messageUtil.setPluginName("Guidebook");
    }
    
    public static boolean isExcluded(Player player) {
        return excludedPlayers.contains(player.getUniqueId());
    }
    
    public static InfoArea addInfoArea(String name, InfoArea newArea) {
        return infoAreas.put(name, newArea);
    }
    
    public static InfoArea deleteInfoArea(String name) {
        InfoArea area = infoAreas.get(name);
        area.clearInformedPlayers();
        return infoAreas.remove(name);
    }
    
    public static InfoArea getInfoArea(String name) {
        return infoAreas.get(name);
    }
    
    public static boolean hasInfoArea(String name) {
        return infoAreas.get(name)!=null;
    }
    
    public static void include(Player player) {
        excludedPlayers.remove(player.getUniqueId());
    }
    
    public static void exclude(Player player) {
        excludedPlayers.add(player.getUniqueId());
    }
    
    public static void saveData() throws IOException {
        for(String areaName : infoAreas.keySet()) {
            DevUtil.log("SaveData "+areaName);
            FileConfiguration config = new YamlConfiguration();
            infoAreas.get(areaName).save(config);
            File worldFolder = new File(dataFolder,infoAreas.get(areaName).getLocation().getWorld().getName());
            if(!worldFolder.exists()) {
                worldFolder.mkdir();
            }
            File dataFile = new File(worldFolder, areaName+".yml");
            config.save(dataFile);    
        }
    }
    
    public static void loadData() {
        for(InfoArea area:infoAreas.values()) {
            area.clearInformedPlayers();
        }
        infoAreas.clear();
            File[] worldFolders = dataFolder.listFiles(FileUtil.getDirFilter());
            for(File folder: worldFolders) {
                File[] dataFiles = folder.listFiles(FileUtil.getFileExtFilter("yml"));
                for(File dataFile : dataFiles) {
                    String areaName = FileUtil.getShortName(dataFile);
                    DevUtil.log("Load area "+areaName);
                    FileConfiguration config = new YamlConfiguration();
                    try {
                        config.load(dataFile);
                        if(SphericalRegion.isValidConfig(config)) {
                            infoAreas.put(areaName, 
                                    new SphericalInfoArea(config));
                        }
                        else if(PrismoidRegion.isValidConfig(config)) {
                            infoAreas.put(areaName, 
                                    new PrismoidInfoArea(config));
                        }
                        else if(CuboidRegion.isValidConfig(config) || config.contains("xSize")) { // xSize is to notice old data format
                            infoAreas.put(areaName, 
                                    new CuboidInfoArea(config));
                        }
                    } catch (IOException | InvalidConfigurationException ex) {
                        Logger.getLogger(PluginData.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
        }
    }
    
    public static void disable() {
        for(InfoArea area: infoAreas.values()) {
            area.clearInformedPlayers();
        }
    }
}
