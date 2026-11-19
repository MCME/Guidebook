package com.mcmiddleearth.guidebook.welcome;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WatchmanTest {

    /** An Area filling x from {@code minX} to {@code maxX}, near up to 10 blocks outside that. */
    static final class TestArea implements WelcomeArea {
        final String name;
        final int minX;
        final int maxX;
        String title = "";
        String subtitle = "";
        String description = "<guide>A place.";
        boolean showTitle;
        boolean showBossBar;
        boolean enabled = true;

        TestArea(String name, int minX, int maxX) {
            this.name = name;
            this.minX = minX;
            this.maxX = maxX;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getSubtitle() {
            return subtitle;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public boolean isShowTitle() {
            return showTitle;
        }

        @Override
        public boolean isShowBossBar() {
            return showBossBar;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public boolean isInside(Location location) {
            return location.getX() >= minX && location.getX() <= maxX;
        }

        @Override
        public boolean isNear(Location location) {
            return location.getX() >= minX - 10 && location.getX() <= maxX + 10;
        }
    }

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
                            case "getLocation" -> new Location(null, this.x, 64, 0);
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
        TestArea bree = new TestArea("bree", 0, 100);
        bree.title = "Bree";
        bree.subtitle = "The Prancing Pony";
        bree.showTitle = true;
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

    final TestArea bree = new TestArea("bree", 0, 100);

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
        bree.enabled = false;
        TestPlayer player = new TestPlayer(-50);

        moveTo(player, 50);

        assertEquals(List.of(), shown);
    }

    @Test
    void reEnablingAnAreaWelcomesAPlayerStandingInIt() {
        TestPlayer player = new TestPlayer(-50);
        moveTo(player, 50);

        bree.enabled = false;
        moveTo(player, 50);
        bree.enabled = true;
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
            bree.title = "<gold>Bree";
            bree.showBossBar = true;
        }

        String bossBarText() {
            assertEquals(1, player.bossBars.size(), "Boss bars shown");
            return PlainTextComponentSerializer.plainText()
                    .serialize(player.bossBars.getFirst().name());
        }

        @Test
        void appearsAfterTheTitleOfAWelcome() {
            bree.showTitle = true;

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

            bree.enabled = false;
            moveTo(player, 50);

            assertEquals(List.of(), player.bossBars);
        }

        @Test
        void disappearsWhenTheAreaStopsShowingIt() {
            moveTo(player, 50);
            runLaterTasks();

            bree.showBossBar = false;
            moveTo(player, 50);

            assertEquals(List.of(), player.bossBars);
        }

        @Test
        void followsAChangedTitle() {
            moveTo(player, 50);
            runLaterTasks();

            bree.title = "Bree-land";
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
