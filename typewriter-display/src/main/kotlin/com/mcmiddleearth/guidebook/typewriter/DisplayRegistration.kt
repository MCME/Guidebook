package com.mcmiddleearth.guidebook.typewriter

import com.mcmiddleearth.guidebook.GuidebookPlugin
import com.typewritermc.core.books.pages.PageType
import com.typewritermc.core.entries.Query
import com.typewritermc.core.extension.Initializable
import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.engine.paper.logger
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Registers the Typewriter display with Guidebook once Typewriter has loaded its pages, and again after every
 * Typewriter reload. Each reload loads fresh classes, so shutting down must unregister the Display from the last load.
 *
 * Guidebook's own classes are only reached through [GuidebookSide], once Guidebook's version is known to have them.
 */
@Singleton
class DisplayRegistration : Initializable {

    private val installChecks = InstallChecks(
        log = { logger.warning("The Typewriter display can't show Welcomes, so they're shown in chat. $it") },
        welcomeEntries = ::findWelcomeEntries,
    )
    private var registered = false

    override suspend fun initialize() {
        val guidebook = Bukkit.getPluginManager().getPlugin(GUIDEBOOK) ?: return
        InstallChecks.versionProblem(guidebook.pluginMeta.version, InstallChecks.LOWEST_GUIDEBOOK_VERSION)?.let {
            logger.warning("The Typewriter display isn't available, so Welcomes are shown in chat. $it")
            return
        }
        if (installChecks.problem() != null) return
        Bukkit.getPluginManager().registerEvents(QuitListener, guidebook)
        GuidebookSide.register(installChecks)
        registered = true
    }

    override suspend fun shutdown() {
        if (registered) GuidebookSide.unregister()
        registered = false
        HandlerList.unregisterAll(QuitListener)
        WelcomeQueues.clear()
    }

    /** Forgets a player's Welcome queue when they quit. */
    private object QuitListener : Listener {
        @EventHandler
        fun onQuit(event: PlayerQuitEvent) {
            WelcomeQueues.forget(event.player.uniqueId)
        }
    }
}

private const val GUIDEBOOK = "Guidebook"

private fun findWelcomeEntries(): List<FoundWelcomeEntry> =
    PageType.entries.asSequence()
        .flatMap { Query.findPagesOfType(it) }
        .flatMap { page ->
            page.entries.filterIsInstance<GuidebookWelcomeEntry>().map { FoundWelcomeEntry(it.id, page.name) }
        }
        .toList()

/** Every use of Guidebook's own classes in registering, kept apart so an older Guidebook never loads them. */
private object GuidebookSide {

    fun register(installChecks: InstallChecks) {
        switchboard().register(
            TypewriterDisplay.NAME,
            TypewriterDisplay(TypewriterGlue, installChecks, WelcomeQueues) { Bukkit.getCurrentTick() },
        )
    }

    fun unregister() {
        switchboard().unregister(TypewriterDisplay.NAME)
    }

    private fun switchboard() = (Bukkit.getPluginManager().getPlugin(GUIDEBOOK) as GuidebookPlugin).displaySwitchboard
}
