package com.mcmiddleearth.guidebook.area;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Area files kept in memory, so tests can see what was written and make a write or delete fail. Loading builds new
 * Areas from what was written, as reading the files again would.
 */
public final class InMemoryAreaFiles implements AreaFiles {

    private record Key(String world, String name) {}

    private final Map<Key, YamlConfiguration> files = new LinkedHashMap<>();
    private boolean failNextWrite;
    private boolean failNextDelete;
    private Optional<File> nextBackup = Optional.empty();
    private List<String> nextProblems = List.of();

    @Override
    public Loaded loadAll() {
        List<Area> areas = new ArrayList<>();
        files.forEach((key, contents) -> {
            if (AreaFileFormat.read(key.name(), contents) instanceof AreaFileFormat.Read.Found(Area area)) {
                areas.add(area);
            }
        });
        Loaded loaded = new Loaded(areas, nextBackup, nextProblems);
        nextBackup = Optional.empty();
        nextProblems = List.of();
        return loaded;
    }

    @Override
    public void write(Area area) throws IOException {
        if (failNextWrite) {
            failNextWrite = false;
            throw new IOException("Told to fail this write");
        }
        YamlConfiguration contents = new YamlConfiguration();
        AreaFileFormat.write(area, contents);
        files.put(new Key(area.worldName(), area.name()), contents);
    }

    @Override
    public boolean delete(String worldName, String areaName) {
        if (failNextDelete) {
            failNextDelete = false;
            return false;
        }
        return files.remove(new Key(worldName, areaName)) != null;
    }

    /** What the file of the Area {@code areaName} in {@code worldName} holds, if there is one. */
    Optional<YamlConfiguration> contents(String worldName, String areaName) {
        return Optional.ofNullable(files.get(new Key(worldName, areaName)));
    }

    void failNextWrite() {
        failNextWrite = true;
    }

    void failNextDelete() {
        failNextDelete = true;
    }

    /** Makes the next load report a backup and problems, as a load converting Legacy markup can. */
    void reportOnNextLoad(File backup, List<String> problems) {
        nextBackup = Optional.of(backup);
        nextProblems = List.copyOf(problems);
    }
}
