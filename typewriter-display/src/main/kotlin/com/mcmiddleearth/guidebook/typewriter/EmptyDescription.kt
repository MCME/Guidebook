package com.mcmiddleearth.guidebook.typewriter

import com.mcmiddleearth.guidebook.util.AreaText

/**
 * Whether a Description shows the player nothing once rendered, such as one of only tags and spaces. A Welcome with
 * one gets no box (spec decision 32). Hidden text, such as a hover's tooltip, doesn't count as visible.
 */
fun isEmptyDescription(description: String): Boolean = AreaText.isVisiblyBlank(description)
