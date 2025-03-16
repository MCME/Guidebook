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
        super(new String[]{"s", "a", "i", "d", "r", "c", "x"});
    }

    @Override
    public String getPromptText(ConversationContext cc) {
        return "What do you want to do? \n's': show lines\n'a': add a line\n"
            + "'i': insert a line\n'd': delete a line\n'r': replace a line\n"
            + "'c': clear all lines\n'x': exit";
    }

    @Override
    protected String getFailedValidationText(ConversationContext context, String invalidInput) {
        return "Invalid input, type in chat one of the following letters or '!cancel'";
    }

    @Override
    protected Prompt acceptValidatedInput(ConversationContext cc, String input) {
        switch (input) {
            case "s":
                PluginData.getMessageUtil().sendInfoMessage((Player) cc.getSessionData("player"), "Current description:");
                int i = 1;
                for (String line : ((InfoArea) cc.getSessionData("area")).getDescription()) {
                    new FancyMessage(MessageType.HIGHLIGHT_NO_PREFIX, PluginData.getMessageUtil())
                        .addSimple(ChatColor.DARK_AQUA + "[" + i + "] ")
                        .addFancy(InputUtil.replaceColorCodeWithAltCode(line),
                            InputUtil.replaceColorCodeWithAltCode(line),
                            "Click to copy into chat.")
                        .send((Player) cc.getSessionData("player"));
                    //PluginData.getMessageUtil().sendIndentedInfoMessage((Player)cc.getSessionData("player"), "["+i+"] "+line);
                    i++;
                }
                return new DescriptionEditEnterSubcommandPrompt();
            case "c":
                ((InfoArea) cc.getSessionData("area")).getDescription().clear();
                sendDescriptionCleared((Player) cc.getSessionData("player"));
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
