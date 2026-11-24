package com.mcmiddleearth.guidebook.area;

import org.bukkit.Location;
import org.bukkit.World;

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

    /** The corner of this block in {@code world}, facing as the Warp point does; the reverse of {@link #of}. */
    public Location toLocation(World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }
}
