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
import com.mcmiddleearth.pluginutil.message.FancyMessage;
import com.mcmiddleearth.pluginutil.message.MessageType;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Eriol_Eandur
 */
public class GuidebookDetails extends GuidebookCommand {

    public GuidebookDetails(String... permissionNodes) {
        super(1, true, permissionNodes);
        setShortDescription(": Shows details of a Guidebook area.");
        setUsageDescription(" <AreaName>: Shows details of area <AreaName>.");
    }

    @Override
    protected void execute(CommandSender cs, String... args) {
        InfoArea area = PluginData.getInfoArea(args[0]);
        if (area == null) {
            sendNoAreaErrorMessage(cs);
        } else {
            new FancyMessage(MessageType.INFO, PluginData.getMessageUtil())
                .addSimple("Details for Guidebook area ")
                .addFancy(PluginData.getMessageUtil().STRESSED + args[0] + PluginData.getMessageUtil().INFO + ".",
                    "/guidebook show " + args[0],
                    "Click for welcome message.")
                .send((Player) cs);
            new FancyMessage(MessageType.INFO_INDENTED, PluginData.getMessageUtil())
                .addFancy(ChatColor.GOLD
                        + "Location" + ChatColor.YELLOW
                        + ": " + area.getLocation().getWorld().getName()
                        + " " + area.getLocation().getBlockX()
                        + " " + area.getLocation().getBlockY()
                        + " " + area.getLocation().getBlockZ(),
                    "/guidebook warp " + args[0], "Click for warp command.")
                .send((Player) cs);
            if (area instanceof CuboidInfoArea) {
                CuboidInfoArea cuboid = (CuboidInfoArea) area;
                new FancyMessage(MessageType.INFO_INDENTED, PluginData.getMessageUtil())
                    .addTooltipped(ChatColor.YELLOW + "Cuboid shaped area",
                        " min corner: (" + cuboid.getMinPos().getBlockX() + ","
                            + cuboid.getMinPos().getBlockY() + ","
                            + cuboid.getMinPos().getBlockZ() + ")\n"
                            + " max corner: (" + cuboid.getMaxPos().getBlockX() + ","
                            + cuboid.getMaxPos().getBlockY() + ","
                            + cuboid.getMaxPos().getBlockZ() + ")")
                    .send((Player) cs);
            } else if (area instanceof SphericalInfoArea) {
                SphericalInfoArea sphere = (SphericalInfoArea) area;
                new FancyMessage(MessageType.INFO_INDENTED, PluginData.getMessageUtil())
                    .addSimple(ChatColor.YELLOW + "Spherical area with radius " + sphere.getRadius())
                    .send((Player) cs);
            } else if (area instanceof PrismoidInfoArea) {
                PrismoidInfoArea prism = (PrismoidInfoArea) area;
                String cornerData = "";
                Integer[] xPoints = prism.getXPoints();
                Integer[] zPoints = prism.getZPoints();
                for (int i = 0; i < xPoints.length; i++) {
                    if (i > 0) {
                        cornerData = cornerData + "\n";
                    }
                    cornerData = cornerData + "(" + xPoints[i] + "," + zPoints[i] + ")";
                }
                new FancyMessage(MessageType.INFO_INDENTED, PluginData.getMessageUtil())
                    .addTooltipped(ChatColor.YELLOW + "Prism shaped area",
                        " corners: (x,z)\n" + cornerData)
                    .send((Player) cs);
            }
            /*else {
                SphericalTeleportationArea sphere = (SphericalTeleportationArea)area;
                MessageUtil.sendNoPrefixInfoMessage(cs, ChatColor.YELLOW+"Spheric area with "
                                                       +"radius "+sphere.getRadius());
            }*/
            /*PluginData.getMessageUtil().sendNoPrefixInfoMessage(cs, ChatColor.GOLD+"Description: "
                                                  + ChatColor.YELLOW+area.getDescription());*/
        }
    }

}
