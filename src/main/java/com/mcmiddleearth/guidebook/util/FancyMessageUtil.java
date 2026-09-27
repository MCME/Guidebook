package com.mcmiddleearth.guidebook.util;

import com.mcmiddleearth.pluginutil.message.FancyMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class FancyMessageUtil {

    private FancyMessageUtil() {}

    /**
     * {@link FancyMessage#send} only reaches players, because it runs {@code /tellraw}. Anyone else, such as the
     * console, gets the message's text without its click and hover actions.
     */
    public static void send(CommandSender recipient, FancyMessage message) {
        if (recipient instanceof Player player) {
            message.send(player);
            return;
        }
        StringBuilder text = new StringBuilder().append(message.getBaseColor());
        for (String[] part : message.getData()) {
            text.append(part[0]);
        }
        recipient.sendMessage(text.toString());
    }
}
