package com.mcmiddleearth.guidebook.welcome;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mcmiddleearth.guidebook.area.Area;
import com.mcmiddleearth.guidebook.area.AreaDetails;
import com.mcmiddleearth.guidebook.area.AreaGeometry;
import com.mcmiddleearth.guidebook.area.AreaStore;
import com.mcmiddleearth.guidebook.area.ChangeOutcome;
import com.mcmiddleearth.guidebook.area.InMemoryAreaFiles;
import com.mcmiddleearth.guidebook.area.RecordingMarkerLayer;
import com.mcmiddleearth.guidebook.area.TestAreas;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WatchmanTest {

    /** Holds every Area a test builds, so tests change Areas as a saved edit in the Edit dialog does. */
    final AreaStore store =
            new AreaStore(new InMemoryAreaFiles(), new RecordingMarkerLayer(), Logger.getAnonymousLogger());

    /** An Area filling x from {@code minX} to {@code maxX} at every height and depth the test players use. */
    Area area(String name, int minX, int maxX) {
        Area area = TestAreas.area(name, "middle-earth", new AreaGeometry.Cuboid(minX, 0, -100, maxX, 128, 100));
        assertEquals(
                new ChangeOutcome.Done(),
                store.create(area, new AreaDetails(false, "", "", false, "<guide>A place.", true)));
        return area;
    }

    /** Gives the Area {@code details} through the Area store. */
    void edit(Area area, AreaDetails details) {
        assertEquals(new ChangeOutcome.Done(), store.edit(area, details));
    }

    void setTitle(Area area, String title) {
        AreaDetails d = area.details();
        edit(area, new AreaDetails(d.showTitle(), title, d.subtitle(), d.showBossBar(), d.description(), d.enabled()));
    }

    void setSubtitle(Area area, String subtitle) {
        AreaDetails d = area.details();
        edit(area, new AreaDetails(d.showTitle(), d.title(), subtitle, d.showBossBar(), d.description(), d.enabled()));
    }

    void setShowTitle(Area area, boolean showTitle) {
        AreaDetails d = area.details();
        edit(area, new AreaDetails(showTitle, d.title(), d.subtitle(), d.showBossBar(), d.description(), d.enabled()));
    }

    void setShowBossBar(Area area, boolean showBossBar) {
        AreaDetails d = area.details();
        edit(area, new AreaDetails(d.showTitle(), d.title(), d.subtitle(), showBossBar, d.description(), d.enabled()));
    }

    void setEnabled(Area area, boolean enabled) {
        AreaDetails d = area.details();
        edit(area, new AreaDetails(d.showTitle(), d.title(), d.subtitle(), d.showBossBar(), d.description(), enabled));
    }

    /**
     * The world every test player stands in, answering only its name. Kept here, because a {@link Location} holds its
     * world only weakly.
     */
    static final World WORLD = (World) Proxy.newProxyInstance(
            World.class.getClassLoader(), new Class<?>[] {World.class}, (proxy, method, args) -> {
                return switch (method.getName()) {
                    case "getName" -> "middle-earth";
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "test world";
                    default -> null;
                };
            });

    /**
     * A player standing at an x coordinate, who keeps the Boss bars they're shown. Bukkit's Player is an interface, so
     * a proxy stands in for the server.
     */
    static final class TestPlayer {
        final UUID id = UUID.randomUUID();
        double x;
        final List<BossBar> bossBars = new ArrayList<>();
        final Player player;

        TestPlayer(double x) {
            this.x = x;
            player = (Player) Proxy.newProxyInstance(
                    Player.class.getClassLoader(), new Class<?>[] {Player.class}, (proxy, method, args) -> {
                        return switch (method.getName()) {
                            case "getUniqueId" -> id;
                            case "getLocation" -> new Location(WORLD, this.x, 64, 0);
                            case "showBossBar" -> {
                                if (!bossBars.contains((BossBar) args[0])) {
                                    bossBars.add((BossBar) args[0]);
                                }
                                yield null;
                            }
                            case "hideBossBar" -> {
                                bossBars.remove((BossBar) args[0]);
                                yield null;
                            }
                            case "equals" -> proxy == args[0];
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "toString" -> "player at " + this.x;
                            default -> null;
                        };
                    });
        }
    }

    record Shown(Player player, Welcome welcome) {}

    final List<Shown> shown = new ArrayList<>();
    final Display recording = (player, welcome) -> shown.add(new Shown(player, welcome));
    Instant now = Instant.parse("2026-11-18T12:00:00Z");

    /** The Areas GuidebookSendEvent was fired for, and whether listeners cancel it. */
    final List<String> fired = new ArrayList<>();

    boolean cancelEvents;

    record Task(long ticks, Runnable task) {}

    /** Tasks waiting to run later, run by {@link #runLaterTasks}. */
    final List<Task> later = new ArrayList<>();

    void runLaterTasks() {
        List<Task> due = List.copyOf(later);
        later.clear();
        due.forEach(task -> task.task().run());
    }

    final Watchman watchman = new Watchman(
            recording,
            (player, areaName) -> {
                fired.add(areaName);
                return cancelEvents;
            },
            () -> now,
            (ticks, task) -> later.add(new Task(ticks, task)),
            Set.of());

    @Test
    void aPlayerWalkingIntoAnAreaIsWelcomed() {
        setTitle(bree, "Bree");
        setSubtitle(bree, "The Prancing Pony");
        setShowTitle(bree, true);
        TestPlayer player = new TestPlayer(-50);

        watchman.tick(List.of(bree), List.of(player.player));
        assertEquals(List.of(), shown);

        player.x = 50;
        watchman.tick(List.of(bree), List.of(player.player));

        assertEquals(
                List.of(new Shown(
                        player.player, new Welcome("bree", "Bree", "The Prancing Pony", "<guide>A place.", true))),
                shown);
    }

    final Area bree = area("bree", 0, 100);

    /** Moves the player to {@code x} and ticks over just Bree. */
    void moveTo(TestPlayer player, double x) {
        player.x = x;
        watchman.tick(List.of(bree), List.of(player.player));
    }

    @Test
    void reEnteringWithinAMinuteGivesNoSecondWelcome() {
        TestPlayer player = new TestPlayer(50);
        moveTo(player, 50);

        now = now.plusSeconds(59);
        moveTo(player, 200);
        moveTo(player, 50);

        assertEquals(1, shown.size());
    }

    @Test
    void reEnteringAfterAMinuteWelcomesAgain() {
        TestPlayer player = new TestPlayer(50);
        moveTo(player, 50);

        now = now.plusSeconds(60);
        moveTo(player, 200);
        moveTo(player, 50);

        assertEquals(2, shown.size());
    }

    @Test
    void walkingJustOutsideTheShapeIsStillInside() {
        TestPlayer player = new TestPlayer(50);
        moveTo(player, 50);

        now = now.plusSeconds(120);
        moveTo(player, 105);
        moveTo(player, 50);

        assertEquals(1, shown.size());
    }

    @Test
    void anOptedOutPlayerIsNotWelcomedAndNoEventIsFired() {
        TestPlayer player = new TestPlayer(-50);
        watchman.optOut(player.id);

        moveTo(player, 50);

        assertEquals(List.of(), shown);
        assertEquals(List.of(), fired);
    }

    @Test
    void aPlayerWhoOptsBackInIsWelcomedOnTheirNextEntry() {
        TestPlayer player = new TestPlayer(-50);
        watchman.optOut(player.id);
        moveTo(player, 50);
        moveTo(player, 200);

        watchman.optIn(player.id);
        moveTo(player, 50);

        assertEquals(1, shown.size());
    }

    @Test
    void aPlayerWhoOptsBackInInsideAnAreaIsWelcomedToIt() {
        TestPlayer player = new TestPlayer(-50);
        watchman.optOut(player.id);
        moveTo(player, 50);

        watchman.optIn(player.id);
        moveTo(player, 50);

        assertEquals(1, shown.size());
    }

    @Test
    void optingBackInWithinTheCooldownGivesNoSecondWelcome() {
        TestPlayer player = new TestPlayer(-50);
        moveTo(player, 50);
        watchman.optOut(player.id);

        watchman.optIn(player.id);
        moveTo(player, 50);

        assertEquals(1, shown.size());
    }

    @Test
    void aCancelledEventStopsTheWelcomeAndIsFiredOncePerEntry() {
        cancelEvents = true;
        TestPlayer player = new TestPlayer(-50);

        moveTo(player, 50);
        moveTo(player, 60);

        assertEquals(List.of(), shown);
        assertEquals(List.of("bree"), fired);
    }

    @Test
    void aCancelledWelcomeStartsNoCooldown() {
        cancelEvents = true;
        TestPlayer player = new TestPlayer(-50);
        moveTo(player, 50);
        moveTo(player, 200);

        cancelEvents = false;
        moveTo(player, 50);

        assertEquals(1, shown.size());
    }

    @Test
    void aDisabledAreaWelcomesNobody() {
        setEnabled(bree, false);
        TestPlayer player = new TestPlayer(-50);

        moveTo(player, 50);

        assertEquals(List.of(), shown);
    }

    @Test
    void reEnablingAnAreaWelcomesAPlayerStandingInIt() {
        TestPlayer player = new TestPlayer(-50);
        moveTo(player, 50);

        setEnabled(bree, false);
        moveTo(player, 50);
        setEnabled(bree, true);
        moveTo(player, 50);

        assertEquals(2, shown.size());
    }

    @Test
    void anAreaGoneFromTheTickHasBeenLeft() {
        TestPlayer player = new TestPlayer(-50);
        moveTo(player, 50);

        // Deleted, then made again under the same name by a reload
        watchman.tick(List.of(), List.of(player.player));
        moveTo(player, 50);

        assertEquals(2, shown.size());
    }

    @Test
    void aPlayerWhoRejoinsInsideAnAreaIsWelcomedAgain() {
        TestPlayer player = new TestPlayer(-50);
        moveTo(player, 50);

        watchman.quit(player.player);
        moveTo(player, 50);

        assertEquals(2, shown.size());
    }

    @Test
    void aPreviewIgnoresTheCooldownOptingOutAndTheEvent() {
        cancelEvents = true;
        TestPlayer player = new TestPlayer(-50);
        moveTo(player, 50);
        watchman.optOut(player.id);
        fired.clear();

        watchman.preview(player.player, bree);

        assertEquals(List.of(new Shown(player.player, new Welcome("bree", "", "", "<guide>A place.", false))), shown);
        assertEquals(List.of(), fired);
    }

    @Test
    void aPreviewStartsNoCooldown() {
        TestPlayer player = new TestPlayer(-50);
        watchman.preview(player.player, bree);

        moveTo(player, 50);

        assertEquals(2, shown.size());
    }

    @Test
    void aPreviewInsideTheAreaCountsAsEnteringIt() {
        TestPlayer player = new TestPlayer(50);
        watchman.preview(player.player, bree);

        moveTo(player, 50);

        assertEquals(1, shown.size());
    }

    @Nested
    class TheBossBar {

        final TestPlayer player = new TestPlayer(-50);

        @BeforeEach
        void showTheBossBar() {
            setTitle(bree, "<gold>Bree");
            setShowBossBar(bree, true);
        }

        String bossBarText() {
            assertEquals(1, player.bossBars.size(), "Boss bars shown");
            return PlainTextComponentSerializer.plainText()
                    .serialize(player.bossBars.getFirst().name());
        }

        @Test
        void appearsAfterTheTitleOfAWelcome() {
            setShowTitle(bree, true);

            moveTo(player, 50);

            assertEquals(List.of(), player.bossBars);
            assertEquals(List.of(50L), later.stream().map(Task::ticks).toList());
            runLaterTasks();
            assertEquals("Bree", bossBarText());
        }

        @Test
        void appearsStraightAwayWhenTheTitleIsntShown() {
            moveTo(player, 50);

            assertEquals(List.of(0L), later.stream().map(Task::ticks).toList());
        }

        @Test
        void isntShownToAPlayerWhoLeftBeforeItAppeared() {
            moveTo(player, 50);
            moveTo(player, 200);
            runLaterTasks();

            assertEquals(List.of(), player.bossBars);
        }

        @Test
        void disappearsWhenThePlayerLeaves() {
            moveTo(player, 50);
            runLaterTasks();

            moveTo(player, 200);

            assertEquals(List.of(), player.bossBars);
        }

        @Test
        void disappearsWhenTheAreaIsDisabled() {
            moveTo(player, 50);
            runLaterTasks();

            setEnabled(bree, false);
            moveTo(player, 50);

            assertEquals(List.of(), player.bossBars);
        }

        @Test
        void disappearsWhenTheAreaStopsShowingIt() {
            moveTo(player, 50);
            runLaterTasks();

            setShowBossBar(bree, false);
            moveTo(player, 50);

            assertEquals(List.of(), player.bossBars);
        }

        @Test
        void followsAChangedTitle() {
            moveTo(player, 50);
            runLaterTasks();

            setTitle(bree, "Bree-land");
            moveTo(player, 50);

            assertEquals("Bree-land", bossBarText());
        }

        @Test
        void disappearsWhenGuidebookDisables() {
            moveTo(player, 50);
            runLaterTasks();

            watchman.close();

            assertEquals(List.of(), player.bossBars);
        }
    }
}
