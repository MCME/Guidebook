/*
 *  Copyright (C) 2016 MCME
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

import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;

/**
 *
 * @author Fraspace5
 */
public class GuidebookRen extends GuidebookCommand {

    public GuidebookRen(String... permissionNodes) {
        super(1, true, permissionNodes);
        setShortDescription(": Rename a guidebook");
        setUsageDescription(" To use that command type /guidebook rename oldname newname");
    }

    //guidebook rename oldname newname
    //                  0       1
    @Override
    protected void execute(CommandSender cs, String... args) {

        if (args.length == 2) {

            if (PluginData.getInfoAreas().containsKey(args[0])) {

                InfoArea area = PluginData.getInfoAreas().get(args[0]);

                PluginData.addInfoArea(args[1], area);

                PluginData.deleteInfoArea(args[0]);

                PluginData.getMessageUtil().sendInfoMessage(cs, "Guidebook area " + args[0] + " has been renamed with " + args[1]);
                try {
                    PluginData.saveArea(area);
                } catch (IOException ex) {
                    Logger.getLogger(GuidebookRen.class.getName()).log(Level.SEVERE, null, ex);
                }

            } else {
                PluginData.getMessageUtil().sendErrorMessage(cs, "This area doesn't exist");
            }

        } else {

            PluginData.getMessageUtil().sendErrorMessage(cs, "Invalid Usage! /guidebook rename oldname newname");

        }

    }

}
