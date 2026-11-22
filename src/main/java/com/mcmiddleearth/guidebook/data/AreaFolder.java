package com.mcmiddleearth.guidebook.data;

import com.mcmiddleearth.guidebook.util.DevUtil;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Area files kept as YAML in Guidebook's data folder, one subfolder per world holding {@code <Area name>.yml}. Loading
 * converts Legacy markup as {@link AreaFileLoader} describes. The keys each file holds are {@link AreaFileFormat}'s.
 */
public final class AreaFolder implements AreaFiles {

    private final File dataFolder;
    private final Function<String, World> worlds;

    /**
     * Creates {@code dataFolder} if it doesn't exist yet.
     *
     * @param worlds looks a world up by its name, or gives null if it isn't loaded
     */
    public AreaFolder(File dataFolder, Function<String, World> worlds) {
        this.dataFolder = dataFolder;
        this.worlds = worlds;
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    @Override
    public Loaded loadAll() {
        AreaFileLoader.Outcome outcome = AreaFileLoader.load(dataFolder, LocalDateTime.now());
        List<InfoArea> areas = new ArrayList<>();
        List<String> problems = new ArrayList<>(outcome.problems());
        for (AreaFileLoader.LoadedArea loaded : outcome.areas()) {
            String areaName = loaded.name();
            DevUtil.log("Load area " + areaName);
            AreaFileFormat.read(areaName, loaded.config(), worlds)
                    .ifPresentOrElse(
                            areas::add,
                            () -> problems.add("Guidebook area " + areaName + " isn't loaded, because " + loaded.file()
                                    + " holds no Shape Guidebook knows."));
        }
        return new Loaded(areas, outcome.backup(), problems);
    }

    @Override
    public void write(InfoArea area) throws IOException {
        final String areaName = area.getName();
        DevUtil.log("SaveData " + areaName);

        FileConfiguration config = new YamlConfiguration();
        AreaFileFormat.write(area, config);

        File dataFile = fileOf(area.getWorldName(), areaName);
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
