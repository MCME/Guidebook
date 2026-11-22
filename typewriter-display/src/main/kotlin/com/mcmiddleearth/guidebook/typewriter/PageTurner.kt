package com.mcmiddleearth.guidebook.typewriter

import java.time.Duration

/**
 * Which of one Welcome's [pages] the box shows and how far it's typed, as the player advances it. Kept apart from
 * Typewriter so tests can drive it tick by tick.
 */
class PageTurner(private val pages: List<Page>, private val settings: LayoutSettings) {

    /** The index of the Page shown. */
    var page = 0
        private set

    // Null until the Description starts, as the box isn't drawn before then
    private var pageOpenedAt: Duration? = null
    private var typingSkipped = false
    private var ticksSinceTyped: Int? = null
    private var now = Duration.ZERO

    /** The Page shown. */
    val shown: Page get() = pages[page]

    /** How far into typing the Page shown is. */
    val typingElapsed: Duration
        get() {
            if (typingSkipped) return shown.typingDuration(settings)
            return pageOpenedAt?.let { now - it } ?: Duration.ZERO
        }

    val isPageTyped: Boolean
        get() = typingSkipped || (pageOpenedAt != null && typingElapsed >= shown.typingDuration(settings))

    /** Whether the next advance closes the box: the last Page is typed out. */
    val mayClose: Boolean get() = page == pages.lastIndex && isPageTyped

    /**
     * Finishes typing the Page, or turns to the next one once it's typed. Does nothing before the Description starts,
     * so a player can't skip or close a box they haven't seen.
     */
    fun advance() {
        if (pageOpenedAt == null) return
        if (!isPageTyped) {
            typingSkipped = true
        } else if (page < pages.lastIndex) {
            page++
            pageOpenedAt = now
            typingSkipped = false
            ticksSinceTyped = null
        }
    }

    /**
     * Called every tick from when the Description starts, [now] into the Welcome: whether to send the box again. As
     * Typewriter's spoken dialogue does, it's every tick while typing, then twice per cycle, which also stops chat
     * fading the box out.
     */
    fun tick(now: Duration): Boolean {
        this.now = now
        if (pageOpenedAt == null) pageOpenedAt = now
        if (!isPageTyped) return true
        val ticks = ticksSinceTyped?.plus(1) ?: 0
        ticksSinceTyped = ticks
        return ticks % RESEND_CYCLE_TICKS == 0 || ticks % RESEND_CYCLE_TICKS == RESEND_SECOND_TICK
    }

    private companion object {
        const val RESEND_CYCLE_TICKS = 40
        const val RESEND_SECOND_TICK = 10
    }
}
