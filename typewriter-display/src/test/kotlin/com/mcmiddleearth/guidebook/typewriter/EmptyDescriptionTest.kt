package com.mcmiddleearth.guidebook.typewriter

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EmptyDescriptionTest {

    @Test
    fun `no text at all is an empty Description`() {
        assertTrue(isEmptyDescription(""))
    }

    @Test
    fun `only spaces and newlines is an empty Description`() {
        assertTrue(isEmptyDescription("  \n "))
    }

    @Test
    fun `tags with no visible text are an empty Description`() {
        assertTrue(isEmptyDescription("<gold><bold> </bold></gold><newline>"))
    }

    @Test
    fun `a hover's tooltip text alone is an empty Description`() {
        assertTrue(isEmptyDescription("<hover:show_text:'The gate of Bree'> </hover>"))
    }

    @Test
    fun `the guide tag alone is not empty, as it shows Guide`() {
        assertFalse(isEmptyDescription("<guide>"))
    }

    @Test
    fun `visible text is not empty`() {
        assertFalse(isEmptyDescription("<gold>The gate of Bree."))
    }
}
