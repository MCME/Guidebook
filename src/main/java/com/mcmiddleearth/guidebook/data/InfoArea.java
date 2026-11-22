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
import com.mcmiddleearth.guidebook.welcome.WelcomeArea;
import com.mcmiddleearth.pluginutil.region.CuboidRegion;
import com.mcmiddleearth.pluginutil.region.PrismoidRegion;
import com.mcmiddleearth.pluginutil.region.Region;
import com.mcmiddleearth.pluginutil.region.SphericalRegion;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

/**
 * An Area, made with {@link #create} or read from its file with {@link #load}. Its Shape is whichever region it holds,
 * which a move can change.
 *
 * @author Eriol_Eandur
 */
public final class InfoArea implements MarkableArea, WelcomeArea {

    private static final int NEAR_DISTANCE = 10;
    private Region region;

    private String areaName;

    /** The world named in the Area's file, kept for when that world isn't loaded and the region has none. */
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

    private InfoArea(String areaName, ConfigurationSection config) {
        this(areaName);

        if (config.contains("title")) {
            setTitle((String) config.get("title"));
            setSubtitle((String) config.get("subtitle"));
            // Stored under its old name, from when it was a scoreboard
            showBossBar = config.getBoolean("showScoreboard");
            showTitle = config.getBoolean("showTitle");
            status = config.getBoolean("enabled", true);
        }
        description = config.getString("description", "");
        // An old cuboid file keeps its world under its center
        loadedWorldName = config.getString("world", config.getString("center.world"));
    }

    /** A new Area filling {@code geometry}, with its Warp point at {@code warpPoint}. */
    public static InfoArea create(String areaName, Location warpPoint, AreaGeometry geometry) {
        InfoArea area = new InfoArea(areaName);
        area.region = regionOf(geometry, warpPoint);
        return area;
    }

    /**
     * Reads the Area {@code areaName} from its file's contents.
     *
     * @return the Area, or empty if the file holds no Shape Guidebook knows
     */
    public static Optional<InfoArea> load(String areaName, ConfigurationSection config) {
        Region region;
        if (SphericalRegion.isValidConfig(config)) {
            region = SphericalRegion.load(config);
        } else if (PrismoidRegion.isValidConfig(config)) {
            DevUtil.log("loadPrism " + config);
            region = PrismoidRegion.load(config);
        } else if (CuboidRegion.isValidConfig(config) || config.contains("xSize")) { // xSize is the old cuboid format
            region = loadCuboid(config);
        } else {
            return Optional.empty();
        }
        return Optional.of(loaded(areaName, config, region));
    }

    /**
     * The Area {@code areaName} with its file's contents and the region already read from them. Kept apart from
     * reading the region, which looks its world up on the server, so tests can build Areas from file contents.
     */
    static InfoArea loaded(String areaName, ConfigurationSection config, Region region) {
        InfoArea area = new InfoArea(areaName, config);
        area.region = region;
        return area;
    }

    private static CuboidRegion loadCuboid(ConfigurationSection config) {
        if (!config.contains("center")) {
            DevUtil.log("loadCuboid " + config);
            return CuboidRegion.load(config);
        }
        // The old format: a centre and the sizes along each axis
        Location center = deserializeLocation(config.getConfigurationSection("center"));
        int sizeX = config.getInt("xSize");
        int sizeY = config.getInt("ySize");
        int sizeZ = config.getInt("zSize");
        Vector minPos = new Vector(
                center.getBlockX() - sizeX / 2, center.getBlockY() - sizeY / 2, center.getBlockZ() - sizeZ / 2);
        Vector maxPos = new Vector(
                center.getBlockX() + sizeX / 2, center.getBlockY() + sizeY / 2, center.getBlockZ() + sizeZ / 2);
        DevUtil.log("CreateCuboid " + center + "     " + minPos + "    " + maxPos);
        return new CuboidRegion(center, minPos, maxPos);
    }

    private static Location deserializeLocation(ConfigurationSection data) {
        if (data == null) {
            return null;
        }
        World world = Bukkit.getWorld(data.getString("world"));
        if (world == null) {
            return null;
        } else {
            return new Location(world, (Double) data.get("x"), (Double) data.get("y"), (Double) data.get("z"));
        }
    }

    public Region getRegion() {
        return this.region;
    }

    public void setRegion(Region region) {
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

    public void statusOn() {
        status = true;
    }

    public void statusOff() {
        status = false;
    }

    public void save(ConfigurationSection config) {
        /*if(region.getLocation()==null) {
            GuidebookPlugin.getPluginInstance().getLogger().warning("Save region call with NULL location.");
            return;
        }*/
        DevUtil.log("saveInfo " + config + " " + region.toString());
        region.save(config);
        config.set("description", description);
        config.set("title", title);
        config.set("subtitle", subtitle);
        config.set("showScoreboard", showBossBar);
        config.set("showTitle", showTitle);
        config.set("enabled", status);
        config.set(LegacyMarkupConverter.MARKER, true);
    }

    void setDetails(AreaDetails details) {
        setShowTitle(details.showTitle());
        setTitle(details.title());
        setSubtitle(details.subtitle());
        setShowBossBar(details.showBossBar());
        setDescription(details.description());
        status = details.enabled();
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getTitle() {
        return title;
    }

    public final void setTitle(String newTitle) {
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

    public void setSubtitle(String subtitle) {
        this.subtitle = Objects.requireNonNullElse(subtitle, "");
    }

    @Override
    public boolean isShowTitle() {
        return showTitle;
    }

    public void setShowTitle(boolean showTitle) {
        this.showTitle = showTitle;
    }

    @Override
    public boolean isShowBossBar() {
        return showBossBar;
    }

    public void setShowBossBar(boolean showBossBar) {
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
