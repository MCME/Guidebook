package com.mcmiddleearth.guidebook.text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.flattener.FlattenerListener;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * An Area's text as HTML, in the colours and styles a player sees. Styles are inline, so the HTML needs no stylesheet.
 * Hovers and clicks are left out: a browser can't run them, and a tooltip standing in for them didn't show on
 * LiveAtlas.
 */
public final class AreaHtml {

    private AreaHtml() {}

    /** The text rendered with {@link AreaText}'s rules, as HTML with each line break a {@code <br>}. */
    public static String of(String miniMessage) {
        StringBuilder html = new StringBuilder();
        ComponentFlattener.basic().flatten(AreaText.render(miniMessage), new FlattenerListener() {
            // Each component's own style, merged onto those of the components it sits inside
            private final Deque<Style> styles = new ArrayDeque<>(List.of(Style.empty()));

            @Override
            public void pushStyle(Style style) {
                styles.push(styles.getFirst().merge(style));
            }

            @Override
            public void popStyle(Style style) {
                styles.pop();
            }

            @Override
            public void component(String text) {
                if (!text.isEmpty()) {
                    html.append(styledText(text, styles.getFirst()));
                }
            }
        });
        return html.toString();
    }

    private static String styledText(String text, Style style) {
        List<String> css = new ArrayList<>();
        if (style.color() != null) {
            css.add("color:" + style.color().asHexString());
        }
        if (has(style, TextDecoration.BOLD)) {
            css.add("font-weight:bold");
        }
        if (has(style, TextDecoration.ITALIC)) {
            css.add("font-style:italic");
        }
        List<String> lines = new ArrayList<>();
        if (has(style, TextDecoration.UNDERLINED)) {
            lines.add("underline");
        }
        if (has(style, TextDecoration.STRIKETHROUGH)) {
            lines.add("line-through");
        }
        if (!lines.isEmpty()) {
            css.add("text-decoration:" + String.join(" ", lines));
        }
        return span(String.join(";", css), escape(text).replace("\n", "<br>"));
    }

    /**
     * @param css inline CSS, or empty for none
     * @param html HTML whose text is already escaped
     * @return the HTML in a span styled with the CSS
     */
    public static String span(String css, String html) {
        return (css.isEmpty() ? "<span>" : "<span style=\"" + css + "\">") + html + "</span>";
    }

    private static boolean has(Style style, TextDecoration decoration) {
        return style.decoration(decoration) == TextDecoration.State.TRUE;
    }

    /** Text made safe to put in HTML, as content or as a quoted attribute. */
    public static String escape(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
