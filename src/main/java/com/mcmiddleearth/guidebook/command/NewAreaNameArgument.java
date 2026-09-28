package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.data.PluginData;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import java.util.Optional;
import net.kyori.adventure.text.Component;

/**
 * A name for a new or renamed Area. A name that is invalid, or already used by an Area (ignoring case), fails when
 * the command is parsed.
 */
final class NewAreaNameArgument implements CustomArgumentType.Converted<String, String> {

    private static final DynamicCommandExceptionType UNUSABLE_NAME = new DynamicCommandExceptionType(
            problem -> MessageComponentSerializer.message().serialize(Component.text((String) problem)));

    @Override
    public String convert(String name) throws CommandSyntaxException {
        Optional<String> problem = PluginData.newAreaNameProblem(name);
        if (problem.isPresent()) {
            throw UNUSABLE_NAME.create(problem.get());
        }
        return name;
    }

    @Override
    public ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }
}
