package com.mcmiddleearth.guidebook.typewriter

import com.mcmiddleearth.guidebook.GuidebookPlugin
import com.mcmiddleearth.guidebook.welcome.DisplaySwitchboard
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
 */
@Singleton
class DisplayRegistration : Initializable {

    private val installChecks = InstallChecks { TypewriterGlue.welcomeEntries().size }

    override suspend fun initialize() {
        installChecks.problem()?.let {
            logger.warning("The Typewriter display isn't available, so Welcomes are shown in chat. $it")
            return
        }
        Bukkit.getPluginManager().registerEvents(QuitListener, guidebook())
        switchboard().register(
            TypewriterDisplay.NAME,
            TypewriterDisplay(TypewriterGlue, installChecks, WelcomeQueues) { Bukkit.getCurrentTick() },
        )
    }

    override suspend fun shutdown() {
        switchboard().unregister(TypewriterDisplay.NAME)
        HandlerList.unregisterAll(QuitListener)
        WelcomeQueues.clear()
    }

    private fun switchboard(): DisplaySwitchboard = guidebook().displaySwitchboard

    private fun guidebook() = Bukkit.getPluginManager().getPlugin("Guidebook") as GuidebookPlugin

    /** Forgets a player's Welcome queue when they quit. */
    private object QuitListener : Listener {
        @EventHandler
        fun onQuit(event: PlayerQuitEvent) {
            WelcomeQueues.forget(event.player.uniqueId)
        }
    }
}
