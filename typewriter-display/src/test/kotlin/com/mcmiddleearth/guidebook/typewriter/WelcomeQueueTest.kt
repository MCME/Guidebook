package com.mcmiddleearth.guidebook.typewriter

import com.mcmiddleearth.guidebook.welcome.Welcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WelcomeQueueTest {

    private val bree = Welcome("me-bree-gate", "Bree", "", "<guide>The gate of Bree.", true)

    @Test
    fun `a Welcome arriving while Idle starts`() {
        val queue = WelcomeQueue()

        assertEquals(QueueDecision.Start, queue.arrive(bree, tick = 1, inOtherDialogue = false))
    }
}
