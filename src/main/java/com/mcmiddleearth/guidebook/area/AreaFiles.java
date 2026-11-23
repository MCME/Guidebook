package com.mcmiddleearth.guidebook.area;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * A way of keeping each Area in a file of its own, filed under its world's name and its Area name. Loading builds new
 * Areas from the files every time, so an Area loaded earlier is never handed out again.
 */
public interface AreaFiles {

    /**
     * Every Area the files hold. A file that can't be read is left out and reported as a problem instead.
     *
     * @param backup where the files were copied to before this load changed any of them, if it did
     * @param problems one line per file that couldn't be loaded, to be logged
     */
    record Loaded(List<Area> areas, Optional<File> backup, List<String> problems) {}

    Loaded loadAll();

    /** Writes the Area to the file for its world and name, replacing what that file held. */
    void write(Area area) throws IOException;

    /** @return false if there was no such file or it couldn't be deleted */
    boolean delete(String worldName, String areaName);
}
