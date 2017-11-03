/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.GuidebookPlugin;
import com.mcmiddleearth.guidebook.conversation.TitleEditFactory;
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
public class GuidebookTitle extends GuidebookCommand{
    
    public GuidebookTitle(String... permissionNodes) {
        super(1, true, permissionNodes);
        setShortDescription(": Defines the title of a Guidebook area.");
        setUsageDescription(" <AreaName>: Initiates a conversation to configure a Guidebook area's title.");
    }
    
    @Override
    protected void execute(CommandSender cs, String... args) {
        InfoArea area = PluginData.getInfoArea(args[0]);
        if(area==null) {
            sendNoAreaErrorMessage(cs);
        }
        else {
            if(((Player)cs).isConversing()) {
                sendAlreadyConversing((Player) cs);
            }
            new TitleEditFactory(GuidebookPlugin.getPluginInstance()).start((Player)cs, area, args[0]);
        /*    int line = -1;
            int descriptionIndexShift=0;
            if(args.length>3 && NumericUtil.isInt(args[2])) {
                    descriptionIndexShift=1;
                    line = Math.max(NumericUtil.getInt(args[2]),-1);
            }
Logger.getGlobal().log(Level.INFO, "Line *** {0}", line);
            if(args.length>3 || args[1].equals("-d")) {
                switch (args[1]) {
                    case "-i":
                    {
                        String areaDescription = getDescription(args,2+descriptionIndexShift);
                        if(line>0 && line<area.getDescription().size()) {
                            area.getDescription().add(line-1, areaDescription);
                            sendLineInsertedMessage(cs);
                        } else {
                            sendIndexOutOfBoundsMessage(cs);
                            return;
                        }
                        break;
                    }
                    case "-r":
                    {
                        String areaDescription = getDescription(args,2+descriptionIndexShift);
                        if(line>0 && line<area.getDescription().size()) {
                            area.getDescription().set(line-1, areaDescription);
                            sendLineReplacedMessage(cs);
                        } else {
                            sendIndexOutOfBoundsMessage(cs);
                            return;
                        }
                        break;
                    }
                    case "-d":
                    {
                        if(line>0 && line<area.getDescription().size()) {
                            area.getDescription().remove(line-1);
                            sendLineRemovedMessage(cs);
                        } else {
                            sendIndexOutOfBoundsMessage(cs);
                            return;
                        }
                        break;
                    }
                    default:
                    {
                        String areaDescription = getDescription(args,1);
                        area.getDescription().add(areaDescription);
                        sendLineAddedMessage(cs);
                    }
                }
            } else {
                String areaDescription = getDescription(args,1);
                area.getDescription().add(areaDescription);
                sendLineAddedMessage(cs);
            }
            saveData(cs);
            sendDescriptionSetMessage(cs);
            try {
                GuidebookShow.sendDescription((Player)cs, area);
            } catch (MessageParseException ex) {
                Logger.getLogger(GuidebookDescription.class.getName()).log(Level.SEVERE, null, ex);
                sendParseError(cs);
            }*/
        }
    }
    
    private String getDescription(String[] args, int startIndex) {
        String areaDescription = args[startIndex];
        for(int i = startIndex+1; i<args.length;i++) {
            areaDescription = areaDescription + " "+args[i];
        }
Logger.getGlobal().info("Desc*** "+areaDescription);
        return areaDescription;
    }

    /*private void saveData(CommandSender cs){
        try {
            PluginData.saveData();
        } catch (IOException ex) {
            sendIOErrorMessage(cs);
            Logger.getLogger(GuidebookTitle.class.getName()).log(Level.SEVERE, null, ex);
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

}
