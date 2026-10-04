/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.util.DevUtil;
import com.mcmiddleearth.guidebook.util.GuidebookMessages;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

/**
 * @author Eriol_Eandur
 */
final class GuidebookDev {

    private GuidebookDev() {}

    /** Shows the debug level, whether debug output goes to the console, and the developers watching it in chat. */
    static void showState(CommandSender sender) {
        GuidebookMessages.sendInfo(
                sender, "Debug level: " + DevUtil.getLevel() + "; console output: " + DevUtil.isConsoleOutput());
        if (DevUtil.getDeveloper().isEmpty()) {
            sendIndented(sender, "No developers are watching.");
            return;
        }
        sendIndented(sender, "Watching developers:");
        for (OfflinePlayer developer : DevUtil.getDeveloper()) {
            sendIndented(sender, "- " + developer.getName());
        }
    }

    private static void sendIndented(CommandSender sender, String text) {
        GuidebookMessages.send(sender, GuidebookMessages.infoIndented(Component.text(text)));
    }
}
