/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.pluginutil.message.FancyMessage;
import com.mcmiddleearth.pluginutil.message.MessageType;
import com.mcmiddleearth.pluginutil.message.config.FancyMessageConfigUtil;
import com.mcmiddleearth.pluginutil.message.config.MessageParseException;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Eriol_Eandur
 */
public class GuidebookShow extends GuidebookCommand {

    public GuidebookShow(String... permissionNodes) {
        super(1, true, permissionNodes);
        setShortDescription(": Shows the description message of a Guidebook area.");
        setUsageDescription(" <AreaName>: Shows the stored description message of details of area <AreaName> as shown to a player entering the area.");
    }

    @Override
    protected void execute(CommandSender cs, String... args) {
        InfoArea area = PluginData.getInfoArea(args[0]);
        if (area == null) {
            sendNoAreaErrorMessage(cs);
        } else {
            PluginData.getMessageUtil().sendInfoMessage(cs, "Welcome message for Guidebook area " + args[0] + ":");
            try {
                sendDescription((Player) cs, area);
            } catch (MessageParseException ex) {
                Logger.getLogger(GuidebookShow.class.getName()).log(Level.SEVERE, null, ex);
                PluginData.getMessageUtil().sendErrorMessage(cs, "There was an error while loading the message.");
            }
        }
    }

    public static void sendDescription(Player player, InfoArea area) throws MessageParseException {
        if (area.getDescription().isEmpty()) {
            PluginData.getMessageUtil().sendInfoMessage(player, "Welcome to "
                + PluginData.getMessageUtil().STRESSED + area.getTitle()
                + PluginData.getMessageUtil().INFO
                + ". Unfortunately there is no further description for this area.");
        } else {
            FancyMessageConfigUtil.addFromStringList(new FancyMessage(MessageType.WHITE, PluginData.getMessageUtil()),
                    area.getDescription())
                .setRunDirect()
                .send(player);
        }
    }

}
