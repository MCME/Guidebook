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
     * Gives a new, unregistered Area {@code details} and writes its file, then adds it and draws it, so an Area whose
     * file couldn't be written is never added.
     *
     * @return {@link ChangeOutcome.Done}, {@link ChangeOutcome.NameTaken} if another Area took the name meanwhile
     *     (ignoring case), or {@link ChangeOutcome.NotWritten}
     */
    public ChangeOutcome create(InfoArea area, AreaDetails details) {
        if (newNameProblem(area.getName()).isPresent()) {
            return new ChangeOutcome.NameTaken();
        }
        area.setDetails(details);
        try {
            files.write(area);
        } catch (IOException ex) {
            return new ChangeOutcome.NotWritten(ex);
        }
        areas.put(area.getName(), area);
        drawAreas();
        return new ChangeOutcome.Done();
    }

    /**
     * Deletes the Area's file, then removes the Area, erases its marker and redraws the Web map, so an Area whose file
     * couldn't be deleted is kept.
     *
     * @return {@link ChangeOutcome.Done}, {@link ChangeOutcome.Gone} or {@link ChangeOutcome.NotWritten}
     */
    public ChangeOutcome delete(InfoArea area) {
        if (!isCurrent(area)) {
            return new ChangeOutcome.Gone();
        }
        if (!files.delete(area.getWorldName(), area.getName())) {
            return new ChangeOutcome.NotWritten(new IOException("Couldn't delete the file of Area " + area.getName()
                    + " in the folder of world " + area.getWorldName() + "."));
        }
        areas.remove(area.getName());
        eraseArea(area.getName());
        drawAreas();
        return new ChangeOutcome.Done();
    }

    /**
     * Gives the Area {@code newName}, writes its file under that name, and redraws the Web map with its marker under
     * the new name. If the file can't be written, the Area keeps its old name. Then its old file is deleted. The Area
     * stays the same object, so players standing in it aren't Welcomed again.
     *
     * @return {@link ChangeOutcome.Done}, {@link ChangeOutcome.Gone}, {@link ChangeOutcome.NameTaken} if an Area
     *     already has that name (ignoring case), {@link ChangeOutcome.NotWritten}, or
     *     {@link ChangeOutcome.DoneButOldFileLeft} if the old file couldn't be deleted, so the Area would load under
     *     both names
     */
    public ChangeOutcome rename(InfoArea area, String newName) {
        if (!isCurrent(area)) {
            return new ChangeOutcome.Gone();
        }
        if (newNameProblem(newName).isPresent()) {
            return new ChangeOutcome.NameTaken();
        }
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
            return new ChangeOutcome.NotWritten(ex);
        }
        boolean oldFileDeleted = files.delete(area.getWorldName(), oldName);
        eraseArea(oldName);
        drawAreas();
        return oldFileDeleted
                ? new ChangeOutcome.Done()
                : new ChangeOutcome.DoneButOldFileLeft(area.getWorldName(), oldName);
    }

    /**
     * Gives the Area {@code geometry}, which may be another Shape, with its Warp point at {@code warpPoint}, writes its
     * file, and redraws the Web map. If the file can't be written, the Area goes back where it was. Moving it to
     * another world deletes its file in the old world's folder.
     *
     * @return {@link ChangeOutcome.Done}, {@link ChangeOutcome.Gone}, {@link ChangeOutcome.NotWritten}, or
     *     {@link ChangeOutcome.DoneButOldFileLeft} if the old world's file couldn't be deleted
     */
    public ChangeOutcome move(InfoArea area, AreaGeometry geometry, Location warpPoint) {
        if (!isCurrent(area)) {
            return new ChangeOutcome.Gone();
        }
        Region region = InfoArea.regionOf(geometry, warpPoint);
        Region oldRegion = area.getRegion();
        // Found before the move: the world comes from the region
        String oldWorldName = area.getWorldName();

        area.setRegion(region);
        try {
            files.write(area);
        } catch (IOException ex) {
            area.setRegion(oldRegion);
            return new ChangeOutcome.NotWritten(ex);
        }
        boolean oldFileGone =
                Objects.equals(oldWorldName, area.getWorldName()) || files.delete(oldWorldName, area.getName());
        // The marker's id doesn't change, so this replaces it even in another world
        drawAreas();
        return oldFileGone
                ? new ChangeOutcome.Done()
                : new ChangeOutcome.DoneButOldFileLeft(oldWorldName, area.getName());
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
