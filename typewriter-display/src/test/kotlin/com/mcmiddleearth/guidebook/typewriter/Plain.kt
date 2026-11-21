package com.mcmiddleearth.guidebook.typewriter

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

/** The text a player sees, without its styling. */
fun plain(component: Component): String = PlainTextComponentSerializer.plainText().serialize(component)

/** What a player sees of a component, however it's built: each character with its colour, formats and actions. */
fun styledCharacters(component: Component, parent: Style = Style.empty()): List<Pair<Char, Style>> {
    val style = component.style().merge(parent, Style.Merge.Strategy.IF_ABSENT_ON_TARGET)
    val own = if (component is TextComponent) component.content().map { it to style } else emptyList()
    return own + component.children().flatMap { styledCharacters(it, style) }
}
