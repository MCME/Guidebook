package com.mcmiddleearth.guidebook.data;

/** The geometry of an Area. */
public enum Shape {
    SPHERE("sphere"),
    CUBOID("cuboid"),
    PRISM("prism");

    private final String displayName;

    Shape(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
