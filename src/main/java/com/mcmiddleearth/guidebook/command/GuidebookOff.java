/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Eriol_Eandur
 */
public class GuidebookOff extends GuidebookCommand {

    public GuidebookOff(String... permissionNodes) {
        super(0, true, permissionNodes);
        setShortDescription(": Excludes a player from Guidebook messages.");
        setUsageDescription(": The player issuing this command will no longer receive Guidebook messages.");
    }

    @Override
    protected void execute(CommandSender cs, String... args) {
        Player player = (Player) cs;
        PluginData.exclude(player);

        for (InfoArea area : PluginData.getInfoAreas().values()) {
            if (area.isInformed(player)) {
                area.removeInformedPlayer(player);
            }
        }

        PluginData.getMessageUtil().sendInfoMessage(cs, "You will no longer receive info messages from Guidebook.");
    }

}
