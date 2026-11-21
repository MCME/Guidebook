package com.mcmiddleearth.guidebook.typewriter

// SEAM: Page layout. How a Welcome's Description is cut into Pages and drawn as the box. Pure: no Typewriter or
// Bukkit, so its rules are tested without a server. The rules are the spec's "Page layout" module.

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import java.time.Duration

/** How the box looks and types, from the settings on the Guidebook Welcome entry. */
data class LayoutSettings(
    val typingSpeed: Duration = Duration.ofMillis(20),
    val pageTypingCap: Duration = Duration.ofSeconds(4),
    val headerColor: TextColor = TextColor.color(0xE3C77D),
)

/** One screenful of a Welcome: its lines of text, each already broken to fit one chat line. */
class Page(val lines: List<Component>)

/** The Pages of a Welcome whose [description] is already rendered. */
fun layOutPages(description: Component): List<Page> {
    val text = StyledText.of(description)
    val lines = mutableListOf<Component>()
    var start = 0
    do {
        val line = breakLine(text, start, LINE_WIDTH_PX)
        lines += text.slice(start, line.end)
        start = line.next
    } while (line.endsOnNewline || line.next < text.length)
    return listOf(Page(lines))
}

// Default chat is 320px wide, and the client never wraps a line again if none is measured wider than that
private const val LINE_WIDTH_PX = 320

/**
 * The widest a character can be in Minecraft's default font, including the 1px gap after it. Measuring narrow
 * letters (i, l, t...) as wide means a line can never come out wider than measured.
 */
private fun widestWidth(character: Char, bold: Boolean): Int =
    (if (character == '@' || character == '~') 7 else 6) + (if (bold) 1 else 0)

/** A line of text ending before [end], with the next line starting at [next]. */
private class LineBreak(val end: Int, val next: Int, val endsOnNewline: Boolean)

/**
 * Where the line starting at [start] ends: at the last space that fits in [width], or mid-word for a word wider than
 * a line. An explicit newline also ends it. The space or newline a line breaks at belongs to neither line.
 */
private fun breakLine(text: StyledText, start: Int, width: Int): LineBreak {
    var used = 0
    var lastSpace = -1
    for (i in start until text.length) {
        val character = text[i]
        if (character == '\n') return LineBreak(i, i + 1, endsOnNewline = true)
        used += widestWidth(character, text.isBold(i))
        if (used > width) {
            return when {
                character == ' ' -> LineBreak(i, i + 1, endsOnNewline = false)
                lastSpace > start -> LineBreak(lastSpace, lastSpace + 1, endsOnNewline = false)
                else -> maxOf(i, start + 1).let { LineBreak(it, it, endsOnNewline = false) }
            }
        }
        if (character == ' ') lastSpace = i
    }
    return LineBreak(text.length, text.length, endsOnNewline = false)
}

/** Text as a player sees it, one style per character, so it can be cut anywhere without losing hovers or clicks. */
private class StyledText(private val runs: List<Run>) {
    class Run(val text: String, val style: Style)

    private val plain = runs.joinToString("") { it.text }
    private val bold = runs.flatMap { run -> List(run.text.length) { run.style.hasDecoration(TextDecoration.BOLD) } }

    val length = plain.length

    operator fun get(index: Int): Char = plain[index]

    fun isBold(index: Int): Boolean = bold[index]

    fun slice(from: Int, to: Int): Component {
        val builder = Component.text()
        var offset = 0
        for (run in runs) {
            val start = (from - offset).coerceIn(0, run.text.length)
            val end = (to - offset).coerceIn(0, run.text.length)
            if (start < end) builder.append(Component.text(run.text.substring(start, end), run.style))
            offset += run.text.length
        }
        return builder.build()
    }

    companion object {
        fun of(component: Component): StyledText = StyledText(mutableListOf<Run>().also { flatten(component, Style.empty(), it) })

        private fun flatten(component: Component, parent: Style, out: MutableList<Run>) {
            val style = component.style().merge(parent, Style.Merge.Strategy.IF_ABSENT_ON_TARGET)
            if (component is TextComponent && component.content().isNotEmpty()) out += Run(component.content(), style)
            component.children().forEach { flatten(it, style, out) }
        }
    }
}

/**
 * The box as the player sees it [elapsed] into typing [page], with [key] being the key that turns or closes it.
 */
fun drawBox(header: String, page: Page, key: Component, settings: LayoutSettings, elapsed: Duration): Component =
    Component.join(net.kyori.adventure.text.JoinConfiguration.newlines(), page.lines)
