package com.mcmiddleearth.guidebook.welcome;

import com.mcmiddleearth.guidebook.area.Area;
import com.mcmiddleearth.guidebook.area.AreaDetails;
import com.mcmiddleearth.guidebook.area.WorldPoint;
import com.mcmiddleearth.guidebook.text.AreaText;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Notices players entering and leaving Areas, decides which of them get a Welcome, and hands each Welcome to the
 * server's Display. It also shows each Area's Boss bar to the players inside it.
 */
public final class Watchman {

    private static final Duration COOLDOWN = Duration.ofMinutes(1);

    // About as long as a Title is on screen, so the two don't usually overlap. Fixed whatever the Display, since
    // nobody needs it exact
    private static final long BOSS_BAR_DELAY_AFTER_TITLE = 50;

    private final Display display;
    private final Veto veto;
    private final InstantSource clock;
    private final Later later;
    private final Set<UUID> optedOut = new HashSet<>();

    // Keyed by the Area object, so a renamed Area keeps its players and a reloaded one starts afresh

    /** The players inside each Area. */
    private final Map<Area, Set<UUID>> inside = new HashMap<>();

    /** When each player was last Welcomed to each Area, for the cooldown. */
    private final Map<Area, Map<UUID, Instant>> lastWelcomed = new HashMap<>();

    private final Map<Area, BossBar> bossBars = new HashMap<>();

    /** The players each Area's Boss bar is shown to. */
    private final Map<Area, Map<UUID, Player>> bossBarViewers = new HashMap<>();

    /**
     * @param display shows each Welcome
     * @param veto fires {@link com.mcmiddleearth.guidebook.events.GuidebookSendEvent}, so other plugins can cancel a
     *     Welcome
     * @param later runs a task some ticks from now, on the main thread
     * @param optedOut the players who are Opted out to start with
     */
    public Watchman(Display display, Veto veto, InstantSource clock, Later later, Collection<UUID> optedOut) {
        this.display = display;
        this.veto = veto;
        this.clock = clock;
        this.later = later;
        this.optedOut.addAll(optedOut);
    }

    /** Asks other plugins whether a player may be Welcomed to an Area. */
    @FunctionalInterface
    public interface Veto {

        /** @return true if the Welcome is cancelled */
        boolean cancels(Player player, String areaName);
    }

    /** Runs a task later on the main thread. */
    @FunctionalInterface
    public interface Later {

        void run(long ticks, Runnable task);
    }

    public void optOut(UUID playerId) {
        optedOut.add(playerId);
    }

    /** Opts a player back in. The Areas they're standing in count as entered again, so they get those Welcomes now. */
    public void optIn(UUID playerId) {
        optedOut.remove(playerId);
        inside.values().forEach(players -> players.remove(playerId));
    }

    private boolean isOptedOut(UUID playerId) {
        return optedOut.contains(playerId);
    }

    /** The players who are Opted out now, to save. */
    public Set<UUID> optedOut() {
        return Set.copyOf(optedOut);
    }

    /**
     * Notices which players entered or left which Areas since the last tick. An Area that is Disabled or no longer
     * among {@code areas} (deleted, or replaced by a reload) has been left by everyone, and is forgotten.
     */
    public void tick(Collection<Area> areas, Collection<? extends Player> players) {
        List<Area> enabled = areas.stream().filter(Area::isEnabled).toList();
        forgetAllBut(new HashSet<>(enabled));
        updateBossBars();

        for (Player player : players) {
            WorldPoint point = WorldPoint.from(player.getLocation());
            for (Area area : enabled) {
                Set<UUID> playersInside = inside.computeIfAbsent(area, a -> new HashSet<>());
                if (playersInside.contains(player.getUniqueId())) {
                    if (!area.isNear(point)) {
                        playersInside.remove(player.getUniqueId());
                        hideBossBar(area, player.getUniqueId());
                    }
                } else if (area.isInside(point)) {
                    playersInside.add(player.getUniqueId());
                    enter(player, area);
                }
            }
        }
    }

    /**
     * Shows staff an Area's Welcome now through the server's Display. None of the checks a player entering the Area
     * goes through apply, and it doesn't count as Welcoming them.
     */
    public void preview(Player player, Area area) {
        // An Area just created or Enabled doesn't know yet that the player is inside. Marking them stops their next
        // move counting as an entry and giving them the Welcome again
        if (area.isEnabled() && area.isInside(WorldPoint.from(player.getLocation()))) {
            inside.computeIfAbsent(area, a -> new HashSet<>()).add(player.getUniqueId());
        }
        welcome(player, area);
    }

    /** Forgets a player who left the server, so they're Welcomed again when they come back. */
    public void quit(Player player) {
        UUID playerId = player.getUniqueId();
        inside.values().forEach(players -> players.remove(playerId));
        lastWelcomed.values().forEach(welcomed -> welcomed.remove(playerId));
        bossBars.keySet().forEach(area -> hideBossBar(area, playerId));
    }

    /** Hides every Boss bar, for when Guidebook disables. */
    public void close() {
        forgetAllBut(Set.of());
    }

    private void enter(Player player, Area area) {
        Instant now = clock.instant();
        Map<UUID, Instant> welcomed = lastWelcomed.computeIfAbsent(area, a -> new HashMap<>());
        Instant last = welcomed.get(player.getUniqueId());
        if (last != null && Duration.between(last, now).compareTo(COOLDOWN) < 0) {
            return;
        }
        if (isOptedOut(player.getUniqueId()) || veto.cancels(player, area.name())) {
            return;
        }
        welcome(player, area);
        welcomed.put(player.getUniqueId(), now);
    }

    private void welcome(Player player, Area area) {
        display.show(player, welcomeTo(area));

        if (area.details().showBossBar()) {
            long delay = area.details().showTitle() ? BOSS_BAR_DELAY_AFTER_TITLE : 0;
            later.run(delay, () -> {
                // Only while still inside, or a player who left during the Title would keep the bar
                if (area.details().showBossBar()
                        && inside.getOrDefault(area, Set.of()).contains(player.getUniqueId())) {
                    showBossBar(area, player);
                }
            });
        }
    }

    private static Welcome welcomeTo(Area area) {
        AreaDetails details = area.details();
        return new Welcome(
                area.name(), details.title(), details.subtitle(), details.description(), details.showTitle());
    }

    /** Forgets every Area not in {@code kept}, hiding its Boss bar. */
    private void forgetAllBut(Set<Area> kept) {
        inside.keySet().retainAll(kept);
        lastWelcomed.keySet().retainAll(kept);
        for (Area area : List.copyOf(bossBars.keySet())) {
            if (!kept.contains(area)) {
                hideBossBar(area);
                bossBars.remove(area);
            }
        }
    }

    /** Hides Boss bars their Areas no longer show, and renames the rest after their Areas' Titles. */
    private void updateBossBars() {
        bossBars.forEach((area, bar) -> {
            if (area.details().showBossBar()) {
                bar.name(AreaText.render(area.details().title()));
            } else {
                hideBossBar(area);
            }
        });
    }

    private void showBossBar(Area area, Player player) {
        BossBar bar = bossBars.computeIfAbsent(
                area, a -> BossBar.bossBar(Component.empty(), 0, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS));
        bar.name(AreaText.render(area.details().title()));
        bossBarViewers.computeIfAbsent(area, a -> new HashMap<>()).put(player.getUniqueId(), player);
        player.showBossBar(bar);
    }

    private void hideBossBar(Area area, UUID playerId) {
        Map<UUID, Player> viewers = bossBarViewers.getOrDefault(area, Map.of());
        Player player = viewers.get(playerId);
        if (player != null) {
            viewers.remove(playerId);
            player.hideBossBar(bossBars.get(area));
        }
    }

    private void hideBossBar(Area area) {
        Map<UUID, Player> viewers = bossBarViewers.remove(area);
        if (viewers != null) {
            viewers.values().forEach(player -> player.hideBossBar(bossBars.get(area)));
        }
    }
}
