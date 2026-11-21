package com.mcmiddleearth.guidebook.typewriter

import com.mcmiddleearth.guidebook.welcome.Welcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class WelcomeQueueTest {

    private val bree = welcome("Bree")
    private val prancing = welcome("The Prancing Pony")
    private val weathertop = welcome("Weathertop")
    private val rivendell = welcome("Rivendell")

    private val queue = WelcomeQueue()

    // Idle | Welcome arrives on tick N | start it | Showing, started on N

    @Test
    fun `a Welcome arriving while Idle starts`() {
        assertEquals(QueueDecision.Start, queue.arrive(bree, tick = 1, inOtherDialogue = false))
    }

    @Test
    fun `a Welcome arriving while Idle and the player is in another dialogue goes to chat`() {
        assertEquals(QueueDecision.ShowInChat, queue.arrive(bree, tick = 1, inOtherDialogue = true))
        assertEquals(QueueDecision.Start, queue.arrive(prancing, tick = 1, inOtherDialogue = false))
    }

    // Showing, started on N | Welcome arrives on tick N | queue it | Showing

    @Test
    fun `a Welcome arriving on the tick the open box started queues`() {
        queue.arrive(bree, tick = 1, inOtherDialogue = false)

        assertEquals(QueueDecision.Queue, queue.arrive(prancing, tick = 1, inOtherDialogue = true))
        assertEquals(prancing, queue.closedByPlayer(bree, tick = 9))
    }

    // Showing | Welcome arrives on a later tick | replace the box, empty the queue | Showing, started on the new tick

    @Test
    fun `a Welcome arriving on a later tick replaces the box and empties the queue`() {
        queue.arrive(bree, tick = 1, inOtherDialogue = false)
        queue.arrive(prancing, tick = 1, inOtherDialogue = true)

        assertEquals(QueueDecision.Replace, queue.arrive(weathertop, tick = 2, inOtherDialogue = true))
        assertEquals(QueueDecision.Queue, queue.arrive(rivendell, tick = 2, inOtherDialogue = true))
        assertEquals(rivendell, queue.closedByPlayer(weathertop, tick = 9))
        assertEquals(null, queue.closedByPlayer(rivendell, tick = 10))
    }

    @Test
    fun `the replaced box ending changes nothing`() {
        queue.arrive(bree, tick = 1, inOtherDialogue = false)
        queue.arrive(weathertop, tick = 2, inOtherDialogue = true)
        queue.arrive(prancing, tick = 2, inOtherDialogue = true)

        queue.endedOtherwise(bree)

        assertEquals(prancing, queue.closedByPlayer(weathertop, tick = 9))
    }

    @Test
    fun `a box replaced by an equal Welcome is told apart from its replacement`() {
        val again = bree.copy()
        queue.arrive(bree, tick = 1, inOtherDialogue = false)
        queue.arrive(again, tick = 2, inOtherDialogue = true)

        queue.endedOtherwise(bree)

        assertEquals(QueueDecision.Queue, queue.arrive(prancing, tick = 2, inOtherDialogue = true))
    }

    // Showing | player closes the box, queue not empty | start the next queued Welcome | Showing, started on now

    @Test
    fun `closing the box opens the next queued Welcome, started on the tick it opens`() {
        queue.arrive(bree, tick = 1, inOtherDialogue = false)
        queue.arrive(prancing, tick = 1, inOtherDialogue = true)

        assertEquals(prancing, queue.closedByPlayer(bree, tick = 9))
        assertEquals(QueueDecision.Queue, queue.arrive(weathertop, tick = 9, inOtherDialogue = true))
    }

    // Showing | player closes the box, queue empty | nothing | Idle

    @Test
    fun `closing the box with nothing queued leaves the player Idle`() {
        queue.arrive(bree, tick = 1, inOtherDialogue = false)

        assertEquals(null, queue.closedByPlayer(bree, tick = 9))
        assertEquals(QueueDecision.ShowInChat, queue.arrive(prancing, tick = 9, inOtherDialogue = true))
    }

    // Showing | box ends any other way | empty the queue | Idle

    @Test
    fun `a box ending any other way empties the queue and leaves the player Idle`() {
        queue.arrive(bree, tick = 1, inOtherDialogue = false)
        queue.arrive(prancing, tick = 1, inOtherDialogue = true)

        queue.endedOtherwise(bree)

        assertEquals(QueueDecision.ShowInChat, queue.arrive(weathertop, tick = 1, inOtherDialogue = true))
        assertEquals(QueueDecision.Start, queue.arrive(weathertop, tick = 1, inOtherDialogue = false))
        assertEquals(null, queue.closedByPlayer(weathertop, tick = 9))
    }

    // any | player quits | forget the player | Idle

    @Test
    fun `quitting forgets the player`() {
        val player = UUID.randomUUID()
        WelcomeQueues.of(player).arrive(bree, tick = 1, inOtherDialogue = false)

        WelcomeQueues.forget(player)

        assertEquals(QueueDecision.Start, WelcomeQueues.of(player).arrive(prancing, tick = 1, inOtherDialogue = false))
    }

    @Test
    fun `quitting while Welcomes are queued drops them`() {
        val player = UUID.randomUUID()
        WelcomeQueues.of(player).arrive(bree, tick = 1, inOtherDialogue = false)
        WelcomeQueues.of(player).arrive(prancing, tick = 1, inOtherDialogue = true)

        WelcomeQueues.forget(player)

        assertEquals(null, WelcomeQueues.find(player))
        WelcomeQueues.of(player).arrive(weathertop, tick = 5, inOtherDialogue = false)
        assertEquals(null, WelcomeQueues.of(player).closedByPlayer(weathertop, tick = 9))
    }

    // Sequences

    @Test
    fun `two Welcomes on one tick then a third on the next replaces the box and empties the queue`() {
        assertEquals(QueueDecision.Start, queue.arrive(bree, tick = 1, inOtherDialogue = false))
        assertEquals(QueueDecision.Queue, queue.arrive(prancing, tick = 1, inOtherDialogue = true))
        assertEquals(QueueDecision.Replace, queue.arrive(weathertop, tick = 2, inOtherDialogue = true))

        assertEquals(null, queue.closedByPlayer(weathertop, tick = 9))
    }

    @Test
    fun `closing through a queue of three opens each in turn`() {
        queue.arrive(bree, tick = 1, inOtherDialogue = false)
        queue.arrive(prancing, tick = 1, inOtherDialogue = true)
        queue.arrive(weathertop, tick = 1, inOtherDialogue = true)
        queue.arrive(rivendell, tick = 1, inOtherDialogue = true)

        assertEquals(prancing, queue.closedByPlayer(bree, tick = 5))
        assertEquals(weathertop, queue.closedByPlayer(prancing, tick = 9))
        assertEquals(rivendell, queue.closedByPlayer(weathertop, tick = 14))
        assertEquals(null, queue.closedByPlayer(rivendell, tick = 20))
        assertEquals(QueueDecision.Start, queue.arrive(bree, tick = 20, inOtherDialogue = false))
    }

    @Test
    fun `a queued Welcome is replaced by one arriving a tick after it opens`() {
        queue.arrive(bree, tick = 1, inOtherDialogue = false)
        queue.arrive(prancing, tick = 1, inOtherDialogue = true)
        queue.closedByPlayer(bree, tick = 9)

        assertEquals(QueueDecision.Replace, queue.arrive(weathertop, tick = 10, inOtherDialogue = true))
    }

    private fun welcome(title: String) = Welcome(title.lowercase().replace(' ', '-'), title, "", "<guide>A place.", true)

    private fun Welcome.copy() = Welcome(areaName(), title(), subtitle(), description(), showTitle())
}
