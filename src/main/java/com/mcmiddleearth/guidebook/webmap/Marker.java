package com.mcmiddleearth.guidebook.webmap;

/**
 * One thing drawn on a Web map.
 *
 * @param label plain text, shown on hover
 * @param popup plain text with lines separated by {@code \n}, shown on click. Each Web map escapes it for its own
 *     format
 */
public record Marker(String id, String world, String label, String popup, Outline outline, MarkerStyle style) {}
