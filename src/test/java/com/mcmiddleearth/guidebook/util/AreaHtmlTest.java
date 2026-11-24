package com.mcmiddleearth.guidebook.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AreaHtmlTest {

    @Test
    void plainTextIsOneUnstyledSpan() {
        assertEquals("<span>Minas Tirith</span>", AreaHtml.of("Minas Tirith"));
    }

    @Test
    void coloursAndDecorationsAreInlineStyles() {
        assertEquals(
                "<span style=\"color:#FFAA00;font-weight:bold;font-style:italic;"
                        + "text-decoration:underline line-through\">Edoras</span>",
                AreaHtml.of("<gold><b><i><u><st>Edoras"));
    }

    @Test
    void anInnerStyleAddsToTheOneAroundIt() {
        assertEquals(
                "<span style=\"color:#55FF55\">Rohan </span>"
                        + "<span style=\"color:#55FF55;font-weight:bold\">Edoras</span>",
                AreaHtml.of("<green>Rohan <b>Edoras"));
    }

    @Test
    void hexColoursAreKept() {
        assertEquals("<span style=\"color:#FF8800\">Orange</span>", AreaHtml.of("<#FF8800>Orange"));
    }

    @Test
    void theGuideTagKeepsItsLabel() {
        assertEquals(
                "<span style=\"color:#00AAAA\">Guide: </span><span style=\"color:#FFFFFF\">The city</span>",
                AreaHtml.of("<guide>The city"));
    }

    @Test
    void lineBreaksBecomeBreakTags() {
        assertEquals("<span>One<br>Two</span>", AreaHtml.of("One\nTwo"));
    }

    @Test
    void hoversAndClicksAreLeftOut() {
        assertEquals(
                "<span style=\"color:#55FF55;text-decoration:underline\">here</span>",
                AreaHtml.of("<warp:edoras>here</warp>"));
    }

    @Test
    void textIsEscaped() {
        assertEquals("<span>a &lt; b &amp; c &quot;d&quot;</span>", AreaHtml.of("a \\< b & c \"d\""));
    }
}
