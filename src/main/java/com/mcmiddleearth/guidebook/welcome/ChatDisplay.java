package com.mcmiddleearth.guidebook.welcome;

import com.mcmiddleearth.guidebook.text.AreaText;
import com.mcmiddleearth.guidebook.text.GuidebookMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Guidebook's own Display: the Title and Subtitle on screen, then the Description in chat once the Title has gone. */
public final class ChatDisplay implements Display {

    private static final Title.Times TITLE_TIMES =
            Title.Times.times(Ticks.duration(15), Ticks.duration(25), Ticks.duration(10));

    // Once the Title has faded out
    private static final long DESCRIPTION_DELAY_AFTER_TITLE = TITLE_TIMES
                    .fadeIn()
                    .plus(TITLE_TIMES.stay())
                    .plus(TITLE_TIMES.fadeOut())
                    .toMillis()
            / Ticks.SINGLE_TICK_DURATION_MS;

    private final Plugin plugin;

    public ChatDisplay(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void show(Player player, Welcome welcome) {
        long descriptionDelay = 0;
        if (welcome.showTitle()) {
            player.showTitle(
                    Title.title(AreaText.render(welcome.title()), AreaText.render(welcome.subtitle()), TITLE_TIMES));
            descriptionDelay = DESCRIPTION_DELAY_AFTER_TITLE;
        }
        plugin.getServer()
                .getScheduler()
                .runTaskLater(plugin, () -> sendDescription(player, welcome), descriptionDelay);
    }

    private static void sendDescription(Player player, Welcome welcome) {
        if (welcome.description().isEmpty()) {
            GuidebookMessages.send(
                    player,
                    GuidebookMessages.info(
                            "Welcome to ",
                            Component.text()
                                    .color(GuidebookMessages.STRESSED)
                                    .append(AreaText.render(welcome.title()))
                                    .append(Component.text(" (" + welcome.areaName() + ")")),
                            ". Unfortunately there is no further description for this area."));
        } else {
            GuidebookMessages.send(player, AreaText.render(welcome.description()));
        }
    }
}
