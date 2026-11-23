package com.mcmiddleearth.guidebook.area;

/** Real Areas built from plain numbers, for tests outside the Area store. */
public final class TestAreas {

    private TestAreas() {}

    /**
     * An Enabled Area filling {@code geometry} in the world {@code worldName}, with no Title, Subtitle or Description.
     * Its Warp point is a sphere's centre, a cuboid's lowest corner (minX, minY, minZ), or a prism's first corner at
     * its minY.
     */
    public static Area area(String name, String worldName, AreaGeometry geometry) {
        return area(name, worldName, geometry, new AreaDetails(false, "", "", false, "", true));
    }

    /** An Area filling {@code geometry} in the world {@code worldName}, with {@code details}. */
    public static Area area(String name, String worldName, AreaGeometry geometry, AreaDetails details) {
        return Area.loaded(name, worldName, warpPointOf(geometry), geometry, details);
    }

    private static WarpPoint warpPointOf(AreaGeometry geometry) {
        return switch (geometry) {
            case AreaGeometry.Sphere s -> new WarpPoint(s.centerX(), s.centerY(), s.centerZ(), 0, 0);
            case AreaGeometry.Cuboid c -> new WarpPoint(c.minX(), c.minY(), c.minZ(), 0, 0);
            case AreaGeometry.Prism p -> new WarpPoint(p.xs().getFirst(), p.minY(), p.zs().getFirst(), 0, 0);
        };
    }
}
