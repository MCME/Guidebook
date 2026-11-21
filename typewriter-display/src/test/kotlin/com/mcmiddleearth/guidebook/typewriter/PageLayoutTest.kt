package com.mcmiddleearth.guidebook.typewriter

import com.mcmiddleearth.guidebook.util.AreaText
import net.kyori.adventure.text.Component
import java.time.Duration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PageLayoutTest {

    @Test
    fun `a short Description fits on one Page`() {
        val pages = layOutPages(AreaText.render("<guide>The gate of Bree."), "Bree", LayoutSettings())

        assertEquals(1, pages.size)
        assertEquals("Guide: The gate of Bree.", plain(pages.single().text))
    }

    @Test
    fun `the box names the Area above the Description and says how to close it`() {
        val settings = LayoutSettings()
        val page = layOutPages(AreaText.render("<guide>The gate of Bree."), "Bree", settings).single()

        val box = drawBox("Bree", page, Component.text("F"), settings, Duration.ZERO)

        assertEquals("Bree\nGuide: The gate of Bree.\nPress F to close", plain(box))
    }
}
