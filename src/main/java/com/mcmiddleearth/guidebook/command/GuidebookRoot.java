package com.mcmiddleearth.guidebook.command;

import static net.strokkur.commands.arguments.StringArgType.GREEDY;

import com.mcmiddleearth.guidebook.data.PluginData;
import net.strokkur.commands.Command;
import net.strokkur.commands.Executes;
import net.strokkur.commands.arguments.StringArg;
import net.strokkur.commands.paper.Description;
import net.strokkur.commands.paper.Executor;
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
