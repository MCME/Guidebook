/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.guidebook.dialog.ConfirmDialog;
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
        // The confirmation is a Dialog, which only a player can answer
        if (!(sender instanceof Player player)) {
            PluginData.getMessageUtil()
                    .sendErrorMessage(
                            sender,
                            "Deleting a Guidebook area needs a confirmation that only a player can give. Run delete in game.");
            return;
        }
        ConfirmDialog.openToDelete(player, area, confirmer -> {
            if (PluginData.deleteInfoArea(area)) {
                PluginData.getMessageUtil()
                        .sendInfoMessage(confirmer, "Guidebook area " + area.getName() + " was deleted.");
            } else {
                PluginData.getMessageUtil()
                        .sendErrorMessage(confirmer, "There was an error while deleting the data file from disk.");
            }
        });
    }
}
