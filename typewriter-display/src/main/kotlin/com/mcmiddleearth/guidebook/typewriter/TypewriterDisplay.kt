package com.mcmiddleearth.guidebook.typewriter

import com.mcmiddleearth.guidebook.welcome.Display
import com.mcmiddleearth.guidebook.welcome.Welcome
import org.bukkit.entity.Player

/** What the Typewriter display needs from Typewriter, so tests can fake it. */
interface TypewriterSide {
    /** Whether Typewriter has a session for [player] yet; it drops anything triggered for a player without one. */
    fun hasSession(player: Player): Boolean

    fun isInDialogue(player: Player): Boolean

    /** Opens the Guidebook Welcome entry for [player], with [welcome] in its context. */
    fun start(player: Player, welcome: Welcome)

    /** Shows [player] only [welcome]'s Title step, with no box: for a Welcome whose Description is empty. */
    fun showTitle(player: Player, welcome: Welcome)
}

/**
 * The Typewriter display: types each Welcome into a box over the chat. Throwing from [show] sends that Welcome
 * through the Chat display instead, so no player misses one.
 */
class TypewriterDisplay(
    private val typewriter: TypewriterSide,
    private val installChecks: InstallChecks,
    private val queues: WelcomeQueues,
    private val currentTick: () -> Int,
) : Display {

    override fun show(player: Player, welcome: Welcome) {
        // No box to close, so it neither waits behind nor replaces one
        if (isEmptyDescription(welcome.description())) {
            if (welcome.showTitle()) typewriter.showTitle(player, welcome)
            return
        }
        check(typewriter.hasSession(player)) { "${player.name} has no Typewriter session yet" }
        installChecks.problem()?.let { error(it) }

        val queue = queues.of(player.uniqueId)
        when (queue.arrive(welcome, currentTick(), typewriter.isInDialogue(player))) {
            QueueDecision.Start, QueueDecision.Replace -> typewriter.start(player, welcome)
            QueueDecision.Queue -> {}
            QueueDecision.ShowInChat -> error("${player.name} is in another Typewriter dialogue")
        }
    }

    companion object {
        /** The name the server's config chooses this Display by. */
        const val NAME = "typewriter"
    }
}
