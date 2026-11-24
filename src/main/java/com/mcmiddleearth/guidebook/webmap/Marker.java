package com.mcmiddleearth.guidebook.webmap;

/**
 * One thing drawn on a Web map.
 *
 * @param label plain text, shown on hover
 * @param popup an HTML fragment, shown on click. It is styled inline, since a Web map lets a popup bring no stylesheet
 *     of its own, and the caller has escaped any text in it
 */
public record Marker(String id, String world, String label, String popup, Outline outline, MarkerStyle style) {}
