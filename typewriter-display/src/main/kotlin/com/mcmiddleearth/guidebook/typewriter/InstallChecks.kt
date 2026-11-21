package com.mcmiddleearth.guidebook.typewriter

// SEAM: Install checks. The one place that answers "may the Display run?", asked when Typewriter initializes the
// extension and on every Welcome. The rules are the spec's "Registration" module.
//
// STAND-IN: only checks that a Guidebook Welcome entry exists.

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
    }
}
