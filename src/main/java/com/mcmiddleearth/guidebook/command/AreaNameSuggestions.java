package com.mcmiddleearth.guidebook.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeSet;

/**
 * The name suggestions for a new Area, following the {@code <world>-<project>-<place>} convention: {@code <world>-} for
 * every world, and {@code <world>-<project>-} for the projects that existing Areas use, as far as they continue what's
 * typed. They come in one list because accepting a suggestion with Tab doesn't ask for new ones, so Tab has to be able
 * to cycle from a world on to its projects. The place is left to the staff member, since every existing name is taken.
 */
final class AreaNameSuggestions {

    private AreaNameSuggestions() {}

    /**
     * @param typed what has been typed so far
     * @param worlds the world names, which may contain hyphens themselves
     * @param areaNames the existing Areas' names
     */
    static List<String> suggest(String typed, Collection<String> worlds, Collection<String> areaNames) {
        List<String> candidates = new ArrayList<>();
        worlds.stream().map(world -> world + "-").forEach(candidates::add);
        areaNames.stream()
                .map(name -> projectPrefix(name, worlds))
                .flatMap(Optional::stream)
                .forEach(candidates::add);
        return matching(candidates, typed.toLowerCase(Locale.ROOT));
    }

    // The candidates that continue what's typed, without repeats and in alphabetical order
    private static List<String> matching(List<String> candidates, String search) {
        TreeSet<String> matches = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        candidates.stream()
                .filter(candidate -> {
                    String lower = candidate.toLowerCase(Locale.ROOT);
                    return lower.startsWith(search) && !lower.equals(search);
                })
                .forEach(matches::add);
        return List.copyOf(matches);
    }

    // "moria-dwarrowdelf-gate" -> "moria-dwarrowdelf-", taking the longest world the name starts with
    private static Optional<String> projectPrefix(String name, Collection<String> worlds) {
        String lower = name.toLowerCase(Locale.ROOT);
        return worlds.stream()
                .map(world -> world.toLowerCase(Locale.ROOT) + "-")
                .filter(lower::startsWith)
                .max(Comparator.comparingInt(String::length))
                .flatMap(worldPrefix -> {
                    int projectEnd = name.indexOf('-', worldPrefix.length());
                    return projectEnd > worldPrefix.length()
                            ? Optional.of(name.substring(0, projectEnd + 1))
                            : Optional.empty();
                });
    }
}
