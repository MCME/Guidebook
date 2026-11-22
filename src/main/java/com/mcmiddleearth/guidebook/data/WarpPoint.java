package com.mcmiddleearth.guidebook.data;

import org.bukkit.Location;

/** An Area's Warp point in whole blocks, with the way it faces, as its file stores it. Its world is the Area's. */
public record WarpPoint(int x, int y, int z, float yaw, float pitch) {

    /** The block {@code location} is in, facing as it does. */
    public static WarpPoint of(Location location) {
        return new WarpPoint(
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                location.getYaw(),
                location.getPitch());
    }
}
