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
import com.mcmiddleearth.pluginutil.region.Region;
import com.mcmiddleearth.pluginutil.region.SphericalRegion;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * @author Eriol_Eandur
 */
public class PluginData {

    private static final MessageUtil messageUtil = new MessageUtil();

    private static final Map<String, InfoArea> infoAreas = new HashMap<>();

    private static final AreaRegistry<InfoArea> registry = new AreaRegistry<>(infoAreas.values());

    private static Set<UUID> excludedPlayers = new HashSet<>();

    private static final File dataFolder = GuidebookPlugin.getPluginInstance().getDataFolder();

    static {
        if (!GuidebookPlugin.getPluginInstance().getDataFolder().exists()) {
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

    public static boolean deleteInfoArea(InfoArea area) {
        area.clearPlayers();
        boolean result = getDataFile(area, area.getName()).delete();
        if (result) {
            infoAreas.remove(area.getName());
        }
        return result;
    }

    /**
     * @return false if the Area was renamed but its old data file couldn't be deleted, so it would load under both
     *     names after a reload
     */
    public static boolean renameInfoArea(InfoArea area, String newName) throws IOException {
        String oldName = area.getName();
        // Found before the rename, while it still points at the old name's file
        File oldDataFile = getDataFile(area, oldName);

        infoAreas.remove(oldName);
        area.setName(newName);
        infoAreas.put(newName, area);
        try {
            saveArea(area);
        } catch (IOException ex) {
            infoAreas.remove(newName);
            area.setName(oldName);
            infoAreas.put(oldName, area);
            throw ex;
        }
        return oldDataFile.delete();
    }

    /**
     * @return false if the Area was moved to another world but its old world's data file couldn't be deleted, so it
     *     would load twice after a reload
     */
    public static boolean moveInfoArea(InfoArea area, Region region) throws IOException {
        Region oldRegion = area.getRegion();
        // Found before the move: the world folder comes from the region
        File oldDataFile = getDataFile(area, area.getName());

        area.setRegion(region);
        try {
            saveArea(area);
        } catch (IOException ex) {
            area.setRegion(oldRegion);
            throw ex;
        }
        return oldDataFile.equals(getDataFile(area, area.getName())) || oldDataFile.delete();
    }

    /**
     * @return the Area the typed name reaches (ignoring case, exact case wins), or null if there is none
     */
    public static InfoArea getInfoArea(String name) {
        return registry.resolve(name).orElse(null);
    }

    /**
     * @return the Area with exactly this name, or null if there is none
     */
    public static InfoArea getInfoAreaExact(String name) {
        return registry.resolveExact(name).orElse(null);
    }

    /**
     * @return why the name can't be given to a new or renamed Area, or empty if it can
     */
    public static Optional<String> newAreaNameProblem(String name) {
        return registry.newNameProblem(name);
    }

    public static void include(Player player) {
        excludedPlayers.remove(player.getUniqueId());
    }

    public static void exclude(Player player) {
        excludedPlayers.add(player.getUniqueId());
    }

    public static void saveExcluded() throws IOException {
        FileConfiguration config = GuidebookPlugin.getPluginInstance().getConfig();
        ArrayList<String> tempList = new ArrayList<>();
        for (UUID id : excludedPlayers) {
            tempList.add(id.toString());
        }
        config.set("excludedPlayers", tempList);
        GuidebookPlugin.getPluginInstance().saveConfig();
        Logger.getGlobal().info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    }

    public static void saveArea(InfoArea area) throws IOException {
        final String areaName = area.getName();
        DevUtil.log("SaveData " + areaName);

        FileConfiguration config = new YamlConfiguration();
        area.save(config);

        File dataFile = getDataFile(area, areaName);
        dataFile.getParentFile().mkdir();
        config.save(dataFile);
    }

    public static void loadData() {
        FileConfiguration config = GuidebookPlugin.getPluginInstance().getConfig();
        excludedPlayers = new HashSet<UUID>();
        List<String> tempList = new ArrayList<>();
        tempList = config.getStringList("excludedPlayers");
        for (String id : tempList) {
            excludedPlayers.add(UUID.fromString(id));
        }
        for (InfoArea area : infoAreas.values()) {
            area.clearPlayers();
        }
        infoAreas.clear();
        File[] worldFolders = dataFolder.listFiles(FileUtil.getDirFilter());
        for (File folder : worldFolders) {
            File[] dataFiles = folder.listFiles(FileUtil.getFileExtFilter("yml"));
            for (File dataFile : dataFiles) {
                String areaName = FileUtil.getShortName(dataFile);
                DevUtil.log("Load area " + areaName);
                config = new YamlConfiguration();
                try {
                    config.load(dataFile);
                    if (SphericalRegion.isValidConfig(config)) {
                        addInfoArea(areaName, new SphericalInfoArea(areaName, config));
                    } else if (PrismoidRegion.isValidConfig(config)) {
                        addInfoArea(areaName, new PrismoidInfoArea(areaName, config));
                    } else if (CuboidRegion.isValidConfig(config)
                            || config.contains("xSize")) { // xSize is to notice old data format
                        addInfoArea(areaName, new CuboidInfoArea(areaName, config));
                    }
                } catch (IOException | InvalidConfigurationException ex) {
                    Logger.getLogger(PluginData.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private static File getDataFile(InfoArea area, String areaName) {
        return new File(new File(dataFolder, area.getLocation().getWorld().getName()), areaName + ".yml");
    }

    public static void disable() {
        try {
            saveExcluded();
        } catch (IOException ex) {
            Logger.getLogger(PluginData.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (InfoArea area : infoAreas.values()) {
            area.clearPlayers();
        }
    }

    public static MessageUtil getMessageUtil() {
        return messageUtil;
    }

    public static Map<String, InfoArea> getInfoAreas() {
        return infoAreas;
    }

    public static List<String> suggestAreaNames(String typed) {
        return suggestAreas(typed).stream().map(AreaRegistry.Suggestion::name).toList();
    }

    public static List<AreaRegistry.Suggestion> suggestAreas(String typed) {
        return registry.suggest(typed);
    }

    public static File getDataFolder() {
        return dataFolder;
    }
}
