package com.mcmiddleearth.guidebook.welcome;

/**
 * Where another plugin registers a Display by name, for the server's config to choose (ADR 0008). Guidebook offers it
 * as a Bukkit service while it's enabled:
 *
 * <pre>{@code
 * DisplayRegistry displays = Bukkit.getServicesManager().load(DisplayRegistry.class);
 * displays.register("typewriter", new TypewriterDisplay());
 * }</pre>
 */
public interface DisplayRegistry {

    /** Makes {@code display} available as {@code name}, which the server's config can choose. Names ignore case. */
    void register(String name, Display display);

    /** Removes the Display registered as {@code name}, such as when its plugin disables. */
    void unregister(String name);
}
