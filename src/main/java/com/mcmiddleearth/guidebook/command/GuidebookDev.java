/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.guidebook.util.DevUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

/**
 * @author Eriol_Eandur
 */
final class GuidebookDev {

    private GuidebookDev() {}

    /** Shows the debug level, whether debug output goes to the console, and the developers watching it in chat. */
    static void showState(CommandSender sender) {
        PluginData.getMessageUtil()
                .sendInfoMessage(
                        sender,
                        "Debug level: " + DevUtil.getLevel() + "; console output: " + DevUtil.isConsoleOutput());
        if (DevUtil.getDeveloper().isEmpty()) {
            PluginData.getMessageUtil().sendIndentedInfoMessage(sender, "No developers are watching.");
            return;
        }
        PluginData.getMessageUtil().sendIndentedInfoMessage(sender, "Watching developers:");
        for (OfflinePlayer developer : DevUtil.getDeveloper()) {
            PluginData.getMessageUtil().sendIndentedInfoMessage(sender, "- " + developer.getName());
        }
    }
}
