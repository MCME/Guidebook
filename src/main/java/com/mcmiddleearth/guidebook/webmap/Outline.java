package com.mcmiddleearth.guidebook.webmap;

import java.util.List;
import java.util.Optional;

/** The shape of a marker seen from above, in block coordinates. */
public sealed interface Outline {

    record Circle(double x, double z, double radius) implements Outline {}

    /**
     * @param yRange the heights it spans, for Web maps that can show it as a volume
     */
    record Polygon(List<Point> points, Optional<YRange> yRange) implements Outline {

        public Polygon {
            points = List.copyOf(points);
        }
    }

    record Point(double x, double z) {}

    record YRange(double minY, double maxY) {}
}
