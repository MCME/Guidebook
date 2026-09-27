package com.mcmiddleearth.guidebook.command;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

/** Registers {@code /guidebook} with Paper's Brigadier command registrar. */
public final class GuidebookCommands {

    private GuidebookCommands() {}

    public static void register(JavaPlugin plugin) {
        plugin.getLifecycleManager()
                .registerEventHandler(
                        LifecycleEvents.COMMANDS, event -> GuidebookRootBrigadier.register(event.registrar()));
    }
}
