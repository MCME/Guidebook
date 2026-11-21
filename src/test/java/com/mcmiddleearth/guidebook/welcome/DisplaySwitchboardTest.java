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
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class DisplaySwitchboardTest {

    final List<String> chatted = new ArrayList<>();
    final List<String> typed = new ArrayList<>();
    final Display chat = (player, welcome) -> chatted.add(welcome.areaName());
    final Display typewriter = (player, welcome) -> typed.add(welcome.areaName());
    final List<Level> logged = new ArrayList<>();
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
    void aChosenDisplayFailingIsLoggedEveryTime() {
        switchboard.register("typewriter", (p, welcome) -> {
            throw new IllegalStateException("Typewriter isn't ready");
        });

        switchboard.show(player, bree);
        switchboard.show(player, bree);

        assertEquals(List.of(Level.SEVERE, Level.SEVERE), logged);
    }

    private Logger recordingLogger() {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(record.getLevel());
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        });
        return logger;
    }
}
