package com.mcmiddleearth.guidebook.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.concurrent.CompletableFuture;
import net.strokkur.commands.CustomSuggestion;
import org.bukkit.command.CommandSender;

/**
 * TEMPORARY until ticket 08: hands the text after {@code /guidebook} to the legacy {@link GuidebookCommandExecutor}, for
 * execution and for tab completion, so subcommands not yet migrated to Brigadier keep working.
 */
final class LegacyFallthrough {

    private static final GuidebookCommandExecutor EXECUTOR = new GuidebookCommandExecutor();

    private LegacyFallthrough() {}

    /** Marks the greedy argument whose completions come from the legacy handlers. */
    @CustomSuggestion
    @interface LegacySuggestions {}

    static void execute(CommandSender sender, String args) {
        // Splits the way Bukkit did for executors: trailing spaces drop out, repeated ones leave empty arguments
        String[] split = args.isEmpty() ? new String[0] : args.split(" ");
        EXECUTOR.execute(sender, split);
    }

    @LegacySuggestions
    static CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        // Splits the way Bukkit did for tab completion: a trailing space starts a new, empty argument
        String remaining = builder.getRemaining();
        String[] split = remaining.split(" ", -1);
        SuggestionsBuilder lastArgument = builder.createOffset(builder.getStart() + remaining.lastIndexOf(' ') + 1);
        EXECUTOR.complete(ctx.getSource().getSender(), split).forEach(lastArgument::suggest);
        return lastArgument.buildFuture();
    }
}
