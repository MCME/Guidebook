package com.mcmiddleearth.guidebook.typewriter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class InstallInspectorTest {

    private val shipped = FoundWelcomeEntry("guidebookWelcome", "guidebook")
    private val copy = FoundWelcomeEntry("k3x9", "welcomes")

    private var entries = listOf(shipped)
    private val logged = mutableListOf<String>()
    private val inspector = InstallInspector(log = { logged += it }) { entries }

    @Test
    fun `the Display may run when the Guidebook Welcome entry exists`() {
        assertNull(inspector.problem())
    }

    @Test
    fun `the Display may not run without the Guidebook Welcome entry`() {
        entries = emptyList()

        val problem = inspector.problem()!!
        assert(InstallInspector.TYPEWRITER_PAGE in problem) { problem }
    }

    @Test
    fun `the Display may not run with more than one Guidebook Welcome entry, and says where each is`() {
        entries = listOf(shipped, copy)

        val problem = inspector.problem()!!
        assert(listOf("guidebookWelcome", "guidebook", "k3x9", "welcomes").all { it in problem }) { problem }
    }

    @Test
    fun `nothing is logged while the Guidebook Welcome entry is fine`() {
        repeat(3) { inspector.problem() }

        assertEquals(emptyList<String>(), logged)
    }

    @Test
    fun `a missing Guidebook Welcome entry is logged once, not on every Welcome`() {
        entries = emptyList()

        val problem = inspector.problem()
        repeat(3) { inspector.problem() }

        assertEquals(listOf(problem), logged)
    }

    @Test
    fun `each change of problem is logged once`() {
        entries = emptyList()
        val missing = inspector.problem()
        inspector.problem()
        entries = listOf(shipped, copy)
        val duplicated = inspector.problem()
        inspector.problem()

        assertEquals(listOf(missing, duplicated), logged)
    }

    @Test
    fun `a problem fixed then back is logged again`() {
        entries = emptyList()
        val missing = inspector.problem()
        entries = listOf(shipped)
        inspector.problem()
        entries = emptyList()
        inspector.problem()

        assertEquals(listOf(missing, missing), logged)
    }

    @Test
    fun `Guidebook at or past the lowest version may run the Display`() {
        assertNull(InstallInspector.versionProblem(guidebook = "2.1.0", lowest = "2.1.0"))
        assertNull(InstallInspector.versionProblem(guidebook = "2.1.1", lowest = "2.1.0"))
        assertNull(InstallInspector.versionProblem(guidebook = "2.10.0", lowest = "2.9.3"))
        assertNull(InstallInspector.versionProblem(guidebook = "3.0", lowest = "2.1.0"))
    }

    @Test
    fun `Guidebook older than the lowest version may not run the Display`() {
        assertNotNull(InstallInspector.versionProblem(guidebook = "2.0.1", lowest = "2.1.0"))
        assertNotNull(InstallInspector.versionProblem(guidebook = "1.9.9", lowest = "2.1.0"))
        assertNotNull(InstallInspector.versionProblem(guidebook = "2.9.3", lowest = "2.10.0"))
        assertNotNull(InstallInspector.versionProblem(guidebook = "2.1", lowest = "2.1.1"))
    }

    @Test
    fun `a development build of the lowest version may run the Display`() {
        assertNull(InstallInspector.versionProblem(guidebook = "2.1.0-SNAPSHOT", lowest = "2.1.0"))
    }

    @Test
    fun `the log line names both versions`() {
        val problem = InstallInspector.versionProblem(guidebook = "2.0.1", lowest = "2.1.0")!!

        assert("2.0.1" in problem && "2.1.0" in problem) { problem }
    }

    @Test
    fun `the lowest Guidebook version comes from the build`() {
        val lowest = InstallInspector.LOWEST_GUIDEBOOK_VERSION

        assert(lowest.matches(Regex("""\d+(\.\d+)*"""))) { lowest }
    }
}
