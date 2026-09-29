package com.mcmiddleearth.guidebook.util;

/**
 * How a Description is stored: its lines are sent joined end to end, and a line break is the two characters {@code \n}
 * in the text.
 */
public final class DescriptionText {

    /** The two characters that mark a line break in a stored Description. */
    public static final String LINE_BREAK = "\\n";

    private DescriptionText() {}
}
