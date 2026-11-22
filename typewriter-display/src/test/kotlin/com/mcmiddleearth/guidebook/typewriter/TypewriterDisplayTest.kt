package com.mcmiddleearth.guidebook.typewriter

import com.mcmiddleearth.guidebook.welcome.Display
import com.mcmiddleearth.guidebook.welcome.Welcome
import net.kyori.adventure.text.Component
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
    private val inspector = InstallInspector {
        List(welcomeEntries) { FoundWelcomeEntry("guidebookWelcome", "guidebook") }
    }
    private val display = TypewriterDisplay(typewriter, inspector, WelcomeQueues) { 1 }

    @Test
    fun `a Welcome opens the Guidebook Welcome entry for the player`() {
        display.show(player, bree)

        assertEquals(listOf(bree), typewriter.started)
    }

    @Test
    fun `a player without a Typewriter session gets the Welcome in chat`() {
        typewriter.hasSession = false

        assertThrows(Display.Unavailable::class.java) { display.show(player, bree) }
        assertEquals(emptyList<Welcome>(), typewriter.started)
    }

    @Test
    fun `without the Guidebook Welcome entry the Welcome goes to chat`() {
        welcomeEntries = 0

        assertThrows(Display.Unavailable::class.java) { display.show(player, bree) }
        assertEquals(emptyList<Welcome>(), typewriter.started)
    }

    @Test
    fun `with more than one Guidebook Welcome entry the Welcome goes to chat`() {
        welcomeEntries = 2

        assertThrows(Display.Unavailable::class.java) { display.show(player, bree) }
        assertEquals(emptyList<Welcome>(), typewriter.started)
    }

    @Test
    fun `a player in someone else's Typewriter dialogue gets the Welcome in chat`() {
        typewriter.inDialogue = true

        assertThrows(Display.Unavailable::class.java) { display.show(player, bree) }
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

    @Test
    fun `a Welcome with an empty Description gets its Title and no box`() {
        val empty = Welcome("me-weathertop", "Weathertop", "Amon Sûl", "<gray> ", true)

        display.show(player, empty)

        assertEquals(listOf(empty), typewriter.titlesOnly)
        assertEquals(emptyList<Welcome>(), typewriter.started)
    }

    @Test
    fun `a Welcome with an empty Description and Show title off shows nothing`() {
        display.show(player, Welcome("me-weathertop", "Weathertop", "Amon Sûl", "", false))

        assertEquals(emptyList<Welcome>(), typewriter.titlesOnly)
        assertEquals(emptyList<Welcome>(), typewriter.started)
    }

    @Test
    fun `a Welcome with an empty Description leaves an open box alone`() {
        display.show(player, bree)

        display.show(player, Welcome("me-weathertop", "Weathertop", "Amon Sûl", "", true))

        assertEquals(listOf(bree), typewriter.started)
    }

    @Test
    fun `a Description of only the guide tag gets its Title and no box, as the box shows no Guide label`() {
        val guide = Welcome("me-weathertop", "Weathertop", "Amon Sûl", "<guide>", true)

        display.show(player, guide)

        assertEquals(listOf(guide), typewriter.titlesOnly)
        assertEquals(emptyList<Welcome>(), typewriter.started)
    }

    @Test
    fun `a Description is previewed as its boxes, one for each Page, fully typed and with no Guide label`() {
        val lines = (1..9).joinToString("\n") { "Line $it." }
        val long = Welcome("me-bree-gate", "Bree", "", "<guide>$lines", true)

        val boxes = display.previewDescription(player, long).map { plain(it).lines() }

        assertEquals(2, boxes.size)
        assertEquals("Bree", boxes[0].first().trim())
        assertEquals(listOf("Line 1.", "Line 8...."), listOf(boxes[0][1], boxes[0][8]))
        assertEquals("Press F to continue (1/2)", boxes[0].last().trim())
        assertEquals("Line 9.", boxes[1][2])
        assertEquals("Press F to close (2/2)", boxes[1].last().trim())
    }

    @Test
    fun `a Description of only the guide tag has no boxes to preview, as it gets none`() {
        assertEquals(emptyList<Any>(), display.previewDescription(player, Welcome("me-weathertop", "Weathertop", "", "<guide>", true)))
    }

    @Test
    fun `without the Guidebook Welcome entry the Description previews as chat`() {
        welcomeEntries = 0

        assertEquals(emptyList<Any>(), display.previewDescription(player, bree))
    }

    private class FakeTypewriter : BoxOpener {
        var hasSession = true
        var inDialogue = false
        val started = mutableListOf<Welcome>()
        val titlesOnly = mutableListOf<Welcome>()

        override fun hasSession(player: Player) = hasSession

        override fun isInDialogue(player: Player) = inDialogue

        override fun start(player: Player, welcome: Welcome) {
            started += welcome
        }

        override fun showTitle(player: Player, welcome: Welcome) {
            titlesOnly += welcome
        }

        override fun boxLook(player: Player) = BoxLook(LayoutSettings(), Component.text("F"))
    }
}
