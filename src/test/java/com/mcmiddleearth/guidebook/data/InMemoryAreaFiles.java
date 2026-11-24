package com.mcmiddleearth.guidebook.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Area files kept in memory, so tests can see what was written and make a write or delete fail. Loading builds new
 * Areas from what was written, as reading the files again would.
 */
final class InMemoryAreaFiles implements AreaFiles {

    private record Key(String world, String name) {}

    /** What an Area's file holds, plus where its Shape is, since tests can't look a world up by its name. */
    private record StoredFile(YamlConfiguration contents, AreaGeometry geometry, Location warpPoint) {}

    private final Map<Key, StoredFile> files = new LinkedHashMap<>();
    private boolean failNextWrite;
    private boolean failNextDelete;
    private Optional<File> nextBackup = Optional.empty();
    private List<String> nextProblems = List.of();

    @Override
    public Loaded loadAll() {
        List<InfoArea> areas = new ArrayList<>();
        files.forEach((key, file) -> areas.add(InfoArea.loaded(
                key.name(),
                file.contents(),
                InfoArea.regionOf(file.geometry(), file.warpPoint().clone()))));
        Loaded loaded = new Loaded(areas, nextBackup, nextProblems);
        nextBackup = Optional.empty();
        nextProblems = List.of();
        return loaded;
    }

    @Override
    public void write(InfoArea area) throws IOException {
        if (failNextWrite) {
            failNextWrite = false;
            throw new IOException("Told to fail this write");
        }
        YamlConfiguration contents = new YamlConfiguration();
        area.save(contents);
        files.put(
                new Key(area.getWorldName(), area.getName()),
                new StoredFile(contents, area.getGeometry(), area.getLocation().clone()));
    }

    @Override
    public boolean delete(String worldName, String areaName) {
        if (failNextDelete) {
            failNextDelete = false;
            return false;
        }
        return files.remove(new Key(worldName, areaName)) != null;
    }

    /** Holds a file for an Area in a world that isn't loaded, as a server can find one on disk. */
    void holdInUnloadedWorld(String worldName, String areaName, AreaGeometry.Sphere sphere) {
        YamlConfiguration contents = new YamlConfiguration();
        contents.set("world", worldName);
        contents.set("title", "Title of " + areaName);
        contents.set("subtitle", "");
        files.put(
                new Key(worldName, areaName),
                new StoredFile(
                        contents, sphere, new Location(null, sphere.centerX(), sphere.centerY(), sphere.centerZ())));
    }

    /** What the file of the Area {@code areaName} in {@code worldName} holds, if there is one. */
    Optional<YamlConfiguration> contents(String worldName, String areaName) {
        return Optional.ofNullable(files.get(new Key(worldName, areaName))).map(StoredFile::contents);
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
