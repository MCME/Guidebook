package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.data.AreaRegistry;
import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * An existing Area, typed as its name. The name resolves through the {@link AreaRegistry} (ignoring case, exact case
 * wins), and an unknown name fails when the command is parsed.
 */
final class AreaArgument implements CustomArgumentType.Converted<InfoArea, String> {

    private static final DynamicCommandExceptionType UNKNOWN_AREA = new DynamicCommandExceptionType(name ->
            MessageComponentSerializer.message().serialize(Component.text("No Guidebook area called '" + name + "'")));

    @Override
    public InfoArea convert(String name) throws CommandSyntaxException {
        InfoArea area = PluginData.getInfoArea(name);
        if (area == null) {
            throw UNKNOWN_AREA.create(name);
        }
        return area;
    }

    @Override
    public ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> ctx, SuggestionsBuilder builder) {
        return suggestAreaNames(builder);
    }

    /** Suggests the Area names matching the typed text, each with its Title, Shape and whether it's disabled. */
    static CompletableFuture<Suggestions> suggestAreaNames(SuggestionsBuilder builder) {
        for (AreaRegistry.Suggestion suggestion : PluginData.suggestAreas(builder.getRemaining())) {
            builder.suggest(
                    suggestion.name(), MessageComponentSerializer.message().serialize(tooltip(suggestion)));
        }
        return builder.buildFuture();
    }

    // e.g. "Minas Tirith ⬟ prism ✖ disabled"
    private static Component tooltip(AreaRegistry.Suggestion suggestion) {
        Component tooltip = Component.text(suggestion.title(), NamedTextColor.YELLOW)
                .append(Component.text(
                        " " + suggestion.shape().symbol() + " "
                                + suggestion.shape().displayName(),
                        NamedTextColor.AQUA));
        if (suggestion.disabled()) {
            tooltip = tooltip.append(Component.text(" ✖ disabled", NamedTextColor.RED));
        }
        return tooltip;
    }
}
