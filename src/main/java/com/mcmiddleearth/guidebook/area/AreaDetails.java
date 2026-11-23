package com.mcmiddleearth.guidebook.area;

/**
 * What the Edit dialog changes about an Area, taken together so an edit is saved or left undone as a whole. The
 * Description is as stored.
 */
public record AreaDetails(
        boolean showTitle, String title, String subtitle, boolean showBossBar, String description, boolean enabled) {}
