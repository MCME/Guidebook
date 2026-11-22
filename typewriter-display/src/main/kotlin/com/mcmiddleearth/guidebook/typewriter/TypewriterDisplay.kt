package com.mcmiddleearth.guidebook.typewriter

import com.mcmiddleearth.guidebook.util.AreaText
import com.mcmiddleearth.guidebook.welcome.Display
import com.mcmiddleearth.guidebook.welcome.Welcome
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

/**
 * A way of opening a Welcome's box for a player, or of showing only its Title step when it gets no box. An
 * implementation must say truthfully whether the player can be given a box now, as opening one for a player who can't
 * is silently dropped.
 */
interface BoxOpener {
    /** Whether Typewriter has a session for [player] yet; it drops anything triggered for a player without one. */
    fun hasSession(player: Player): Boolean

    /** Whether [player] is in any Typewriter dialogue, a box of this Display's included. */
    fun isInDialogue(player: Player): Boolean

    /** Opens the Guidebook Welcome entry for [player], with [welcome] in its context. */
    fun start(player: Player, welcome: Welcome)

    /** Shows [player] only [welcome]'s Title step, with no box: for a Welcome whose Description is empty. */
    fun showTitle(player: Player, welcome: Welcome)

    /** How [player]'s box looks: the settings on the Guidebook Welcome entry, and the key that turns its Pages. */
    fun boxLook(player: Player): BoxLook
}

/** How a player's box looks: its [settings], and the [key] that turns its Pages as the footer names it. */
class BoxLook(val settings: LayoutSettings, val key: Component)

/**
 * The Typewriter display: types each Welcome into a box over the chat. When it can't now, it throws
 * [Display.Unavailable], so the Chat display shows that Welcome instead and no player misses one.
 */
class TypewriterDisplay(
    private val boxes: BoxOpener,
    private val inspector: InstallInspector,
    private val queues: WelcomeQueues,
    private val currentTick: () -> Int,
) : Display {

    override fun show(player: Player, welcome: Welcome) {
        // No box to close, so it neither waits behind nor replaces one
        if (isEmptyDescription(welcome.description())) {
            if (welcome.showTitle()) boxes.showTitle(player, welcome)
            return
        }
        if (!boxes.hasSession(player)) throw Display.Unavailable("${player.name} has no Typewriter session yet")
        // Already logged once by the inspector, so each Welcome only needs to go to the Chat display
        inspector.problem()?.let { throw Display.Unavailable(it) }

        val queue = queues.of(player.uniqueId)
        when (queue.arrive(welcome, currentTick(), boxes.isInDialogue(player))) {
            QueueDecision.Start, QueueDecision.Replace -> boxes.start(player, welcome)
            QueueDecision.Queue -> {}
            QueueDecision.ShowInChat -> throw Display.Unavailable("${player.name} is in another Typewriter dialogue")
        }
    }

    /**
     * The Description's boxes, one for each Page as it looks once typed, each line filled to the box's width so it
     * stays flush left wherever lines are centred. None for a Welcome that gets no box.
     */
    override fun previewDescription(player: Player, welcome: Welcome): List<Component> {
        if (isEmptyDescription(welcome.description()) || inspector.problem() != null) return emptyList()
        val look = boxes.boxLook(player)
        val header = AreaText.plainText(welcome.title())
        return descriptionPages(welcome).map { page ->
            drawBox(header, page, look.key, look.settings, page.typingDuration(look.settings), fillLines = true)
        }
    }

    companion object {
        /** The name the server's config chooses this Display by. */
        const val NAME = "typewriter"
    }
}
