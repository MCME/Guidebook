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
import com.mcmiddleearth.pluginutil.message.FancyMessage;
import com.mcmiddleearth.pluginutil.message.MessageType;
import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.FixedSetPrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;

/**
 * @author Eriol_Eandur
 */
public class DescriptionEditEnterSubcommandPrompt extends FixedSetPrompt {

    public DescriptionEditEnterSubcommandPrompt() {
        super("s", "a", "i", "d", "r", "c", "x");
    }

    @Override
    public String getPromptText(ConversationContext cc) {
        return """
            What do you want to do?
            's': show lines
            'a': add a line
            'i': insert a line
            'd': delete a line
            'r': replace a line
            'c': clear all lines
            'x': exit""";
    }

    @Override
    protected String getFailedValidationText(ConversationContext context, String invalidInput) {
        return "Invalid input, type in chat one of the following letters or '!cancel'";
    }

    @Override
    protected Prompt acceptValidatedInput(ConversationContext cc, String input) {
        Player player = (Player) cc.getSessionData("player");
        InfoArea area = (InfoArea) cc.getSessionData("area");

        switch (input) {
            case "s":
                PluginData.getMessageUtil().sendInfoMessage(player, "Current description:");
                int i = 1;
                for (String line : area.getDescription()) {
                    new FancyMessage(MessageType.HIGHLIGHT_NO_PREFIX, PluginData.getMessageUtil())
                        .addSimple(ChatColor.DARK_AQUA + "[" + i + "] ")
                        .addFancy(InputUtil.replaceAltColorCode(line),
                            InputUtil.replaceAltColorCode(line),
                            "Click to copy into chat.")
                        .send(player);
                    i++;
                }
                return new DescriptionEditEnterSubcommandPrompt();
            case "c":
                area.getDescription().clear();
                sendDescriptionCleared(player);
                cc.setSessionData("save", true);
                return new DescriptionEditEnterSubcommandPrompt();
            case "a":
                cc.setSessionData("mode", input);
                return new DescriptionEditEnterDescriptionPrompt();
            case "x":
                return Prompt.END_OF_CONVERSATION;
            default:
                cc.setSessionData("mode", input);
                return new DescriptionEditEnterLinePrompt();
        }
    }

    public void sendDescriptionCleared(Player player) {
        PluginData.getMessageUtil().sendInfoMessage(player, "All lines cleared.");
    }
}
