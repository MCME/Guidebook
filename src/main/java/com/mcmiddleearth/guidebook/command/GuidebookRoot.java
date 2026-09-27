package com.mcmiddleearth.guidebook.command;

import static net.strokkur.commands.arguments.StringArgType.GREEDY;

import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.pluginutil.message.config.MessageParseException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.strokkur.commands.Command;
import net.strokkur.commands.Executes;
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

    @Executes("on")
    @Permission("guidebook.user")
    void on(CommandSender sender, @Executor Player player) {
        PluginData.include(player);
        PluginData.getMessageUtil().sendInfoMessage(sender, "You will now receive info messages from Guidebook.");
    }

    @Executes("off")
    @Permission("guidebook.user")
    void off(CommandSender sender, @Executor Player player) {
        PluginData.exclude(player);
        PluginData.getMessageUtil().sendInfoMessage(sender, "You will no longer receive info messages from Guidebook.");
    }

    @Executes("show")
    @Permission("guidebook.staff")
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
    @Permission("guidebook.staff")
    void details(CommandSender sender, @CustomArg(AreaArgument.class) InfoArea area) {
        GuidebookDetails.send(sender, area);
    }

    @Executes("warp")
    @Permission("guidebook.staff")
    void warp(CommandSender sender, @Executor Player player, @CustomArg(AreaArgument.class) InfoArea area) {
        player.teleport(area.getLocation());
        PluginData.getMessageUtil().sendInfoMessage(sender, "You are now at Guidebook area " + area.getName() + ".");
    }

    @Executes("enable")
    @Permission("guidebook.staff")
    void enable(CommandSender sender, @CustomArg(AreaArgument.class) InfoArea area) {
        setEnabled(sender, area, true);
    }

    @Executes("disable")
    @Permission("guidebook.staff")
    void disable(CommandSender sender, @CustomArg(AreaArgument.class) InfoArea area) {
        setEnabled(sender, area, false);
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

    // TEMPORARY until ticket 08: everything below routes subcommands not yet migrated to Brigadier
    // to their legacy handlers. Remove it with GuidebookCommandExecutor once every subcommand is migrated.

    @Executes
    void legacyNoArgs(CommandSender sender) {
        LegacyFallthrough.execute(sender, "");
    }

    @Executes
    void legacyWithArgs(CommandSender sender, @LegacyFallthrough.LegacySuggestions @StringArg(GREEDY) String args) {
        LegacyFallthrough.execute(sender, args);
    }
}
