package com.mcmiddleearth.guidebook.typewriter

import com.mcmiddleearth.guidebook.text.AreaText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration

class PageTurnerTest {

    private val settings = LayoutSettings()
    private val start = Duration.ofSeconds(2)

    private fun turner(description: String) = PageTurner(layOutPages(AreaText.render(description)), settings)

    @Test
    fun `an advance while it types shows the rest of the Page, and the next one closes the box`() {
        val turner = turner(BREE_GATE)
        turner.tick(start)

        turner.advance()

        assertTrue(turner.isPageTyped)
        assertTrue(turner.mayClose)
    }

    @Test
    fun `an advance on a typed Page turns to the next one`() {
        val turner = turner(HELMS_DEEP)
        turner.tick(start)

        turner.advance()
        turner.advance()

        assertEquals(1, turner.page)
        assertFalse(turner.isPageTyped)
    }

    @Test
    fun `advances before the Description starts neither skip its typing nor let the box close`() {
        val turner = turner(BREE_GATE)

        turner.advance()
        turner.advance()
        turner.tick(start)

        assertFalse(turner.isPageTyped)
        assertFalse(turner.mayClose)
    }

    @Test
    fun `the box may close once the last Page has typed out`() {
        val turner = turner(BREE_GATE)
        turner.tick(start)
        turner.tick(start + settings.pageTypingCap)

        assertTrue(turner.mayClose)
    }
}
