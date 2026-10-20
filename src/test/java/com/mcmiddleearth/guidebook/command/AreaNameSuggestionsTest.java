package com.mcmiddleearth.guidebook.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class AreaNameSuggestionsTest {

    private static final List<String> WORLDS = List.of("moria", "gondor", "world_nether");
    private static final List<String> AREAS =
            List.of("moria-dwarrowdelf-gate", "moria-dwarrowdelf-hall", "moria-khazad-bridge", "gondor-minas-gate");

    private static List<String> suggest(String typed) {
        return AreaNameSuggestions.suggest(typed, WORLDS, AREAS);
    }

    @Test
    void nothingTypedSuggestsEveryWorldAndEveryProject() {
        assertEquals(
                List.of("gondor-", "gondor-minas-", "moria-", "moria-dwarrowdelf-", "moria-khazad-", "world_nether-"),
                suggest(""));
    }

    @Test
    void thePartOfAWorldNameNarrowsToThatWorldAndItsProjects() {
        assertEquals(List.of("moria-", "moria-dwarrowdelf-", "moria-khazad-"), suggest("mo"));
    }

    // Tab only cycles through the suggestions the client already has, so the projects must come with the world
    @Test
    void theWorldAndItsProjectsComeTogetherSoTabCanReachTheProjects() {
        assertEquals(List.of("gondor-", "gondor-minas-"), suggest("gon"));
    }

    @Test
    void worldsMatchIgnoringCase() {
        assertEquals(List.of("moria-", "moria-dwarrowdelf-", "moria-khazad-"), suggest("MO"));
    }

    @Test
    void aWholeWorldSuggestsTheProjectsItsAreasUse() {
        assertEquals(List.of("moria-dwarrowdelf-", "moria-khazad-"), suggest("moria-"));
    }

    @Test
    void thePartOfAProjectNameNarrowsTheProjects() {
        assertEquals(List.of("moria-khazad-"), suggest("moria-kh"));
    }

    @Test
    void aWorldWithNoAreasHasNoProjectsToSuggest() {
        assertEquals(List.of(), suggest("world_nether-"));
    }

    @Test
    void aWholeProjectSuggestsNothingSinceTheExistingNamesAreTaken() {
        assertEquals(List.of(), suggest("moria-dwarrowdelf-"));
    }

    @Test
    void aNameNotFollowingTheConventionGivesNoProject() {
        assertEquals(
                List.of("moria-dwarrowdelf-"),
                AreaNameSuggestions.suggest(
                        "moria-", WORLDS, List.of("moria-dwarrowdelf-gate", "moria", "moria-hall")));
    }

    @Test
    void aWorldWhoseNameHasAHyphenKeepsItsWholeName() {
        assertEquals(
                List.of("old-moria-gate-"),
                AreaNameSuggestions.suggest("old-moria-", List.of("old-moria"), List.of("old-moria-gate-west")));
    }
}
