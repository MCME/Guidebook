package com.mcmiddleearth.guidebook.area;

import com.mcmiddleearth.guidebook.webmap.MarkerLayer;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Holds every Area, keeps each one's file up to date through {@link AreaFiles}, and draws every Area on the Web map
 * after each change (ADR 0007).
 *
 * <p>A change either reaches the Area's file or doesn't happen: if the file can't be written or deleted, the Area is
 * left as it was and the change returns {@link ChangeOutcome.NotWritten}, so players and the Web map never see an
 * Area its file doesn't hold. An Area stays the same object through every change, since the Watchman and open dialogs
 * hold it. A change to an Area deleted or reloaded since the caller found it returns {@link ChangeOutcome.Gone}.
 */
public final class AreaStore {

    // The characters Brigadier allows in an unquoted word, so every name can be typed as one argument
    private static final Pattern VALID_NAME = Pattern.compile("[0-9A-Za-z_\\-.+]+");

    private final Map<String, Area> areas = new HashMap<>();

    private final AreaFiles files;
    private final MarkerLayer webMapLayer;
    private final Predicate<String> worldLoaded;
    private final Logger logger;

    /**
     * Holds no Areas until {@link #reload()}.
     *
     * @param worldLoaded whether the world of that name is loaded now. An Area whose world isn't has nowhere to be
     *     drawn on the Web map; it's asked at each drawing, so a world loaded later shows its Areas at the next change
     * @param logger where load problems and Areas left off the Web map are logged
     */
    public AreaStore(AreaFiles files, MarkerLayer webMapLayer, Predicate<String> worldLoaded, Logger logger) {
        this.files = files;
        this.webMapLayer = webMapLayer;
        this.worldLoaded = worldLoaded;
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
     * Adds a new Area with {@code details}. Returns {@link ChangeOutcome.NameTaken} if its name can no longer be given
     * (see {@link #newNameProblem}), since another Area may have taken it while staff filled in the dialog.
     */
    public ChangeOutcome create(Area area, AreaDetails details) {
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

    public ChangeOutcome delete(Area area) {
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
     * Gives the Area {@code newName}, so players standing in it aren't Welcomed again. Its file under the old name is
     * deleted afterwards; if that fails, the rename stands but returns {@link ChangeOutcome.DoneButOldFileLeft},
     * because the Area would load under both names.
     */
    public ChangeOutcome rename(Area area, String newName) {
        if (!isCurrent(area)) {
            return new ChangeOutcome.Gone();
        }
        if (newNameProblem(newName).isPresent()) {
            return new ChangeOutcome.NameTaken();
        }

        String oldName = area.getName();
        giveName(area, newName);
        try {
            files.write(area);
        } catch (IOException ex) {
            giveName(area, oldName);
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
     * Places the Area as {@link Area#place} does, possibly in another world or with another Shape. Moving it to
     * another world deletes its file in the old world's folder; if that fails, the move stands but returns
     * {@link ChangeOutcome.DoneButOldFileLeft}.
     */
    public ChangeOutcome move(Area area, String worldName, WarpPoint warpPoint, AreaGeometry geometry) {
        if (!isCurrent(area)) {
            return new ChangeOutcome.Gone();
        }

        String oldWorldName = area.getWorldName();
        WarpPoint oldWarpPoint = area.getWarpPoint();
        AreaGeometry oldGeometry = area.getGeometry();
        area.place(worldName, warpPoint, geometry);
        try {
            files.write(area);
        } catch (IOException ex) {
            area.place(oldWorldName, oldWarpPoint, oldGeometry);
            return new ChangeOutcome.NotWritten(ex);
        }

        boolean oldFileGone = Objects.equals(oldWorldName, worldName) || files.delete(oldWorldName, area.getName());
        // The marker's id doesn't change, so this replaces it even in another world
        drawAreas();
        return oldFileGone
                ? new ChangeOutcome.Done()
                : new ChangeOutcome.DoneButOldFileLeft(oldWorldName, area.getName());
    }

    public ChangeOutcome edit(Area area, AreaDetails details) {
        if (!isCurrent(area)) {
            return new ChangeOutcome.Gone();
        }

        AreaDetails oldDetails = area.details();
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

    /**
     * The Area a typed name reaches, ignoring case. An Area with exactly that name wins, for folders that already hold
     * names differing only in case.
     */
    public Optional<Area> find(String name) {
        Area exact = areas.get(name);
        if (exact != null) {
            return Optional.of(exact);
        }
        return areas.values().stream()
                .filter(area -> area.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    /** Why the name can't be given to a new or renamed Area, or empty if it can. */
    public Optional<String> newNameProblem(String name) {
        if (!VALID_NAME.matcher(name).matches()) {
            return Optional.of("'" + name + "' isn't a valid Area name. Use only letters, digits and _ - . +");
        }
        return find(name).map(area -> "Area " + area.getName() + " already has that name");
    }

    /** A suggested Area name, with what its tooltip shows about the Area. */
    public record Suggestion(String name, String title, Shape shape, boolean disabled) {}

    /**
     * The Areas whose name has a hyphen segment starting with the typed text, ignoring case, since an Area name is
     * written {@code <world>-<project>-<place>}: {@code minas} and {@code gondor-mi} both reach
     * {@code world-gondor-minas}.
     */
    public List<Suggestion> suggest(String typed) {
        String search = typed.toLowerCase(Locale.ROOT);
        return areas.values().stream()
                .filter(area -> anySegmentStartsWith(area.getName(), search))
                .map(area -> new Suggestion(area.getName(), area.details().title(), area.getShape(), !area.isEnabled()))
                .toList();
    }

    /** Every Area, as it is now; later changes show through. */
    public Collection<Area> all() {
        return Collections.unmodifiableCollection(areas.values());
    }

    private static boolean anySegmentStartsWith(String name, String search) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        if (lowerName.startsWith(search)) {
            return true;
        }
        for (int i = lowerName.indexOf('-'); i != -1; i = lowerName.indexOf('-', i + 1)) {
            if (lowerName.startsWith(search, i + 1)) {
                return true;
            }
        }
        return false;
    }

    /** Gives the Area {@code name}, keeping it under that name in the store. */
    private void giveName(Area area, String name) {
        areas.remove(area.getName());
        area.setName(name);
        areas.put(name, area);
    }

    /**
     * Whether the Area is the one the store holds under its name. A rename keeps the same Area, so it stays current,
     * but a delete or a reload drops it.
     */
    private boolean isCurrent(Area area) {
        return areas.get(area.getName()) == area;
    }

    private void drawWebMap() {
        webMapLayer.clear();
        areas.values().stream()
                .filter(area -> !isInLoadedWorld(area))
                .forEach(area -> logger.warning(
                        "Area " + area.getName() + " isn't drawn on the Web map because its world isn't loaded."));
        drawAreas();
    }

    /**
     * Draws every Area again, since a change to one can change whether another holds an Area and so is drawn unfilled.
     * An Area whose world isn't loaded has nowhere to be drawn, so its marker is erased.
     */
    private void drawAreas() {
        List<Area> drawn = areas.values().stream().filter(this::isInLoadedWorld).toList();
        drawn.forEach(area -> webMapLayer.put(AreaMarkers.toMarker(area, drawn)));
        areas.values().stream().filter(area -> !isInLoadedWorld(area)).forEach(area -> eraseArea(area.getName()));
    }

    private boolean isInLoadedWorld(Area area) {
        return worldLoaded.test(area.getWorldName());
    }

    private void eraseArea(String areaName) {
        webMapLayer.remove(AreaMarkers.id(areaName));
    }
}
