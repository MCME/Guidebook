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

import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.guidebook.util.InputUtil;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;

/**
 * @author Eriol_Eandur
 */
public class DescriptionEditEnterDescriptionPrompt extends StringPrompt {

    @Override
    public String getPromptText(ConversationContext cc) {
        return "Enter a new description!";
    }

    @Override
    public Prompt acceptInput(ConversationContext cc, String input) {
        List<String> description = ((InfoArea) cc.getSessionData("area")).getDescription();
        Player player = (Player) cc.getSessionData("player");
        switch ((String) cc.getSessionData("mode")) {
            case "a":
                description.add(InputUtil.replaceAltColorCode(input));
                sendLineAddedMessage(player);
                break;
            case "i":
                int line = (int) cc.getSessionData("line");
                description.add(line - 1, InputUtil.replaceAltColorCode(input));
                sendLineInsertedMessage(player);
                break;
            case "r":
                line = (int) cc.getSessionData("line");
                description.set(line - 1, InputUtil.replaceAltColorCode(input));
                sendLineReplacedMessage(player);
                break;
            default:
                return Prompt.END_OF_CONVERSATION;
        }
        cc.setSessionData("save", true);
        return new DescriptionEditSavePrompt();
    }

    private void sendLineInsertedMessage(CommandSender cs) {
        PluginData.getMessageUtil().sendInfoMessage(cs, "New line inserted.");
    }

    private void sendLineReplacedMessage(CommandSender cs) {
        PluginData.getMessageUtil().sendInfoMessage(cs, "Line replaced.");
    }

    private void sendLineAddedMessage(CommandSender cs) {
        PluginData.getMessageUtil().sendInfoMessage(cs, "Line added.");
    }

}
