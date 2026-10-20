package com.mcmiddleearth.guidebook.webmap;

/**
 * How a marker's outline is drawn.
 *
 * @param lineRgb a colour as {@code 0xRRGGBB}
 * @param fillOpacity from 0 (no fill) to 1 (solid)
 */
public record MarkerStyle(int lineRgb, int lineWeight, int fillRgb, double fillOpacity) {}
