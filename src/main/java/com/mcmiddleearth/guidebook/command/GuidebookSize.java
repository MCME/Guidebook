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
import com.mcmiddleearth.guidebook.data.Shape;
import com.mcmiddleearth.guidebook.data.SphericalInfoArea;
import com.mcmiddleearth.guidebook.util.GuidebookMessages;
import io.papermc.paper.math.BlockPosition;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

/**
 * {@code size <area> [radius|corners|height ...]}: changes the dimensions of an Area. Each form applies to one Shape.
 * Bare {@code size <area>}, or a form that doesn't match the Area's Shape, names the one that does.
 *
 * @author Eriol_Eandur
 */
final class GuidebookSize {

    private GuidebookSize() {}

    /** Bare {@code size <area>}: shows the Area's Shape and dimensions, and the form that resizes it. */
    static void show(CommandSender sender, InfoArea area) {
        PluginData.getMessageUtil()
                .sendInfoMessage(sender, "Guidebook area " + area.getName() + " is a " + dimensions(area) + ".");
        sendForm(sender, area);
    }

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
        PluginData.getMessageUtil()
                .sendErrorMessage(
                        sender,
                        "Guidebook area " + area.getName() + " is a "
                                + area.getShape().displayName() + ".");
        sendForm(sender, area);
    }

    /** The {@code size} form for each Shape: the literal, then the arguments it takes. */
    private record Form(String literal, String arguments) {

        static Form of(Shape shape) {
            return switch (shape) {
                case SPHERE -> new Form("radius", "<radius>");
                case CUBOID -> new Form("corners", "<pos1> <pos2>");
                case PRISM -> new Form("height", "<minY> <maxY>");
            };
        }
    }

    // Clicking the form fills in the command up to its arguments
    private static void sendForm(CommandSender sender, InfoArea area) {
        Form form = Form.of(area.getShape());
        String command = "/guidebook size " + area.getName() + " " + form.literal() + " ";
        GuidebookMessages.send(
                sender,
                GuidebookMessages.info(
                        Component.text("Resize it with "),
                        GuidebookMessages.suggestsCommand(
                                GuidebookMessages.stressed(command + form.arguments()),
                                command,
                                "Click to fill in the command.")));
    }

    private static String dimensions(InfoArea area) {
        return switch (area) {
            case SphericalInfoArea sphere -> "sphere with radius " + sphere.getRadius();
            case CuboidInfoArea cuboid ->
                "cuboid from " + blockText(cuboid.getMinPos()) + " to " + blockText(cuboid.getMaxPos());
            case PrismoidInfoArea prism -> "prism from Y " + prism.getMinY() + " to " + prism.getMaxY();
            default -> area.getShape().displayName();
        };
    }

    private static String blockText(Vector position) {
        return position.getBlockX() + " " + position.getBlockY() + " " + position.getBlockZ();
    }
}
