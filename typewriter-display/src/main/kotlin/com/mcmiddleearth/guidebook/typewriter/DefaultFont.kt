package com.mcmiddleearth.guidebook.typewriter

/**
 * Minecraft's default font, as far as laying out the box needs it: how wide each character is, with the 1px gap after it, from `glyph-widths.txt`. That file
 * is written from the font itself by `tools/glyph_widths.py`.
 */
internal object DefaultFont {

    private val widths: Map<Char, Int> = load()

    /** How wide [character] is, bold adding 1px. A character the table doesn't list counts as [UNLISTED_PX]. */
    fun width(character: Char, bold: Boolean): Int = (widths[character] ?: UNLISTED_PX) + if (bold) 1 else 0

    // The table lists only the Unifont characters Guidebook writes itself, so any other is counted as the widest a
    // Unifont glyph can be, and a line can't come out wider than measured
    private const val UNLISTED_PX = 9

    private fun load(): Map<Char, Int> {
        val text = DefaultFont::class.java.getResourceAsStream("glyph-widths.txt")!!.bufferedReader().use { it.readText() }
        return buildMap {
            for (line in text.lines()) {
                if (line.isBlank() || line.startsWith("#")) continue
                val fields = line.split(' ')
                val width = fields.first().toInt()
                for (code in fields.drop(1)) {
                    // A character beyond U+FFFF is two Chars, each counted as unlisted
                    code.toInt(16).takeIf { it <= Char.MAX_VALUE.code }?.let { put(it.toChar(), width) }
                }
            }
        }
    }
}
