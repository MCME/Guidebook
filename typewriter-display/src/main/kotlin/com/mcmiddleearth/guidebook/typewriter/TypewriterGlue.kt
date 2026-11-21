package com.mcmiddleearth.guidebook.typewriter

import com.mcmiddleearth.guidebook.welcome.Welcome
import com.typewritermc.core.entries.Query
import com.typewritermc.core.entries.ref
import com.typewritermc.core.interaction.context
import com.typewritermc.engine.paper.entry.dialogue.isInDialogue
import com.typewritermc.engine.paper.entry.triggerFor
import com.typewritermc.engine.paper.interaction.PlayerSessionManager
import org.bukkit.entity.Player
import org.koin.java.KoinJavaComponent

/** The Typewriter display's way into Typewriter itself. */
object TypewriterGlue : TypewriterSide {

    fun welcomeEntries(): List<GuidebookWelcomeEntry> = Query.find<GuidebookWelcomeEntry>().toList()

    override fun hasSession(player: Player): Boolean =
        with(KoinJavaComponent.get<PlayerSessionManager>(PlayerSessionManager::class.java)) { player.session != null }

    override fun isInDialogue(player: Player): Boolean = player.isInDialogue

    override fun start(player: Player, welcome: Welcome) {
        val entry = welcomeEntries().firstOrNull() ?: error("The Guidebook Welcome entry is missing")
        entry.ref().triggerFor(player, context { WelcomeKey withValue welcome })
    }
}
