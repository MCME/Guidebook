 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.GuidebookPlugin;
import com.mcmiddleearth.guidebook.conversation.ConfirmationFactory;
import com.mcmiddleearth.guidebook.conversation.Confirmationable;
import com.mcmiddleearth.guidebook.data.CuboidInfoArea;
import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.guidebook.data.PrismoidInfoArea;
import com.mcmiddleearth.guidebook.data.SphericalInfoArea;
import com.mcmiddleearth.pluginutil.NumericUtil;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Eriol_Eandur
 */
public class GuidebookSet extends GuidebookCommand implements Confirmationable{
    
    private InfoArea area;
    
    private String areaName;
    
    private Location location;
    
    private boolean notSpherical;
    
    private boolean newDynamic;
    
    private Region region = null;
    
    private int radius;
    
    public GuidebookSet(String... permissionNodes) {
        super(1, true, permissionNodes);
        setShortDescription(": Defines a Guidebook area.");
        setUsageDescription(" <AreaName>: Location of command sender becomes center of Guidebook area with name <AreaName>.");
    }
    
    @Override
    protected void execute(CommandSender cs, String... args) {
        areaName = args[0];
        area = PluginData.getInfoArea(args[0]);
        location = ((Player)cs).getLocation().clone();
        notSpherical = true;
        Player p = (Player) cs;
        if(args.length>1 && args[1].equalsIgnoreCase("sphere")) {
            if(args.length>2) {
                if(NumericUtil.isInt(args[2])) {
                    notSpherical = false;
                    radius = NumericUtil.getInt(args[2]);
                } else {
                    sentInvalidArgumentMessage(cs);
                    return;
                }
            } else {
                sendMissingArgumentErrorMessage(cs);
                return;
            }
        } else {
            try {
                region = WorldEdit.getInstance().getSession(p.getName()).getRegion();
            } catch (NullPointerException | IncompleteRegionException ex) {}
            if(!(region instanceof CuboidRegion || region instanceof Polygonal2DRegion) ) {
                sendInvalidSelection(p);
                return;
            }
        }
        if(area==null) {
            if(notSpherical) {
                if(region instanceof CuboidRegion) {
                    area = new CuboidInfoArea(location, (CuboidRegion)region);
                } else {
                    area = new PrismoidInfoArea(location, (Polygonal2DRegion)region);
                }
            }
            else {
                area = new SphericalInfoArea(location, radius);
            }
            PluginData.addInfoArea(areaName, area);
            saveData(cs,area);
            sendNewAreaMessage(cs);
        }/*
        else {
            String message = "A teleportation area with this name already exists. "+
                    "Do you want to redefine it?";
            newDynamic = area.isDynamic();
            if(area.isDynamic()
                    && !area.getTarget().getWorld().equals(((Player)cs).getWorld())) {
                newDynamic = false;
                message = message+" Center and target location will be in different worlds. "
                                 +"Teleportation type will changed to static.";
            }
            /*boolean wasCuboid = area instanceof CuboidTeleportationArea;
            if(cuboid != wasCuboid) {*/
           /* new ConfirmationFactory(AutoTeleportPlugin.getPluginInstance()).start((Player) cs, 
                                    message,this);
                    /*"Specified shape differs from current shape. "+
                    "You will need to define area size again. Do you want to continue?", this);
            /*    return;
            }
            area.setCenter(location);
            area.setDynamic(newDynamic);
            saveData(cs);
            sendCenterSetMessage(cs);*/
        //}

        /*areaName = args[0];
        area = PluginData.getInfoArea(args[0]);
        center = ((Player)cs).getLocation().clone();
        cuboid = true;
        /*if(args.length>1) {
            if(args[1].equalsIgnoreCase("sphere")) {
                cuboid = false;
            }
            else if(!args[1].equalsIgnoreCase("cuboid")) {
                sentInvalidArgumentMessage(cs);
            }
        }*/
        /*if(area==null) {
            if(cuboid) {
                area = new CuboidInfoArea(center);
            }
            /*else {
                area = new SphericalInfoArea(center);
            }*/
            /*PluginData.addInfoArea(areaName, area);
            saveData(cs);
            sendNewAreaMessage(cs);
        }*/
        else {
            new ConfirmationFactory(GuidebookPlugin.getPluginInstance()).start((Player) cs, 
                    "An area with that name already exists. Do you want to move it to your location and selection?", this);
        }
    }

    private void saveData(CommandSender cs, InfoArea area){
        try {
            PluginData.saveData(area);
        } catch (IOException ex) {
            sendIOErrorMessage(cs);
            Logger.getLogger(GuidebookSet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void confirmed(Player player) {
        List<String> description = area.getDescription();
        PluginData.deleteInfoArea(areaName);
        if(notSpherical) {
            if(region instanceof CuboidRegion) {
                area = new CuboidInfoArea(location, (CuboidRegion)region);
            } else {
                area = new PrismoidInfoArea(location, (Polygonal2DRegion)region);
            }
            //area = new CuboidTeleportationArea(location,(CuboidRegion)region);
        }
        else {
            area = new SphericalInfoArea(location, radius);
        }
        area.setDescription(description);
        PluginData.addInfoArea(areaName, area);
        saveData(player,area);
        sendCenterSetMessage(player);
    }
    /*@Override
    public void confirmed(Player player) {
        area.setCenter(player.getLocation());
        saveData(player);
        sendCenterSetMessage(player);
    }*/

    @Override
    public void cancelled(Player player) {
        PluginData.getMessageUtil().sendErrorMessage(player, "You cancelled setting of area. No changes were made.");
    }

    private void sendCenterSetMessage(CommandSender cs) {
        PluginData.getMessageUtil().sendInfoMessage(cs, "Guidebook area was moved to your location and selection.");
    }

    private void sendNewAreaMessage(CommandSender cs) {
        PluginData.getMessageUtil().sendInfoMessage(cs, "New guidebook area created.");
    }
    
    private void sendInvalidSelection(Player player) {
        PluginData.getMessageUtil().sendErrorMessage(player, "For a cuboid area make a valid WorldEdit selection first.");
    }

}
