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
import com.mcmiddleearth.guidebook.listener.PlayerListener;
import com.mcmiddleearth.guidebook.util.DevUtil;
import com.mcmiddleearth.guidebook.util.InputUtil;
import com.mcmiddleearth.pluginutil.TitleUtil;
import com.mcmiddleearth.pluginutil.message.FancyMessage;
import com.mcmiddleearth.pluginutil.message.config.FancyMessageConfigUtil;
import com.mcmiddleearth.pluginutil.message.config.MessageParseException;
import com.mcmiddleearth.pluginutil.region.Region;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Eriol_Eandur
 */
public abstract class InfoArea {

    private static final int CHAT_LENGTH = 90;
    private static final int NEAR_DISTANCE = 10;
    private static final Duration COOLDOWN = Duration.ofMinutes(1);

    protected Region region;

    private final String areaName;

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

    private List<String> description = new ArrayList<>();

    protected InfoArea(String areaName) {
        this.areaName = areaName;

        //scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        bossBar = Bukkit.getServer().createBossBar("unnamed Guidebook area", BarColor.YELLOW, BarStyle.SOLID);
        bossBar.setProgress(0);
        setTitle("unnamed Guidebook area");
        subtitle = "";
        status = true;
    }

    public InfoArea(String areaName, ConfigurationSection config) {
        this(areaName);

        if (config.contains("title")) {
            setTitle((String) config.get("title"));
            subtitle = (String) config.get("subtitle");
            showScoreboard = config.getBoolean("showScoreboard");
            showTitle = config.getBoolean("showTitle");
            status = config.getBoolean("enabled", true);
        }
        if (config.isList("description")) {
            this.description = config.getStringList("description");
        } else {
            this.description.add(config.getString("description"));
        }
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

    public boolean isEnable() {
        return status;
    }

    public void statusOn() {
        status = true;
    }

    public void statusOff() {
        status = false;
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
        bossBar.removePlayer(player);
    }

    public void clearPlayer(Player player) {
        onRegionLeave(player);
        UUID playerId = player.getUniqueId();
        lastInformedTimes.remove(playerId);
    }

    public void clearPlayers() {
        for (UUID uuid : areaPlayers) {
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
    }

    private void welcomePlayer(final Player player) {
        final InfoArea thisArea = this;
        int messageDelay = 0;
        if (isShowTitle()) {
            TitleUtil.showTitle(player, getTitle(), getSubtitle(), 25, 20, 10);
            messageDelay = 50;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                if (isShowScoreboard()) {
                    //player.setScoreboard(area.getScoreboard());
                    bossBar.addPlayer(player);
                }
                try {
                    GuidebookShow.sendDescription(player, thisArea);
                } catch (MessageParseException ex) {
                    Logger.getLogger(PlayerListener.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.runTaskLater(GuidebookPlugin.getPluginInstance(), messageDelay);

    }

    public ItemStack getDescriptionBook() {
        ItemStack book = new ItemStack(Material.WRITABLE_BOOK, 1);
        BookMeta bookMeta = (BookMeta) book.getItemMeta();
        for (String line : description) {
            bookMeta.addPage(InputUtil.replaceColorCodeWithAltCode(line));
        }
        book.setItemMeta(bookMeta);
        return book;
    }

    public void setDescription(BookMeta bookMeta) throws MessageParseException {
        List<String> lines = new ArrayList<>();
        for (int i = 1; i <= bookMeta.getPageCount(); i++) { //first page has index 1!!!
            String line = bookMeta.getPage(i);
            lines.add(InputUtil.replaceBookColorCode(line.substring(0, Math.min(CHAT_LENGTH, line.length()))));
            //debugString(lines.get(lines.size()-1));
            if (line.length() > CHAT_LENGTH) {
                lines.add(InputUtil.replaceBookColorCode(line.substring(CHAT_LENGTH, line.length()))); //string from book seem to contain random 'Â§0' characters
                //debugString(lines.get(lines.size()-1));
            }
        }
        FancyMessageConfigUtil.addFromStringList(new FancyMessage(PluginData.getMessageUtil()),
            lines); //throws MessageParseExeption
        description = lines;
    }

    public void setDescription(List<String> lines) {
        description = lines;
    }

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
        bossBar.setTitle(newTitle);
        title = newTitle;
    }

    public List<String> getDescription() {
        return description;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
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
    }

    private void debugString(String string) {
        for (int i = 0; i < string.length(); i++) {
            Logger.getGlobal().info("i: " + string.charAt(i) + " " + Integer.parseInt(String.valueOf(string.charAt(i))) + " " + string.codePointAt(i));
        }
    }

    public String getName() {
        return this.areaName;
    }
}
