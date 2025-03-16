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

import com.mcmiddleearth.pluginutil.region.SphericalRegion;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

/**
 * @author Eriol_Eandur
 */
public class SphericalInfoArea extends InfoArea {

    public SphericalInfoArea(Location center, int radius) {
        region = new SphericalRegion(center, radius);
    }

    public SphericalInfoArea(ConfigurationSection config) {
        super(config);
        region = SphericalRegion.load(config);
    }

    public void setRadius(int radius) {
        ((SphericalRegion) region).setRadius(radius);
    }

    public int getRadius() {
        return ((SphericalRegion) region).getRadius();
    }

    /*@Override
    public boolean isInside(Location loc) {
        return region.isInside(loc);
        /*return getCenter().getWorld().equals(loc.getWorld())
            && getCenter().distance(loc) <= radius;*/
    /*}
    
    @Override
    public boolean isNear(Location loc) {
//Logger.getGlobal().info("spherical isNear "+getPreloadDistance());
        /*return getCenter().getWorld().equals(loc.getWorld())
            && getCenter().distance(loc) <= radius+getPreloadDistance();*/
    /*    return region.isNear(loc, getPreloadDistance());
    }
    
    
    /*
    @Override
    public Map<String,Object> serialize() {
        Map<String,Object> result = super.serialize();
        result.put("radius", radius);
        return result;
    }*/
}
