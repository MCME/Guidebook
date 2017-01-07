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

import com.mcmiddleearth.guidebook.util.DevUtil;
import com.mcmiddleearth.pluginutil.region.CuboidRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

/**
 *
 * @author Eriol_Eandur
 */
public class CuboidInfoArea extends InfoArea {
    
    public CuboidInfoArea(Location center, com.sk89q.worldedit.regions.CuboidRegion weRegion) {
        region = new CuboidRegion(center, weRegion);
    }
    
    public CuboidInfoArea(ConfigurationSection config) {
        super(config);
        if(config.contains("center")) {
            Location center = deserializeLocation(config.getConfigurationSection("center"));
            int sizeX = config.getInt("xSize");
            int sizeY = config.getInt("ySize");
            int sizeZ = config.getInt("zSize");
            Vector minPos = new Vector(center.getBlockX()-sizeX/2,
                                       center.getBlockY()-sizeY/2,
                                       center.getBlockZ()-sizeZ/2);
            Vector maxPos = new Vector(center.getBlockX()+sizeX/2,
                                       center.getBlockY()+sizeY/2,
                                       center.getBlockZ()+sizeZ/2);
            DevUtil.log("CreateCuboid "+center+"     "+minPos+"    "+maxPos);
            region = new CuboidRegion(center,minPos, maxPos);
        } else {
            DevUtil.log("loadCuboid "+config);
            region = CuboidRegion.load(config);
        }
    }
    
public void setCorners(Vector pos1, Vector pos2) {
        ((CuboidRegion)region).setCorners(pos1, pos2);
        /*sizeX = x;
        sizeY = y;
        sizeZ = z;*/
    }

    public Vector getMinPos() {
        return ((CuboidRegion)region).getMinCorner();
    }
    
    public Vector getMaxPos() {
        return ((CuboidRegion)region).getMaxCorner();
    }
    
    private static Location deserializeLocation(ConfigurationSection data) {
        if(data == null) {
            return null;
        }
        World world = Bukkit.getWorld(data.getString("world"));
        if(world == null) {
            return null;
        }
        else {
            return new Location(world, (Double) data.get("x"), 
                                       (Double) data.get("y"), 
                                       (Double) data.get("z"));
        }
    }
    
}
