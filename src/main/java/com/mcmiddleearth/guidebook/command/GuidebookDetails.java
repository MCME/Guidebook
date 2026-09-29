/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.data.CuboidInfoArea;
import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PrismoidInfoArea;
import com.mcmiddleearth.guidebook.data.SphericalInfoArea;
import com.mcmiddleearth.guidebook.util.GuidebookMessages;
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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
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

        GuidebookMessages.startBlock(sender);
        GuidebookMessages.send(
                sender,
                GuidebookMessages.info(
                        Component.text("Details for Guidebook area "),
                        GuidebookMessages.suggestsCommand(
                                GuidebookMessages.stressed(areaName)
                                        .append(Component.text(".", GuidebookMessages.INFO)),
                                "/guidebook show " + areaName,
                                "Click for welcome message.")));

        Location location = area.getLocation();
        GuidebookMessages.send(
                sender,
                GuidebookMessages.infoIndented(GuidebookMessages.suggestsCommand(
                        Component.text("Location", NamedTextColor.GOLD)
                                .append(Component.text(
                                        ": " + location.getWorld().getName()
                                                + " " + location.getBlockX()
                                                + " " + location.getBlockY()
                                                + " " + location.getBlockZ(),
                                        NamedTextColor.YELLOW)),
                        "/guidebook warp " + areaName,
                        "Click for warp command.")));

        GuidebookMessages.send(sender, GuidebookMessages.infoIndented(shapeLine(area)));

        if (sender instanceof Player player) {
            selectArea(player, area);
        }
    }

    // The Shape, with its corners in a tooltip for cuboids and prisms
    private static Component shapeLine(InfoArea area) {
        return switch (area) {
            case SphericalInfoArea sphere ->
                Component.text("Spherical area with radius " + sphere.getRadius(), NamedTextColor.YELLOW);
            case CuboidInfoArea cuboid ->
                tooltipped(
                        "Cuboid shaped area",
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
                yield tooltipped("Prism shaped area", " corners: (x,z)\n" + cornerData);
            }
            default -> Component.empty();
        };
    }

    private static Component tooltipped(String text, String tooltip) {
        return GuidebookMessages.withHover(Component.text(text, NamedTextColor.YELLOW), tooltip);
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
