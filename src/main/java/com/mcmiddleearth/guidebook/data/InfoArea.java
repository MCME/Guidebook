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
import com.mcmiddleearth.guidebook.command.GuidebookShow;
import com.mcmiddleearth.guidebook.events.GuidebookSendEvent;
import com.mcmiddleearth.guidebook.util.AreaText;
import com.mcmiddleearth.guidebook.util.DevUtil;
import com.mcmiddleearth.pluginutil.region.Region;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * @author Eriol_Eandur
 */
public abstract class InfoArea implements AreaView {

    private static final int NEAR_DISTANCE = 10;
    private static final Duration COOLDOWN = Duration.ofMinutes(1);
    private static final Title.Times TITLE_TIMES =
            Title.Times.times(Ticks.duration(25), Ticks.duration(20), Ticks.duration(10));

    protected Region region;

    private String areaName;

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

    protected InfoArea(String areaName) {
        this.areaName = areaName;

        // scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        bossBar = BossBar.bossBar(Component.empty(), 0, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        setTitle("unnamed Guidebook area");
        subtitle = "";
        status = true;
    }

    public InfoArea(String areaName, ConfigurationSection config) {
        this(areaName);

        if (config.contains("title")) {
            setTitle((String) config.get("title"));
            setSubtitle((String) config.get("subtitle"));
            showScoreboard = config.getBoolean("showScoreboard");
            showTitle = config.getBoolean("showTitle");
            status = config.getBoolean("enabled", true);
        }
        description = config.getString("description", "");
    }

    public Region getRegion() {
        return this.region;
    }

    public void setRegion(Region region) {
        this.region = region;
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
        final InfoArea thisArea = this;
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
                GuidebookShow.sendDescription(player, thisArea);
            }
        }.runTaskLater(GuidebookPlugin.getPluginInstance(), messageDelay);
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

    public String getDescription() {
        return description;
    }

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
