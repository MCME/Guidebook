package com.mcmiddleearth.guidebook.typewriter

import com.mcmiddleearth.guidebook.welcome.Welcome
import com.typewritermc.core.entries.Query
import com.typewritermc.core.entries.ref
import com.typewritermc.core.interaction.context
import com.typewritermc.engine.paper.entry.dialogue.isInDialogue
import com.typewritermc.engine.paper.entry.triggerFor
import com.typewritermc.engine.paper.interaction.PlayerSessionManager
import com.typewritermc.engine.paper.plugin
import com.typewritermc.engine.paper.utils.asMiniWithResolvers
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.koin.java.KoinJavaComponent

/** Opens boxes through Typewriter itself, by triggering the Guidebook Welcome entry. */
object TypewriterBoxOpener : BoxOpener {

    private fun welcomeEntries(): List<GuidebookWelcomeEntry> = Query.find<GuidebookWelcomeEntry>().toList()

    override fun hasSession(player: Player): Boolean =
        with(KoinJavaComponent.get<PlayerSessionManager>(PlayerSessionManager::class.java)) { player.session != null }

    override fun isInDialogue(player: Player): Boolean = player.isInDialogue

    override fun start(player: Player, welcome: Welcome) {
        val entry = welcomeEntries().firstOrNull() ?: error("The Guidebook Welcome entry is missing")
        entry.ref().triggerFor(player, context { WelcomeKey withValue welcome })
    }

    override fun showTitle(player: Player, welcome: Welcome) {
        val settings = welcomeEntries().firstOrNull()?.titleSettings() ?: TitleSettings()
        val titleStep = TitleStep(welcome, settings)
        var tick = 0L
        Bukkit.getScheduler().runTaskTimer(plugin, { task: BukkitTask ->
            val elapsed = ticks(tick++)
            if (player.isOnline) titleStep.tick(player, elapsed)
            // Its last frame is shown by the time the Description would have started
            if (!player.isOnline || elapsed >= titleStep.descriptionDelay) task.cancel()
        }, 0L, 1L)
    }

    override fun boxLook(player: Player) = BoxLook(
        welcomeEntries().firstOrNull()?.layoutSettings(player) ?: LayoutSettings(),
        "<confirmation_key>".asMiniWithResolvers(player),
    )
}
