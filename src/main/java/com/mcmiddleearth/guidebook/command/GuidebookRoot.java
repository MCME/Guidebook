package com.mcmiddleearth.guidebook.command;

import static com.mcmiddleearth.guidebook.command.GuidebookHelp.STAFF;
import static com.mcmiddleearth.guidebook.command.GuidebookHelp.USER;

import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.guidebook.util.DevUtil;
import com.mcmiddleearth.pluginutil.message.config.MessageParseException;
import io.papermc.paper.math.BlockPosition;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.strokkur.commands.Command;
import net.strokkur.commands.Executes;
import net.strokkur.commands.Literal;
import net.strokkur.commands.arguments.IntArg;
import net.strokkur.commands.arguments.StringArg;
import net.strokkur.commands.paper.Description;
import net.strokkur.commands.paper.Executor;
import net.strokkur.commands.paper.arguments.CustomArg;
import net.strokkur.commands.permission.Permission;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * The Brigadier {@code /guidebook} command. StrokkCommands generates {@link GuidebookRootBrigadier} from it.
 */
@Command("guidebook")
@Description("manage guidebook areas")
class GuidebookRoot {

    // Bare /guidebook has no requirement, so everyone can see the help, filtered to what they may run
    @Executes
    void help(CommandSender sender) {
        GuidebookHelp.sendAll(sender);
    }

    @Executes("help")
    @Permission(USER)
    void helpAll(CommandSender sender) {
        GuidebookHelp.sendAll(sender);
    }

    @Executes("help")
    @Permission(USER)
    void helpOne(CommandSender sender, @GuidebookHelp.CommandSuggestions @StringArg String command) {
        GuidebookHelp.sendOne(sender, command);
    }

    @Executes("on")
    @Permission(USER)
    void on(CommandSender sender, @Executor Player player) {
        PluginData.include(player);
        PluginData.getMessageUtil().sendInfoMessage(sender, "You will now receive info messages from Guidebook.");
    }

    @Executes("off")
    @Permission(USER)
    void off(CommandSender sender, @Executor Player player) {
        PluginData.exclude(player);
        PluginData.getMessageUtil().sendInfoMessage(sender, "You will no longer receive info messages from Guidebook.");
    }

    @Executes("show")
    @Permission(STAFF)
    void show(CommandSender sender, @CustomArg(AreaArgument.class) InfoArea area) {
        PluginData.getMessageUtil()
                .sendInfoMessage(sender, "Welcome message for Guidebook area " + area.getName() + ":");
        try {
            GuidebookShow.sendDescription(sender, area);
        } catch (MessageParseException ex) {
            Logger.getLogger(GuidebookRoot.class.getName()).log(Level.SEVERE, null, ex);
            PluginData.getMessageUtil().sendErrorMessage(sender, "There was an error while loading the message.");
        }
    }

    @Executes("details")
    @Permission(STAFF)
    void details(CommandSender sender, @CustomArg(AreaArgument.class) InfoArea area) {
        GuidebookDetails.send(sender, area);
    }

    @Executes("warp")
    @Permission(STAFF)
    void warp(CommandSender sender, @Executor Player player, @CustomArg(AreaArgument.class) InfoArea area) {
        player.teleport(area.getLocation());
        PluginData.getMessageUtil().sendInfoMessage(sender, "You are now at Guidebook area " + area.getName() + ".");
    }

    @Executes("enable")
    @Permission(STAFF)
    void enable(CommandSender sender, @CustomArg(AreaArgument.class) InfoArea area) {
        setEnabled(sender, area, true);
    }

    @Executes("disable")
    @Permission(STAFF)
    void disable(CommandSender sender, @CustomArg(AreaArgument.class) InfoArea area) {
        setEnabled(sender, area, false);
    }

    @Executes("set")
    @Permission(STAFF)
    void set(CommandSender sender, @Executor Player player, @GuidebookSet.AreaNameSuggestions @StringArg String name) {
        GuidebookSet.fromSelection(player, name);
    }

    @Executes("set")
    @Permission(STAFF)
    void setSphere(
            CommandSender sender,
            @Executor Player player,
            @GuidebookSet.AreaNameSuggestions @StringArg String name,
            @SuppressWarnings({"unused", "SameParameterValue"}) @Literal("sphere") String sphere,
            @IntArg(min = 1) int radius) {
        GuidebookSet.sphere(player, name, radius);
    }

    @Executes("size")
    @Permission(STAFF)
    void size(CommandSender sender, @CustomArg(AreaArgument.class) InfoArea area) {
        GuidebookSize.show(sender, area);
    }

    @Executes("size")
    @Permission(STAFF)
    void sizeRadius(
            CommandSender sender,
            @CustomArg(AreaArgument.class) InfoArea area,
            @SuppressWarnings({"unused", "SameParameterValue"}) @Literal("radius") String radiusForm,
            @IntArg(min = 1) int radius) {
        GuidebookSize.radius(sender, area, radius);
    }

    @Executes("size")
    @Permission(STAFF)
    void sizeCorners(
            CommandSender sender,
            @CustomArg(AreaArgument.class) InfoArea area,
            @SuppressWarnings({"unused", "SameParameterValue"}) @Literal("corners") String cornersForm,
            BlockPosition pos1,
            BlockPosition pos2) {
        GuidebookSize.corners(sender, area, pos1, pos2);
    }

    @Executes("size")
    @Permission(STAFF)
    void sizeHeight(
            CommandSender sender,
            @CustomArg(AreaArgument.class) InfoArea area,
            @SuppressWarnings({"unused", "SameParameterValue"}) @Literal("height") String heightForm,
            @IntArg int minY,
            @IntArg int maxY) {
        GuidebookSize.height(sender, area, minY, maxY);
    }

    @Executes("title")
    @Permission(STAFF)
    void title(CommandSender sender, @Executor Player player, @CustomArg(AreaArgument.class) InfoArea area) {
        GuidebookTitle.start(player, area);
    }

    @Executes("description")
    @Permission(STAFF)
    void description(CommandSender sender, @Executor Player player, @CustomArg(AreaArgument.class) InfoArea area) {
        GuidebookDescription.start(player, area);
    }

    @Executes("description")
    @Permission(STAFF)
    void descriptionGetBook(
            CommandSender sender,
            @Executor Player player,
            @CustomArg(AreaArgument.class) InfoArea area,
            @SuppressWarnings({"unused", "SameParameterValue"}) @Literal("getbook") String getbook) {
        GuidebookDescription.giveBook(player, area);
    }

    @Executes("description")
    @Permission(STAFF)
    void descriptionSave(
            CommandSender sender,
            @Executor Player player,
            @CustomArg(AreaArgument.class) InfoArea area,
            @SuppressWarnings({"unused", "SameParameterValue"}) @Literal("save") String save) {
        GuidebookDescription.saveBook(player, area);
    }

    @Executes("rename")
    @Permission(STAFF)
    void rename(
            CommandSender sender,
            @CustomArg(AreaArgument.class) InfoArea area,
            @CustomArg(NewAreaNameArgument.class) String newName) {
        String oldName = area.getName();
        boolean oldFileDeleted;
        try {
            oldFileDeleted = PluginData.renameInfoArea(area, newName);
        } catch (IOException ex) {
            Logger.getLogger(GuidebookRoot.class.getName()).log(Level.SEVERE, null, ex);
            PluginData.getMessageUtil()
                    .sendErrorMessage(sender, "There was an error. Guidebook area " + oldName + " was NOT renamed.");
            return;
        }
        PluginData.getMessageUtil()
                .sendInfoMessage(sender, "Guidebook area " + oldName + " has been renamed to " + newName + ".");
        if (!oldFileDeleted) {
            PluginData.getMessageUtil()
                    .sendErrorMessage(
                            sender,
                            "The old data file " + oldName
                                    + ".yml couldn't be deleted. Delete it before reloading, or the Area will load under both names.");
        }
    }

    @Executes("delete")
    @Permission(STAFF)
    void delete(CommandSender sender, @CustomArg(AreaArgument.class) InfoArea area) {
        GuidebookDelete.delete(sender, area);
    }

    @Executes("list")
    @Permission(STAFF)
    void list(CommandSender sender) {
        GuidebookList.send(sender, "", 1);
    }

    // Registered ahead of the filter branch, so a bare number is always a page. Out-of-range pages are clamped rather
    // rejected, because a rejected number would be parsed as a filter instead
    @Executes("list")
    @Permission(STAFF)
    void listPage(CommandSender sender, @IntArg int page) {
        GuidebookList.send(sender, "", page);
    }

    @Executes("list")
    @Permission(STAFF)
    void listFiltered(CommandSender sender, @StringArg String filter) {
        GuidebookList.send(sender, filter, 1);
    }

    @Executes("list")
    @Permission(STAFF)
    void listFilteredPage(CommandSender sender, @StringArg String filter, @IntArg int page) {
        GuidebookList.send(sender, filter, page);
    }

    @Executes("reload")
    @Permission(STAFF)
    void reload(CommandSender sender) {
        PluginData.loadData();
        PluginData.getMessageUtil().sendInfoMessage(sender, "All Guidebook areas reloaded from file.");
    }

    @Executes("dev")
    @Permission(STAFF)
    void dev(CommandSender sender) {
        GuidebookDev.showState(sender);
    }

    @Executes("dev")
    @Permission(STAFF)
    void devConsole(
            CommandSender sender,
            @SuppressWarnings({"unused", "SameParameterValue"}) @Literal("console") String console,
            boolean output) {
        DevUtil.setConsoleOutput(output);
        GuidebookDev.showState(sender);
    }

    @Executes("dev")
    @Permission(STAFF)
    void devLevel(
            CommandSender sender,
            @SuppressWarnings({"unused", "SameParameterValue"}) @Literal("level") String levelLiteral,
            @IntArg int level) {
        DevUtil.setLevel(level);
        GuidebookDev.showState(sender);
    }

    @Executes("dev")
    @Permission(STAFF)
    void devWatch(
            CommandSender sender,
            @Executor Player player,
            @SuppressWarnings({"unused", "SameParameterValue"}) @Literal("watch") String watch) {
        DevUtil.add(player);
        GuidebookDev.showState(sender);
    }

    @Executes("dev")
    @Permission(STAFF)
    void devUnwatch(
            CommandSender sender,
            @Executor Player player,
            @SuppressWarnings({"unused", "SameParameterValue"}) @Literal("unwatch") String unwatch) {
        DevUtil.remove(player);
        GuidebookDev.showState(sender);
    }

    private static void setEnabled(CommandSender sender, InfoArea area, boolean enabled) {
        if (enabled) {
            area.statusOn();
        } else {
            area.statusOff();
        }
        try {
            PluginData.saveArea(area);
        } catch (IOException ex) {
            Logger.getLogger(GuidebookRoot.class.getName()).log(Level.SEVERE, null, ex);
            PluginData.getMessageUtil()
                    .sendErrorMessage(
                            sender, "There was an error. Guidebook area " + area.getName() + " was NOT saved.");
            return;
        }
        PluginData.getMessageUtil()
                .sendInfoMessage(sender, "Guidebook area " + area.getName() + (enabled ? " Enabled" : " Disabled"));
    }
}
