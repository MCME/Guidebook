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
import com.mcmiddleearth.pluginutil.region.PrismoidRegion;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

/**
 *
 * @author Eriol_Eandur
 */
public class PrismoidInfoArea extends InfoArea {
    
    public PrismoidInfoArea(Location center, com.sk89q.worldedit.regions.Polygonal2DRegion weRegion) {
       region = new PrismoidRegion(center, weRegion);
    }
    
    public PrismoidInfoArea(ConfigurationSection config) {
        super(config);
        DevUtil.log("LoadCuboid "+config);
        region = PrismoidRegion.load(config);
    }
    
    public void setHeight(int minY, int maxY) {
        ((PrismoidRegion)region).setMinY(minY);
        ((PrismoidRegion)region).setMaxY(maxY);
    }
    
    public int getMinY() {
        return ((PrismoidRegion)region).getMinY();
    }
    
    public int getMaxY() {
        return ((PrismoidRegion)region).getMaxY();
    }
    
    public Integer[] getXPoints() {
        return ((PrismoidRegion) region).getXPoints();
    }
    
    public Integer[] getZPoints() {
        return ((PrismoidRegion) region).getZPoints();
    }
    
    /*@Override
    public boolean isInside(Location loc) {
        return region.isInside(loc);
                /*getCenter().getWorld().equals(loc.getWorld())
            && loc.getBlockX() >= getCenter().getBlockX()-sizeX/2
            && loc.getBlockX() <= getCenter().getBlockX()+sizeX/2
            && loc.getBlockY() >= getCenter().getBlockY()-sizeY/2
            && loc.getBlockY() <= getCenter().getBlockY()+sizeY/2
            && loc.getBlockZ() >= getCenter().getBlockZ()-sizeZ/2
            && loc.getBlockZ() <= getCenter().getBlockZ()+sizeZ/2;*/
    /*}
    
    @Override
    public boolean isNear(Location loc) {
//Logger.getGlobal().info("cuboid isNear "+getPreloadDistance());
        return region.isNear(loc, getPreloadDistance());
                /*getCenter().getWorld().equals(loc.getWorld())
            && loc.getBlockX() >= getCenter().getBlockX()-(sizeX+getPreloadDistance())/2
            && loc.getBlockX() <= getCenter().getBlockX()+(sizeX+getPreloadDistance())/2
            && loc.getBlockY() >= getCenter().getBlockY()-(sizeY+getPreloadDistance())/2
            && loc.getBlockY() <= getCenter().getBlockY()+(sizeY+getPreloadDistance())/2
            && loc.getBlockZ() >= getCenter().getBlockZ()-(sizeZ+getPreloadDistance())/2
            && loc.getBlockZ() <= getCenter().getBlockZ()+(sizeZ+getPreloadDistance())/2;*/
    //}
    
    /*@Override
    public Map<String,Object> serialize() {
        Map<String,Object> result = super.serialize();
        result.put("xSize", sizeX);
        result.put("ySize", sizeY);
        result.put("zSize", sizeZ);
        return result;
    }*/
}
