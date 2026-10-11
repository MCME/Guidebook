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
import com.mcmiddleearth.guidebook.dialog.ConfirmDialog;
import com.mcmiddleearth.guidebook.util.GuidebookMessages;
import com.mcmiddleearth.pluginutil.WEUtil;
import com.mcmiddleearth.pluginutil.region.PrismoidRegion;
import com.mcmiddleearth.pluginutil.region.Region;
import com.mcmiddleearth.pluginutil.region.SphericalRegion;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.strokkur.commands.CustomSuggestion;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * {@code set <name> [sphere <radius>]}: moves an existing Area after the player confirms. New Areas are made with
 * {@code create}.
 *
 * @author Eriol_Eandur
 */
final class GuidebookSet {

    private GuidebookSet() {}

    /** Marks {@code set}'s name argument, which suggests existing Areas but accepts new names too. */
    @CustomSuggestion
    @interface AreaNameSuggestions {}

    @AreaNameSuggestions
    static CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return AreaArgument.suggestAreaNames(builder);
    }

    static void fromSelection(Player player, String name) {
        Location location = player.getLocation().clone();
        com.sk89q.worldedit.regions.Region selection = WEUtil.getSelection(player);
        if (selection instanceof CuboidRegion cuboid) {
            set(player, name, new com.mcmiddleearth.pluginutil.region.CuboidRegion(location, cuboid));
        } else if (selection instanceof Polygonal2DRegion polygon) {
            set(player, name, new PrismoidRegion(location, polygon));
        } else {
            GuidebookMessages.sendError(
                    player,
                    "No cuboid or polygon WorldEdit selection found! Either make one and try again, or add sphere <radius> after the Area name.");
        }
    }

    static void sphere(Player player, String name, int radius) {
        set(player, name, new SphericalRegion(player.getLocation().clone(), radius));
    }

    private static void set(Player player, String name, Region region) {
        InfoArea existing = PluginData.getInfoAreaExact(name);
        if (existing == null) {
            // Creating moved to create, so a typo here can't start a new Area
            GuidebookMessages.sendError(
                    player,
                    "There is no Guidebook area " + name + " to move. To create one, use /guidebook create " + name
                            + ".");
            return;
        }
        confirmMove(player, existing, region);
    }

    private static void confirmMove(Player player, InfoArea area, Region region) {
        ConfirmDialog.openToMove(player, area, confirmer -> move(confirmer, area, region));
    }

    private static void move(Player player, InfoArea area, Region region) {
        String oldWorld = area.getLocation().getWorld().getName();
        boolean oldFileDeleted;
        try {
            oldFileDeleted = PluginData.moveInfoArea(area, region);
        } catch (IOException ex) {
            Logger.getLogger(GuidebookSet.class.getName()).log(Level.SEVERE, null, ex);
            GuidebookMessages.sendNotDone(player, area.getName(), "moved");
            return;
        }
        GuidebookMessages.sendInfo(
                player, GuidebookMessages.area(area.getName()), " was moved to your location and selection.");
        if (!oldFileDeleted) {
            GuidebookMessages.sendError(
                    player,
                    "The old data file " + oldWorld + "/" + area.getName()
                            + ".yml couldn't be deleted. Delete it before reloading, or the Area will load twice.");
        }
    }
}
