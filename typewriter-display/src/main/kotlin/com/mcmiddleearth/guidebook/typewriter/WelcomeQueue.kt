package com.mcmiddleearth.guidebook.typewriter

// The Welcome queue: what happens to each Welcome a player is given while a box may already be open, and to each
// way a box can end. Pure: no Typewriter or Bukkit, so the glue acts on what it answers. The rules are the spec's
// "Welcome queue" state table.

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

/**
 * One player's open box and the Welcomes waiting behind it. Idle while no box is open. A box is told apart by the very
 * [Welcome] it shows, not an equal one, since the same Area can be Welcomed again while its last box is still ending.
 * Synchronized, as Typewriter ends boxes off the main thread.
 */
class WelcomeQueue {
    /** The open box, or null when Idle. */
    private var open: Welcome? = null
    private var startedOn = 0
    private val waiting = ArrayDeque<Welcome>()

    /**
     * Decides what to do with [welcome], given on server tick [tick]. [inOtherDialogue] is whether the player is in a
     * Typewriter dialogue, which is someone else's unless this queue has a box open.
     */
    @Synchronized
    fun arrive(welcome: Welcome, tick: Int, inOtherDialogue: Boolean): QueueDecision {
        if (open == null) {
            if (inOtherDialogue) return QueueDecision.ShowInChat
            open(welcome, tick)
            return QueueDecision.Start
        }
        if (tick == startedOn) {
            waiting.addLast(welcome)
            return QueueDecision.Queue
        }
        waiting.clear()
        open(welcome, tick)
        return QueueDecision.Replace
    }

    /** The player closed the box showing [welcome] on [tick]: the queued Welcome to open next, if any. */
    @Synchronized
    fun closedByPlayer(welcome: Welcome, tick: Int): Welcome? {
        if (open !== welcome) return null
        val next = waiting.removeFirstOrNull()
        if (next == null) {
            open = null
        } else {
            open(next, tick)
        }
        return next
    }

    /**
     * The box showing [welcome] ended some way other than the player closing it, such as another dialogue taking over.
     * A box this queue replaced ends this way too, and changes nothing.
     */
    @Synchronized
    fun endedOtherwise(welcome: Welcome) {
        if (open !== welcome) return
        open = null
        waiting.clear()
    }

    private fun open(welcome: Welcome, tick: Int) {
        open = welcome
        startedOn = tick
    }
}

/** Every online player's Welcome queue. */
object WelcomeQueues {
    private val queues = ConcurrentHashMap<UUID, WelcomeQueue>()

    fun of(player: UUID): WelcomeQueue = queues.computeIfAbsent(player) { WelcomeQueue() }

    /** The player's Welcome queue, without making one for a player who has quit. */
    fun find(player: UUID): WelcomeQueue? = queues[player]

    fun forget(player: UUID) {
        queues.remove(player)
    }

    fun clear() {
        queues.clear()
    }
}
