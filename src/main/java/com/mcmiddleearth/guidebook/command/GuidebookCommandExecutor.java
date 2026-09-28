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
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;

/**
 * The legacy {@code /guidebook} dispatcher. TEMPORARY until ticket 08: {@link LegacyFallthrough} routes every
 * subcommand not yet migrated to Brigadier here.
 *
 * @author Eriol_Eandur
 */
class GuidebookCommandExecutor {
    private final Map<String, GuidebookCommand> commands = new LinkedHashMap<>();

    private final String permissionStaff = "guidebook.staff";

    GuidebookCommandExecutor() {

        addCommandHandler("help", new GuidebookHelp(this, permissionStaff));
        addCommandHandler("list", new GuidebookList(permissionStaff));
        addCommandHandler("size", new GuidebookSize(permissionStaff));
        addCommandHandler("description", new GuidebookDescription(permissionStaff));
        addCommandHandler("title", new GuidebookTitle(permissionStaff));
        addCommandHandler("reload", new GuidebookReload(permissionStaff));
        addCommandHandler("dev", new GuidebookDev(permissionStaff));
    }

    void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendNoSubcommandErrorMessage(sender);
            return;
        }
        if (commands.containsKey(args[0].toLowerCase())) {
            commands.get(args[0].toLowerCase()).handle(sender, Arrays.copyOfRange(args, 1, args.length));
        } else {
            sendSubcommandNotFoundErrorMessage(sender);
        }
    }

    List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return GuidebookCommand.startingWith(args[0], commands.keySet());
        }

        GuidebookCommand subcommand = commands.get(args[0].toLowerCase());
        if (subcommand == null) {
            return List.of();
        }
        return subcommand.getCompletions(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    private void sendNoSubcommandErrorMessage(CommandSender cs) {
        // MessageUtil.sendErrorMessage(cs, "You're missing subcommand name for this command.");
        PluginDescriptionFile descr = GuidebookPlugin.getPluginInstance().getDescription();
        PluginData.getMessageUtil().sendErrorMessage(cs, descr.getName() + " - version " + descr.getVersion());
    }

    private void sendSubcommandNotFoundErrorMessage(CommandSender cs) {
        PluginData.getMessageUtil().sendErrorMessage(cs, "Subcommand not found.");
    }

    private void addCommandHandler(String name, GuidebookCommand handler) {
        commands.put(name, handler);
    }

    Map<String, GuidebookCommand> getCommands() {
        return commands;
    }
}
