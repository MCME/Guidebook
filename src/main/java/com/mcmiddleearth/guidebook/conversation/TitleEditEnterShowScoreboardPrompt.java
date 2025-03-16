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
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ValidatingPrompt;

/**
 * @author Eriol_Eandur
 */
public class TitleEditEnterShowScoreboardPrompt extends ValidatingPrompt {

    @Override
    public String getPromptText(ConversationContext cc) {
        if (((InfoArea) cc.getSessionData("area")).isShowTitle()) {
            return "Should this Guidebook area show it's title also at the top of screen as long as the player is inside the area?";
        } else {
            return "Should this Guidebook area show a title at the top of screen as long as the player is inside the area?";
        }
    }

    @Override
    protected String getFailedValidationText(ConversationContext context, String invalidInput) {
        return "Type in 'yes' or 'no'.";
    }

    @Override
    protected boolean isInputValid(ConversationContext cc, String input) {
        return input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("no");
    }

    @Override
    protected Prompt acceptValidatedInput(ConversationContext cc, String input) {
        InfoArea area = (InfoArea) cc.getSessionData("area");
        if (input.equalsIgnoreCase("yes")) {
            area.setShowScoreboard(true);
            if (area.isShowTitle()) {
                return Prompt.END_OF_CONVERSATION;
            } else {
                return new TitleEditEnterTitlePrompt();
            }
        } else {
            area.setShowScoreboard(false);
            return Prompt.END_OF_CONVERSATION;
        }
    }

}
