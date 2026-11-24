package com.mcmiddleearth.guidebook.welcome;

/**
 * A register of Displays by name, from which the server's config chooses one (ADR 0008). Guidebook offers it as a
 * Bukkit service while it's enabled, so a plugin finds it with
 * {@code Bukkit.getServicesManager().load(DisplayRegistry.class)}.
 *
 * <p>Names ignore case, and registering a name again replaces its Display. {@link DisplaySwitchboard#CHAT} names
 * Guidebook's own Chat display, so it can't be registered or unregistered.
 */
public interface DisplayRegistry {

    /**
     * Makes {@code display} available as {@code name}, which the server's config can choose.
     *
     * @throws IllegalArgumentException if {@code name} is the Chat display's
     */
    void register(String name, Display display);

    /** Removes the Display registered as {@code name}, such as when its plugin disables. */
    void unregister(String name);
}
