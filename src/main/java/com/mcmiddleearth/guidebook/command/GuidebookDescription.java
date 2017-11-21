/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.GuidebookPlugin;
import com.mcmiddleearth.guidebook.conversation.DescriptionEditFactory;
import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.pluginutil.message.config.MessageParseException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/**
 *
 * @author Eriol_Eandur
 */
public class GuidebookDescription extends GuidebookCommand{
    
    public GuidebookDescription(String... permissionNodes) {
        super(1, true, permissionNodes);
        setShortDescription(": Defines the description of a Guidebook area.");
        setUsageDescription(" <AreaName>: Initiates a conversation to edit the Guidbook area's description.");
    }
    
    @Override
    protected void execute(CommandSender cs, String... args) {
        InfoArea area = PluginData.getInfoArea(args[0]);
        if(area==null) {
            sendNoAreaErrorMessage(cs);
        }
        else {
            if(args.length>1) {
                Player player = (Player)cs;
                if(args[1].equalsIgnoreCase("getbook")) {
                    player.getInventory().addItem(area.getDescriptionBook());
                    sendBookGivenMessage(cs);
                    return;
                } else if(args[1].equalsIgnoreCase("save")) {
                    ItemStack handItem = player.getInventory().getItemInMainHand();
                    if(!(handItem.getType().equals(Material.BOOK_AND_QUILL)
                            || handItem.getType().equals(Material.BOOK))) {
                        sendNoBookMessage(cs);
                        return;
                    } else {
                        try {
                            area.setDescription((BookMeta) handItem.getItemMeta());
                            try {
                                PluginData.saveArea(area);
                            } catch (IOException ex) {
                                sendIOErrorMessage(player);
                                Logger.getLogger(GuidebookDescription.class.getName()).log(Level.SEVERE, null, ex);
                            }
                           sendDescriptionSetMessage(cs);
                            GuidebookShow.sendDescription(player, area);
                        } catch (MessageParseException ex) {
                            Logger.getLogger(GuidebookDescription.class.getName()).log(Level.SEVERE, null, ex);
                            sendParseError(player);
                        }
                        return;
                    }
                }
            }
            if(((Player)cs).isConversing()) {
                sendAlreadyConversing((Player) cs);
            }
            new DescriptionEditFactory(GuidebookPlugin.getPluginInstance()).start((Player)cs, area, args[0]);
        }
    }
    
    private String getDescription(String[] args, int startIndex) {
        String areaDescription = args[startIndex];
        for(int i = startIndex+1; i<args.length;i++) {
            areaDescription = areaDescription + " "+args[i];
        }
        return areaDescription;
    }

    /*private void saveData(CommandSender cs){
        try {
            PluginData.saveData();
        } catch (IOException ex) {
            sendIOErrorMessage(cs);
            Logger.getLogger(GuidebookDescription.class.getName()).log(Level.SEVERE, null, ex);
        }
    }*/
    
    private void sendAlreadyConversing(CommandSender cs) {
        PluginData.getMessageUtil().sendErrorMessage(cs, "You are already in a converstion.");
    }

    private void sendDescriptionSetMessage(CommandSender cs) {
        PluginData.getMessageUtil().sendInfoMessage(cs, "Description of guidebook area was saved.");
    }

    private void sendLineInsertedMessage(CommandSender cs) {
        PluginData.getMessageUtil().sendInfoMessage(cs, "New line inserted.");
    }

    private void sendIndexOutOfBoundsMessage(CommandSender cs) {
        PluginData.getMessageUtil().sendErrorMessage(cs, "You specified an invalid line number.");
    }

    private void sendLineReplacedMessage(CommandSender cs) {
        PluginData.getMessageUtil().sendInfoMessage(cs, "Line replaced.");
    }

    private void sendLineRemovedMessage(CommandSender cs) {
        PluginData.getMessageUtil().sendInfoMessage(cs, "Line deleted.");
    }

    private void sendParseError(CommandSender cs) {
        PluginData.getMessageUtil().sendErrorMessage(cs, "There was an error while loading the Descriptions. Probably you entered an invalid description.");
    }

    private void sendLineAddedMessage(CommandSender cs) {
        PluginData.getMessageUtil().sendInfoMessage(cs, "Line added.");
    }

    private void sendBookGivenMessage(CommandSender cs) {
        PluginData.getMessageUtil().sendInfoMessage(cs, "Descriptions was written into a book and placed in your inventory.");
    }

    private void sendNoBookMessage(CommandSender cs) {
        PluginData.getMessageUtil().sendErrorMessage(cs, "No book in main hand to get the description from.");
    }

}
