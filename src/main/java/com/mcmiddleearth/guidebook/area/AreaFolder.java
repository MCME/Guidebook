package com.mcmiddleearth.guidebook.area;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Area files kept as YAML in Guidebook's data folder, one subfolder per world holding {@code <Area name>.yml}. Loading
 * converts Legacy markup as {@link AreaFileLoader} describes. The keys each file holds are {@link AreaFileFormat}'s.
 */
public final class AreaFolder implements AreaFiles {

    private final File dataFolder;

    /** Creates {@code dataFolder} if it doesn't exist yet. */
    public AreaFolder(File dataFolder) {
        this.dataFolder = dataFolder;
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    @Override
    public Loaded loadAll() {
        AreaFileLoader.Outcome outcome = AreaFileLoader.load(dataFolder, LocalDateTime.now());
        List<Area> areas = new ArrayList<>();
        List<String> problems = new ArrayList<>(outcome.problems());
        for (AreaFileLoader.LoadedArea loaded : outcome.areas()) {
            String areaName = loaded.name();
            switch (AreaFileFormat.read(areaName, loaded.config())) {
                case AreaFileFormat.Read.Found(Area area) -> areas.add(area);
                case AreaFileFormat.Read.Unreadable(String why) ->
                    problems.add(
                            "Guidebook area " + areaName + " isn't loaded, because " + loaded.file() + " " + why + ".");
            }
        }
        return new Loaded(areas, outcome.backup(), problems);
    }

    @Override
    public void write(Area area) throws IOException {
        final String areaName = area.name();
        FileConfiguration config = new YamlConfiguration();
        AreaFileFormat.write(area, config);

        File dataFile = fileOf(area.worldName(), areaName);
        dataFile.getParentFile().mkdir();
        config.save(dataFile);
    }

    @Override
    public boolean delete(String worldName, String areaName) {
        return fileOf(worldName, areaName).delete();
    }

    private File fileOf(String worldName, String areaName) {
        return new File(new File(dataFolder, worldName), areaName + ".yml");
    }
}
