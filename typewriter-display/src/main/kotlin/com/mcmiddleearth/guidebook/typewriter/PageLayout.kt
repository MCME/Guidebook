package com.mcmiddleearth.guidebook.typewriter

import com.mcmiddleearth.guidebook.util.AreaText
import com.mcmiddleearth.guidebook.welcome.Welcome
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.KeybindComponent
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import java.time.Duration

/** How the box looks and types, from the settings on the Guidebook Welcome entry. Its defaults are the entry's. */
data class LayoutSettings(
    val typingSpeed: Duration = Duration.ofMillis(20),
    val pageTypingCap: Duration = Duration.ofSeconds(4),
    val headerColor: TextColor = TextColor.color(0xE3C77D),
)

/**
 * One screenful of a Welcome: its [lines] of text, each already broken to fit one chat line, and where it falls among
 * the Welcome's Pages.
 */
class Page(val lines: List<Component>, val number: Int, val total: Int) {

    /** Whether the Description carries on onto the next Page. */
    val carriesOn: Boolean get() = number < total

    /** What the key does on this Page, as the footer says it. */
    val footer: String
        get() = when {
            total == 1 -> "close"
            carriesOn -> "continue ($number/$total)"
            else -> "close ($number/$total)"
        }

    private val text = lines.map(StyledText::of)
    private val characters = text.sumOf { line -> line.count { it != ' ' } }

    /** How long typing this Page takes: spaces take no time, and no Page takes longer than the cap. */
    fun typingDuration(settings: LayoutSettings): Duration =
        minOf(settings.typingSpeed.multipliedBy(characters.toLong()), settings.pageTypingCap)

    /** The lines as far as they're typed [elapsed] into this Page: whole characters, one at a time. */
    internal fun revealedLines(elapsed: Duration, settings: LayoutSettings): List<Component> {
        val duration = typingDuration(settings)
        var left = if (elapsed >= duration) characters else (characters * elapsed.toNanos() / duration.toNanos()).toInt()
        return text.map { line ->
            var end = 0
            while (end < line.length && (line[end] == ' ' || left > 0)) {
                if (line[end] != ' ') left--
                end++
            }
            line.slice(0, end)
        }
    }
}

/** The Pages of [welcome]'s Description. The box already says the Guidebook is speaking, so <guide> shows no "Guide:". */
fun descriptionPages(welcome: Welcome): List<Page> = layOutPages(AreaText.renderWithoutGuideLabel(welcome.description()))

/**
 * The Pages of a Welcome whose [description] is already rendered. Page layout, here and in [drawBox], uses no
 * Typewriter or Bukkit class, so tests can lay out real Descriptions without a server.
 */
fun layOutPages(description: Component): List<Page> {
    val text = StyledText.of(description)
    val pages = mutableListOf<List<Component>>()
    var lines = mutableListOf<Component>()
    var start = 0
    while (true) {
        var line = breakLine(text, start, LINE_WIDTH_PX)
        val more = line.endsOnNewline || line.next < text.length
        if (lines.size == LINES_PER_PAGE - 1 && more) {
            // The last line of a Page that carries on leaves room for the ellipsis it ends with
            line = breakLine(text, start, LINE_WIDTH_PX - ELLIPSIS_WIDTH_PX)
        }
        lines += text.slice(start, line.end)
        start = line.next
        if (!more) break
        if (lines.size == LINES_PER_PAGE) {
            pages += lines
            lines = mutableListOf()
        }
    }
    pages += lines
    return pages.mapIndexed { index, pageLines -> Page(pageLines, index + 1, pages.size) }
}

/**
 * The box as the player sees it [elapsed] into typing [page]: the [header] (the Title, as plain text) in the top
 * border, the Page's text, and in the bottom border what [key] does. An Area with no Title gets "Guide" there, so the
 * box still says who is speaking.
 */
fun drawBox(header: String, page: Page, key: Component, settings: LayoutSettings, elapsed: Duration): Component {
    val title = Component.text()
        .append(Component.text(" "))
        .append(Component.text(header.ifBlank { NO_TITLE_HEADER }, settings.headerColor, TextDecoration.BOLD))
        .append(Component.text(" "))
        .build()
    val prompt = Component.text()
        .color(NamedTextColor.GRAY)
        .append(Component.text(" Press "))
        .append(key.colorIfAbsent(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
        .append(Component.text(" to ${page.footer} "))
        .build()
    // Both borders come out the same width, however long the Title or the prompt
    val width = maxOf(BORDER_WIDTH_PX, realWidth(title) + 2 * MIN_RULE_PX, realWidth(prompt) + 2 * MIN_RULE_PX)

    val text = page.revealedLines(elapsed, settings).toMutableList()
    if (page.carriesOn && elapsed >= page.typingDuration(settings)) {
        text[text.lastIndex] = text.last().append(Component.text(ELLIPSIS))
    }
    if (text.size < LINES_PER_PAGE) text.add(0, Component.empty())
    // Short text leaves blank lines below it, so the box is always the same height
    while (text.size < LINES_PER_PAGE) text += Component.empty()

    return Component.join(JoinConfiguration.newlines(), listOf(framed(title, width)) + text + framed(prompt, width))
}

private const val NO_TITLE_HEADER = "Guide"

// Default unfocused chat shows 10 lines: the box's header, its footer, and this many lines of text between them
private const val LINES_PER_PAGE = 8

// Default chat is 320px wide, and the client never wraps a line again if none is measured wider than that
private const val LINE_WIDTH_PX = 320

private const val ELLIPSIS = "..."
private const val ELLIPSIS_WIDTH_PX = 3 * 6

// The header and footer span about the chat's width, a few pixels short of it in case a character is measured narrow
private const val BORDER_WIDTH_PX = LINE_WIDTH_PX - 8

// The shortest rule either side of the header or footer, so a long Title is still framed
private const val MIN_RULE_PX = 16

/**
 * The widest a character can be in Minecraft's default font, including the 1px gap after it. Measuring narrow
 * letters (i, l, t...) as wide means a line can never come out wider than measured.
 */
private fun widestWidth(character: Char, bold: Boolean): Int =
    (if (character == '@' || character == '~') 7 else 6) + (if (bold) 1 else 0)

/** How wide a character is in Minecraft's default font, including the 1px gap after it. Bold adds 1px. */
private fun realWidth(character: Char, bold: Boolean): Int = (if (bold) 1 else 0) + when (character) {
    '!', '\'', ',', '.', ':', ';', 'i', '|' -> 2
    '`', 'l' -> 3
    ' ', 'I', '[', ']', 't' -> 4
    '"', '(', ')', '*', '<', '>', 'f', 'k', '{', '}' -> 5
    '@', '~' -> 7
    else -> 6
}

// The client shows a key by its name on the player's keyboard; these are Minecraft's default bindings
private val KEY_NAMES = mapOf(
    "key.swapOffhand" to "F",
    "key.jump" to "Space",
    "key.sneak" to "Left Shift",
    "key.attack" to "Left Button",
    "key.use" to "Right Button",
)

private fun realWidth(component: Component, parent: Style = Style.empty()): Int {
    val style = component.style().merge(parent, Style.Merge.Strategy.IF_ABSENT_ON_TARGET)
    val bold = style.hasDecoration(TextDecoration.BOLD)
    val own = when (component) {
        is TextComponent -> component.content()
        is KeybindComponent -> KEY_NAMES[component.keybind()] ?: "?"
        else -> ""
    }
    return own.sumOf { realWidth(it, bold) } + component.children().sumOf { realWidth(it, style) }
}

/** [content] between two rules, filling [width] in all. */
private fun framed(content: Component, width: Int): Component {
    val fill = width - realWidth(content)
    return Component.text()
        .append(rule(fill / 2))
        .append(content)
        .append(rule(fill - fill / 2))
        .build()
}

/**
 * A horizontal rule [width] pixels long: struck-through spaces, 4px each or 5px in bold. Mixing the two makes any
 * width from 12px exact.
 */
private fun rule(width: Int): Component {
    val bold = width % 4
    val plain = (width - 5 * bold) / 4
    val style = Style.style(NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH)
    return Component.text()
        .append(Component.text(" ".repeat(plain), style))
        .append(Component.text(" ".repeat(bold), style.decorate(TextDecoration.BOLD)))
        .build()
}

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
internal class StyledText(private val runs: List<Run>) {
    class Run(val text: String, val style: Style)

    private val plain = runs.joinToString("") { it.text }
    private val bold = runs.flatMap { run -> List(run.text.length) { run.style.hasDecoration(TextDecoration.BOLD) } }

    val length = plain.length

    operator fun get(index: Int): Char = plain[index]

    fun count(predicate: (Char) -> Boolean): Int = plain.count(predicate)

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
        fun of(component: Component): StyledText =
            StyledText(mutableListOf<Run>().also { flatten(component, Style.empty(), it) })

        private fun flatten(component: Component, parent: Style, out: MutableList<Run>) {
            val style = component.style().merge(parent, Style.Merge.Strategy.IF_ABSENT_ON_TARGET)
            if (component is TextComponent && component.content().isNotEmpty()) out += Run(component.content(), style)
            component.children().forEach { flatten(it, style, out) }
        }
    }
}
