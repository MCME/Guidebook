/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.GuidebookPlugin;
import com.mcmiddleearth.guidebook.conversation.ConfirmationFactory;
import com.mcmiddleearth.guidebook.conversation.Confirmationable;
import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code delete <area>}: deletes an Area after the player confirms.
 *
 * @author Eriol_Eandur
 */
final class GuidebookDelete {

    private GuidebookDelete() {}

    static void delete(CommandSender sender, InfoArea area) {
        // The confirmation is a chat conversation, which only a player can answer. Ticket 10 replaces it.
        if (!(sender instanceof Player player)) {
            PluginData.getMessageUtil()
                    .sendErrorMessage(
                            sender,
                            "Deleting a Guidebook area needs a confirmation that only a player can give. Run delete in game.");
            return;
        }
        new ConfirmationFactory(GuidebookPlugin.getPluginInstance())
                .start(
                        player,
                        "Do you really want to delete Guidebook area " + area.getName() + "?",
                        new Confirmationable() {
                            @Override
                            public void confirmed(Player player) {
                                if (PluginData.deleteInfoArea(area)) {
                                    PluginData.getMessageUtil()
                                            .sendInfoMessage(
                                                    player, "Guidebook area " + area.getName() + " was deleted.");
                                } else {
                                    PluginData.getMessageUtil()
                                            .sendErrorMessage(
                                                    player,
                                                    "There was an error while deleting the data file from disk.");
                                }
                            }

                            @Override
                            public void cancelled(Player player) {
                                PluginData.getMessageUtil()
                                        .sendErrorMessage(player, "You cancelled deleting of the area.");
                            }
                        });
    }
}
