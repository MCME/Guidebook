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
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;

/**
 *
 * @author Eriol_Eandur
 */
public class DescriptionEditSavePrompt extends MessagePrompt{

    @Override
    protected Prompt getNextPrompt(ConversationContext cc) {
        Player player = (Player) cc.getSessionData("player");
        InfoArea area = (InfoArea) cc.getSessionData("area");
        if((Boolean) cc.getSessionData("save")) {
            try {
                PluginData.saveData(area);
            } catch (IOException ex) {
                sendIOErrorMessage(player);
                Logger.getLogger(GuidebookDescription.class.getName()).log(Level.SEVERE, null, ex);
                return Prompt.END_OF_CONVERSATION;
            }
            sendDescriptionSetMessage(player);
            try {
                GuidebookShow.sendDescription(player, area);
            } catch (MessageParseException ex) {
                Logger.getLogger(GuidebookDescription.class.getName()).log(Level.SEVERE, null, ex);
                sendParseError(player);
            }
        }
        cc.setSessionData("save", false);
        return new DescriptionEditEnterSubcommandPrompt();
    }

    @Override
    public String getPromptText(ConversationContext cc) {
        return "....";
    }
    
    private void sendParseError(Player cs) {
        PluginData.getMessageUtil().sendErrorMessage(cs, "There was an error while loading the Descriptions. Probably you entered an invalid description.");
    }

    private void sendDescriptionSetMessage(CommandSender cs) {
        PluginData.getMessageUtil().sendInfoMessage(cs, "This description of guidebook area was saved:");
    }

    protected void sendIOErrorMessage(CommandSender cs) {
        PluginData.getMessageUtil().sendErrorMessage(cs, "There was an error. Guidebook data were NOT saved.");
    }

}
