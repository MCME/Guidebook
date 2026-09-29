package com.mcmiddleearth.guidebook.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Converts a Description between how it's stored and how staff type it in the Edit dialog. The stored lines are sent
 * joined end to end, and a line break is the two characters {@code \n} in the text. Typed text has real line breaks and
 * writes colour codes as {@code #}.
 */
public final class DescriptionText {

    /** The two characters that mark a line break in a stored Description. */
    public static final String LINE_BREAK = "\\n";

    private DescriptionText() {}

    public static String toTyped(List<String> stored) {
        String joined = String.join("", stored.stream().filter(Objects::nonNull).toList());
        return InputUtil.replaceColorCodeWithAltCode(joined).replace(LINE_BREAK, "\n");
    }

    /** Each typed line is stored as one line ending in a line break marker, apart from the last. */
    public static List<String> toStored(String typed) {
        List<String> lines = typed.stripTrailing().lines().toList();
        List<String> stored = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = InputUtil.replaceAltColorCode(lines.get(i));
            stored.add(i < lines.size() - 1 ? line + LINE_BREAK : line);
        }
        return stored;
    }
}
