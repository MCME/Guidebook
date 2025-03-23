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
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eriol_Eandur
 */
public class GuidebookDetails extends GuidebookCommand {

    public GuidebookDetails(String... permissionNodes) {
        super(1, true, permissionNodes);
        setShortDescription(": Shows details of a Guidebook area.");
        setUsageDescription(" <AreaName>: Displays the Area shape and location (position of the area creator),\n" +
            "also sets the WorldEdit selection of the user to that of the Area");
    }

    @Override
    protected List<String> getCompletions(CommandSender cs, String... args) {
        if (args.length == 1) {
            return PluginData.getAreaNames();
        }

        return List.of();
    }

    @Override
    protected void execute(CommandSender cs, String... args) {
        String areaName = args[0];
        InfoArea area = PluginData.getInfoArea(areaName);
        if (area == null) {
            sendNoAreaErrorMessage(cs);
            return;
        }

        Player sender = (Player) cs;

        new FancyMessage(MessageType.INFO, PluginData.getMessageUtil())
            .addSimple("Details for Guidebook area ")
            .addFancy(PluginData.getMessageUtil().STRESSED + areaName + PluginData.getMessageUtil().INFO + ".",
                "/guidebook show " + areaName,
                "Click for welcome message.")
            .send(sender);

        new FancyMessage(MessageType.INFO_INDENTED, PluginData.getMessageUtil())
            .addFancy(ChatColor.GOLD
                    + "Location" + ChatColor.YELLOW
                    + ": " + area.getLocation().getWorld().getName()
                    + " " + area.getLocation().getBlockX()
                    + " " + area.getLocation().getBlockY()
                    + " " + area.getLocation().getBlockZ(),
                "/guidebook warp " + areaName, "Click for warp command.")
            .send(sender);

        SessionManager manager = WorldEdit.getInstance().getSessionManager();
        com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(sender);
        LocalSession localSession = manager.get(actor);
        World actorWorld = actor.getWorld();

        // Displaying info for each area type and setting the player's world edit selection
        if (area instanceof SphericalInfoArea sphere) {
            new FancyMessage(MessageType.INFO_INDENTED, PluginData.getMessageUtil())
                .addSimple(ChatColor.YELLOW + "Spherical area with radius " + sphere.getRadius())
                .send((Player) cs);

            RegionSelector areaSelection = new SphereRegionSelector(actorWorld,
                toBlockVector3(sphere.getLocation().toVector()), sphere.getRadius()
            );
            localSession.setRegionSelector(actorWorld, areaSelection);
            return;
        }

        if (area instanceof CuboidInfoArea cuboid) {
            new FancyMessage(MessageType.INFO_INDENTED, PluginData.getMessageUtil())
                .addTooltipped(ChatColor.YELLOW + "Cuboid shaped area",
                    " min corner: (" + cuboid.getMinPos().getBlockX() + ","
                        + cuboid.getMinPos().getBlockY() + ","
                        + cuboid.getMinPos().getBlockZ() + ")\n"
                        + " max corner: (" + cuboid.getMaxPos().getBlockX() + ","
                        + cuboid.getMaxPos().getBlockY() + ","
                        + cuboid.getMaxPos().getBlockZ() + ")")
                .send(sender);

            CuboidRegion region = (CuboidRegion) cuboid.getRegion();
            RegionSelector areaSelection = new CuboidRegionSelector(
                actorWorld, toBlockVector3(region.getMinCorner()), toBlockVector3(region.getMaxCorner())
            );
            localSession.setRegionSelector(actorWorld, areaSelection);
            return;
        }

        if (area instanceof PrismoidInfoArea prism) {
            StringBuilder cornerData = new StringBuilder();
            List<BlockVector2> points = new ArrayList<>();
            Integer[] xPoints = prism.getXPoints();
            Integer[] zPoints = prism.getZPoints();

            for (int i = 0; i < xPoints.length; i++) {
                points.add(BlockVector2.at(xPoints[i], zPoints[i]));

                if (i > 0) cornerData.append("\n");
                cornerData.append("(").append(xPoints[i]).append(",").append(zPoints[i]).append(")");
            }
            new FancyMessage(MessageType.INFO_INDENTED, PluginData.getMessageUtil())
                .addTooltipped(ChatColor.YELLOW + "Prism shaped area",
                    " corners: (x,z)\n" + cornerData)
                .send(sender);

            RegionSelector areaSelection = new Polygonal2DRegionSelector(
                actorWorld, points, prism.getMinY(), prism.getMaxY()
            );
            localSession.setRegionSelector(actorWorld, areaSelection);
            return;
        }
    }

    private static BlockVector3 toBlockVector3(Vector vector) {
        return BlockVector3.at(
            vector.getBlockX(),
            vector.getBlockY(),
            vector.getBlockZ()
        );
    }
}
