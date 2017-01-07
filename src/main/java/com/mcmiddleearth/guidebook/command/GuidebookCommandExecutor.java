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
package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.GuidebookPlugin;
import com.mcmiddleearth.guidebook.data.PluginData;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;

/**
 *
 * @author Eriol_Eandur
 */
public class GuidebookCommandExecutor implements CommandExecutor {

    @Getter
    private final Map <String, GuidebookCommand> commands = new LinkedHashMap <>();
    
    private final String permission = "guidebook.user";
    private final String permissionStaff = "guidebook.staff";
    
    public GuidebookCommandExecutor() {
        addCommandHandler("delete", new GuidebookDelete(permissionStaff));
        addCommandHandler("details", new GuidebookDetails(permissionStaff));
        addCommandHandler("help", new GuidebookHelp(permissionStaff));
        addCommandHandler("list", new GuidebookList(permissionStaff));
        addCommandHandler("set", new GuidebookSet(permissionStaff));
        addCommandHandler("show", new GuidebookShow(permissionStaff));
        addCommandHandler("size", new GuidebookSize(permissionStaff));
        addCommandHandler("warp", new GuidebookWarp(permissionStaff));
        addCommandHandler("description", new GuidebookDescription(permissionStaff));
        addCommandHandler("title", new GuidebookTitle(permissionStaff));
        addCommandHandler("on", new GuidebookOn(permission));
        addCommandHandler("off", new GuidebookOff(permission));
        addCommandHandler("reload", new GuidebookReload(permissionStaff));
        addCommandHandler("dev", new GuidebookDev(permissionStaff));
    }
    
    @Override
    public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] strings) {
        if(!string.equalsIgnoreCase("guidebook")) {
            return false;
        }
        if(strings == null || strings.length == 0) {
            sendNoSubcommandErrorMessage(cs);
            return true;
        }
        if(commands.containsKey(strings[0].toLowerCase())) {
            commands.get(strings[0].toLowerCase()).handle(cs, Arrays.copyOfRange(strings, 1, strings.length));
        } else {
            sendSubcommandNotFoundErrorMessage(cs);
        }
        return true;
    }
    
    private void sendNoSubcommandErrorMessage(CommandSender cs) {
        //MessageUtil.sendErrorMessage(cs, "You're missing subcommand name for this command.");
        PluginDescriptionFile descr = GuidebookPlugin.getPluginInstance().getDescription();
        PluginData.getMessageUtil().sendErrorMessage(cs, descr.getName()+" - version "+descr.getVersion());
    }
    
    private void sendSubcommandNotFoundErrorMessage(CommandSender cs) {
        PluginData.getMessageUtil().sendErrorMessage(cs, "Subcommand not found.");
    }
    
    private void addCommandHandler(String name, GuidebookCommand handler) {
        commands.put(name, handler);
    }
    
}
