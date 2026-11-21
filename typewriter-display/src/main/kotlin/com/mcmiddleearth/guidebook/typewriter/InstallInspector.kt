package com.mcmiddleearth.guidebook.typewriter

import java.util.Properties

/** One Guidebook Welcome entry found in a Typewriter page: its id, and the name of the Typewriter page holding it. */
data class FoundWelcomeEntry(val id: String, val typewriterPage: String)

/**
 * Whether the Typewriter display is installed well enough to show Welcomes, asked when Typewriter loads the extension
 * and on every Welcome. [welcomeEntries] finds every Guidebook Welcome entry in the Typewriter pages, passed in so
 * tests can fake them. Each new problem goes to [log] once, so asking on every Welcome doesn't fill the log.
 *
 * It must use no Guidebook class: it checks Guidebook's version before anything else does, and a Guidebook older
 * than [LOWEST_GUIDEBOOK_VERSION] may not have those classes, which would crash loading instead of logging one line.
 */
class InstallInspector(
    private val log: (String) -> Unit = {},
    private val welcomeEntries: () -> List<FoundWelcomeEntry>,
) {
    private var lastProblem: String? = null

    /** Why the Display can't run, or null if it may. Logs the problem if it isn't the one found last time. */
    fun problem(): String? {
        val problem = entryProblem(welcomeEntries())
        if (problem != null && problem != lastProblem) log(problem)
        lastProblem = problem
        return problem
    }

    private fun entryProblem(entries: List<FoundWelcomeEntry>): String? = when (entries.size) {
        1 -> null
        0 -> "No Typewriter page holds the Guidebook Welcome entry. Copy $TYPEWRITER_PAGE into Typewriter's " +
            "pages folder and restart."
        else -> "There's more than one Guidebook Welcome entry: " +
            entries.joinToString { "${it.id} on Typewriter page ${it.typewriterPage}" } + ". Keep only one."
    }

    companion object {
        /** The Typewriter page shipped with the extension, holding the Guidebook Welcome entry. */
        const val TYPEWRITER_PAGE = "guidebook.json"

        /** The lowest Guidebook version the Typewriter display works with, set in its build. */
        val LOWEST_GUIDEBOOK_VERSION: String by lazy {
            val properties = Properties()
            InstallInspector::class.java.getResourceAsStream("build.properties")!!.use(properties::load)
            properties.getProperty("lowestGuidebookVersion")
        }

        /**
         * Why the [guidebook] version running can't run the Display, for the log, or null if it's [lowest] or newer.
         * Only the numbers count, so a development build of the lowest version, such as `2.1.0-SNAPSHOT`, may run it.
         */
        fun versionProblem(guidebook: String, lowest: String): String? =
            if (isOlder(numbers(guidebook), numbers(lowest))) {
                "Guidebook $guidebook is older than $lowest, the lowest version the Typewriter display works with. " +
                    "Update Guidebook."
            } else {
                null
            }

        private fun numbers(version: String): List<Int> =
            version.substringBefore('-').split('.').map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }

        private fun isOlder(version: List<Int>, lowest: List<Int>): Boolean {
            for (i in 0 until maxOf(version.size, lowest.size)) {
                val difference = version.getOrElse(i) { 0 } - lowest.getOrElse(i) { 0 }
                if (difference != 0) return difference < 0
            }
            return false
        }
    }
}
