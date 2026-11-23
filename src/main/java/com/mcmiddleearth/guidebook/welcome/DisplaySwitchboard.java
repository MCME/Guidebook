package com.mcmiddleearth.guidebook.welcome;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Connects each Welcome to the Display the server's config chose, out of those registered by name. Other plugins
 * register theirs, and may do so after Guidebook enables, so the chosen Display is looked up for every Welcome.
 * Whenever it isn't registered, or fails, the Chat display shows the Welcome instead (ADR 0008).
 */
public final class DisplaySwitchboard implements Display {

    /** The name of the Chat display, Guidebook's own, which is always registered. */
    public static final String CHAT = "chat";

    // A LinkageError means the Display's jar was built against another Guidebook, such as one updated without the other
    private static final String MISMATCH =
            "Its plugin was built for another version of Guidebook: update both to versions released together.";

    private final String chosen;
    private final Display chat;
    private final Logger logger;
    private final Map<String, Display> registered = new HashMap<>();
    // Set once the missing chosen Display is logged, so the log says so once each time it goes missing
    private boolean missingLogged;

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
        missingLogged = false;
    }

    /** Removes the Display registered as {@code name}, such as when its plugin disables. */
    public void unregister(String name) {
        if (!normalise(name).equals(CHAT)) {
            registered.remove(normalise(name));
        }
    }

    /**
     * Whether the Display chosen in config.yml isn't registered right now, so every Welcome goes to the Chat display.
     * Never true when the Chat display is the one chosen.
     */
    public boolean isChosenMissing() {
        return !registered.containsKey(chosen);
    }

    /**
     * Logs that the chosen Display isn't registered, once until it registers again. Checked when it matters, on a
     * Welcome or a staff join, rather than at startup, because a Display's plugin may register after Guidebook enables.
     */
    public void logIfChosenIsMissing() {
        if (isChosenMissing() && !missingLogged) {
            logger.warning("The display \"" + chosen + "\" chosen in config.yml isn't registered, so Welcomes use the"
                    + " Chat display. Is the plugin that provides it installed and enabled?");
            missingLogged = true;
        }
    }

    /** The name of the Display chosen in config.yml. */
    public String chosen() {
        return chosen;
    }

    @Override
    public void show(Player player, Welcome welcome) {
        Display display = registered.get(chosen);
        if (display == null) {
            logIfChosenIsMissing();
        } else if (display != chat) {
            try {
                display.show(player, welcome);
                return;
            } catch (Display.Unavailable ex) {
                logger.fine("The display \"" + chosen + "\" can't show the Welcome to " + welcome.areaName()
                        + " now, so it was shown with the Chat display: " + ex.getMessage());
            } catch (RuntimeException ex) {
                logger.log(
                        Level.SEVERE,
                        "The display \"" + chosen + "\" failed to show the Welcome to " + welcome.areaName()
                                + ", so it was shown with the Chat display",
                        ex);
            } catch (LinkageError ex) {
                logger.log(
                        Level.SEVERE,
                        "The display \"" + chosen + "\" failed to show the Welcome to " + welcome.areaName()
                                + ", so it was shown with the Chat display. " + MISMATCH,
                        ex);
            }
        }
        chat.show(player, welcome);
    }

    /** The chosen Display's preview, or none (so it previews as chat) when it isn't registered or fails to make one. */
    @Override
    public List<Component> previewDescription(Player player, Welcome welcome) {
        Display display = registered.get(chosen);
        if (display == null) {
            return List.of();
        }
        try {
            return display.previewDescription(player, welcome);
        } catch (RuntimeException ex) {
            logger.log(
                    Level.SEVERE,
                    "The display \"" + chosen + "\" failed to preview the Description of " + welcome.areaName()
                            + ", so it was previewed as chat",
                    ex);
            return List.of();
        } catch (LinkageError ex) {
            logger.log(
                    Level.SEVERE,
                    "The display \"" + chosen + "\" failed to preview the Description of " + welcome.areaName()
                            + ", so it was previewed as chat. " + MISMATCH,
                    ex);
            return List.of();
        }
    }

    private static String normalise(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
