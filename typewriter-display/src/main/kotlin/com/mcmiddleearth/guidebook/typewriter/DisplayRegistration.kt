package com.mcmiddleearth.guidebook.typewriter

import com.mcmiddleearth.guidebook.welcome.DisplayRegistry
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
 * Registers the Typewriter display with Guidebook once Typewriter has loaded its Typewriter pages, and again after
 * every Typewriter reload. Each reload loads fresh classes, so shutting down must unregister the Display from the last
 * load.
 *
 * Guidebook's own classes are only reached through [DisplayRegistrar], once Guidebook's version is known to have
 * them.
 */
@Singleton
class DisplayRegistration : Initializable {

    private val inspector = InstallInspector(
        log = { logger.warning("The Typewriter display can't show Welcomes, so they use the Chat display. $it") },
        welcomeEntries = ::findWelcomeEntries,
    )
    private var registered = false

    override suspend fun initialize() {
        val guidebook = Bukkit.getPluginManager().getPlugin(GUIDEBOOK) ?: return
        InstallInspector.versionProblem(guidebook.pluginMeta.version, InstallInspector.LOWEST_GUIDEBOOK_VERSION)?.let {
            logger.warning("The Typewriter display isn't available, so Welcomes use the Chat display. $it")
            return
        }
        if (inspector.problem() != null) return
        if (!DisplayRegistrar.register(inspector)) {
            logger.warning("The Typewriter display isn't available, so Welcomes use the Chat display. Guidebook isn't enabled.")
            return
        }
        Bukkit.getPluginManager().registerEvents(QuitListener, guidebook)
        registered = true
        // The one line in the log that says the Typewriter display is working
        logger.info("The Typewriter display is registered with Guidebook, so Welcomes are shown in its box.")
    }

    override suspend fun shutdown() {
        if (registered) DisplayRegistrar.unregister()
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

/**
 * Registers the Typewriter display with Guidebook's DisplayRegistry service. Every use of Guidebook's own classes in
 * registering is here, kept apart so an older Guidebook never loads them.
 */
private object DisplayRegistrar {

    /** Whether it registered, which it can't while Guidebook isn't enabled. */
    fun register(inspector: InstallInspector): Boolean {
        val registry = registry() ?: return false
        registry.register(
            TypewriterDisplay.NAME,
            TypewriterDisplay(TypewriterBoxOpener, inspector, WelcomeQueues) { Bukkit.getCurrentTick() },
        )
        return true
    }

    /** Guidebook disabling first leaves nothing to unregister from. */
    fun unregister() {
        registry()?.unregister(TypewriterDisplay.NAME)
    }

    // Guidebook offers it while it's enabled
    private fun registry(): DisplayRegistry? = Bukkit.getServicesManager().load(DisplayRegistry::class.java)
}
