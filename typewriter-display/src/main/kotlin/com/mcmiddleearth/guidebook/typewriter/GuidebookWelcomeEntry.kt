package com.mcmiddleearth.guidebook.typewriter

import com.mcmiddleearth.guidebook.util.AreaText
import com.mcmiddleearth.guidebook.welcome.Welcome
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.interaction.GlobalContextKey
import com.typewritermc.core.interaction.InteractionContext
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.Modifier
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.dialogue.DialogueMessenger
import com.typewritermc.engine.paper.entry.dialogue.MessengerState
import com.typewritermc.engine.paper.entry.dialogue.TickContext
import com.typewritermc.engine.paper.entry.entries.ConstVar
import com.typewritermc.engine.paper.entry.entries.DialogueEntry
import com.typewritermc.engine.paper.entry.entries.SpeakerEntry
import com.typewritermc.engine.paper.entry.entries.Var
import com.typewritermc.engine.paper.interaction.Confirmation
import com.typewritermc.engine.paper.interaction.awaitConfirmation
import com.typewritermc.engine.paper.interaction.chatHistory
import com.typewritermc.engine.paper.utils.asMiniWithResolvers
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.time.Duration

/** Carries the Welcome from the Display to the Guidebook Welcome entry it triggers. */
object WelcomeKey : GlobalContextKey<Welcome>(Welcome::class) {
    override fun toString(): String = "GuidebookWelcome"
}

/**
 * The Guidebook Welcome entry: the dialogue every Welcome is shown through, holding the Typewriter display's settings
 * so staff can tune them in Typewriter's panel. It lives in the Typewriter page shipped with the extension.
 */
@Entry(
    "guidebook_welcome",
    "Shows a Guidebook Welcome in a box over the chat",
    Colors.BLUE,
    "mdi:book-open-page-variant",
)
class GuidebookWelcomeEntry(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    override val speaker: Ref<SpeakerEntry> = emptyRef(),
    @Help("How long each character of the Description takes to type; spaces take no time")
    val typingSpeed: Var<Duration> = ConstVar(Duration.ofMillis(20)),
    @Help("The most time typing one Page may take, so long Pages don't drag")
    val pageTypingCap: Var<Duration> = ConstVar(Duration.ofSeconds(4)),
    @Help("Colour of the Area name in the box's top border, the same for every Area, as hex (#e3c77d)")
    val headerColor: String = "#e3c77d",
    @Help("Ticks between each character as the Title types in")
    val titleTicksPerCharacter: Int = 2,
    @Help("Ticks the finished Title is held before the Subtitle appears")
    val titleSubtitlePauseTicks: Int = 6,
    @Help("Ticks after the Subtitle appears before the Description starts")
    val descriptionDelayTicks: Int = 24,
) : DialogueEntry {

    override fun messenger(player: Player, context: InteractionContext): DialogueMessenger<*>? {
        val welcome = context[WelcomeKey] ?: return null
        return GuidebookWelcomeMessenger(player, context, this, welcome)
    }

    fun layoutSettings(player: Player) = LayoutSettings(
        typingSpeed = typingSpeed.get(player),
        pageTypingCap = pageTypingCap.get(player),
        headerColor = TextColor.fromHexString(headerColor) ?: LayoutSettings().headerColor,
    )

    fun titleSettings() = TitleSettings(titleTicksPerCharacter, titleSubtitlePauseTicks, descriptionDelayTicks)

    companion object {
        /** The entry's id in the shipped Typewriter page. It never changes, so staff's copies keep working. */
        const val ID = "guidebookWelcome"
    }
}

/** Types one Welcome into the box: the Title step, then each Page, turned and closed with the confirmation key. */
class GuidebookWelcomeMessenger(
    player: Player,
    context: InteractionContext,
    entry: GuidebookWelcomeEntry,
    private val welcome: Welcome,
) : DialogueMessenger<GuidebookWelcomeEntry>(player, context, entry) {

    private val settings = entry.layoutSettings(player)
    private val header = AreaText.plainText(welcome.title())
    private val pages = layOutPages(AreaText.render(welcome.description()))
    private val titleStep = TitleStep(welcome, entry.titleSettings())

    private var confirmation: Confirmation? = null
    private var page = 0
    private var pageOpenedAt: Duration? = null
    private var typingSkipped = false
    private var ticksSinceTyped: Int? = null
    private var now = Duration.ZERO
    private var closedByPlayer = false
    private var ended = false

    // Typewriter asks this before ending the dialogue on any advance, and sets it instead while it's false, so every
    // advance acts like the key: the box only closes once the last Page is typed
    override var animationComplete: Boolean
        get() = page == pages.lastIndex && isPageTyped()
        set(value) {
            if (value && !animationComplete) advance()
        }

    override fun init() {
        super.init()
        confirmation = player.awaitConfirmation {
            if (animationComplete) {
                closedByPlayer = true
                state = MessengerState.FINISHED
            } else {
                advance()
            }
        }
    }

    /** Finishes typing the Page, or turns to the next one once it's typed. */
    private fun advance() {
        if (!isPageTyped()) {
            typingSkipped = true
        } else if (page < pages.lastIndex) {
            page++
            pageOpenedAt = null
            typingSkipped = false
            ticksSinceTyped = null
        }
    }

    private fun typingElapsed(): Duration {
        if (typingSkipped) return pages[page].typingDuration(settings)
        return pageOpenedAt?.let { now - it } ?: Duration.ZERO
    }

    private fun isPageTyped(): Boolean =
        typingSkipped || (pageOpenedAt != null && typingElapsed() >= pages[page].typingDuration(settings))

    override fun tick(context: TickContext) {
        if (state != MessengerState.RUNNING) return
        titleStep.tick(player, context.playTime)
        if (context.playTime < titleStep.descriptionDelay) return
        now = context.playTime
        if (pageOpenedAt == null) pageOpenedAt = now

        // As Typewriter's spoken dialogue does: every tick while typing, then twice per cycle, which also stops chat
        // fading the box out
        if (isPageTyped()) {
            val ticks = ticksSinceTyped?.plus(1) ?: 0
            ticksSinceTyped = ticks
            if (ticks % RESEND_CYCLE_TICKS != 0 && ticks % RESEND_CYCLE_TICKS != RESEND_SECOND_TICK) return
        }

        val key = "<confirmation_key>".asMiniWithResolvers(player)
        val box = drawBox(header, pages[page], key, settings, typingElapsed())
        // Clears the chat, with the last few messages darkened above the box
        player.sendMessage(player.chatHistory.composeDarkMessage(box))
    }

    override fun end() {
        ended = true
        super.end()
    }

    override fun dispose() {
        super.dispose()
        confirmation?.dispose()
        confirmation = null
        // Typewriter restores the chat only when the dialogue ends. When another dialogue replaces this box (a new
        // Welcome), only dispose runs, and the old box would stay up through the new Title
        if (!ended) player.chatHistory.resendMessages(player)
        val queue = WelcomeQueues.find(player.uniqueId) ?: return
        if (closedByPlayer) {
            queue.closedByPlayer(welcome, Bukkit.getCurrentTick())?.let { TypewriterGlue.start(player, it) }
        } else {
            queue.endedOtherwise(welcome)
        }
    }

    private companion object {
        const val RESEND_CYCLE_TICKS = 40
        const val RESEND_SECOND_TICK = 10
    }
}
