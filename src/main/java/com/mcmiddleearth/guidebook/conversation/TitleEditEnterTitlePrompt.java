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
public class TitleEditEnterTitlePrompt extends StringPrompt {

    @Override
    public String getPromptText(ConversationContext cc) {
        InfoArea area = ((InfoArea) cc.getSessionData("area"));
        if (area.isShowTitle()) {
            return "Enter a new title with 20 characters at most. You can give more information in following subtitle.";
        } else {
            return "Enter a new title.";
        }
    }

    @Override
    public Prompt acceptInput(ConversationContext cc, String input) {
        InfoArea area = ((InfoArea) cc.getSessionData("area"));
        if (area.isShowTitle()) {
            area.setTitle(InputUtil.replaceAltColorCode(input).substring(0, Math.min(input.length(), 20)));
            return new TitleEditEnterSubtitlePrompt();
        } else {
            area.setTitle(InputUtil.replaceAltColorCode(input));
            return Prompt.END_OF_CONVERSATION;
        }
    }

}
