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

    @Test
    fun `Guidebook at or past the lowest version may run the Display`() {
        assertNull(InstallChecks.versionProblem(guidebook = "2.1.0", lowest = "2.1.0"))
        assertNull(InstallChecks.versionProblem(guidebook = "2.1.1", lowest = "2.1.0"))
        assertNull(InstallChecks.versionProblem(guidebook = "2.10.0", lowest = "2.9.3"))
        assertNull(InstallChecks.versionProblem(guidebook = "3.0", lowest = "2.1.0"))
    }

    @Test
    fun `Guidebook older than the lowest version may not run the Display`() {
        assertNotNull(InstallChecks.versionProblem(guidebook = "2.0.1", lowest = "2.1.0"))
        assertNotNull(InstallChecks.versionProblem(guidebook = "1.9.9", lowest = "2.1.0"))
        assertNotNull(InstallChecks.versionProblem(guidebook = "2.9.3", lowest = "2.10.0"))
        assertNotNull(InstallChecks.versionProblem(guidebook = "2.1", lowest = "2.1.1"))
    }

    @Test
    fun `a development build of the lowest version may run the Display`() {
        assertNull(InstallChecks.versionProblem(guidebook = "2.1.0-SNAPSHOT", lowest = "2.1.0"))
    }

    @Test
    fun `the log line names both versions`() {
        val problem = InstallChecks.versionProblem(guidebook = "2.0.1", lowest = "2.1.0")!!

        assert("2.0.1" in problem && "2.1.0" in problem) { problem }
    }
}
