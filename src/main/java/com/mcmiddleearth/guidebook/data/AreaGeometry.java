package com.mcmiddleearth.guidebook.data;

import java.util.List;

/** Where an Area's Shape is in its world, in block coordinates. */
public sealed interface AreaGeometry {

    record Sphere(int centerX, int centerY, int centerZ, int radius) implements AreaGeometry {}

    record Cuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) implements AreaGeometry {}

    /** A polygon of {@code xs} and {@code zs} corners, extruded between two heights. */
    record Prism(List<Integer> xs, List<Integer> zs, int minY, int maxY) implements AreaGeometry {}
}
