package com.mcmiddleearth.guidebook.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Area files kept in memory, so tests can see what was written and make a write or delete fail. Loading builds new
 * Areas from what was written, as reading the files again would.
 */
final class InMemoryAreaFiles implements AreaFiles {

    private record Key(String world, String name) {}

    private final Map<Key, YamlConfiguration> files = new LinkedHashMap<>();
    private final Set<String> unloadedWorlds = new HashSet<>();
    private boolean failNextWrite;
    private boolean failNextDelete;
    private Optional<File> nextBackup = Optional.empty();
    private List<String> nextProblems = List.of();

    @Override
    public Loaded loadAll() {
        List<InfoArea> areas = new ArrayList<>();
        files.forEach((key, contents) ->
                AreaFileFormat.read(key.name(), contents, this::world).ifPresent(areas::add));
        Loaded loaded = new Loaded(areas, nextBackup, nextProblems);
        nextBackup = Optional.empty();
        nextProblems = List.of();
        return loaded;
    }

    private World world(String name) {
        return unloadedWorlds.contains(name) ? null : TestWorlds.named(name);
    }

    @Override
    public void write(InfoArea area) throws IOException {
        if (failNextWrite) {
            failNextWrite = false;
            throw new IOException("Told to fail this write");
        }
        YamlConfiguration contents = new YamlConfiguration();
        AreaFileFormat.write(area, contents);
        files.put(new Key(area.getWorldName(), area.getName()), contents);
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
        unloadedWorlds.add(worldName);
        YamlConfiguration contents = new YamlConfiguration();
        contents.set("world", worldName);
        contents.set("locationX", sphere.centerX());
        contents.set("locationY", sphere.centerY());
        contents.set("locationZ", sphere.centerZ());
        contents.set("radius", sphere.radius());
        contents.set("title", "Title of " + areaName);
        contents.set("subtitle", "");
        files.put(new Key(worldName, areaName), contents);
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
