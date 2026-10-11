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

/** An Area loaded or created as a prism. A move can give it another Shape, which {@link InfoArea} follows. */
public class PrismoidInfoArea extends InfoArea {

    /** A new Area filling {@code prism}, with its Warp point at {@code warpPoint}. */
    public PrismoidInfoArea(String areaName, Location warpPoint, AreaGeometry.Prism prism) {
        super(areaName);
        region = regionOf(prism, warpPoint);
    }

    public PrismoidInfoArea(String areaName, ConfigurationSection config) {
        super(areaName, config);
        DevUtil.log("LoadCuboid " + config);
        region = PrismoidRegion.load(config);
    }
}
