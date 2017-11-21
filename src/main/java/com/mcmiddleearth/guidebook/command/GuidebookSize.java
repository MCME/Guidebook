/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.data.CuboidInfoArea;
import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.guidebook.data.PrismoidInfoArea;
import com.mcmiddleearth.guidebook.data.SphericalInfoArea;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import org.bukkit.util.Vector;

/**
 *
 * @author Eriol_Eandur
 */
public class GuidebookSize extends GuidebookCommand{
    
    public GuidebookSize(String... permissionNodes) {
        super(2, true, permissionNodes);
        setShortDescription(": Defines the size of a Guidebook area.");
        setUsageDescription(" <AreaName> <size>: Defines the size of <AreaName>. <size> must be: \nFor spherical areas: <radius>\nFor cuboid areas: <x1 y1 z1 x2 y2 z2> (coords of opposite corners)\nFor prism areas: <y1 y2> (Height range)");
    }
    
    @Override
    protected void execute(CommandSender cs, String... args) {
        InfoArea area = PluginData.getInfoArea(args[0]);
        if(area==null) {
            sendNoAreaErrorMessage(cs);
        }
        else {
            if(area instanceof SphericalInfoArea) {
                int radius = parseInt(cs, args[1]);
                if(radius==-1) {
                    sendNotANumberMessage(cs);
                    return;
                }
                ((SphericalInfoArea)area).setRadius(radius);
            }
            else if(area instanceof CuboidInfoArea) {
                if(args.length<7) {
                    sendMissingArgumentErrorMessage(cs);
                    return;
                }
                int[] data = new int[6];
                for(int i = 0; i<6; i++) {
                    data[i] = parseInt(cs, args[i+1]);
                    if(data[i]==-1) {
                        sendNotANumberMessage(cs);
                        return;
                    }
                }
                ((CuboidInfoArea)area).setCorners(new Vector(data[0],data[1],data[2]),
                                                           new Vector(data[3],data[4],data[5]));
            } else {
                if(args.length<3) {
                    sendMissingArgumentErrorMessage(cs);
                    return;
                }
                int[] data = new int[2];
                for(int i = 0; i<2; i++) {
                    data[i] = parseInt(cs, args[i+1]);
                    if(data[i]==-1) {
                        sendNotANumberMessage(cs);
                        return;
                    }
                }
                ((PrismoidInfoArea)area).setHeight(data[0],data[1]);
            }
            try {
                PluginData.saveArea(area);
            } catch (IOException ex) {
                sendIOErrorMessage(cs);
                Logger.getLogger(GuidebookSize.class.getName()).log(Level.SEVERE, null, ex);
            }
            sendSizeSetMessage(cs);
        }

        /*InfoArea area = PluginData.getInfoArea(args[0]);
        if(area==null) {
            sendNoAreaErrorMessage(cs);
        }
        else {
            /*if(area instanceof SphericalTeleportationArea) {
                int radius = parseInt(cs, args[1]);
                if(radius==-1) {
                    return;
                }
                ((SphericalTeleportationArea)area).setRadius(radius);
            }
            else {*/
                /*if(args.length<4) {
                    sendMissingArgumentErrorMessage(cs);
                    return;
                }
                Integer xSize = parseInt(cs, args[1]);
                if(xSize==-1) {
                    return;
                }
                Integer ySize = parseInt(cs, args[2]);
                if(ySize==-1) {
                    return;
                }
                Integer zSize = parseInt(cs, args[3]);
                if(zSize==-1) {
                    return;
                }
                ((CuboidInfoArea)area).setSize(xSize,ySize,zSize);
            //}
            try {
                PluginData.saveData();
            } catch (IOException ex) {
                sendIOErrorMessage(cs);
                Logger.getLogger(GuidebookSize.class.getName()).log(Level.SEVERE, null, ex);
            }
            sendSizeSetMessage(cs);
        }*/
    }

    private int parseInt(CommandSender cs, String arg) {
        try {
            return Integer.parseInt(arg);
        }
        catch(NumberFormatException e) {
            sendNotANumberMessage(cs);
            return -1;
        }
    }

    private void sendSizeSetMessage(CommandSender cs) {
        PluginData.getMessageUtil().sendInfoMessage(cs, "Size of Guidebook area set.");
    }

    private void sendNotANumberMessage(CommandSender cs) {
        PluginData.getMessageUtil().sendInfoMessage(cs, "Invalid argument. Not a whole number.");
    }
}
