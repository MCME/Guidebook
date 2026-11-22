package com.mcmiddleearth.guidebook.typewriter

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.KeybindComponent
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

/** The text a player sees, without its styling. */
fun plain(component: Component): String = PlainTextComponentSerializer.plainText().serialize(component)

/**
 * How wide the characters are on screen in Minecraft's 26.3 default font, each with the 1px gap after it, bold adding
 * 1px: ASCII as read off the font's bitmap, which is all the tests measure. Keys are shown by their default binding, F
 * for swapping hands.
 */
fun pixelWidth(characters: List<Pair<Char, Style>>): Int = characters.sumOf { (character, style) ->
    val width = when (character) {
        '!', '\'', ',', '.', ':', ';', 'i', '|' -> 2
        '`', 'l' -> 3
        ' ', 'I', '[', ']', 't', '"', '(', ')', '*', '{', '}' -> 4
        '<', '>', 'f', 'k' -> 5
        '@', '~' -> 7
        else -> 6
    }
    width + if (style.hasDecoration(TextDecoration.BOLD)) 1 else 0
}

/** What a player sees of a component, however it's built: each character with its colour, formats and actions. */
fun styledCharacters(component: Component, parent: Style = Style.empty()): List<Pair<Char, Style>> {
    val style = component.style().merge(parent, Style.Merge.Strategy.IF_ABSENT_ON_TARGET)
    val own = when (component) {
        is TextComponent -> component.content().map { it to style }
        is KeybindComponent if component.keybind() == "key.swapOffhand" -> listOf('F' to style)
        else -> emptyList()
    }
    return own + component.children().flatMap { styledCharacters(it, style) }
}
