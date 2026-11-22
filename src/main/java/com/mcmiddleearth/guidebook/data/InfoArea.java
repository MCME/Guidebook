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

import com.mcmiddleearth.guidebook.welcome.WelcomeArea;
import com.mcmiddleearth.pluginutil.region.CuboidRegion;
import com.mcmiddleearth.pluginutil.region.PrismoidRegion;
import com.mcmiddleearth.pluginutil.region.Region;
import com.mcmiddleearth.pluginutil.region.SphericalRegion;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * An Area, made with {@link #create} or read from its file by the Area file adapter. Its Shape is whichever region it
 * holds, which a move can change.
 *
 * @author Eriol_Eandur
 */
public final class InfoArea implements MarkableArea, WelcomeArea {

    private static final int NEAR_DISTANCE = 10;
    private Region region;

    private String areaName;

    /** The name of the Area's world, kept for when that world isn't loaded and the region has none. */
    private String loadedWorldName;

    private boolean status;

    private String title;
    private String subtitle;
    private boolean showTitle;
    private boolean showBossBar;

    private String description = "";

    private InfoArea(String areaName) {
        this.areaName = areaName;

        setTitle("");
        subtitle = "";
        status = true;
    }

    /** A new Area filling {@code geometry}, with its Warp point at {@code warpPoint}. */
    public static InfoArea create(String areaName, Location warpPoint, AreaGeometry geometry) {
        InfoArea area = new InfoArea(areaName);
        area.region = regionOf(geometry, warpPoint);
        return area;
    }

    /**
     * The Area {@code areaName} as read from its file. Its world is named, because it may not be loaded: then
     * {@code warpPoint} has no world.
     */
    static InfoArea loaded(
            String areaName, String worldName, Location warpPoint, AreaGeometry geometry, AreaDetails details) {
        InfoArea area = new InfoArea(areaName);
        area.region = regionOf(geometry, warpPoint);
        area.loadedWorldName = worldName;
        area.setDetails(details);
        return area;
    }

    public Region getRegion() {
        return this.region;
    }

    void setRegion(Region region) {
        this.region = region;
    }

    /**
     * The region filling {@code geometry}, with its Warp point at {@code warpPoint}. A sphere is centred on its Warp
     * point, so its own centre is ignored.
     */
    static Region regionOf(AreaGeometry geometry, Location warpPoint) {
        return switch (geometry) {
            case AreaGeometry.Sphere sphere -> new SphericalRegion(warpPoint, sphere.radius());
            case AreaGeometry.Cuboid cuboid ->
                new CuboidRegion(
                        warpPoint,
                        new Vector(cuboid.minX(), cuboid.minY(), cuboid.minZ()),
                        new Vector(cuboid.maxX(), cuboid.maxY(), cuboid.maxZ()));
            case AreaGeometry.Prism prism ->
                new PrismoidRegion(
                        warpPoint,
                        new ArrayList<>(prism.xs()),
                        new ArrayList<>(prism.zs()),
                        prism.minY(),
                        prism.maxY());
        };
    }

    @Override
    public AreaGeometry getGeometry() {
        return switch (region) {
            case SphericalRegion sphere -> {
                Location center = sphere.getLocation();
                yield new AreaGeometry.Sphere(
                        center.getBlockX(), center.getBlockY(), center.getBlockZ(), sphere.getRadius());
            }
            case CuboidRegion cuboid -> {
                Vector min = cuboid.getMinCorner();
                Vector max = cuboid.getMaxCorner();
                yield new AreaGeometry.Cuboid(
                        min.getBlockX(),
                        min.getBlockY(),
                        min.getBlockZ(),
                        max.getBlockX(),
                        max.getBlockY(),
                        max.getBlockZ());
            }
            case PrismoidRegion prism ->
                new AreaGeometry.Prism(
                        List.of(prism.getXPoints()), List.of(prism.getZPoints()), prism.getMinY(), prism.getMaxY());
            default -> throw new IllegalStateException("Unsupported region " + region);
        };
    }

    /** The name of the Area's world, even when that world isn't loaded. */
    @Override
    public String getWorldName() {
        return region.isValid() ? region.getWorld().getName() : loadedWorldName;
    }

    public Location getLocation() {
        return region.getLocation();
    }

    @Override
    public boolean isNear(Location loc) {
        return region.isNear(loc, NEAR_DISTANCE);
    }

    @Override
    public boolean isInside(Location loc) {
        return region.isInside(loc);
    }

    @Override
    public boolean isEnabled() {
        return status;
    }

    void setDetails(AreaDetails details) {
        setShowTitle(details.showTitle());
        setTitle(details.title());
        setSubtitle(details.subtitle());
        setShowBossBar(details.showBossBar());
        setDescription(details.description());
        status = details.enabled();
    }

    void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getTitle() {
        return title;
    }

    final void setTitle(String newTitle) {
        /*Objective obj = scoreboard.getObjective(newTitle);
        if(obj!=null) {
            obj.unregister();
        }
        Objective objective = scoreboard.registerNewObjective(newTitle, "dummy");
        objective.getScore("dummy").setScore(0);
        objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);*/
        // An older file can hold an empty Title or Subtitle as null
        title = Objects.requireNonNullElse(newTitle, "");
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getSubtitle() {
        return subtitle;
    }

    void setSubtitle(String subtitle) {
        this.subtitle = Objects.requireNonNullElse(subtitle, "");
    }

    @Override
    public boolean isShowTitle() {
        return showTitle;
    }

    void setShowTitle(boolean showTitle) {
        this.showTitle = showTitle;
    }

    @Override
    public boolean isShowBossBar() {
        return showBossBar;
    }

    void setShowBossBar(boolean showBossBar) {
        this.showBossBar = showBossBar;
    }

    @Override
    public String getName() {
        return this.areaName;
    }

    void setName(String areaName) {
        this.areaName = areaName;
    }
}
