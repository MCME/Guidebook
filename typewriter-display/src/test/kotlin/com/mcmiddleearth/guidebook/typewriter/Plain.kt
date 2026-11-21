package com.mcmiddleearth.guidebook.typewriter

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

/** The text a player reads, without its styling. */
fun plain(component: Component): String = PlainTextComponentSerializer.plainText().serialize(component)
