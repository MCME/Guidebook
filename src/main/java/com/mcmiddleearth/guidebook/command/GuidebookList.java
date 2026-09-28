/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.guidebook.command;

import com.mcmiddleearth.guidebook.data.InfoArea;
import com.mcmiddleearth.guidebook.data.PluginData;
import com.mcmiddleearth.guidebook.util.GuidebookMessages;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

/**
 * @author Eriol_Eandur
 */
final class GuidebookList {

    private static final int PAGE_LENGTH = 10;

    private GuidebookList() {}

    /**
     * Sends one page of the Area names containing {@code filter}, ignoring case. Clicking a name fills in its
     * {@code details} command, and the page arrows show the neighbouring pages.
     *
     * @param filter the text to search for, or empty to list every Area
     * @param page the page to show, moved into range if there's no such page
     */
    static void send(CommandSender sender, String filter, int page) {
        String search = filter.toLowerCase(Locale.ROOT);
        List<String> names = PluginData.getInfoAreas().values().stream()
                .map(InfoArea::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).contains(search))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        if (names.isEmpty()) {
            PluginData.getMessageUtil()
                    .sendInfoMessage(
                            sender,
                            filter.isEmpty()
                                    ? "There are no Guidebook areas."
                                    : "No Guidebook area names contain '" + filter + "'.");
            return;
        }

        int maxPage = (names.size() + PAGE_LENGTH - 1) / PAGE_LENGTH;
        int shownPage = Math.clamp(page, 1, maxPage);
        GuidebookMessages.send(
                sender,
                GuidebookMessages.info(Component.text(
                        "Guidebook areas (click for details) [page " + shownPage + "/" + maxPage + "]")));
        if (shownPage > 1) {
            GuidebookMessages.send(sender, pageLink("---^ page up ^---", filter, shownPage - 1));
        }
        names.stream()
                .skip((long) (shownPage - 1) * PAGE_LENGTH)
                .limit(PAGE_LENGTH)
                .forEach(name -> GuidebookMessages.send(sender, areaLine(name)));
        if (shownPage < maxPage) {
            GuidebookMessages.send(sender, pageLink("---v page down v---", filter, shownPage + 1));
        }
    }

    private static Component areaLine(String name) {
        return GuidebookMessages.infoIndented(
                Component.text("- "),
                GuidebookMessages.suggestsCommand(
                        Component.text(name, NamedTextColor.BLUE), "/guidebook details " + name, "Click for details."),
                Component.text("."));
    }

    private static Component pageLink(String text, String filter, int page) {
        String command = "/guidebook list " + (filter.isEmpty() ? "" : filter + " ") + page;
        return GuidebookMessages.infoIndented(GuidebookMessages.runsCommand(
                Component.text(text, NamedTextColor.BLUE), command, "Click for page " + page + "."));
    }
}
