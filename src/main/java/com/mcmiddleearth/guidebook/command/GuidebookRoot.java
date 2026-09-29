package com.mcmiddleearth.guidebook.command;

import static com.mcmiddleearth.guidebook.command.GuidebookHelp.STAFF;
import static com.mcmiddleearth.guidebook.command.GuidebookHelp.USER;

import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.guidebook.dialog.EditDialog;
import com.mcmiddleearth.guidebook.util.DevUtil;
import com.mcmiddleearth.guidebook.util.GuidebookMessages;
import com.mcmiddleearth.pluginutil.message.config.MessageParseException;
import io.papermc.paper.math.BlockPosition;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.strokkur.commands.Command;
import net.strokkur.commands.DefaultExecutes;
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

    // Every @Permission goes on an @Executes("<command>") with a non-empty path, and never on this class. A permission
    // lands on the node where its path ends, so an empty path would put it on the /guidebook root and lock
    // guidebook.user players out of on, off and help. See docs/adr/0001-commands-via-strokkcommands.md

    // Runs for bare /guidebook and every incomplete command, such as "size" or "size <area> radius". The root needs
    // guidebook.user or guidebook.staff, and the help is filtered to what the sender may run
    @DefaultExecutes
    void help(CommandSender sender, List<String> args) {
        // args is the whole input, which starts with "execute ... run" when run through /execute
        int root = 0;
        while (root < args.size() && !args.get(root).matches("(\\w+:)?guidebook")) {
            root++;
        }
        if (root + 1 < args.size()) {
            GuidebookHelp.sendOne(sender, args.get(root + 1));
        } else {
            GuidebookHelp.sendAll(sender);
        }
    }

    @Executes("help")
    @Permission(USER)
    void help(CommandSender sender, @GuidebookHelp.CommandSuggestions Optional<String> command) {
        command.ifPresentOrElse(c -> GuidebookHelp.sendOne(sender, c), () -> GuidebookHelp.sendAll(sender));
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
        GuidebookMessages.startBlock(sender);
        PluginData.getMessageUtil()
                .sendInfoMessage(sender, "Welcome message for Guidebook area " + area.getName() + ":");
        if (sender instanceof Player player) {
            area.previewWelcome(player);
            return;
        }
        // The console can't see a Title or Boss bar, so it gets the Description only
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

    @Executes("edit")
    @Permission(STAFF)
    void editHere(CommandSender sender, @Executor Player player) {
        List<InfoArea> here = PluginData.getInfoAreas().values().stream()
                .filter(area -> area.isInside(player.getLocation()))
                .toList();
        if (here.size() == 1) {
            EditDialog.open(player, here.getFirst());
        } else if (here.isEmpty()) {
            PluginData.getMessageUtil()
                    .sendErrorMessage(
                            player, "You aren't standing in a Guidebook area. Name one: /guidebook edit <area>");
        } else {
            PluginData.getMessageUtil()
                    .sendErrorMessage(
                            player,
                            "You're standing in more than one Guidebook area ("
                                    + here.stream()
                                            .map(InfoArea::getName)
                                            .sorted()
                                            .collect(Collectors.joining(", "))
                                    + "). Name one: /guidebook edit <area>");
        }
    }

    @Executes("edit")
    @Permission(STAFF)
    void edit(CommandSender sender, @Executor Player player, @CustomArg(AreaArgument.class) InfoArea area) {
        EditDialog.open(player, area);
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

    // Declared ahead of listFiltered, so the page branch is registered ahead of the filter branch and a bare number is
    // always a page. Out-of-range pages are clamped rather than rejected, because a rejected number would be parsed
    // as a filter instead
    @Executes("list")
    @Permission(STAFF)
    void list(CommandSender sender, OptionalInt page) {
        GuidebookList.send(sender, "", page.orElse(1));
    }

    @Executes("list")
    @Permission(STAFF)
    void listFiltered(CommandSender sender, @StringArg String filter, OptionalInt page) {
        GuidebookList.send(sender, filter, page.orElse(1));
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
    void devWatchOrUnwatch(
            CommandSender sender, @Executor Player player, @Literal({"watch", "unwatch"}) String action) {
        if (action.equals("watch")) {
            DevUtil.add(player);
        } else {
            DevUtil.remove(player);
        }
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
