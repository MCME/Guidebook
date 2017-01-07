/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.pluginutil.NumericUtil;
import com.mcmiddleearth.pluginutil.message.FancyMessage;
import com.mcmiddleearth.pluginutil.message.MessageType;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Eriol_Eandur
 */
public class GuidebookList extends GuidebookCommand{
    
    public GuidebookList(String... permissionNodes) {
        super(0, true, permissionNodes);
        setShortDescription(": Lists all guidebook areas.");
        setUsageDescription(" [selection]: Lists all guidebook areas which names contains [selection].");
    }
    
    @Override
    protected void execute(CommandSender cs, String... args) {
        int pageIndex = 0;
        String selection = "";
        if(args.length>0 && (!NumericUtil.isInt(args[0]))) {
            selection = args[0];
            pageIndex = 1;
        }
        int page = 1;
        if(args.length>pageIndex && NumericUtil.isInt(args[pageIndex])) {
            page = NumericUtil.getInt(args[pageIndex]);
        }
        FancyMessage header = new FancyMessage(MessageType.INFO,PluginData.getMessageUtil())
                                    .addSimple("Guidebook areas (click for details)");
        List<FancyMessage> messages = new ArrayList<>();
        for(String areaName : PluginData.getInfoAreas().keySet()) {
            if(selection.equals("") || areaName.contains(selection)) {
                InfoArea area = PluginData.getInfoArea(areaName);
                FancyMessage message = new FancyMessage(MessageType.INFO_NO_PREFIX, PluginData.getMessageUtil());
                message.addSimple(ChatColor.AQUA+PluginData.getMessageUtil().getNOPREFIX()+"- ");
                message.addClickable(ChatColor.BLUE+areaName,"/guidebook details "+areaName);
                message.addSimple(ChatColor.AQUA+".");
                messages.add(message);
            }
        }
        PluginData.getMessageUtil().sendFancyListMessage((Player)cs, header, messages, "/guidebook list "+selection, page);
    }
    
}
