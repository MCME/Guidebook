package com.mcmiddleearth.guidebook.data;

/** The geometry of an Area. */
public enum Shape {
    SPHERE("sphere", "●"),
    CUBOID("cuboid", "■"),
    PRISM("prism", "⬟");

    private final String displayName;
    private final String symbol;

    Shape(String displayName, String symbol) {
        this.displayName = displayName;
        this.symbol = symbol;
    }

    public String displayName() {
        return displayName;
    }

    public String symbol() {
        return symbol;
    }
}
