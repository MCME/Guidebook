/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.guidebook.util.DescriptionRenderer;
import com.mcmiddleearth.guidebook.util.GuidebookMessages;
import com.mcmiddleearth.pluginutil.message.FancyMessage;
import com.mcmiddleearth.pluginutil.message.MessageType;
import com.mcmiddleearth.pluginutil.message.config.FancyMessageConfigUtil;
import com.mcmiddleearth.pluginutil.message.config.MessageParseException;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

/**
 * Sends an Area's Description as a player entering it sees it.
 *
 * @author Eriol_Eandur
 */
public final class GuidebookShow {

    private GuidebookShow() {}

    public static void sendDescription(CommandSender recipient, InfoArea area) throws MessageParseException {
        if (area.getDescription().isEmpty()) {
            GuidebookMessages.send(
                    recipient,
                    GuidebookMessages.info(
                            Component.text("Welcome to "),
                            Component.text()
                                    .color(GuidebookMessages.STRESSED)
                                    .append(GuidebookMessages.title(area.getTitle()))
                                    .append(Component.text(" (" + area.getName() + ")")),
                            Component.text(". Unfortunately there is no further description for this area.")));
        } else {
            GuidebookMessages.send(
                    recipient,
                    DescriptionRenderer.toComponent(FancyMessageConfigUtil.addFromStringList(
                                    new FancyMessage(MessageType.WHITE, PluginData.getMessageUtil()),
                                    area.getDescription())
                            .setRunDirect()));
        }
    }
}
