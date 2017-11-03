/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.data.PluginData;
import org.bukkit.command.CommandSender;

/**
 *
 * @author Eriol_Eandur
 */
public class GuidebookReload extends GuidebookCommand{
    
    public GuidebookReload(String... permissionNodes) {
        super(0, true, permissionNodes);
        setShortDescription(": Reloads all guidebook areas.");
        setUsageDescription(": Reloads all guidebook areas.");
    }
    
    @Override
    protected void execute(CommandSender cs, String... args) {
        PluginData.loadData();
        PluginData.getMessageUtil().sendInfoMessage(cs, "All guidebook areas reloaded from file.");
    }
    
}
