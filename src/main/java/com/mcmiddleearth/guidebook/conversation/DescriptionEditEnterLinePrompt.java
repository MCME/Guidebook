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
import com.mcmiddleearth.pluginutil.NumericUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.NumericPrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;

/**
 * @author Eriol_Eandur
 */
public class DescriptionEditEnterLinePrompt extends NumericPrompt {

    @Override
    public String getPromptText(ConversationContext cc) {
        return "Enter the number of the line to edit!";
    }

    @Override
    protected String getFailedValidationText(ConversationContext context, String invalidInput) {
        switch ((String) context.getSessionData("mode")) {
            case "e":
                return "Type in chat the number of the line you want to edit.";
            case "i":
                return "Type in chat the number of the line you want to insert in front of.";
            case "d":
                return "Type in chat the number of the line you want to delete.";
        }
        return "Error!!!";
    }

    @Override
    protected boolean isInputValid(ConversationContext cc, String input) {
        if (!NumericUtil.isInt(input)) {
            return false;
        }
        int line = NumericUtil.getInt(input);
        InfoArea area = (InfoArea) cc.getSessionData("area");
        return line > 0 && line <= area.getDescription().size();
    }

    @Override
    protected Prompt acceptValidatedInput(ConversationContext cc, Number input) {
        if (((String) cc.getSessionData("mode")).equalsIgnoreCase("d")) {
            ((InfoArea) cc.getSessionData("area")).getDescription().remove(input.intValue() - 1);
            sendLineRemovedMessage((Player) cc.getSessionData("player"));
            cc.setSessionData("save", true);
            return new DescriptionEditSavePrompt();
        }
        cc.setSessionData("line", input.intValue());
        return new DescriptionEditEnterDescriptionPrompt();
    }

    private void sendLineRemovedMessage(CommandSender cs) {
        PluginData.getMessageUtil().sendInfoMessage(cs, "Line deleted.");
    }

}
