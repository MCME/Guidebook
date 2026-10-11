package com.mcmiddleearth.guidebook.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.strokkur.commands.CustomSuggestion;
import org.bukkit.entity.Entity;

/** {@code create <name>}'s name suggestions. The command itself opens the Placement dialog. */
final class GuidebookCreate {

    private GuidebookCreate() {}

    /**
     * Marks {@code create}'s name argument, which suggests the player's world followed by a hyphen, the start of the
     * {@code <world>-<project>-<place>} convention.
     */
    @CustomSuggestion
    @interface NameSuggestions {}

    @NameSuggestions
    static CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        Entity executor = ctx.getSource().getExecutor();
        if (executor != null) {
            String prefix = executor.getWorld().getName() + "-";
            if (prefix.toLowerCase(Locale.ROOT).startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(prefix);
            }
        }
        return builder.buildFuture();
    }
}
