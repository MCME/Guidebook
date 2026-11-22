package com.mcmiddleearth.guidebook.data;

import com.mcmiddleearth.guidebook.webmap.MarkerLayer;
import com.mcmiddleearth.pluginutil.region.Region;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.Location;

/**
 * Holds every Area, keeps each one's file up to date through {@link AreaFiles}, and draws every Area on the Web map
 * after each change (ADR 0007).
 */
public final class AreaStore {

    private final Map<String, InfoArea> areas = new HashMap<>();
    private final AreaRegistry<InfoArea> registry = new AreaRegistry<>(areas.values());

    private final AreaFiles files;
    private final MarkerLayer webMapLayer;
    private final Logger logger;

    /**
     * Holds no Areas until {@link #reload()}.
     *
     * @param logger where load problems and Areas left off the Web map are logged
     */
    public AreaStore(AreaFiles files, MarkerLayer webMapLayer, Logger logger) {
        this.files = files;
        this.webMapLayer = webMapLayer;
        this.logger = logger;
    }

    /** Replaces every Area with what the files hold, and draws the Web map again from scratch. */
    public void reload() {
        areas.clear();
        AreaFiles.Loaded loaded = files.loadAll();
        loaded.backup()
                .ifPresent(backup -> logger.info("Backed up the Guidebook folder to " + backup
                        + " before converting Guidebook areas from Legacy markup to MiniMessage."));
        loaded.problems().forEach(logger::severe);
        loaded.areas().forEach(area -> areas.put(area.getName(), area));
        drawWebMap();
    }

    /**
     * Writes a new Area's file, then adds it and draws it, so an Area whose file couldn't be written is never added.
     */
    public void create(InfoArea area) throws IOException {
        files.write(area);
        areas.put(area.getName(), area);
        drawAreas();
    }

    /** @return false if the Area's file couldn't be deleted, so the Area is kept */
    public boolean delete(InfoArea area) {
        boolean deleted = files.delete(area.getWorldName(), area.getName());
        if (deleted) {
            areas.remove(area.getName());
            eraseArea(area.getName());
            drawAreas();
        }
        return deleted;
    }

    /**
     * @return false if the Area was renamed but its old file couldn't be deleted, so it would load under both names
     *     after a reload
     */
    public boolean rename(InfoArea area, String newName) throws IOException {
        String oldName = area.getName();
        areas.remove(oldName);
        area.setName(newName);
        areas.put(newName, area);
        try {
            files.write(area);
        } catch (IOException ex) {
            areas.remove(newName);
            area.setName(oldName);
            areas.put(oldName, area);
            throw ex;
        }
        boolean oldFileDeleted = files.delete(area.getWorldName(), oldName);
        eraseArea(oldName);
        drawAreas();
        return oldFileDeleted;
    }

    /**
     * Gives the Area {@code geometry}, which may be another Shape, with its Warp point at {@code warpPoint}.
     *
     * @return false if the Area was moved to another world but its old world's file couldn't be deleted, so it would
     *     load twice after a reload
     */
    public boolean move(InfoArea area, AreaGeometry geometry, Location warpPoint) throws IOException {
        Region region = InfoArea.regionOf(geometry, warpPoint);
        Region oldRegion = area.getRegion();
        // Found before the move: the world comes from the region
        String oldWorldName = area.getWorldName();

        area.setRegion(region);
        try {
            files.write(area);
        } catch (IOException ex) {
            area.setRegion(oldRegion);
            throw ex;
        }
        boolean oldFileGone =
                Objects.equals(oldWorldName, area.getWorldName()) || files.delete(oldWorldName, area.getName());
        // The marker's id doesn't change, so this replaces it even in another world
        drawAreas();
        return oldFileGone;
    }

    /**
     * Gives the Area {@code details}, writes its file, and redraws the Web map. If the file can't be written, the Area
     * gets its old details back, so players and the Web map never see an edit that isn't saved. The Area stays the
     * same object, since the Watchman and open dialogs hold it.
     *
     * @return {@link ChangeOutcome.Done}, {@link ChangeOutcome.Gone} or {@link ChangeOutcome.NotWritten}
     */
    public ChangeOutcome edit(InfoArea area, AreaDetails details) {
        if (!isCurrent(area)) {
            return new ChangeOutcome.Gone();
        }
        AreaDetails oldDetails = AreaDetails.of(area);
        area.setDetails(details);
        try {
            files.write(area);
        } catch (IOException ex) {
            area.setDetails(oldDetails);
            return new ChangeOutcome.NotWritten(ex);
        }
        drawAreas();
        return new ChangeOutcome.Done();
    }

    /** The Area the typed name reaches, ignoring case; an Area with exactly that name wins. */
    public Optional<InfoArea> find(String name) {
        return registry.resolve(name);
    }

    /** The Area with exactly this name. */
    public Optional<InfoArea> findExact(String name) {
        return registry.resolveExact(name);
    }

    /** Why the name can't be given to a new or renamed Area, or empty if it can. */
    public Optional<String> newNameProblem(String name) {
        return registry.newNameProblem(name);
    }

    public List<AreaRegistry.Suggestion> suggest(String typed) {
        return registry.suggest(typed);
    }

    /** Every Area, as it is now; later changes show through. */
    public Collection<InfoArea> all() {
        return Collections.unmodifiableCollection(areas.values());
    }

    /** Every Area by its exact name, for the static store until its callers move here. */
    Map<String, InfoArea> byName() {
        return Collections.unmodifiableMap(areas);
    }

    /**
     * Whether the Area is the one the store holds under its name. A rename keeps the same Area, so it stays current,
     * but a delete or a reload drops it.
     */
    private boolean isCurrent(InfoArea area) {
        return areas.get(area.getName()) == area;
    }

    private void drawWebMap() {
        webMapLayer.clear();
        areas.values().stream()
                .filter(area -> !area.getRegion().isValid())
                .forEach(area -> logger.warning(
                        "Area " + area.getName() + " isn't drawn on the Web map because its world isn't loaded."));
        drawAreas();
    }

    /**
     * Draws every Area again, since a change to one can change whether another holds an Area and so is drawn unfilled.
     * An Area whose world isn't loaded has nowhere to be drawn, so its marker is erased.
     */
    private void drawAreas() {
        List<InfoArea> drawn = areas.values().stream()
                .filter(area -> area.getRegion().isValid())
                .toList();
        drawn.forEach(area -> webMapLayer.put(AreaMarkers.toMarker(area, drawn)));
        areas.values().stream().filter(area -> !area.getRegion().isValid()).forEach(area -> eraseArea(area.getName()));
    }

    private void eraseArea(String areaName) {
        webMapLayer.remove(AreaMarkers.id(areaName));
    }
}
