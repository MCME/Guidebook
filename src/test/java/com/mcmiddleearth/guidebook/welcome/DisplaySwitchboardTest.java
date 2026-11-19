package com.mcmiddleearth.guidebook.welcome;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class DisplaySwitchboardTest {

    final List<String> chatted = new ArrayList<>();
    final List<String> typed = new ArrayList<>();
    final Display chat = (player, welcome) -> chatted.add(welcome.areaName());
    final Display typewriter = (player, welcome) -> typed.add(welcome.areaName());
    final DisplaySwitchboard switchboard = new DisplaySwitchboard("typewriter", chat, Logger.getAnonymousLogger());

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
}
