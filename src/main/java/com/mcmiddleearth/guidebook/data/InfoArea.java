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
import com.mcmiddleearth.guidebook.listener.PlayerListener;
import com.mcmiddleearth.guidebook.util.DevUtil;
import com.mcmiddleearth.guidebook.util.InputUtil;
import com.mcmiddleearth.pluginutil.TitleUtil;
import com.mcmiddleearth.pluginutil.message.FancyMessage;
import com.mcmiddleearth.pluginutil.message.config.FancyMessageConfigUtil;
import com.mcmiddleearth.pluginutil.message.config.MessageParseException;
import com.mcmiddleearth.pluginutil.region.Region;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
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

/**
 *
 * @author Eriol_Eandur
 */
public abstract class InfoArea {
    
    private static final int CHAT_LENGTH = 90;
    
    protected Region region;
    
    private final Set<UUID> informedPlayers = new HashSet<>();
    private final int nearDistance = 10;

    private boolean status;
    
    private final BossBar bossBar;
    
    private String title;
    private String subtitle;
    private boolean showTitle;
    private boolean showScoreboard;
            
    private List<String> description = new ArrayList<>();
    

    protected InfoArea() {
        //scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        bossBar = Bukkit.getServer().createBossBar("unnamed Guidebook area", BarColor.YELLOW, BarStyle.SOLID);
        bossBar.setProgress(0);
        setTitle("unnamed Guidebook area");
        subtitle = "";
        status = true;
    }
    
    public InfoArea(ConfigurationSection config) {
        //this.center = deserializeLocation(config.getConfigurationSection("center"));
        this();
        if(config.contains("title")) {
            setTitle((String) config.get("title"));
            subtitle = (String) config.get("subtitle");
            showScoreboard = config.getBoolean("showScoreboard");
            showTitle = config.getBoolean("showTitle");
            status = config.getBoolean("enabled",true);
        }
        if(config.isList("description")) {
            this.description = config.getStringList("description");
        } else {
            this.description.add(config.getString("description"));
        }
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
    
    public Location getLocation() {
        return region.getLocation();
    }

    public boolean isNear(Location loc) {
        return region.isNear(loc, nearDistance);
    }
    
    public boolean isInside(Location loc) {
        return region.isInside(loc);
    }
    
    public boolean isEnable(){
        return status;
    }
    
    public void statusOn(){
        status = true;
    }
    
    public void statusOff(){
        status = false;
    }
    
    public boolean isInfomed(Player player) {
        return informedPlayers.contains(player.getUniqueId());
    }
    
    public void addInformedPlayer(Player player) {
        informedPlayers.add(player.getUniqueId());
        welcomePlayer(player);
    }
    
    public void removeInformedPlayer(Player player) {
        informedPlayers.remove(player.getUniqueId());
        bossBar.removePlayer(player);
    }
        
    public void clearInformedPlayers() {
        for(UUID uuid: informedPlayers.toArray(new UUID[informedPlayers.size()])) {
            Player player = Bukkit.getPlayer(uuid);
            if(player!=null) {
                removeInformedPlayer(player);
            }
        }
    }
    public void save(ConfigurationSection config) {
        /*if(region.getLocation()==null) {
            GuidebookPlugin.getPluginInstance().getLogger().warning("Save region call with NULL location.");
            return;
        }*/
        DevUtil.log("saveInfo "+config+" "+region.toString());
        region.save(config);
        config.set("description", description);
        config.set("title",title);
        config.set("subtitle",subtitle);
        config.set("showScoreboard", showScoreboard);
        config.set("showTitle",showTitle);
        config.set("enabled",status);
    }
   
    private void welcomePlayer(final Player player) {
        final InfoArea thisArea = this;
        int messageDelay = 0;
        if(isShowTitle()) {
            TitleUtil.showTitle(player, getTitle(), getSubtitle(), 25, 20, 10);
            messageDelay = 50;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                if(isShowScoreboard()) {
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
        ItemStack book = new ItemStack(Material.WRITABLE_BOOK,1);
        BookMeta bookMeta = (BookMeta) book.getItemMeta();
        for(String line: description) {
            bookMeta.addPage(InputUtil.replaceColorCodeWithAltCode(line));
        }
        book.setItemMeta(bookMeta);
        return book;
    }
    
    public void setDescription(BookMeta bookMeta) throws MessageParseException {
        List<String> lines = new ArrayList<>();
        for(int i = 1; i<=bookMeta.getPageCount();i++) { //first page has index 1!!!
            String line = bookMeta.getPage(i);
            lines.add(InputUtil.replaceBookColorCode(line.substring(0,Math.min(CHAT_LENGTH,line.length()))));
            //debugString(lines.get(lines.size()-1));
            if(line.length()>CHAT_LENGTH) {
                lines.add(InputUtil.replaceBookColorCode(line.substring(CHAT_LENGTH, line.length()))); //string from book seem to contain random 'Â§0' characters
                //debugString(lines.get(lines.size()-1));
            }
        }
        FancyMessageConfigUtil.addFromStringList(new FancyMessage(PluginData.getMessageUtil()),
                                                 lines); //throws MessageParseExeption
        description = lines;
    }
    
    private void debugString(String string) {
        for(int i=0; i<string.length();i++){
            Logger.getGlobal().info("i: "+string.charAt(i)+" "+new Integer(string.charAt(i)).intValue()+" "+string.codePointAt(i));
        }
    }
    
    public void setDescription(List<String> lines) {
        description = lines;
    }

    public String getTitle() {
        return title;
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
}
