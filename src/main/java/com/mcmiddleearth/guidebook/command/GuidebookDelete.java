/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.GuidebookPlugin;
import com.mcmiddleearth.guidebook.conversation.ConfirmationFactory;
import com.mcmiddleearth.guidebook.conversation.Confirmationable;
import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Eriol_Eandur
 */
public class GuidebookDelete extends GuidebookCommand implements Confirmationable{
    
    private String areaName;
    
    public GuidebookDelete(String... permissionNodes) {
        super(1, true, permissionNodes);
        setShortDescription(": Deletes a guidebook area.");
        setUsageDescription(" <AreaName>: Deletes guidebook area with name <AreaName>.");
    }
    
    @Override
    protected void execute(CommandSender cs, String... args) {
        InfoArea area = PluginData.getInfoArea(args[0]);
        if(area==null) {
            sendNoAreaErrorMessage(cs);
        }
        else {
            areaName = args[0];
            new ConfirmationFactory(GuidebookPlugin.getPluginInstance()).start((Player) cs, 
                        "Do you really want to delete guidebook area "+areaName+"?", this);
        }
    }

    @Override
    public void confirmed(Player player) {
        if(PluginData.deleteInfoArea(areaName)) {
            PluginData.getMessageUtil().sendInfoMessage(player, "Guidebook area was deleted.");
        } else {
            PluginData.getMessageUtil().sendErrorMessage(player, "There was an error while deleting the data file from disk.");
        }
    }

    @Override
    public void cancelled(Player player) {
        PluginData.getMessageUtil().sendErrorMessage(player, "You cancelled deleting of the area.");
    }
    
}
