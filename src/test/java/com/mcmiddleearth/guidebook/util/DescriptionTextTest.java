package com.mcmiddleearth.guidebook.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class DescriptionTextTest {

    @Test
    void storedLineBreakMarkersShowAsLineBreaks() {
        assertEquals("testing\nto see\n\nworks", DescriptionText.toTyped(List.of("testing\\nto see\\n\\nworks")));
    }

    @Test
    void storedLinesWithoutAMarkerRunTogether_becauseThatIsHowTheyAreSent() {
        assertEquals("Welcome to Edoras.", DescriptionText.toTyped(List.of("Welcome to ", "Edoras.")));
    }

    @Test
    void colourCodesShowAsHashes() {
        assertEquals("#3Guide: #fwhite", DescriptionText.toTyped(List.of("§3Guide: §fwhite")));
    }

    @Test
    void aMissingStoredLineIsSkipped() {
        assertEquals("Guide", DescriptionText.toTyped(Arrays.asList("Guide", null)));
    }

    @Test
    void eachTypedLineIsStoredEndingInALineBreakMarker_exceptTheLast() {
        assertEquals(List.of("first\\n", "\\n", "third"), DescriptionText.toStored("first\n\nthird"));
    }

    @Test
    void typedHashesAreStoredAsColourCodes() {
        assertEquals(List.of("§3Guide:"), DescriptionText.toStored("#3Guide:"));
    }

    @Test
    void trailingBlankLinesAreDropped() {
        assertEquals(List.of("text"), DescriptionText.toStored("text\n\n  \n"));
    }

    @Test
    void anEmptyDescriptionIsStoredAsNoLines() {
        assertEquals(List.of(), DescriptionText.toStored(""));
    }

    @Test
    void windowsLineEndingsCountAsOneBreak() {
        assertEquals(List.of("a\\n", "b"), DescriptionText.toStored("a\r\nb"));
    }

    @Test
    void whatIsTypedSurvivesARoundTrip() {
        String typed = "#3Guide:\nline two\n\nafter a blank line";

        assertEquals(typed, DescriptionText.toTyped(DescriptionText.toStored(typed)));
    }
}
