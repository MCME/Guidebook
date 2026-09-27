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
import com.mcmiddleearth.guidebook.util.FancyMessageUtil;
import com.mcmiddleearth.pluginutil.message.FancyMessage;
import com.mcmiddleearth.pluginutil.message.MessageType;
import com.mcmiddleearth.pluginutil.region.CuboidRegion;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.Polygonal2DRegionSelector;
import com.sk89q.worldedit.regions.selector.SphereRegionSelector;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.world.World;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * @author Eriol_Eandur
 */
public final class GuidebookDetails {

    private GuidebookDetails() {}

    /**
     * Shows the Area's Shape and location. A player's WorldEdit selection is also set to the Area.
     */
    public static void send(CommandSender sender, InfoArea area) {
        String areaName = area.getName();

        FancyMessage header = new FancyMessage(MessageType.INFO, PluginData.getMessageUtil())
                .addSimple("Details for Guidebook area ")
                .addFancy(
                        PluginData.getMessageUtil().STRESSED + areaName + PluginData.getMessageUtil().INFO + ".",
                        "/guidebook show " + areaName,
                        "Click for welcome message.");
        FancyMessageUtil.send(sender, header);

        FancyMessage location = new FancyMessage(MessageType.INFO_INDENTED, PluginData.getMessageUtil())
                .addFancy(
                        ChatColor.GOLD
                                + "Location" + ChatColor.YELLOW
                                + ": " + area.getLocation().getWorld().getName()
                                + " " + area.getLocation().getBlockX()
                                + " " + area.getLocation().getBlockY()
                                + " " + area.getLocation().getBlockZ(),
                        "/guidebook warp " + areaName,
                        "Click for warp command.");
        FancyMessageUtil.send(sender, location);

        FancyMessageUtil.send(sender, shapeMessage(area));

        if (sender instanceof Player player) {
            selectArea(player, area);
        }
    }

    private static FancyMessage shapeMessage(InfoArea area) {
        FancyMessage message = new FancyMessage(MessageType.INFO_INDENTED, PluginData.getMessageUtil());
        return switch (area) {
            case SphericalInfoArea sphere ->
                message.addSimple(ChatColor.YELLOW + "Spherical area with radius " + sphere.getRadius());
            case CuboidInfoArea cuboid ->
                message.addTooltipped(
                        ChatColor.YELLOW + "Cuboid shaped area",
                        " min corner: (" + cuboid.getMinPos().getBlockX() + ","
                                + cuboid.getMinPos().getBlockY() + ","
                                + cuboid.getMinPos().getBlockZ() + ")\n"
                                + " max corner: (" + cuboid.getMaxPos().getBlockX() + ","
                                + cuboid.getMaxPos().getBlockY() + ","
                                + cuboid.getMaxPos().getBlockZ() + ")");
            case PrismoidInfoArea prism -> {
                StringBuilder cornerData = new StringBuilder();
                Integer[] xPoints = prism.getXPoints();
                Integer[] zPoints = prism.getZPoints();
                for (int i = 0; i < xPoints.length; i++) {
                    if (i > 0) cornerData.append("\n");
                    cornerData
                            .append("(")
                            .append(xPoints[i])
                            .append(",")
                            .append(zPoints[i])
                            .append(")");
                }
                yield message.addTooltipped(ChatColor.YELLOW + "Prism shaped area", " corners: (x,z)\n" + cornerData);
            }
            default -> message;
        };
    }

    // Sets the player's WorldEdit selection to the Area
    private static void selectArea(Player player, InfoArea area) {
        SessionManager manager = WorldEdit.getInstance().getSessionManager();
        com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
        LocalSession localSession = manager.get(actor);
        World actorWorld = actor.getWorld();

        RegionSelector areaSelection =
                switch (area) {
                    case SphericalInfoArea sphere ->
                        new SphereRegionSelector(
                                actorWorld, toBlockVector3(sphere.getLocation().toVector()), sphere.getRadius());
                    case CuboidInfoArea cuboid -> {
                        CuboidRegion region = (CuboidRegion) cuboid.getRegion();
                        yield new CuboidRegionSelector(
                                actorWorld,
                                toBlockVector3(region.getMinCorner()),
                                toBlockVector3(region.getMaxCorner()));
                    }
                    case PrismoidInfoArea prism -> {
                        List<BlockVector2> points = new ArrayList<>();
                        Integer[] xPoints = prism.getXPoints();
                        Integer[] zPoints = prism.getZPoints();
                        for (int i = 0; i < xPoints.length; i++) {
                            points.add(BlockVector2.at(xPoints[i], zPoints[i]));
                        }
                        yield new Polygonal2DRegionSelector(actorWorld, points, prism.getMinY(), prism.getMaxY());
                    }
                    default -> null;
                };
        if (areaSelection != null) {
            localSession.setRegionSelector(actorWorld, areaSelection);
        }
    }

    private static BlockVector3 toBlockVector3(Vector vector) {
        return BlockVector3.at(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }
}
