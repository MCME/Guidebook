package com.mcmiddleearth.guidebook.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/** What a player sees of a component, however it's built: each character with its colour, formats and actions. */
public final class StyledCharacters {

    private StyledCharacters() {}

    public static List<String> of(Component component) {
        List<String> characters = new ArrayList<>();
        add(component, Style.empty(), characters);
        return characters;
    }

    private static void add(Component component, Style parent, List<String> characters) {
        Style style = component.style().merge(parent, Style.Merge.Strategy.IF_ABSENT_ON_TARGET);
        if (component instanceof TextComponent text) {
            // Chat shows uncoloured text in white, and a format not set is off
            TextColor colour = style.color() == null ? NamedTextColor.WHITE : style.color();
            String formats = Arrays.stream(TextDecoration.values())
                    .filter(decoration -> style.decoration(decoration) == TextDecoration.State.TRUE)
                    .toList()
                    .toString();
            String hover = style.hoverEvent() == null
                    ? null
                    : PlainTextComponentSerializer.plainText()
                            .serialize((Component) style.hoverEvent().value());
            for (char character : text.content().toCharArray()) {
                characters.add(character + " " + colour + " " + formats + " " + style.clickEvent() + " " + hover);
            }
        }
        for (Component child : component.children()) {
            add(child, style, characters);
        }
    }
}
