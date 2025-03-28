 /*
  *  Copyright (C) 2016 MCME
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
 import com.mcmiddleearth.pluginutil.WEUtil;
 import com.mcmiddleearth.pluginutil.region.PrismoidRegion;
 import com.mcmiddleearth.pluginutil.region.SphericalRegion;
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
  * @author Eriol_Eandur
  */
 public class GuidebookSet extends GuidebookCommand implements Confirmationable {

     private InfoArea area;

     private Location location;
     private Region WERegion = null;

     private boolean spherical;
     private int radius;

     public GuidebookSet(String... permissionNodes) {
         super(1, true, permissionNodes);
         setShortDescription(": Define a new area, or redefine an existing one");
         setUsageDescription(" Set's the region of the area to your WE selection or to a sphere (if the sphere & radius arguments are provided)");
     }

     @Override
     protected void execute(CommandSender cs, String... args) {
         String areaName = args[0];
         area = PluginData.getInfoArea(areaName);
         spherical = false;

         Player p = (Player) cs;
         location = p.getLocation().clone();

         if (args.length > 1 && args[1].equalsIgnoreCase("sphere")) {
             if (args.length > 2) {
                 String radiusArg = args[2];
                 if (NumericUtil.isInt(radiusArg)) {
                     spherical = true;
                     radius = NumericUtil.getInt(radiusArg);
                 } else {
                     sendInvalidArgumentMessage(cs);
                     return;
                 }
             } else {
                 sendMissingArgumentErrorMessage(cs);
                 return;
             }
         } else {
             WERegion = WEUtil.getSelection(p);
             if (!(WERegion instanceof CuboidRegion || WERegion instanceof Polygonal2DRegion)) {
                 sendInvalidSelection(p);
                 return;
             }
         }

         // Determine if we're creating or moving a region
         if (area == null) {
             if (spherical) {
                 area = new SphericalInfoArea(location, radius);
             } else {
                 if (WERegion instanceof CuboidRegion) {
                     area = new CuboidInfoArea(location, (CuboidRegion) WERegion);
                 } else {
                     area = new PrismoidInfoArea(location, (Polygonal2DRegion) WERegion);
                 }
             }
             PluginData.addInfoArea(areaName, area);

             saveData(cs, area);
             sendNewAreaMessage(cs);
         } else {
             new ConfirmationFactory(GuidebookPlugin.getPluginInstance()).start(
                 p,
                 "An area with that name already exists. Do you want to move it to your location and selection?",
                 this
             );
         }
     }

     @Override
     protected List<String> getCompletions(CommandSender cs, String... args) {
         if (args.length == 1) {
             return PluginData.getAreaNames();
         }

         if (args.length == 2) {
             return List.of("sphere");
         }

         return List.of();
     }

     private void saveData(CommandSender cs, InfoArea area) {
         try {
             PluginData.saveArea(area);
         } catch (IOException ex) {
             sendIOErrorMessage(cs);
             Logger.getLogger(GuidebookSet.class.getName()).log(Level.SEVERE, null, ex);
         }
     }

     @Override
     public void confirmed(Player player) {
         com.mcmiddleearth.pluginutil.region.Region newRegion = null;
         if (spherical) {
             newRegion = new SphericalRegion(location, radius);
         } else {
             if (WERegion instanceof CuboidRegion cuboid) {
                 newRegion = new com.mcmiddleearth.pluginutil.region.CuboidRegion(location, cuboid);
             } else if (WERegion instanceof Polygonal2DRegion polygon) {
                 newRegion = new PrismoidRegion(location, polygon);
             }
         }

         if (newRegion == null) {
             PluginData.getMessageUtil().sendErrorMessage(
                 player,
                 "Unable to move area because the new region for the area is empty!"
             );
             return;
         }
         area.setRegion(newRegion);

         saveData(player, area);
         sendAreaMovedMessage(player);
     }

     @Override
     public void cancelled(Player player) {
         PluginData.getMessageUtil().sendErrorMessage(player, "You cancelled setting of area. No changes were made.");
     }

     private void sendAreaMovedMessage(CommandSender cs) {
         PluginData.getMessageUtil().sendInfoMessage(cs, "Guidebook area was moved to your location and selection.");
     }

     private void sendNewAreaMessage(CommandSender cs) {
         PluginData.getMessageUtil().sendInfoMessage(cs, "New guidebook area created.");
     }

     private void sendInvalidSelection(Player player) {
         PluginData.getMessageUtil().sendErrorMessage(player, "No WorldEdit selection found! Either make a selection and try again, or specify sphere <radius> after the AreaName");
     }
 }
