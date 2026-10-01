/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.util.AreaText;
import com.mcmiddleearth.guidebook.util.GuidebookMessages;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

/**
 * Sends an Area's Description as a player entering it sees it.
 *
 * @author Eriol_Eandur
 */
public final class GuidebookShow {

    private GuidebookShow() {}

    public static void sendDescription(CommandSender recipient, InfoArea area) {
        if (area.getDescription().isEmpty()) {
            GuidebookMessages.send(
                    recipient,
                    GuidebookMessages.info(
                            Component.text("Welcome to "),
                            Component.text()
                                    .color(GuidebookMessages.STRESSED)
                                    .append(AreaText.render(area.getTitle()))
                                    .append(Component.text(" (" + area.getName() + ")")),
                            Component.text(". Unfortunately there is no further description for this area.")));
        } else {
            GuidebookMessages.send(recipient, AreaText.render(area.getDescription()));
        }
    }
}
