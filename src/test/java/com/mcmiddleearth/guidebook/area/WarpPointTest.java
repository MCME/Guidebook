package com.mcmiddleearth.guidebook.area;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

class WarpPointTest {

    /** A world that answers nothing, since a Location only holds it. */
    private static final World WORLD = (World) Proxy.newProxyInstance(
            World.class.getClassLoader(), new Class<?>[] {World.class}, (proxy, method, args) -> {
                return switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> null;
                };
            });

    @Test
    void aWarpPointBecomesTheLocationOfItsBlockInTheGivenWorldFacingTheSameWay() {
        Location location = new WarpPoint(100, 64, -200, 90.5f, -15f).toLocation(WORLD);

        assertSame(WORLD, location.getWorld());
        assertEquals(100, location.getX());
        assertEquals(64, location.getY());
        assertEquals(-200, location.getZ());
        assertEquals(90.5f, location.getYaw());
        assertEquals(-15f, location.getPitch());
    }

    @Test
    void readingTheLocationBackGivesTheSameWarpPoint() {
        WarpPoint warpPoint = new WarpPoint(-7, 12, 3, -45f, 30f);

        assertEquals(warpPoint, WarpPoint.of(warpPoint.toLocation(WORLD)));
    }
}
