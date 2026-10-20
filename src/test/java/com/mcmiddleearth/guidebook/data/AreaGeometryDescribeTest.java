package com.mcmiddleearth.guidebook.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** How a Shape is described in dialogs and the list: its footprint and the heights it spans. */
class AreaGeometryDescribeTest {

    @Test
    void aCuboidGivesItsFootprintAndHeights() {
        assertEquals(
                "cuboid, 21 × 30 blocks, from y 60 to 69", new AreaGeometry.Cuboid(0, 60, 0, 20, 69, 29).describe());
    }

    @Test
    void aPrismGivesItsCornersFootprintAndHeights() {
        assertEquals(
                "prism with 3 corners, 11 × 6 blocks, from y 60 to 60",
                new AreaGeometry.Prism(List.of(0, 10, 0), List.of(0, 0, 5), 60, 60).describe());
    }

    @Test
    void aSphereGivesItsRadiusAndHeights() {
        assertEquals("sphere, radius 20, from y 44 to 84", new AreaGeometry.Sphere(0, 64, 0, 20).describe());
    }
}
