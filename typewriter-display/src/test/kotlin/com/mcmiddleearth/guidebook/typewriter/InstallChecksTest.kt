package com.mcmiddleearth.guidebook.typewriter

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class InstallChecksTest {

    @Test
    fun `the Display may run when the Guidebook Welcome entry exists`() {
        assertNull(InstallChecks(countWelcomeEntries = { 1 }).problem())
    }

    @Test
    fun `the Display may not run without the Guidebook Welcome entry`() {
        assertNotNull(InstallChecks(countWelcomeEntries = { 0 }).problem())
    }
}
