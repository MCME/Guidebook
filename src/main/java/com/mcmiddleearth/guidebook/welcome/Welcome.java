package com.mcmiddleearth.guidebook.welcome;

/**
 * An Area's name and text, copied from the Area when a Welcome starts.
 *
 * <p>The Title, Subtitle and Description are all MiniMessage with Guidebook's tags. Render them with
 * {@link com.mcmiddleearth.guidebook.util.AreaText#render}.
 *
 * @param areaName the Area's name
 * @param showTitle whether to show the Title and Subtitle (the Area's Show title)
 */
public record Welcome(String areaName, String title, String subtitle, String description, boolean showTitle) {}
