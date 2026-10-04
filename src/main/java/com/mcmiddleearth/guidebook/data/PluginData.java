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
import com.mcmiddleearth.guidebook.webmap.MarkerLayer;
import com.mcmiddleearth.guidebook.webmap.NoWebMap;
import com.mcmiddleearth.guidebook.webmap.WebMap;
import com.mcmiddleearth.pluginutil.region.CuboidRegion;
import com.mcmiddleearth.pluginutil.region.PrismoidRegion;
import com.mcmiddleearth.pluginutil.region.Region;
import com.mcmiddleearth.pluginutil.region.SphericalRegion;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * @author Eriol_Eandur
 */
public class PluginData {

    private static final Map<String, InfoArea> infoAreas = new HashMap<>();

    private static final AreaRegistry<InfoArea> registry = new AreaRegistry<>(infoAreas.values());

    private static MarkerLayer webMapLayer = new NoWebMap().layer(AreaMarkers.LAYER_ID, AreaMarkers.LAYER_LABEL, true);

    private static Set<UUID> excludedPlayers = new HashSet<>();

    private static final File dataFolder = GuidebookPlugin.getPluginInstance().getDataFolder();

    static {
        if (!GuidebookPlugin.getPluginInstance().getDataFolder().exists()) {
            GuidebookPlugin.getPluginInstance().getDataFolder().mkdirs();
        }
    }

    public static boolean isExcluded(Player player) {
        return excludedPlayers.contains(player.getUniqueId());
    }

    public static InfoArea addInfoArea(String name, InfoArea newArea) {
        return infoAreas.put(name, newArea);
    }

    /**
     * Writes a new Area's file, then adds it and draws it, so an Area whose file couldn't be written is never added.
     */
    public static void createInfoArea(InfoArea area) throws IOException {
        writeArea(area);
        addInfoArea(area.getName(), area);
        drawArea(area);
    }

    public static boolean deleteInfoArea(InfoArea area) {
        area.clearPlayers();
        boolean result = getDataFile(area, area.getName()).delete();
        if (result) {
            infoAreas.remove(area.getName());
            eraseArea(area.getName());
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
            writeArea(area);
        } catch (IOException ex) {
            infoAreas.remove(newName);
            area.setName(oldName);
            infoAreas.put(oldName, area);
            throw ex;
        }
        boolean oldFileDeleted = oldDataFile.delete();
        eraseArea(oldName);
        drawArea(area);
        return oldFileDeleted;
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
            writeArea(area);
        } catch (IOException ex) {
            area.setRegion(oldRegion);
            throw ex;
        }
        boolean oldFileGone = oldDataFile.equals(getDataFile(area, area.getName())) || oldDataFile.delete();
        // The marker's id doesn't change, so this replaces it even in another world
        drawArea(area);
        return oldFileGone;
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

    /** Writes the Area's file, then redraws it on the Web map, so a failed save leaves its marker as it was. */
    public static void saveArea(InfoArea area) throws IOException {
        writeArea(area);
        drawArea(area);
    }

    private static void writeArea(InfoArea area) throws IOException {
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
        AreaFileLoader.Outcome outcome = AreaFileLoader.load(dataFolder, LocalDateTime.now());
        Logger logger = GuidebookPlugin.getPluginInstance().getLogger();
        outcome.backup()
                .ifPresent(backup -> logger.info("Backed up the Guidebook folder to " + backup
                        + " before converting Guidebook areas from Legacy markup to MiniMessage."));
        outcome.problems().forEach(logger::severe);
        for (AreaFileLoader.LoadedArea loaded : outcome.areas()) {
            String areaName = loaded.name();
            config = loaded.config();
            DevUtil.log("Load area " + areaName);
            if (SphericalRegion.isValidConfig(config)) {
                addInfoArea(areaName, new SphericalInfoArea(areaName, config));
            } else if (PrismoidRegion.isValidConfig(config)) {
                addInfoArea(areaName, new PrismoidInfoArea(areaName, config));
            } else if (CuboidRegion.isValidConfig(config)
                    || config.contains("xSize")) { // xSize is to notice old data format
                addInfoArea(areaName, new CuboidInfoArea(areaName, config));
            }
        }
        drawWebMap();
    }

    /** Draws every Area on this Web map from now on, starting with the Areas already loaded. */
    public static void useWebMap(WebMap webMap) {
        webMapLayer = webMap.layer(AreaMarkers.LAYER_ID, AreaMarkers.LAYER_LABEL, true);
        drawWebMap();
    }

    private static void drawWebMap() {
        webMapLayer.clear();
        infoAreas.values().forEach(PluginData::drawArea);
    }

    private static void drawArea(InfoArea area) {
        // Its world isn't loaded, so there's nowhere to draw it
        if (!area.getRegion().isValid()) {
            eraseArea(area.getName());
            GuidebookPlugin.getPluginInstance()
                    .getLogger()
                    .warning("Area " + area.getName() + " isn't drawn on the Web map because its world isn't loaded.");
            return;
        }
        webMapLayer.put(AreaMarkers.toMarker(area));
    }

    private static void eraseArea(String areaName) {
        webMapLayer.remove(AreaMarkers.id(areaName));
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
