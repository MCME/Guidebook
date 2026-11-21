package com.mcmiddleearth.guidebook.typewriter

// SEAM: Welcome queue. What happens to each Welcome a player is given while a box may already be open, and to each
// way a box can end. Pure: no Typewriter or Bukkit, so the glue acts on what it answers. The rules are the spec's
// "Welcome queue" state table.
//
// STAND-IN: every Welcome starts at once, and nothing is ever queued.

import com.mcmiddleearth.guidebook.welcome.Welcome
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** What the Display does with a Welcome it's given. */
sealed interface QueueDecision {
    /** Open a box for it. */
    data object Start : QueueDecision

    /** Keep it until the open box is closed. */
    data object Queue : QueueDecision

    /** Close the open box and open one for it instead. */
    data object Replace : QueueDecision

    /** Leave it to the Chat display, as the player is in a Typewriter dialogue that isn't a Welcome. */
    data object ShowInChat : QueueDecision
}

/** One player's open box and the Welcomes waiting behind it. */
class WelcomeQueue {

    /**
     * Decides what to do with [welcome], given on server tick [tick]. [inOtherDialogue] is whether the player is in a
     * Typewriter dialogue, which is someone else's unless this queue has a box open.
     */
    fun arrive(welcome: Welcome, tick: Int, inOtherDialogue: Boolean): QueueDecision = QueueDecision.Start

    /** The player closed the box on [tick]: the queued Welcome to open next, if any. */
    fun closedByPlayer(tick: Int): Welcome? = null

    /** The box ended some way other than the player closing it, such as another dialogue taking over. */
    fun endedOtherwise() {}
}

/** Every online player's Welcome queue. */
object WelcomeQueues {
    private val queues = ConcurrentHashMap<UUID, WelcomeQueue>()

    fun of(player: UUID): WelcomeQueue = queues.computeIfAbsent(player) { WelcomeQueue() }

    fun forget(player: UUID) {
        queues.remove(player)
    }

    fun clear() {
        queues.clear()
    }
}
