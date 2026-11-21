package com.mcmiddleearth.guidebook.typewriter

// SEAM: Page layout. How a Welcome's Description is cut into Pages and drawn as the box. Pure: no Typewriter or
// Bukkit, so its rules are tested without a server. The rules are the spec's "Page layout" module.
//
// STAND-IN: the whole Description on one Page, in a plain box.

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import java.time.Duration

/** How the box looks and types, from the settings on the Guidebook Welcome entry. */
data class LayoutSettings(
    val typingSpeed: Duration = Duration.ofMillis(20),
    val pageTypingCap: Duration = Duration.ofSeconds(4),
    val headerColor: TextColor = TextColor.color(0xE3C77D),
)

/** One screenful of a Welcome: its text, and what the key does on it. */
data class Page(val text: Component, val footer: String)

/** The Pages of a Welcome whose [description] is already rendered, under a header naming the Area. */
fun layOutPages(description: Component, header: String, settings: LayoutSettings): List<Page> =
    listOf(Page(description, "close"))

/**
 * The box as the player sees it [elapsed] into typing [page], with [key] being the key that turns or closes it.
 */
fun drawBox(header: String, page: Page, key: Component, settings: LayoutSettings, elapsed: Duration): Component =
    Component.text()
        .append(Component.text(header, settings.headerColor))
        .append(Component.newline())
        .append(page.text)
        .append(Component.newline())
        .append(Component.text("Press "))
        .append(key)
        .append(Component.text(" to ${page.footer}"))
        .build()
