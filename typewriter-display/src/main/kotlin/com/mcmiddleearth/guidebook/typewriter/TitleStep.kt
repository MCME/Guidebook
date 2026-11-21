package com.mcmiddleearth.guidebook.typewriter

// SEAM: Title step. What a Welcome shows on screen when it opens, before the box: the Title typed in, then the
// Subtitle. The rules are the Title timings in the spec's "dialogue entry and its messenger" module.

import com.mcmiddleearth.guidebook.util.AreaText
import com.mcmiddleearth.guidebook.welcome.Welcome
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.title.Title
import java.time.Duration

/** How the Title types in, from the settings on the Guidebook Welcome entry. */
data class TitleSettings(
    val ticksPerCharacter: Int = 2,
    val subtitlePauseTicks: Int = 6,
    val descriptionDelayTicks: Int = 24,
)

/** The on-screen part of opening one [welcome]: nothing at all when its Show title is off. */
class TitleStep(welcome: Welcome, settings: TitleSettings) {

    private val frames: List<Frame> = if (welcome.showTitle()) frames(welcome, settings) else emptyList()
    private var shown = 0

    /** How long the Description waits after the Welcome opens, so the Title can be read first. */
    val descriptionDelay: Duration =
        frames.lastOrNull()?.let { ticks(it.atTick + settings.descriptionDelayTicks.coerceAtLeast(0)) } ?: Duration.ZERO

    /**
     * Called every tick from when the Welcome opens, [elapsed] in, to show [viewer] what's due by then. Missed ticks
     * jump straight to the frame due, rather than replaying the ones in between.
     */
    fun tick(viewer: Audience, elapsed: Duration) {
        val due = frames.count { ticks(it.atTick) <= elapsed }
        if (due <= shown) return
        shown = due
        viewer.showTitle(frames[due - 1].title)
    }

    /** One Title sent to the screen, [atTick] ticks after the Welcome opens. */
    private class Frame(val atTick: Long, val title: Title)

    private companion object {
        /** How long the last frame stays and fades. Not settings: only the typing-in is tuned (spec). */
        const val LAST_FRAME_STAY_TICKS = 40L
        const val LAST_FRAME_FADE_TICKS = 10L

        /** How long a frame outlasts the time until the next, so it never blinks out between the two. */
        const val FRAME_OVERLAP_TICKS = 5L

        /**
         * The Title one visible character more each frame, held, then with the Subtitle. No frame fades in, as each
         * one would restart the fade and it would never finish.
         */
        fun frames(welcome: Welcome, settings: TitleSettings): List<Frame> {
            val perCharacter = settings.ticksPerCharacter.coerceAtLeast(1).toLong()
            val title = AreaText.render(welcome.title())
            val runs = flatten(title)
            val text = runs.joinToString("") { it.text }

            // A space never gets a frame of its own, as it would look like a pause
            val typed = (1..text.length).filter { !text[it - 1].isWhitespace() }.map { slice(runs, it) }
            val subtitleAt = (typed.size - 1).coerceAtLeast(0) * perCharacter + settings.subtitlePauseTicks.coerceAtLeast(0)

            val frames = typed.mapIndexed { i, shown ->
                val atTick = i * perCharacter
                val next = if (i < typed.lastIndex) atTick + perCharacter else subtitleAt
                Frame(atTick, Title.title(shown, Component.empty(), times(next - atTick + FRAME_OVERLAP_TICKS, 0)))
            }
            val last = Frame(
                subtitleAt,
                Title.title(title, AreaText.render(welcome.subtitle()), times(LAST_FRAME_STAY_TICKS, LAST_FRAME_FADE_TICKS)),
            )
            return frames + last
        }

        fun times(stayTicks: Long, fadeOutTicks: Long): Title.Times =
            Title.Times.times(Duration.ZERO, ticks(stayTicks), ticks(fadeOutTicks))

        /** A run of the Title's text in one style, so it can be cut at any character and keep its styling. */
        class Run(val text: String, val style: Style)

        fun flatten(component: Component, parent: Style = Style.empty(), out: MutableList<Run> = mutableListOf()): List<Run> {
            val style = component.style().merge(parent, Style.Merge.Strategy.IF_ABSENT_ON_TARGET)
            if (component is TextComponent && component.content().isNotEmpty()) out += Run(component.content(), style)
            component.children().forEach { flatten(it, style, out) }
            return out
        }

        /** The first [length] characters of [runs], each in its own style. */
        fun slice(runs: List<Run>, length: Int): Component {
            val builder = Component.text()
            var offset = 0
            for (run in runs) {
                val end = (length - offset).coerceIn(0, run.text.length)
                if (end > 0) builder.append(Component.text(run.text.substring(0, end), run.style))
                offset += run.text.length
            }
            return builder.build()
        }
    }
}

private fun ticks(ticks: Long): Duration = Duration.ofMillis(ticks * 50)
