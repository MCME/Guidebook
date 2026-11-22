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
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Location;

/**
 * The plugin's {@link AreaStore}, reached statically until every caller reaches it through the plugin.
 *
 * @author Eriol_Eandur
 */
public class PluginData {

    private static AreaStore store() {
        return GuidebookPlugin.getPluginInstance().getAreaStore();
    }

    /** See {@link AreaStore#create}. */
    public static void createInfoArea(InfoArea area) throws IOException {
        store().create(area);
    }

    /** See {@link AreaStore#delete}. */
    public static boolean deleteInfoArea(InfoArea area) {
        return store().delete(area);
    }

    /** See {@link AreaStore#rename}. */
    public static boolean renameInfoArea(InfoArea area, String newName) throws IOException {
        return store().rename(area, newName);
    }

    /** See {@link AreaStore#move}. */
    public static boolean moveInfoArea(InfoArea area, AreaGeometry geometry, Location warpPoint) throws IOException {
        return store().move(area, geometry, warpPoint);
    }

    /**
     * @return the Area the typed name reaches (ignoring case, exact case wins), or null if there is none
     */
    public static InfoArea getInfoArea(String name) {
        return store().find(name).orElse(null);
    }

    /**
     * @return the Area with exactly this name, or null if there is none
     */
    public static InfoArea getInfoAreaExact(String name) {
        return store().findExact(name).orElse(null);
    }

    /**
     * @return why the name can't be given to a new or renamed Area, or empty if it can
     */
    public static Optional<String> newAreaNameProblem(String name) {
        return store().newNameProblem(name);
    }

    /** See {@link AreaStore#reload}. */
    public static void loadData() {
        store().reload();
    }

    public static Map<String, InfoArea> getInfoAreas() {
        return store().byName();
    }

    public static List<String> suggestAreaNames(String typed) {
        return suggestAreas(typed).stream().map(AreaRegistry.Suggestion::name).toList();
    }

    public static List<AreaRegistry.Suggestion> suggestAreas(String typed) {
        return store().suggest(typed);
    }
}
