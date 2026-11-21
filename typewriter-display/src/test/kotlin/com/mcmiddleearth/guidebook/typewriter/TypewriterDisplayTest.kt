package com.mcmiddleearth.guidebook.typewriter

import com.mcmiddleearth.guidebook.welcome.Welcome
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy
import java.util.UUID

class TypewriterDisplayTest {

    private val id = UUID.randomUUID()
    private val player = Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { _, method, _ ->
        if (method.name == "getUniqueId") id else null
    } as Player
    private val bree = Welcome("me-bree-gate", "Bree", "", "<guide>The gate of Bree.", true)

    private val typewriter = FakeTypewriter()
    private var welcomeEntries = 1
    private val installChecks = InstallChecks {
        List(welcomeEntries) { FoundWelcomeEntry("guidebookWelcome", "guidebook") }
    }
    private val display = TypewriterDisplay(typewriter, installChecks, WelcomeQueues) { 1 }

    @Test
    fun `a Welcome opens the Guidebook Welcome entry for the player`() {
        display.show(player, bree)

        assertEquals(listOf(bree), typewriter.started)
    }

    @Test
    fun `a player without a Typewriter session gets the Welcome in chat`() {
        typewriter.hasSession = false

        assertThrows(IllegalStateException::class.java) { display.show(player, bree) }
        assertEquals(emptyList<Welcome>(), typewriter.started)
    }

    @Test
    fun `without the Guidebook Welcome entry the Welcome goes to chat`() {
        welcomeEntries = 0

        assertThrows(IllegalStateException::class.java) { display.show(player, bree) }
        assertEquals(emptyList<Welcome>(), typewriter.started)
    }

    @Test
    fun `with more than one Guidebook Welcome entry the Welcome goes to chat`() {
        welcomeEntries = 2

        assertThrows(IllegalStateException::class.java) { display.show(player, bree) }
        assertEquals(emptyList<Welcome>(), typewriter.started)
    }

    @Test
    fun `a player in someone else's Typewriter dialogue gets the Welcome in chat`() {
        typewriter.inDialogue = true

        assertThrows(IllegalStateException::class.java) { display.show(player, bree) }
        assertEquals(emptyList<Welcome>(), typewriter.started)
    }

    @Test
    fun `a Welcome on the tick of the open box waits behind it`() {
        val prancing = Welcome("me-bree-prancing-pony", "The Prancing Pony", "", "<guide>An inn.", true)
        display.show(player, bree)
        typewriter.inDialogue = true

        display.show(player, prancing)

        assertEquals(listOf(bree), typewriter.started)
    }

    private class FakeTypewriter : TypewriterSide {
        var hasSession = true
        var inDialogue = false
        val started = mutableListOf<Welcome>()

        override fun hasSession(player: Player) = hasSession

        override fun isInDialogue(player: Player) = inDialogue

        override fun start(player: Player, welcome: Welcome) {
            started += welcome
        }
    }
}
