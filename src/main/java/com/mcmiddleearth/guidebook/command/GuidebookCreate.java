package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.strokkur.commands.CustomSuggestion;
import org.bukkit.Bukkit;
import org.bukkit.World;

/** The new-name suggestions for {@code create} and {@code rename}. */
final class GuidebookCreate {

    private GuidebookCreate() {}

    /**
     * Marks {@code create}'s and {@code rename}'s new-name argument, which suggests the start of a
     * {@code <world>-<project>-<place>} name: each world, and the projects already used in it. See
     * {@link AreaNameSuggestions}.
     */
    @CustomSuggestion
    @interface NameSuggestions {}

    // Hovering a suggestion shows the convention it follows
    private static final Message CONVENTION =
            MessageComponentSerializer.message().serialize(Component.text("<world>-<project>-<place>"));

    @NameSuggestions
    static CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        Collection<InfoArea> areas = PluginData.getInfoAreas().values();
        // Loaded worlds, and the worlds of Areas whose world isn't loaded
        TreeSet<String> worlds = new TreeSet<>();
        Bukkit.getWorlds().stream().map(World::getName).forEach(worlds::add);
        areas.stream().map(InfoArea::getWorldName).forEach(worlds::add);
        List<String> names = areas.stream().map(InfoArea::getName).toList();
        AreaNameSuggestions.suggest(builder.getRemaining(), worlds, names)
                .forEach(suggestion -> builder.suggest(suggestion, CONVENTION));
        return builder.buildFuture();
    }
}
