/*
 * Copyright (C) 2016 MCME
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mcmiddleearth.guidebook.area;

/**
 * An Area, made with {@link #create} or read from its file by the Area file adapter. Its Shape, world and Warp point
 * are plain values, which a move can change; its world is named, and needn't be loaded.
 */
public final class Area {

    /** How far beyond its Shape a player inside the Area can go before they Leave it. */
    private static final int LEAVE_DISTANCE = 10;

    private AreaGeometry geometry;
    private String worldName;
    private WarpPoint warpPoint;

    private String areaName;

    private AreaDetails details = new AreaDetails(false, "", "", false, "", true);

    private Area(String areaName) {
        this.areaName = areaName;
    }

    /** A new Area filling {@code geometry} in the world {@code worldName}, with its Warp point at {@code warpPoint}. */
    public static Area create(String areaName, String worldName, WarpPoint warpPoint, AreaGeometry geometry) {
        Area area = new Area(areaName);
        area.place(worldName, warpPoint, geometry);
        return area;
    }

    /** The Area {@code areaName} as read from its file. Its world needn't be loaded. */
    static Area loaded(
            String areaName, String worldName, WarpPoint warpPoint, AreaGeometry geometry, AreaDetails details) {
        Area area = create(areaName, worldName, warpPoint, geometry);
        area.setDetails(details);
        return area;
    }

    /**
     * Gives the Area {@code geometry} in the world {@code worldName}, with its Warp point at {@code warpPoint}. A
     * sphere is centred on its Warp point, so its own centre is ignored.
     */
    void place(String worldName, WarpPoint warpPoint, AreaGeometry geometry) {
        this.worldName = worldName;
        this.warpPoint = warpPoint;
        this.geometry = switch (geometry) {
            case AreaGeometry.Sphere sphere ->
                new AreaGeometry.Sphere(warpPoint.x(), warpPoint.y(), warpPoint.z(), sphere.radius());
            case AreaGeometry.Cuboid cuboid -> cuboid;
            case AreaGeometry.Prism prism -> prism;
        };
    }

    public AreaGeometry getGeometry() {
        return geometry;
    }

    // The Shape follows the geometry, since a move can give an Area another Shape
    public Shape getShape() {
        return geometry.shape();
    }

    /** The name of the Area's world, even when that world isn't loaded. */
    public String getWorldName() {
        return worldName;
    }

    public WarpPoint getWarpPoint() {
        return warpPoint;
    }

    /** Whether the point is in this Area's world, named exactly as it is, and within its Shape. */
    public boolean isInside(WorldPoint point) {
        return worldName.equals(point.worldName()) && geometry.contains(point.x(), point.y(), point.z());
    }

    /**
     * Whether the point is in this Area's world and no more than {@link #LEAVE_DISTANCE} blocks beyond its Shape, so a
     * player inside the Area who is there doesn't yet Leave it (see Leave in the glossary).
     */
    public boolean isNear(WorldPoint point) {
        return worldName.equals(point.worldName()) && geometry.near(point.x(), point.y(), point.z(), LEAVE_DISTANCE);
    }

    /** What the Area says, and whether it's Enabled. */
    public AreaDetails details() {
        return details;
    }

    void setDetails(AreaDetails details) {
        this.details = details;
    }

    public boolean isEnabled() {
        return details.enabled();
    }

    public String getName() {
        return this.areaName;
    }

    void setName(String areaName) {
        this.areaName = areaName;
    }
}
