package com.mcmiddleearth.guidebook.typewriter

import com.mcmiddleearth.guidebook.welcome.Welcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration

class TitleStepTest {

    @Test
    fun `with Show title off the Description starts at once`() {
        val welcome = Welcome("me-bree-gate", "Bree", "The gate", "<guide>The gate of Bree.", false)

        assertEquals(Duration.ZERO, TitleStep(welcome, TitleSettings()).descriptionDelay)
    }
}
