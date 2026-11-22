package com.mcmiddleearth.guidebook.welcome;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;

/**
 * Connects each Welcome to the Display the server's config chose, out of those registered by name. Other plugins
 * register theirs, and may do so after Guidebook enables, so the chosen Display is looked up for every Welcome.
 * Whenever it isn't registered, or fails, the Chat display shows the Welcome instead (ADR 0008).
 */
public final class DisplaySwitchboard implements Display {

    /** The name of the Chat display, Guidebook's own, which is always registered. */
    public static final String CHAT = "chat";

    private final String chosen;
    private final Display chat;
    private final Logger logger;
    private final Map<String, Display> registered = new HashMap<>();

    /** @param chosen the name of the Display the server uses, from its config */
    public DisplaySwitchboard(String chosen, Display chat, Logger logger) {
        this.chosen = normalise(chosen);
        this.chat = chat;
        this.logger = logger;
        registered.put(CHAT, chat);
    }

    /** Makes {@code display} available as {@code name}, which the server's config can choose. Names ignore case. */
    public void register(String name, Display display) {
        if (normalise(name).equals(CHAT)) {
            throw new IllegalArgumentException("The Chat display is Guidebook's own, so its name can't be registered");
        }
        registered.put(normalise(name), display);
    }

    /** Removes the Display registered as {@code name}, such as when its plugin disables. */
    public void unregister(String name) {
        if (!normalise(name).equals(CHAT)) {
            registered.remove(normalise(name));
        }
    }

    /** Warns if the chosen Display isn't registered, which is worth saying once every plugin has enabled. */
    public void warnIfChosenIsMissing() {
        if (!registered.containsKey(chosen)) {
            logger.warning("The display \"" + chosen + "\" chosen in config.yml isn't registered, so Welcomes are"
                    + " shown in chat. Is the plugin that provides it installed and enabled?");
        }
    }

    @Override
    public void show(Player player, Welcome welcome) {
        Display display = registered.getOrDefault(chosen, chat);
        if (display != chat) {
            try {
                display.show(player, welcome);
                return;
            } catch (RuntimeException ex) {
                logger.log(
                        Level.SEVERE,
                        "The display \"" + chosen + "\" failed to show the Welcome to " + welcome.areaName()
                                + ", so it was shown in chat",
                        ex);
            }
        }
        chat.show(player, welcome);
    }

    private static String normalise(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
