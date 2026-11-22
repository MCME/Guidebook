package com.mcmiddleearth.guidebook.data;

import com.mcmiddleearth.guidebook.util.DevUtil;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Area files kept as YAML in Guidebook's data folder, one subfolder per world holding {@code <Area name>.yml}. Loading
 * converts Legacy markup as {@link AreaFileLoader} describes.
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
        List<InfoArea> areas = new ArrayList<>();
        for (AreaFileLoader.LoadedArea loaded : outcome.areas()) {
            String areaName = loaded.name();
            DevUtil.log("Load area " + areaName);
            InfoArea.load(areaName, loaded.config()).ifPresent(areas::add);
        }
        return new Loaded(areas, outcome.backup(), outcome.problems());
    }

    @Override
    public void write(InfoArea area) throws IOException {
        final String areaName = area.getName();
        DevUtil.log("SaveData " + areaName);

        FileConfiguration config = new YamlConfiguration();
        area.save(config);

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
