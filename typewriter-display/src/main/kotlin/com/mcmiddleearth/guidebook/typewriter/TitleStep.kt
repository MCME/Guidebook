package com.mcmiddleearth.guidebook.typewriter

import com.mcmiddleearth.guidebook.util.AreaText
import com.mcmiddleearth.guidebook.welcome.Welcome
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import java.time.Duration

/** How the Title types in, from the settings on the Guidebook Welcome entry. Its defaults are the entry's. */
data class TitleSettings(
    val ticksPerCharacter: Int = 2,
    val subtitlePauseTicks: Int = 6,
    val descriptionDelayTicks: Int = 24,
)

/**
 * The on-screen part of opening one [welcome], before the box: the Title typed in, then the Subtitle. Nothing at all
 * when its Show title is off.
 */
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
        /** How long the last frame stays and fades. Fixed, as staff tune only how the Title types in. */
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
            val text = StyledText.of(title)

            // A space never gets a frame of its own, as it would look like a pause
            val typed = (1..text.length).filter { !text[it - 1].isWhitespace() }.map { text.slice(0, it) }
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
    }
}

/** How long [ticks] server ticks last when the server keeps up. */
internal fun ticks(ticks: Long): Duration = Duration.ofMillis(ticks * MILLIS_PER_TICK)

private const val MILLIS_PER_TICK = 50L
