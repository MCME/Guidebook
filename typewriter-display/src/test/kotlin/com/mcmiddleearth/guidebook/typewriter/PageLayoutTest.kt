package com.mcmiddleearth.guidebook.typewriter

import com.mcmiddleearth.guidebook.util.AreaText
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import java.time.Duration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PageLayoutTest {

    private fun linesOf(description: String): List<List<String>> =
        layOutPages(AreaText.render(description)).map { page -> page.lines.map(::plain) }

    @Test
    fun `a one-line Description is one Page of one line`() {
        val lines = linesOf("<guide>The West-gate of <gold>Bree</gold>.")

        assertEquals(listOf(listOf("Guide: The West-gate of Bree.")), lines)
    }

    @Test
    fun `a line breaks at the last space that fits in 320px, counting every character as 6px`() {
        // 53 characters at 6px is 318px; one more would be 324px
        val lines = linesOf("${"a".repeat(50)} bb ccc").single()

        assertEquals(listOf("${"a".repeat(50)} bb", "ccc"), lines)
    }

    @Test
    fun `a word wider than a line is broken mid-word`() {
        val lines = linesOf("ab ${"c".repeat(60)}").single()

        assertEquals(listOf("ab", "c".repeat(53), "c".repeat(7)), lines)
    }

    @Test
    fun `an explicit newline breaks the line`() {
        val lines = linesOf("<guide>First.\n<bullet>Second.").single()

        assertEquals(listOf("Guide: First.", "▸ Second."), lines)
    }

    @Test
    fun `bold characters count 7px`() {
        // 45 bold characters are 315px; the space after them would make 322px
        val lines = linesOf("<bold>${"a".repeat(42)} bb c").single()

        assertEquals(listOf("${"a".repeat(42)} bb", "c"), lines)
    }

    @Test
    fun `@ and ~ count 7px, and 8px in bold`() {
        // 44 at 7px is 308px, then 1 at 6px and a space fill the 320px
        assertEquals(listOf("${"@".repeat(44)}a", "b"), linesOf("${"@".repeat(44)}a b").single())
        // 39 bold at 8px is 312px, then 1 bold at 7px is 319px
        assertEquals(listOf("${"~".repeat(39)}a", "b"), linesOf("<bold>${"~".repeat(39)}a b").single())
    }

    @Test
    fun `hover text doesn't count towards a line`() {
        // Measured on the raw text, the long meaning would push "end" onto a second line
        val lines = linesOf("<guide>A <term:'${"meaning ".repeat(10)}'>word</term> at the end.").single()

        assertEquals(listOf("Guide: A word at the end."), lines)
    }

    @Test
    fun `hovers and clicks stay on the text they belong to`() {
        val description = AreaText.render(HELMS_DEEP)
        val pages = layOutPages(description)

        val laidOut = pages.flatMap { page -> page.lines.flatMap { styledCharacters(it) } }
        val original = styledCharacters(description).filterNot { it.first == ' ' || it.first == '\n' }

        assertEquals(original, laidOut.filterNot { it.first == ' ' })
    }

    @Test
    fun `exactly 8 lines fit on one Page`() {
        val pages = layOutPages(AreaText.render(numberedLines(8)))

        assertEquals(1, pages.size)
        assertEquals((1..8).map { "line $it" }, pages.single().lines.map(::plain))
        assertEquals(false, pages.single().carriesOn)
    }

    @Test
    fun `a 9th line continues on a second Page`() {
        val pages = layOutPages(AreaText.render(numberedLines(9)))

        assertEquals(listOf((1..8).map { "line $it" }, listOf("line 9")), pages.map { it.lines.map(::plain) })
        assertEquals(listOf(true, false), pages.map { it.carriesOn })
    }

    @Test
    fun `the 8th line of a Page that carries on is broken about 3 characters shorter`() {
        // 8 full lines of words, then more: the 8th line leaves room for "..."
        val pages = layOutPages(AreaText.render(List(9 * 9) { "words" }.joinToString(" ")))

        val firstPage = pages.first().lines.map(::plain)
        // 9 words and their spaces are 53 characters, 318px. The 8th line has 3 characters less: room for 8
        assertEquals(List(7) { List(9) { "words" }.joinToString(" ") }, firstPage.take(7))
        assertEquals(List(8) { "words" }.joinToString(" "), firstPage[7])
        assertEquals(true, pages.first().carriesOn)
    }

    @Test
    fun `the last line of the last Page is full width`() {
        // 8 lines that only just fit: none of them carries on, so the 8th isn't shortened
        val pages = layOutPages(AreaText.render(List(8) { "a".repeat(53) }.joinToString("\n")))

        assertEquals(1, pages.size)
        assertEquals("a".repeat(53), plain(pages.single().lines[7]))
    }

    @Test
    fun `a long Description runs over several Pages without losing any text`() {
        val pages = layOutPages(AreaText.render(HELMS_DEEP))

        assertEquals(3, pages.size)
        val words = pages.flatMap { page -> page.lines.flatMap { plain(it).split(' ') } }
        assertEquals(AreaText.plainText(HELMS_DEEP).split(' ', '\n'), words)
    }

    @Test
    fun `real MCME Descriptions fit on one Page`() {
        for (description in listOf(MINAS_TIRITH, MINAS_MORGUL, CAIR_ANDROS, BREE_GATE)) {
            assertEquals(1, layOutPages(AreaText.render(description)).size, description)
        }
    }

    @Test
    fun `the box is always 10 lines, with short text at the top under one blank line`() {
        val box = boxLines(layOutPages(AreaText.render(numberedLines(2))).single())

        assertEquals(10, box.size)
        assertEquals(listOf("", "line 1", "line 2", "", "", "", "", ""), box.subList(1, 9))
    }

    @Test
    fun `a full Page has no blank line under the header`() {
        val box = boxLines(layOutPages(AreaText.render(numberedLines(8))).single())

        assertEquals(10, box.size)
        assertEquals((1..8).map { "line $it" }, box.subList(1, 9))
    }

    @Test
    fun `the header names the Area and the footer says how to close a one-Page Welcome`() {
        val box = boxLines(layOutPages(AreaText.render(BREE_GATE)).single())

        assertEquals("Bree West-gate", box.first().trim())
        assertEquals("Press F to close", box.last().trim())
    }

    @Test
    fun `the footer counts the Pages, and a Page that carries on ends with an ellipsis`() {
        val pages = layOutPages(AreaText.render(HELMS_DEEP))

        val boxes = pages.map(::boxLines)

        assertEquals(
            listOf("Press F to continue (1/3)", "Press F to continue (2/3)", "Press F to close (3/3)"),
            boxes.map { it.last().trim() },
        )
        assertEquals(listOf(true, true, false), boxes.map { it[8].endsWith("...") })
    }

    @Test
    fun `the header is the Area name in bold, in the header colour`() {
        val settings = LayoutSettings(headerColor = TextColor.color(0x123456))
        val header = drawBox("Bree", layOutPages(AreaText.render(BREE_GATE)).single(), KEY, settings, TYPED)

        val name = styledCharacters(header).filter { it.first in "Bree" }.take(4)
        assertEquals(List(4) { TextColor.color(0x123456) }, name.map { it.second.color() })
        assertEquals(List(4) { true }, name.map { it.second.hasDecoration(TextDecoration.BOLD) })
    }

    @Test
    fun `the key is highlighted in the footer`() {
        val box = drawBox("Bree", layOutPages(AreaText.render(BREE_GATE)).single(), KEY, LayoutSettings(), TYPED)

        val key = styledCharacters(box).last { it.first == 'F' }
        assertEquals(NamedTextColor.YELLOW, key.second.color())
        assertEquals(true, key.second.hasDecoration(TextDecoration.BOLD))
    }

    @Test
    fun `the header and footer borders are the same width`() {
        val pages = layOutPages(AreaText.render(HELMS_DEEP))

        for ((header, page, key) in listOf(
            Triple("Bree", pages[0], KEY),
            Triple("Helm's Deep", pages[2], KEY),
            Triple("Minas Tirith", pages[1], Component.keybind("key.swapOffhand")),
            // As long as a Title may be
            Triple("W".repeat(32), pages[0], KEY),
            // Longer, from an Area saved before the limit
            Triple("The Ford of Bruinen, below the Last Homely House", pages[0], KEY),
        )) {
            val (top, bottom) = drawBox(header, page, key, LayoutSettings(), TYPED).let { box ->
                val characters = styledCharacters(box)
                characters.takeWhile { it.first != '\n' } to characters.takeLastWhile { it.first != '\n' }
            }

            assertEquals(pixelWidth(top), pixelWidth(bottom), header)
        }
    }

    @Test
    fun `typing takes the typing speed for each character but spaces`() {
        val page = layOutPages(AreaText.render("ab cd\nef")).single()

        assertEquals(Duration.ofMillis(60), page.typingDuration(LayoutSettings(typingSpeed = Duration.ofMillis(10))))
    }

    @Test
    fun `typing a long Page takes no longer than the cap`() {
        val page = layOutPages(AreaText.render(HELMS_DEEP)).first()

        assertEquals(Duration.ofSeconds(4), page.typingDuration(LayoutSettings()))
    }

    @Test
    fun `part-way through typing, the box shows the characters typed so far`() {
        val settings = LayoutSettings(typingSpeed = Duration.ofMillis(10))
        val page = layOutPages(AreaText.render("ab cd\nef gh")).single()

        val typed = { elapsed: Long ->
            plain(drawBox("Bree", page, KEY, settings, Duration.ofMillis(elapsed))).split("\n").subList(2, 4)
        }

        assertEquals(listOf("", ""), typed(0))
        assertEquals(listOf("ab c", ""), typed(35))
        assertEquals(listOf("ab cd", "e"), typed(50))
        assertEquals(listOf("ab cd", "ef gh"), typed(80))
    }

    @Test
    fun `the ellipsis appears once the Page is typed`() {
        val page = layOutPages(AreaText.render(HELMS_DEEP)).first()

        val lastLine = { elapsed: Duration -> plain(drawBox("Helm's Deep", page, KEY, LayoutSettings(), elapsed)).split("\n")[8] }

        assertEquals(false, lastLine(Duration.ofMillis(3990)).endsWith("..."))
        assertEquals(true, lastLine(Duration.ofSeconds(4)).endsWith("..."))
    }

    private fun boxLines(page: Page): List<String> =
        plain(drawBox("Bree West-gate", page, KEY, LayoutSettings(), TYPED)).split("\n")

    private fun numberedLines(count: Int) = (1..count).joinToString("\n") { "line $it" }

    private companion object {
        val KEY: Component = Component.text("F")

        // Long enough for any Page to be typed in full
        val TYPED: Duration = Duration.ofMinutes(1)
    }
}
