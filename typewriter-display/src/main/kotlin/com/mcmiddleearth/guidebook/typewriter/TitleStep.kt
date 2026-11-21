package com.mcmiddleearth.guidebook.typewriter

// SEAM: Title step. What a Welcome shows on screen when it opens, before the box: the Title typed in, then the
// Subtitle. The rules are the Title timings in the spec's "dialogue entry and its messenger" module.
//
// STAND-IN: shows nothing, and the Description starts at once.

import com.mcmiddleearth.guidebook.welcome.Welcome
import net.kyori.adventure.audience.Audience
import java.time.Duration

/** How the Title types in, from the settings on the Guidebook Welcome entry. */
data class TitleSettings(
    val ticksPerCharacter: Int = 2,
    val subtitlePauseTicks: Int = 6,
    val descriptionDelayTicks: Int = 24,
)

/** The on-screen part of opening one [welcome]. */
class TitleStep(private val welcome: Welcome, private val settings: TitleSettings) {

    /** How long the Description waits after the Welcome opens, so the Title can be read first. */
    val descriptionDelay: Duration = Duration.ZERO

    /** Called every tick from when the Welcome opens, [elapsed] in, to show [viewer] what's due by then. */
    fun tick(viewer: Audience, elapsed: Duration) {}
}
