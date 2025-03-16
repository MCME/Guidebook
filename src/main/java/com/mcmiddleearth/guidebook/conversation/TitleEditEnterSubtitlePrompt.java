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
import com.mcmiddleearth.guidebook.util.InputUtil;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;

/**
 * @author Eriol_Eandur
 */
public class TitleEditEnterSubtitlePrompt extends StringPrompt {

    @Override
    public String getPromptText(ConversationContext cc) {
        return "Enter a new subtitle!";
    }

    @Override
    public Prompt acceptInput(ConversationContext cc, String input) {
        InfoArea area = ((InfoArea) cc.getSessionData("area"));
        area.setSubtitle(InputUtil.replaceAltColorCode(input));
        if (area.isShowTitle()) {
            return new TitleEditEnterShowScoreboardPrompt();
        } else {
            return Prompt.END_OF_CONVERSATION;
        }
    }

}
