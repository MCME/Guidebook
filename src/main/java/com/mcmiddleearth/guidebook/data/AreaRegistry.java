package com.mcmiddleearth.guidebook.data;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class AreaRegistry<A extends AreaView> {

    // The characters Brigadier allows in an unquoted word, so every name can be typed as one argument
    private static final Pattern VALID_NAME = Pattern.compile("[0-9A-Za-z_\\-.+]+");

    private final Collection<A> areas;

    public AreaRegistry(Collection<A> areas) {
        this.areas = areas;
    }

    public Optional<A> resolve(String name) {
        return areas.stream()
                .filter(area -> area.getName().equals(name))
                .findFirst()
                .or(() -> areas.stream()
                        .filter(area -> area.getName().equalsIgnoreCase(name))
                        .findFirst());
    }

    public static boolean isValidName(String name) {
        return VALID_NAME.matcher(name).matches();
    }

    public boolean isAvailable(String name) {
        return areas.stream().noneMatch(area -> area.getName().equalsIgnoreCase(name));
    }

    /**
     * Area names are written {@code <world>-<project>-<place>}, so a name is suggested when the typed text starts
     * any of its hyphen segments, e.g. {@code minas} or {@code gondor-mi} for {@code world-gondor-minas}.
     */
    public List<Suggestion> suggest(String typed) {
        String search = typed.toLowerCase(Locale.ROOT);
        return areas.stream()
                .filter(area -> anySegmentStartsWith(area.getName(), search))
                .map(area -> new Suggestion(area.getName(), tooltip(area)))
                .toList();
    }

    private static String tooltip(AreaView area) {
        return area.isEnabled() ? area.getTitle() : area.getTitle() + " (disabled)";
    }

    private static boolean anySegmentStartsWith(String name, String search) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        if (lowerName.startsWith(search)) {
            return true;
        }
        for (int i = lowerName.indexOf('-'); i != -1; i = lowerName.indexOf('-', i + 1)) {
            if (lowerName.startsWith(search, i + 1)) {
                return true;
            }
        }
        return false;
    }

    public record Suggestion(String name, String tooltip) {}
}
