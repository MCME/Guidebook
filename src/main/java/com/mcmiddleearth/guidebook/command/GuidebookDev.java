/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.util.DevUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Eriol_Eandur
 */
public class GuidebookDev extends GuidebookCommand {

    public GuidebookDev(String... permissionNodes) {
        super(0, false, permissionNodes);
        setShortDescription(": ");
        setUsageDescription(": ");
    }

    @Override
    protected void execute(CommandSender cs, String... args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("true")) {
            DevUtil.setConsoleOutput(true);
            showDetails(cs);
            return;
        } else if (args.length > 0 && args[0].equalsIgnoreCase("false")) {
            DevUtil.setConsoleOutput(false);
            showDetails(cs);
            return;
        } else if (args.length > 0) {
            try {
                int level = Integer.parseInt(args[0]);
                DevUtil.setLevel(level);
                showDetails(cs);
                return;
            } catch (NumberFormatException e) {
            }
            ;
        }
        if (cs instanceof Player) {
            Player player = (Player) cs;
            if (args.length > 0 && args[0].equalsIgnoreCase("r")) {
                DevUtil.remove(player);
                showDetails(cs);
                return;
            }
            DevUtil.add(player);
            showDetails(cs);
        }
    }

    private void showDetails(CommandSender cs) {
        cs.sendMessage("DevUtil: Level - " + DevUtil.getLevel() + "; Console - " + DevUtil.isConsoleOutput() + "; ");
        cs.sendMessage("         Developer:");
        for (OfflinePlayer player : DevUtil.getDeveloper()) {
            cs.sendMessage("                " + player.getName());
        }
    }
}

