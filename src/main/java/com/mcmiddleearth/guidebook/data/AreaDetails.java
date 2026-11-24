package com.mcmiddleearth.guidebook.data;

/**
 * What the Edit dialog changes about an Area, taken together so an edit is saved or left undone as a whole. The
 * Description is as stored.
 */
public record AreaDetails(
        boolean showTitle, String title, String subtitle, boolean showBossBar, String description, boolean enabled) {

    /** The details the Area has now. */
    public static AreaDetails of(InfoArea area) {
        return new AreaDetails(
                area.isShowTitle(),
                area.getTitle(),
                area.getSubtitle(),
                area.isShowBossBar(),
                area.getDescription(),
                area.isEnabled());
    }
}
