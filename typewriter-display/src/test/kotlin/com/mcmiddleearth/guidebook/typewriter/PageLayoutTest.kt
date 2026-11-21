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

    private fun plainOf(description: String): String = AreaText.plainText(description)
}
