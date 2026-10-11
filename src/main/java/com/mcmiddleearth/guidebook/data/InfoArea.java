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
import com.mcmiddleearth.guidebook.events.GuidebookSendEvent;
import com.mcmiddleearth.guidebook.util.AreaText;
import com.mcmiddleearth.guidebook.util.DevUtil;
import com.mcmiddleearth.guidebook.util.GuidebookMessages;
import com.mcmiddleearth.pluginutil.region.CuboidRegion;
import com.mcmiddleearth.pluginutil.region.PrismoidRegion;
import com.mcmiddleearth.pluginutil.region.Region;
import com.mcmiddleearth.pluginutil.region.SphericalRegion;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * An Area, made with {@link #create} or read from its file with {@link #load}. Its Shape is whichever region it holds,
 * which a move can change.
 *
 * @author Eriol_Eandur
 */
public final class InfoArea implements MarkableArea {

    private static final int NEAR_DISTANCE = 10;
    private static final Duration COOLDOWN = Duration.ofMinutes(1);
    private static final Title.Times TITLE_TIMES =
            Title.Times.times(Ticks.duration(15), Ticks.duration(25), Ticks.duration(10));

    private Region region;

    private String areaName;

    /** The world named in the Area's file, kept for when that world isn't loaded and the region has none. */
    private String loadedWorldName;

    private final HashMap<UUID, Instant> lastInformedTimes = new HashMap<>();
    /**
     * All players currently within the InfoArea
     * Used to determine if a player has entered or left an InfoArea
     */
    private final Set<UUID> areaPlayers = new HashSet<>();

    private boolean status;

    private final BossBar bossBar;

    private String title;
    private String subtitle;
    private boolean showTitle;
    private boolean showScoreboard;

    private String description = "";

    private InfoArea(String areaName) {
        this.areaName = areaName;

        // scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        bossBar = BossBar.bossBar(Component.empty(), 0, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        setTitle("unnamed Guidebook area");
        subtitle = "";
        status = true;
    }

    private InfoArea(String areaName, ConfigurationSection config) {
        this(areaName);

        if (config.contains("title")) {
            setTitle((String) config.get("title"));
            setSubtitle((String) config.get("subtitle"));
            showScoreboard = config.getBoolean("showScoreboard");
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
        InfoArea area = new InfoArea(areaName, config);
        area.region = region;
        return Optional.of(area);
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

    public boolean isNear(Location loc) {
        return region.isNear(loc, NEAR_DISTANCE);
    }

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
        // A Disabled Area doesn't notice players leaving, so they'd keep the Boss bar
        hideBossBar();
    }

    public boolean containsPlayer(Player player) {
        return areaPlayers.contains(player.getUniqueId());
    }

    public final void onRegionEnter(Player player) {
        UUID playerId = player.getUniqueId();
        Instant now = Instant.now();

        boolean hasBeenInformed = lastInformedTimes.containsKey(playerId);
        if (hasBeenInformed) {
            Instant lastNotified = lastInformedTimes.get(playerId);
            if (Duration.between(lastNotified, now).compareTo(COOLDOWN) < 0) {
                // Player has entered the region, but don't welcome them
                areaPlayers.add(playerId);
                return;
            }
        }

        // Track that the player is in the region even if the Event is cancelled
        // - otherwise onRegionEnter would keep being called!
        areaPlayers.add(playerId);

        GuidebookSendEvent sendEvent = new GuidebookSendEvent(player, areaName);
        sendEvent.callEvent();
        if (sendEvent.isCancelled()) return;

        boolean notificationsEnabled = !PluginData.isExcluded(player);
        if (notificationsEnabled) {
            welcomePlayer(player);
            lastInformedTimes.put(playerId, now);
        }
    }

    public final void onRegionLeave(Player player) {
        UUID playerId = player.getUniqueId();
        areaPlayers.remove(playerId);
        player.hideBossBar(bossBar);
    }

    public void clearPlayer(Player player) {
        onRegionLeave(player);
        UUID playerId = player.getUniqueId();
        lastInformedTimes.remove(playerId);
    }

    public void clearPlayers() {
        for (UUID uuid : new ArrayList<>(areaPlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                clearPlayer(player);
            }
        }
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
        config.set("showScoreboard", showScoreboard);
        config.set("showTitle", showTitle);
        config.set("enabled", status);
        config.set(LegacyMarkupConverter.MARKER, true);
    }

    /**
     * Gives staff the Welcome now, as a player entering the Area gets it, but with no cooldown, send event or opt-out
     * check. The Boss bar appears only if they're inside an Enabled Area, because leaving it is what removes the bar.
     */
    public void previewWelcome(Player player) {
        // An Area just created doesn't know yet that the player is inside. Marking them stops their next move
        // counting as an entry and Welcoming them again. Only Enabled Areas notice a player leaving
        if (isEnabled() && isInside(player.getLocation())) {
            areaPlayers.add(player.getUniqueId());
        }
        welcomePlayer(player);
    }

    private void welcomePlayer(final Player player) {
        int messageDelay = 0;
        if (isShowTitle()) {
            player.showTitle(Title.title(AreaText.render(getTitle()), AreaText.render(getSubtitle()), TITLE_TIMES));
            messageDelay = 50;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                // Only while still inside, or a player who left during the Title would keep the bar
                if (isShowScoreboard() && containsPlayer(player)) {
                    player.showBossBar(bossBar);
                }
                sendDescription(player);
            }
        }.runTaskLater(GuidebookPlugin.getPluginInstance(), messageDelay);
    }

    private void sendDescription(Player player) {
        if (getDescription().isEmpty()) {
            GuidebookMessages.send(
                    player,
                    GuidebookMessages.info(
                            "Welcome to ",
                            Component.text()
                                    .color(GuidebookMessages.STRESSED)
                                    .append(AreaText.render(getTitle()))
                                    .append(Component.text(" (" + getName() + ")")),
                            ". Unfortunately there is no further description for this area."));
        } else {
            GuidebookMessages.send(player, AreaText.render(getDescription()));
        }
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
        bossBar.name(AreaText.render(title));
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

    public boolean isShowTitle() {
        return showTitle;
    }

    public void setShowTitle(boolean showTitle) {
        this.showTitle = showTitle;
    }

    public boolean isShowScoreboard() {
        return showScoreboard;
    }

    public void setShowScoreboard(boolean showScoreboard) {
        this.showScoreboard = showScoreboard;
        if (!showScoreboard) {
            hideBossBar();
        }
    }

    // Only players inside the Area are shown the Boss bar
    private void hideBossBar() {
        for (UUID uuid : areaPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.hideBossBar(bossBar);
            }
        }
    }

    @Override
    public String getName() {
        return this.areaName;
    }

    void setName(String areaName) {
        this.areaName = areaName;
    }
}
