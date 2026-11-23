package com.mcmiddleearth.guidebook.typewriter

import com.mcmiddleearth.guidebook.text.AreaText
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

/**
 * Whether a Description shows the player nothing once rendered as the box renders it, such as one of only tags and
 * spaces, or only the `<guide>` opening, whose label the box leaves out. A Welcome with one gets no box, so no player
 * is asked to close an empty one. Hidden text, such as a hover's tooltip, doesn't count as visible.
 */
fun isEmptyDescription(description: String): Boolean =
    PlainTextComponentSerializer.plainText().serialize(AreaText.renderWithoutGuideLabel(description)).isBlank()
