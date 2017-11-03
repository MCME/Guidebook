/*
 * Copyright (C) 2015 MCME
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mcmiddleearth.guidebook.conversation;

import com.mcmiddleearth.guidebook.GuidebookPlugin;
import com.mcmiddleearth.guidebook.command.GuidebookDescription;
import com.mcmiddleearth.guidebook.command.GuidebookShow;
import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.pluginutil.message.FancyMessage;
import com.mcmiddleearth.pluginutil.message.config.MessageParseException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 *
 * @author Eriol_Eandur
 */
public class TitleEditFactory implements ConversationAbandonedListener{
    
    private final ConversationFactory factory;
    
    public TitleEditFactory(Plugin plugin){
        factory = new ConversationFactory(plugin)
                .withModality(false)
                .withPrefix(new ConfirmationPrefix())
                .withEscapeSequence("!cancel")
                .withFirstPrompt(new TitleEditEnterShowTitlePrompt())
                .withTimeout(120)
                .addConversationAbandonedListener(this);
    }
    
    public void start(Player player, InfoArea area, String name) {
        Conversation conversation = factory.buildConversation(player);
        ConversationContext context = conversation.getContext();
        context.setSessionData("player", player);
        context.setSessionData("area", area);
        context.setSessionData("name", name);
        conversation.begin();
    }
   
    @Override
    public void conversationAbandoned(ConversationAbandonedEvent abandonedEvent) {
        ConversationContext cc = abandonedEvent.getContext();
        Player player = (Player) cc.getSessionData("player");
        InfoArea area = (InfoArea) cc.getSessionData("area");
        if (abandonedEvent.gracefulExit()) {
            try {
                PluginData.saveData(area);
            } catch (IOException ex) {
                sendIOErrorMessage(player);
                Logger.getLogger(GuidebookDescription.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
            sendTitleSetMessage(player);
        } else {
            sendEditCancelledMessage(player);
        }
    }

    private void sendTitleSetMessage(CommandSender cs) {
        PluginData.getMessageUtil().sendInfoMessage(cs, "Title of guidebook area was saved.");
    }

    protected void sendIOErrorMessage(CommandSender cs) {
        PluginData.getMessageUtil().sendErrorMessage(cs, "There was an error. Guidebook data were NOT saved.");
    }

    private void sendEditCancelledMessage(Player player) {
        PluginData.getMessageUtil().sendInfoMessage(player, "Guidebook description conversation was cancelled by command or timeout.");
    }

}
