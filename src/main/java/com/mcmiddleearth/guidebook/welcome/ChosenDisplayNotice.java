package com.mcmiddleearth.guidebook.welcome;

import static com.mcmiddleearth.guidebook.util.GuidebookMessages.stressed;

import com.mcmiddleearth.guidebook.util.GuidebookMessages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

/**
 * Tells staff who join while the Display chosen in config.yml isn't registered, since a warning only in the server log
 * goes unread. Checked on every join, so it's right however late the Display registers or unregisters.
 */
public final class ChosenDisplayNotice implements Listener {

    // From plugin.yml: staff are the ones who can fix it
    private static final String STAFF = "guidebook.staff";

    private final DisplaySwitchboard switchboard;
    private final Plugin plugin;

    public ChosenDisplayNotice(DisplaySwitchboard switchboard, Plugin plugin) {
        this.switchboard = switchboard;
        this.plugin = plugin;
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission(STAFF) || !switchboard.isChosenMissing()) {
            return;
        }
        // So the log has a line to point to even before any Welcome has gone to the Chat display
        switchboard.logIfChosenIsMissing();
        // A tick later, so it isn't lost among the messages every join brings
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> warn(player), 1);
    }

    private void warn(Player player) {
        if (!player.isOnline()) {
            return;
        }
        GuidebookMessages.sendError(
                player,
                "The ",
                stressed(switchboard.chosen()),
                " display chosen in config.yml isn't running, so every Welcome is shown with the ",
                stressed("Chat display"),
                " instead. The server log says why.");
    }
}
