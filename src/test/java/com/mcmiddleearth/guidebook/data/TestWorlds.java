package com.mcmiddleearth.guidebook.data;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.World;

/**
 * Bukkit worlds that answer only their name, so tests can build real Areas. Goes away once Areas are plain geometry.
 *
 * <p>Each world is kept here for the whole test run, because a {@link org.bukkit.Location} holds its world only weakly
 * and loses a world nothing else holds.
 */
final class TestWorlds {

    private static final Map<String, World> WORLDS = new HashMap<>();

    private TestWorlds() {}

    /** The one test world called {@code name}. */
    static synchronized World named(String name) {
        return WORLDS.computeIfAbsent(name, TestWorlds::newWorld);
    }

    private static World newWorld(String name) {
        return (World) Proxy.newProxyInstance(
                World.class.getClassLoader(), new Class<?>[] {World.class}, (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getName" -> name;
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "test world " + name;
                        default -> null;
                    };
                });
    }
}
