/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.data.AreaGeometry;
import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.guidebook.util.AreaText;
import com.mcmiddleearth.guidebook.util.GuidebookMessages;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;

/**
 * @author Eriol_Eandur
 */
final class GuidebookList {

    private static final int PAGE_LENGTH = 10;

    private GuidebookList() {}

    /**
     * Sends one page of the Areas whose names contain {@code filter}, ignoring case, one line each. Hovering a name
     * shows the Area's Title, Shape and Enabled state, and Disabled names are grey. Each line ends with buttons that run
     * {@code edit}, {@code warp}, {@code select} and {@code delete} for its Area. The page arrows show the neighbouring
     * pages.
     *
     * @param filter the text to search for, or empty to list every Area
     * @param page the page to show, moved into range if there's no such page
     */
    static void send(CommandSender sender, String filter, int page) {
        String search = filter.toLowerCase(Locale.ROOT);
        List<InfoArea> areas = PluginData.getInfoAreas().values().stream()
                .filter(area -> area.getName().toLowerCase(Locale.ROOT).contains(search))
                .sorted(Comparator.comparing(InfoArea::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        if (areas.isEmpty()) {
            GuidebookMessages.sendInfo(
                    sender,
                    filter.isEmpty()
                            ? "There are no Guidebook areas."
                            : "No Guidebook area names contain '" + filter + "'.");
            return;
        }

        int maxPage = (areas.size() + PAGE_LENGTH - 1) / PAGE_LENGTH;
        int shownPage = Math.clamp(page, 1, maxPage);
        GuidebookMessages.startBlock(sender);
        GuidebookMessages.send(
                sender, GuidebookMessages.info("Guidebook areas [page " + shownPage + "/" + maxPage + "]"));
        if (shownPage > 1) {
            GuidebookMessages.send(sender, pageLink("---^ page up ^---", filter, shownPage - 1));
        }
        areas.stream()
                .skip((long) (shownPage - 1) * PAGE_LENGTH)
                .limit(PAGE_LENGTH)
                .forEach(area -> GuidebookMessages.send(sender, areaLine(area)));
        if (shownPage < maxPage) {
            GuidebookMessages.send(sender, pageLink("---v page down v---", filter, shownPage + 1));
        }
    }

    // ▸ <name>  ✎ W ⬚ 🗑, short-indented so long names have room before the line wraps
    private static Component areaLine(InfoArea area) {
        String name = area.getName();
        Component nameText = Component.text(name, area.isEnabled() ? NamedTextColor.BLUE : NamedTextColor.GRAY)
                .hoverEvent(HoverEvent.showText(nameHover(area)));
        return GuidebookMessages.infoShortIndented(
                "▸ ",
                nameText,
                "  ",
                button("✎", NamedTextColor.WHITE, "edit", name, "Edit this area"),
                " ",
                button("W", NamedTextColor.GREEN, "warp", name, "Warp to this area"),
                " ",
                button("⬚", NamedTextColor.AQUA, "select", name, "Set your WorldEdit selection to this area"),
                " ",
                button("🗑", NamedTextColor.RED, "delete", name, "Delete this area (asks to confirm)"));
    }

    // Clicking runs the command straight away; delete asks to confirm by itself
    private static Component button(String icon, TextColor color, String command, String name, String hover) {
        return GuidebookMessages.runsCommand(Component.text(icon, color), "/guidebook " + command + " " + name, hover);
    }

    // The Title as players see it, the Shape with its dimensions, and Enabled or Disabled
    private static Component nameHover(InfoArea area) {
        Component title = AreaText.isVisiblyBlank(area.getTitle())
                ? Component.text("(no Title)", NamedTextColor.GRAY)
                : AreaText.render(area.getTitle());
        return Component.text()
                .append(title)
                .append(Component.newline())
                .append(Component.text(shapeText(area), NamedTextColor.GRAY))
                .append(Component.newline())
                .append(
                        area.isEnabled()
                                ? Component.text("Enabled", NamedTextColor.GREEN)
                                : Component.text("Disabled", NamedTextColor.RED))
                .build();
    }

    // Sides are block-inclusive and given as X × Y × Z
    private static String shapeText(InfoArea area) {
        String shape =
                area.getShape().symbol() + " " + capitalised(area.getShape().displayName());
        return switch (area.getGeometry()) {
            case AreaGeometry.Sphere sphere -> shape + ", radius " + sphere.radius();
            case AreaGeometry.Cuboid cuboid ->
                shape + ", "
                        + sides(
                                cuboid.maxX() - cuboid.minX() + 1,
                                cuboid.maxY() - cuboid.minY() + 1,
                                cuboid.maxZ() - cuboid.minZ() + 1);
            case AreaGeometry.Prism prism ->
                shape + " with " + prism.xs().size() + " corners, "
                        + sides(
                                Collections.max(prism.xs()) - Collections.min(prism.xs()) + 1,
                                prism.maxY() - prism.minY() + 1,
                                Collections.max(prism.zs()) - Collections.min(prism.zs()) + 1);
        };
    }

    private static String sides(int x, int y, int z) {
        return x + " × " + y + " × " + z + " blocks";
    }

    private static String capitalised(String text) {
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }

    private static Component pageLink(String text, String filter, int page) {
        String command = "/guidebook list " + (filter.isEmpty() ? "" : filter + " ") + page;
        return GuidebookMessages.infoIndented(GuidebookMessages.runsCommand(
                Component.text(text, NamedTextColor.BLUE), command, "Click for page " + page + "."));
    }
}
