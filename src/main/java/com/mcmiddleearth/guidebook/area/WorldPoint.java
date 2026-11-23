package com.mcmiddleearth.guidebook.area;

import org.bukkit.Location;

/** A position in a world, by the world's name, so that deciding whether it's in an Area needs no loaded world. */
public record WorldPoint(String worldName, double x, double y, double z) {

    /** The location's world name and exact coordinates. */
    public static WorldPoint from(Location location) {
        return new WorldPoint(location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
    }
}
