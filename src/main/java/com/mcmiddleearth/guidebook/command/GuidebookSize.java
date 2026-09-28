/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.data.CuboidInfoArea;
import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.guidebook.data.PrismoidInfoArea;
import com.mcmiddleearth.guidebook.data.SphericalInfoArea;
import io.papermc.paper.math.BlockPosition;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

/**
 * {@code size <area> radius|corners|height ...}: changes the dimensions of an Area. Each form applies to one Shape, and
 * a form that doesn't match the Area's Shape names the one that does.
 *
 * @author Eriol_Eandur
 */
final class GuidebookSize {

    private GuidebookSize() {}

    static void radius(CommandSender sender, InfoArea area, int radius) {
        if (!(area instanceof SphericalInfoArea sphere)) {
            sendWrongFormMessage(sender, area);
            return;
        }
        sphere.setRadius(radius);
        save(sender, area);
    }

    static void corners(CommandSender sender, InfoArea area, BlockPosition pos1, BlockPosition pos2) {
        if (!(area instanceof CuboidInfoArea cuboid)) {
            sendWrongFormMessage(sender, area);
            return;
        }
        cuboid.setCorners(pos1.toVector(), pos2.toVector());
        save(sender, area);
        // Block positions carry no world, so corners typed from another world still land in the Area's world
        if (sender instanceof Entity entity
                && !entity.getWorld().equals(area.getLocation().getWorld())) {
            PluginData.getMessageUtil()
                    .sendErrorMessage(
                            sender,
                            "You are not in the world of Guidebook area " + area.getName()
                                    + ". Its corners were set in its own world, "
                                    + area.getLocation().getWorld().getName() + ".");
        }
    }

    static void height(CommandSender sender, InfoArea area, int minY, int maxY) {
        if (!(area instanceof PrismoidInfoArea prism)) {
            sendWrongFormMessage(sender, area);
            return;
        }
        prism.setHeight(minY, maxY);
        save(sender, area);
    }

    private static void save(CommandSender sender, InfoArea area) {
        try {
            PluginData.saveArea(area);
        } catch (IOException ex) {
            Logger.getLogger(GuidebookSize.class.getName()).log(Level.SEVERE, null, ex);
            PluginData.getMessageUtil()
                    .sendErrorMessage(
                            sender, "There was an error. Guidebook area " + area.getName() + " was NOT saved.");
            return;
        }
        PluginData.getMessageUtil().sendInfoMessage(sender, "Size of Guidebook area " + area.getName() + " set.");
    }

    private static void sendWrongFormMessage(CommandSender sender, InfoArea area) {
        String name = area.getName();
        String message =
                switch (area) {
                    case SphericalInfoArea ignored ->
                        "Guidebook area " + name + " is a sphere. Use /guidebook size " + name + " radius <radius>";
                    case CuboidInfoArea ignored ->
                        "Guidebook area " + name + " is a cuboid. Use /guidebook size " + name
                                + " corners <pos1> <pos2>";
                    case PrismoidInfoArea ignored ->
                        "Guidebook area " + name + " is a prism. Use /guidebook size " + name + " height <minY> <maxY>";
                    default -> "Guidebook area " + name + " can't be resized.";
                };
        PluginData.getMessageUtil().sendErrorMessage(sender, message);
    }
}
