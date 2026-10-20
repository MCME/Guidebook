package com.mcmiddleearth.guidebook.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AreaFileLoaderTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 1, 14, 30, 5);
    private static final String BACKUP_NAME = "Guidebook-legacy-backup-2026-10-01_14-30-05";

    private static final String LEGACY = """
            title: §6Edoras
            subtitle: §7Capital of Rohan
            description:
            - '§3Guide: §fWelcome\\n'
            - to Edoras.
            """;
    private static final String MINIMESSAGE = """
            title: <gold>Edoras
            description: '<dark_aqua>Guide: <white>Welcome'
            minimessage: true
            """;
    private static final String BROKEN_MARKUP = """
            title: §6Broken
            description:
            - '[Click="/warp"][Click="/again"]nested clicks'
            """;

    @TempDir
    Path plugins;

    private Path dataFolder;

    @BeforeEach
    void createDataFolder() throws IOException {
        dataFolder = Files.createDirectories(plugins.resolve("Guidebook"));
        Files.writeString(dataFolder.resolve("config.yml"), "excludedPlayers: []\n");
    }

    private Path area(String name, String yaml) throws IOException {
        Path world = Files.createDirectories(dataFolder.resolve("world"));
        return Files.writeString(world.resolve(name + ".yml"), yaml);
    }

    private AreaFileLoader.Outcome load() {
        return AreaFileLoader.load(dataFolder.toFile(), NOW);
    }

    private List<String> loadedNames(AreaFileLoader.Outcome outcome) {
        return outcome.areas().stream()
                .map(AreaFileLoader.LoadedArea::name)
                .sorted()
                .toList();
    }

    private List<Path> backups() throws IOException {
        try (Stream<Path> files = Files.list(plugins)) {
            return files.filter(path -> !path.equals(dataFolder)).toList();
        }
    }

    @Test
    void convertsLegacyFilesAndRewritesThem() throws IOException {
        Path file = area("edoras", LEGACY);

        AreaFileLoader.Outcome outcome = load();

        assertEquals(List.of("edoras"), loadedNames(outcome));
        YamlConfiguration rewritten = YamlConfiguration.loadConfiguration(file.toFile());
        assertTrue(rewritten.getBoolean("minimessage"));
        assertEquals("<gold>Edoras", rewritten.getString("title"));
        assertEquals("<guide>Welcome\n to Edoras.", rewritten.getString("description"));
        assertEquals(
                rewritten.getString("description"),
                outcome.areas().getFirst().config().getString("description"));
    }

    @Test
    void writesAMultiLineDescriptionAsABlock() throws IOException {
        Path file = area("edoras", LEGACY);

        load();

        assertTrue(Files.readString(file).contains("description: |"), Files.readString(file));
    }

    @Test
    void makesOneTimestampedBackupOfTheWholeFolderBeforeConverting() throws IOException {
        area("edoras", LEGACY);
        area("rohan", LEGACY.replace("Edoras", "Rohan"));

        AreaFileLoader.Outcome outcome = load();

        Path backup = plugins.resolve(BACKUP_NAME);
        assertEquals(List.of(backup), backups());
        assertEquals(backup.toFile(), outcome.backup().orElseThrow());
        assertEquals(LEGACY, Files.readString(backup.resolve("world/edoras.yml")));
        assertTrue(Files.exists(backup.resolve("config.yml")));
    }

    @Test
    void aSecondBackupInTheSameSecondGetsItsOwnFolder() throws IOException {
        area("edoras", LEGACY);
        Files.createDirectory(plugins.resolve(BACKUP_NAME));

        AreaFileLoader.Outcome outcome = load();

        assertEquals(
                plugins.resolve(BACKUP_NAME + "-2").toFile(), outcome.backup().orElseThrow());
        assertEquals(List.of("edoras"), loadedNames(outcome));
    }

    @Test
    void makesNoBackupAndChangesNothingWhenEverythingIsMiniMessage() throws IOException {
        Path file = area("edoras", MINIMESSAGE);

        AreaFileLoader.Outcome outcome = load();

        assertEquals(List.of("edoras"), loadedNames(outcome));
        assertEquals(List.of(), backups());
        assertTrue(outcome.backup().isEmpty());
        assertEquals(MINIMESSAGE, Files.readString(file));
    }

    @Test
    void makesNoBackupOnTheLoadAfterAConversion() throws IOException {
        Path file = area("edoras", LEGACY);
        load();
        String converted = Files.readString(file);

        AreaFileLoader.Outcome second = AreaFileLoader.load(dataFolder.toFile(), NOW.plusMinutes(1));

        assertTrue(second.backup().isEmpty());
        assertEquals(1, backups().size());
        assertEquals(converted, Files.readString(file));
    }

    @Test
    void setsAsideAFileWhoseMarkupFailsToParseAndLoadsTheRest() throws IOException {
        area("edoras", LEGACY);
        Path broken = area("broken", BROKEN_MARKUP);

        AreaFileLoader.Outcome outcome = load();

        assertEquals(List.of("edoras"), loadedNames(outcome));
        assertFalse(Files.exists(broken));
        Path setAside = broken.resolveSibling("broken.yml.unconverted");
        assertEquals(BROKEN_MARKUP, Files.readString(setAside));
        assertEquals(1, outcome.problems().size());
        String problem = outcome.problems().getFirst();
        assertTrue(problem.contains(setAside.toFile().getPath()), problem);
        assertTrue(problem.contains("rename it back"), problem);
    }

    @Test
    void makesNoBackupOnTheLoadAfterAFileWasSetAside() throws IOException {
        area("broken", BROKEN_MARKUP);
        load();

        AreaFileLoader.Outcome second = AreaFileLoader.load(dataFolder.toFile(), NOW.plusMinutes(1));

        assertTrue(second.backup().isEmpty());
        assertEquals(List.of(), second.problems());
        assertEquals(1, backups().size());
    }

    @Test
    void convertsAFixedFileRenamedBack() throws IOException {
        Path broken = area("broken", BROKEN_MARKUP);
        load();
        Path setAside = broken.resolveSibling("broken.yml.unconverted");
        Files.writeString(broken, BROKEN_MARKUP.replace("[Click=\"/again\"]", ""));
        Files.delete(setAside);

        AreaFileLoader.Outcome second = AreaFileLoader.load(dataFolder.toFile(), NOW.plusMinutes(1));

        assertEquals(List.of("broken"), loadedNames(second));
        assertTrue(YamlConfiguration.loadConfiguration(broken.toFile()).getBoolean("minimessage"));
    }

    @Test
    void anInvalidYamlFileIsReportedButCausesNoBackup() throws IOException {
        area("edoras", MINIMESSAGE);
        Path invalid = area("invalid", "title: [unclosed\n");

        AreaFileLoader.Outcome outcome = load();

        assertEquals(List.of("edoras"), loadedNames(outcome));
        assertEquals(List.of(), backups());
        assertTrue(Files.exists(invalid));
        assertEquals(1, outcome.problems().size());
    }

    @Test
    void ignoresFilesThatAreNotYml() throws IOException {
        area("edoras", MINIMESSAGE);
        Files.writeString(dataFolder.resolve("world/old.yml.unconverted"), BROKEN_MARKUP);

        AreaFileLoader.Outcome outcome = load();

        assertEquals(List.of("edoras"), loadedNames(outcome));
        assertEquals(List.of(), backups());
    }
}
