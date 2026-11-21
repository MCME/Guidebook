package com.mcmiddleearth.guidebook.typewriter

import com.mcmiddleearth.guidebook.util.AreaText
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

    private fun numberedLines(count: Int) = (1..count).joinToString("\n") { "line $it" }
}
