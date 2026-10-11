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

/** An Area loaded or created as a sphere. A move can give it another Shape, which {@link InfoArea} follows. */
public class SphericalInfoArea extends InfoArea {

    public SphericalInfoArea(String areaName, Location center, int radius) {
        super(areaName);
        region = new SphericalRegion(center, radius);
    }

    public SphericalInfoArea(String areaName, ConfigurationSection config) {
        super(areaName, config);
        region = SphericalRegion.load(config);
    }
}
