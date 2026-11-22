package com.mcmiddleearth.guidebook.typewriter

import com.mcmiddleearth.guidebook.welcome.Welcome
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.Title
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration

class TitleStepTest {

    private val bree = Welcome("me-bree-gate", "Bree", "The gate", "<guide>The gate of Bree.", true)

    /** A player's screen, keeping each Title shown on it with the tick it was shown on. */
    private class Screen : Audience {
        val shown = mutableListOf<Pair<Long, Title>>()
        var now = 0L

        override fun showTitle(title: Title) {
            shown += now to title
        }

        fun frames(): List<String> = shown.map { (tick, title) -> "$tick ${plain(title.title())}|${plain(title.subtitle())}" }
    }

    private fun play(step: TitleStep, ticks: Long): Screen {
        val screen = Screen()
        for (tick in 0..ticks) {
            screen.now = tick
            step.tick(screen, ticks(tick))
        }
        return screen
    }

    private fun ticks(ticks: Long): Duration = Duration.ofMillis(ticks * 50)

    @Test
    fun `the Title types in a character at a time, then the Subtitle appears after the pause`() {
        val screen = play(TitleStep(bree, TitleSettings(ticksPerCharacter = 2, subtitlePauseTicks = 6)), 40)

        assertEquals(listOf("0 B|", "2 Br|", "4 Bre|", "6 Bree|", "12 Bree|The gate"), screen.frames())
    }

    @Test
    fun `the Description starts its delay after the Subtitle appears`() {
        val step = TitleStep(bree, TitleSettings(ticksPerCharacter = 2, subtitlePauseTicks = 6, descriptionDelayTicks = 24))

        assertEquals(ticks(12 + 24), step.descriptionDelay)
    }

    @Test
    fun `a space gets no frame of its own, so it doesn't look like a pause`() {
        val gate = bree.copy(title = "Bree Gate")
        val screen = play(TitleStep(gate, TitleSettings(ticksPerCharacter = 1, subtitlePauseTicks = 0)), 20)

        assertEquals(
            listOf("0 B|", "1 Br|", "2 Bre|", "3 Bree|", "4 Bree G|", "5 Bree Ga|", "6 Bree Gat|", "7 Bree Gate|The gate"),
            screen.frames(),
        )
    }

    @Test
    fun `missed ticks jump to the frame due rather than replaying the ones between`() {
        val step = TitleStep(bree, TitleSettings(ticksPerCharacter = 2, subtitlePauseTicks = 6))
        val screen = Screen()

        step.tick(screen, ticks(0))
        step.tick(screen, ticks(5))

        assertEquals(listOf("0 B|", "0 Bre|"), screen.frames())
    }

    @Test
    fun `the Title keeps its styling as it types in`() {
        val gold = bree.copy(title = "<gold>Bree")
        val screen = play(TitleStep(gold, TitleSettings()), 0)

        assertEquals("<gold>B", MiniMessage.miniMessage().serialize(screen.shown.single().second.title()))
    }

    @Test
    fun `no frame fades in, and the last stays 40 ticks and fades over 10`() {
        val screen = play(TitleStep(bree, TitleSettings(ticksPerCharacter = 2, subtitlePauseTicks = 6)), 40)

        assertEquals(listOf(Duration.ZERO), screen.shown.map { it.second.times()!!.fadeIn() }.distinct())
        val last = screen.shown.last().second.times()!!
        assertEquals(ticks(40), last.stay())
        assertEquals(ticks(10), last.fadeOut())
    }

    @Test
    fun `each frame before the last stays until the next is shown`() {
        val screen = play(TitleStep(bree, TitleSettings(ticksPerCharacter = 2, subtitlePauseTicks = 6)), 40)

        screen.shown.zipWithNext().forEach { (frame, next) ->
            val times = frame.second.times()!!
            assert(times.stay() >= ticks(next.first - frame.first)) { "frame at ${frame.first} is gone before ${next.first}" }
            assertEquals(Duration.ZERO, times.fadeOut())
        }
    }

    @Test
    fun `with Show title off nothing is shown and the Description starts at once`() {
        val hidden = bree.copy(showTitle = false)
        val step = TitleStep(hidden, TitleSettings())

        assertEquals(Duration.ZERO, step.descriptionDelay)
        assertEquals(emptyList<String>(), play(step, 40).frames())
    }

    private fun Welcome.copy(title: String = title(), showTitle: Boolean = showTitle()) =
        Welcome(areaName(), title, subtitle(), description(), showTitle)
}
