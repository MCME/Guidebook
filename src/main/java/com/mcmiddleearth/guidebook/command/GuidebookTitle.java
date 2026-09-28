/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.GuidebookPlugin;
import com.mcmiddleearth.guidebook.conversation.TitleEditFactory;
import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import org.bukkit.entity.Player;

/**
 * {@code title <area>}: starts the chat conversation that edits an Area's Title, Subtitle, Show title and Boss bar.
 * Ticket 09 replaces it with the Edit dialog.
 *
 * @author Eriol_Eandur
 */
final class GuidebookTitle {

    private GuidebookTitle() {}

    static void start(Player player, InfoArea area) {
        if (player.isConversing()) {
            sendAlreadyConversing(player);
            return;
        }
        new TitleEditFactory(GuidebookPlugin.getPluginInstance()).start(player, area, area.getName());
    }

    static void sendAlreadyConversing(Player player) {
        PluginData.getMessageUtil().sendErrorMessage(player, "You are already in a conversation.");
    }
}
