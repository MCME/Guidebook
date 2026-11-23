package com.mcmiddleearth.guidebook.welcome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class DisplaySwitchboardTest {

    final List<String> chatted = new ArrayList<>();
    final List<String> typed = new ArrayList<>();
    final Display chat = (player, welcome) -> chatted.add(welcome.areaName());
    final Display typewriter = (player, welcome) -> typed.add(welcome.areaName());
    final List<Level> logged = new ArrayList<>();
    final List<String> logMessages = new ArrayList<>();
    final DisplaySwitchboard switchboard = new DisplaySwitchboard("typewriter", chat, recordingLogger());

    final Player player = new WatchmanTest.TestPlayer(0).player;
    final Welcome bree = new Welcome("bree", "Bree", "", "<guide>A place.", true);

    @Test
    void theChosenDisplayShowsTheWelcome() {
        switchboard.register("typewriter", typewriter);

        switchboard.show(player, bree);

        assertEquals(List.of("bree"), typed);
        assertEquals(List.of(), chatted);
    }

    @Test
    void theChosenDisplayIsFoundIgnoringCase() {
        switchboard.register("Typewriter", typewriter);

        switchboard.show(player, bree);

        assertEquals(List.of("bree"), typed);
    }

    @Test
    void chatShowsTheWelcomeWhileTheChosenDisplayIsntRegistered() {
        switchboard.register("other", typewriter);

        switchboard.show(player, bree);

        assertEquals(List.of(), typed);
        assertEquals(List.of("bree"), chatted);
    }

    @Test
    void chatShowsTheWelcomeOnceTheChosenDisplayUnregisters() {
        switchboard.register("typewriter", typewriter);
        switchboard.unregister("typewriter");

        switchboard.show(player, bree);

        assertEquals(List.of(), typed);
        assertEquals(List.of("bree"), chatted);
    }

    @Test
    void chatShowsTheWelcomeTheChosenDisplayFailedToShow() {
        switchboard.register("typewriter", (p, welcome) -> {
            throw new IllegalStateException("Typewriter isn't ready");
        });

        switchboard.show(player, bree);

        assertEquals(List.of("bree"), chatted);
    }

    @Test
    void theChosenDisplayIsMissingUntilItRegisters() {
        assertTrue(switchboard.isChosenMissing());

        switchboard.register("typewriter", typewriter);

        assertFalse(switchboard.isChosenMissing());
    }

    @Test
    void theChosenDisplayIsMissingAgainOnceItUnregisters() {
        switchboard.register("typewriter", typewriter);
        switchboard.unregister("typewriter");

        assertTrue(switchboard.isChosenMissing());
    }

    @Test
    void theChatDisplayIsNeverMissing() {
        assertFalse(new DisplaySwitchboard("Chat", chat, recordingLogger()).isChosenMissing());
    }

    @Test
    void aMissingChosenDisplayIsLoggedOnceOverManyWelcomes() {
        switchboard.show(player, bree);
        switchboard.show(player, bree);

        assertEquals(List.of(Level.WARNING), logged);
    }

    @Test
    void aMissingChosenDisplayIsLoggedAgainAfterItCameAndWent() {
        switchboard.show(player, bree);
        switchboard.register("typewriter", typewriter);
        switchboard.unregister("typewriter");

        switchboard.show(player, bree);

        assertEquals(List.of(Level.WARNING, Level.WARNING), logged);
    }

    @Test
    void aMissingChosenDisplayLoggedForAStaffJoinIsntLoggedAgainForAWelcome() {
        switchboard.logIfChosenIsMissing();

        switchboard.show(player, bree);

        assertEquals(List.of(Level.WARNING), logged);
    }

    @Test
    void aRegisteredChosenDisplayIsntLogged() {
        switchboard.register("typewriter", typewriter);

        switchboard.logIfChosenIsMissing();

        assertEquals(List.of(), logged);
    }

    @Test
    void chatShowsTheWelcomeTheChosenDisplayCantShowNow() {
        switchboard.register("typewriter", (p, welcome) -> {
            throw new Display.Unavailable("Bree's player is in another dialogue");
        });

        switchboard.show(player, bree);

        assertEquals(List.of("bree"), chatted);
        assertEquals(List.of(Level.FINE), logged);
    }

    @Test
    void aChosenDisplayFailingIsLoggedEveryTime() {
        switchboard.register("typewriter", (p, welcome) -> {
            throw new IllegalStateException("Typewriter isn't ready");
        });

        switchboard.show(player, bree);
        switchboard.show(player, bree);

        assertEquals(List.of(Level.SEVERE, Level.SEVERE), logged);
    }

    @Test
    void chatShowsTheWelcomeWhenTheChosenDisplayDoesntMatchThisGuidebook() {
        switchboard.register("typewriter", (p, welcome) -> {
            throw builtForAnotherGuidebook();
        });

        switchboard.show(player, bree);

        assertEquals(List.of("bree"), chatted);
        assertEquals(List.of(Level.SEVERE), logged);
        assertTrue(logMessages.getFirst().endsWith(DisplaySwitchboard.MISMATCH), logMessages.getFirst());
    }

    @Test
    void theChosenDisplayPreviewsTheDescription() {
        switchboard.register("typewriter", previewing(List.of(Component.text("Page 1"), Component.text("Page 2"))));

        assertEquals(
                List.of(Component.text("Page 1"), Component.text("Page 2")),
                switchboard.previewDescription(player, bree));
    }

    @Test
    void theDescriptionPreviewsAsChatWhileTheChosenDisplayIsntRegistered() {
        switchboard.register("other", previewing(List.of(Component.text("Page 1"))));

        assertEquals(List.of(), switchboard.previewDescription(player, bree));
    }

    @Test
    void theDescriptionPreviewsAsChatWhenTheChosenDisplayFailsToPreviewIt() {
        switchboard.register("typewriter", new Display() {
            @Override
            public void show(Player p, Welcome welcome) {}

            @Override
            public List<Component> previewDescription(Player p, Welcome welcome) {
                throw new IllegalStateException("Typewriter isn't ready");
            }
        });

        assertEquals(List.of(), switchboard.previewDescription(player, bree));
        assertEquals(List.of(Level.SEVERE), logged);
    }

    @Test
    void theDescriptionPreviewsAsChatWhenTheChosenDisplayDoesntMatchThisGuidebook() {
        switchboard.register("typewriter", new Display() {
            @Override
            public void show(Player p, Welcome welcome) {}

            @Override
            public List<Component> previewDescription(Player p, Welcome welcome) {
                throw builtForAnotherGuidebook();
            }
        });

        assertEquals(List.of(), switchboard.previewDescription(player, bree));
        assertEquals(List.of(Level.SEVERE), logged);
        assertTrue(logMessages.getFirst().endsWith(DisplaySwitchboard.MISMATCH), logMessages.getFirst());
    }

    /** What a Display's jar throws when it calls a Guidebook method this Guidebook doesn't have. */
    private static NoSuchMethodError builtForAnotherGuidebook() {
        return new NoSuchMethodError("'net.kyori.adventure.text.Component AreaText.render(String)'");
    }

    /** A Display that previews every Description as {@code screens}. */
    private static Display previewing(List<Component> screens) {
        return new Display() {
            @Override
            public void show(Player player, Welcome welcome) {}

            @Override
            public List<Component> previewDescription(Player player, Welcome welcome) {
                return screens;
            }
        };
    }

    private Logger recordingLogger() {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(record.getLevel());
                logMessages.add(record.getMessage());
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        });
        return logger;
    }
}
