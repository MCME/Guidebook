/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.pluginutil.message.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * @author Eriol_Eandur
 */
public class GuidebookWarp extends GuidebookCommand {

    public GuidebookWarp(String... permissionNodes) {
        super(1, true, permissionNodes);
        setShortDescription(": Warp to a Guidebook area.");
        setUsageDescription(" <AreaName>: Warps the player who issues this command to Guidebook area <AreaName>.");
    }

    @Override
    protected void execute(CommandSender cs, String... args) {
        InfoArea area = PluginData.getInfoArea(args[0]);
        if (area == null) {
            sendNoAreaErrorMessage(cs);
        } else {
            ((Player) cs).teleport(area.getLocation());
            sendWelcomeToCenter(cs, args[0]);
        }
    }

    @Override
    protected List<String> getCompletions(CommandSender cs, String... args) {
        if (args.length == 1) {
            return PluginData.getAreaNames();
        }

        return List.of();
    }

    private void sendWelcomeToCenter(CommandSender cs, String arg) {
        PluginData.getMessageUtil().sendInfoMessage(cs, "You are now at Guidebook area " + arg + ".");
    }

}
