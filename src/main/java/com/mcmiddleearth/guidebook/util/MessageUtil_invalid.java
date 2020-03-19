/*
 * Copyright (C) 2016 MCME
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
package com.mcmiddleearth.guidebook.util;

import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Ivanpl, Eriol_Eandur
 */

public class MessageUtil_invalid {
    
    private static final String PREFIX   = "[Guidebook] ";
    private static final String NOPREFIX = "";
    
    public static void sendErrorMessage(CommandSender sender, String message) {
        if (sender instanceof Player) {
            sender.sendMessage(ChatColor.RED + PREFIX + message);
        } else {
            sender.sendMessage(PREFIX + message);
        }
    }
    
    public static void sendInfoMessage(CommandSender sender, String message) {
        if (sender instanceof Player) {
            sender.sendMessage(ChatColor.AQUA + PREFIX + message);
        } else {
            sender.sendMessage(PREFIX + message);
        }
    }
    
    public static void sendNoPrefixInfoMessage(CommandSender sender, String message) {
        if (sender instanceof Player) {
            sender.sendMessage(ChatColor.AQUA + NOPREFIX + message);
        } else {
            sender.sendMessage(NOPREFIX + message);
        }
    }
    
    public static void sendBroadcastMessage(String string) {
        Bukkit.getServer().broadcastMessage(ChatColor.AQUA + PREFIX + string);
    }

    public static void sendClickableMessage(Player sender, String message, String onClickCommand) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw "+ sender.getName()+" "
                +"{ text:\""+message+"\", "
                  +"clickEvent:{ action:run_command,"
                               + "value:\""+ onClickCommand +"\"}}");
    }
        
    public static void sendClickableMessage(Player sender, Map<String,String> data) {
        String rawText = "tellraw "+ sender.getName()+" [";
        boolean first = true;
        for(String message: data.keySet()) {
            if(first) {
                first = false; 
            }
            else {
                rawText = rawText.concat(",");
            }
            rawText = rawText.concat("{text:\""+message+"\"");
            String command = data.get(message);
            if(command!=null) {
                rawText = rawText.concat(",clickEvent:{ action:run_command,value:\"");
                rawText = rawText.concat(command+"\"}");
            }
            rawText = rawText.concat("}");
        }
        rawText = rawText.concat("]");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rawText);
    }

    public static String getPREFIX() {
        return PREFIX;
    }

    public static String getNOPREFIX() {
        return NOPREFIX;
    }
}
