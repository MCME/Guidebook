package com.mcmiddleearth.guidebook.webmap;

/** The shape of a marker seen from above, in block coordinates. */
public sealed interface Outline {

    record Circle(double x, double z, double radius) implements Outline {}
}
