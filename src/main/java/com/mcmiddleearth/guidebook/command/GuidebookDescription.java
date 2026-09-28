/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.GuidebookPlugin;
import com.mcmiddleearth.guidebook.conversation.DescriptionEditFactory;
import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.pluginutil.message.config.MessageParseException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/**
 * {@code description <area> [getbook|save]}: edits an Area's Description in a chat conversation, or through a writable
 * book. Ticket 09 replaces both with the Edit dialog.
 *
 * @author Eriol_Eandur
 */
final class GuidebookDescription {

    private GuidebookDescription() {}

    /** Bare {@code description <area>}: starts the Description conversation. */
    static void start(Player player, InfoArea area) {
        if (player.isConversing()) {
            GuidebookTitle.sendAlreadyConversing(player);
            return;
        }
        new DescriptionEditFactory(GuidebookPlugin.getPluginInstance()).start(player, area, area.getName());
    }

    /** {@code getbook}: gives the player a writable book holding the Description. */
    static void giveBook(Player player, InfoArea area) {
        player.getInventory().addItem(area.getDescriptionBook());
        PluginData.getMessageUtil()
                .sendInfoMessage(player, "The Description was written into a book and placed in your inventory.");
    }

    /** {@code save}: replaces the Description with the book in the player's main hand. */
    static void saveBook(Player player, InfoArea area) {
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (!(handItem.getType() == Material.WRITABLE_BOOK || handItem.getType() == Material.WRITTEN_BOOK)) {
            PluginData.getMessageUtil().sendErrorMessage(player, "No book in main hand to get the Description from.");
            return;
        }
        try {
            area.setDescription((BookMeta) handItem.getItemMeta());
        } catch (MessageParseException ex) {
            Logger.getLogger(GuidebookDescription.class.getName()).log(Level.SEVERE, null, ex);
            sendParseError(player);
            return;
        }
        try {
            PluginData.saveArea(area);
        } catch (IOException ex) {
            Logger.getLogger(GuidebookDescription.class.getName()).log(Level.SEVERE, null, ex);
            PluginData.getMessageUtil()
                    .sendErrorMessage(
                            player, "There was an error. Guidebook area " + area.getName() + " was NOT saved.");
            return;
        }
        PluginData.getMessageUtil()
                .sendInfoMessage(player, "Description of Guidebook area " + area.getName() + " was saved.");
        try {
            GuidebookShow.sendDescription(player, area);
        } catch (MessageParseException ex) {
            Logger.getLogger(GuidebookDescription.class.getName()).log(Level.SEVERE, null, ex);
            sendParseError(player);
        }
    }

    private static void sendParseError(Player player) {
        PluginData.getMessageUtil()
                .sendErrorMessage(
                        player,
                        "There was an error while loading the Descriptions. Probably you entered an invalid description.");
    }
}
