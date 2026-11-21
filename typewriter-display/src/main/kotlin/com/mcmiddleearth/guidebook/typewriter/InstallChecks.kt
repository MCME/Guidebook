package com.mcmiddleearth.guidebook.typewriter

// SEAM: Install checks. The one place that answers "may the Display run?", asked when Typewriter initializes the
// extension and on every Welcome. The rules are the spec's "Registration" module.
//
// STAND-IN: only checks that a Guidebook Welcome entry exists.

import java.util.Properties

/**
 * Whether the Typewriter display is installed well enough to show Welcomes. [countWelcomeEntries] counts the
 * Guidebook Welcome entries in Typewriter's pages, passed in so tests don't need Typewriter.
 */
class InstallChecks(private val countWelcomeEntries: () -> Int) {

    /** Why the Display can't run, for the log, or null if it may. */
    fun problem(): String? =
        if (countWelcomeEntries() == 0) {
            "No Typewriter page holds the Guidebook Welcome entry. Copy $TYPEWRITER_PAGE into Typewriter's pages " +
                "folder and restart."
        } else {
            null
        }

    companion object {
        /** The Typewriter page shipped with the extension, holding the Guidebook Welcome entry. */
        const val TYPEWRITER_PAGE = "guidebook.json"

        /** The lowest Guidebook version the Typewriter display works with, set in its build. */
        val LOWEST_GUIDEBOOK_VERSION: String by lazy {
            val properties = Properties()
            InstallChecks::class.java.getResourceAsStream("build.properties")!!.use(properties::load)
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
