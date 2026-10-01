package com.mcmiddleearth.guidebook.data;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Reads every Area file in the Guidebook data folder, converting any still in Legacy markup to MiniMessage (ADR 0003).
 * Before the first conversion of a load, the whole folder is copied once to a timestamped backup beside it. A converted
 * file is written back straight away. A file whose markup can't be converted is renamed to {@code .yml.unconverted}, so
 * later loads skip it until it's fixed by hand and renamed back.
 */
public final class AreaFileLoader {

    private static final String EXTENSION = ".yml";
    private static final String UNCONVERTED_EXTENSION = ".yml.unconverted";
    private static final DateTimeFormatter BACKUP_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private AreaFileLoader() {}

    /** An Area file that loaded, holding MiniMessage text. */
    public record LoadedArea(String name, File file, YamlConfiguration config) {}

    /**
     * @param backup the folder the data folder was copied to, if this load converted anything
     * @param problems one message per file that couldn't be loaded, to be logged
     */
    public record Outcome(List<LoadedArea> areas, Optional<File> backup, List<String> problems) {}

    /** @param now names the backup folder */
    public static Outcome load(File dataFolder, LocalDateTime now) {
        List<String> problems = new ArrayList<>();
        List<LoadedArea> areas = new ArrayList<>();
        for (File file : areaFiles(dataFolder)) {
            YamlConfiguration config = new YamlConfiguration();
            try {
                config.load(file);
                areas.add(new LoadedArea(shortName(file), file, config));
            } catch (IOException | InvalidConfigurationException ex) {
                problems.add("Couldn't read Guidebook area file " + file + ": " + ex.getMessage());
            }
        }

        if (areas.stream().allMatch(area -> LegacyMarkupConverter.isMiniMessage(area.config()))) {
            return new Outcome(areas, Optional.empty(), problems);
        }
        File backup = backupFolder(dataFolder, now);
        try {
            copyFolder(dataFolder.toPath(), backup.toPath());
        } catch (IOException | UncheckedIOException ex) {
            // Nothing is converted without a backup. The Legacy Areas wait for a load whose backup works
            problems.add("Couldn't back up " + dataFolder + " to " + backup + ", so no Guidebook area in Legacy markup"
                    + " was converted or loaded: " + ex.getMessage());
            List<LoadedArea> miniMessage = areas.stream()
                    .filter(area -> LegacyMarkupConverter.isMiniMessage(area.config()))
                    .toList();
            return new Outcome(miniMessage, Optional.empty(), problems);
        }

        List<LoadedArea> loaded = new ArrayList<>();
        for (LoadedArea area : areas) {
            switch (LegacyMarkupConverter.convert(area.config())) {
                case LegacyMarkupConverter.AlreadyMiniMessage ignored -> loaded.add(area);
                case LegacyMarkupConverter.Converted ignored -> {
                    loaded.add(area);
                    try {
                        area.config().save(area.file());
                    } catch (IOException ex) {
                        problems.add("Converted Guidebook area " + area.name() + " to MiniMessage, but couldn't"
                                + " write " + area.file() + ". It will be converted again on the next load: "
                                + ex.getMessage());
                    }
                }
                case LegacyMarkupConverter.Failed failed -> problems.add(setAside(area, failed.problem()));
            }
        }
        return new Outcome(loaded, Optional.of(backup), problems);
    }

    private static String setAside(LoadedArea area, String problem) {
        File unconverted = new File(area.file().getParentFile(), area.name() + UNCONVERTED_EXTENSION);
        String renamed = area.file().renameTo(unconverted)
                ? "It was renamed to " + unconverted + ". Fix its markup by hand, then rename it back to "
                        + area.file().getName() + " and it will be converted on the next load."
                : "It couldn't be renamed to " + unconverted + ", so it will be tried again on the next load.";
        return "Guidebook area " + area.name() + " isn't loaded, because the Legacy markup in " + area.file()
                + " couldn't be converted to MiniMessage: " + problem + ". " + renamed;
    }

    // Each world's Areas are in a folder named after it
    private static List<File> areaFiles(File dataFolder) {
        List<File> files = new ArrayList<>();
        File[] worldFolders = dataFolder.listFiles(File::isDirectory);
        if (worldFolders == null) {
            return files;
        }
        for (File worldFolder : worldFolders) {
            File[] areaFiles = worldFolder.listFiles(
                    file -> file.isFile() && file.getName().endsWith(EXTENSION));
            if (areaFiles != null) {
                files.addAll(List.of(areaFiles));
            }
        }
        return files;
    }

    private static String shortName(File file) {
        String name = file.getName();
        return name.substring(0, name.length() - EXTENSION.length());
    }

    // Seconds apart is enough in practice, but a quick reload mustn't fail on an existing backup
    private static File backupFolder(File dataFolder, LocalDateTime now) {
        String name = dataFolder.getName() + "-legacy-backup-" + now.format(BACKUP_TIME);
        File backup = new File(dataFolder.getParentFile(), name);
        for (int copy = 2; backup.exists(); copy++) {
            backup = new File(dataFolder.getParentFile(), name + "-" + copy);
        }
        return backup;
    }

    private static void copyFolder(Path from, Path to) throws IOException {
        try (Stream<Path> paths = Files.walk(from)) {
            paths.forEach(path -> {
                try {
                    Files.copy(path, to.resolve(from.relativize(path)));
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            });
        }
    }
}
