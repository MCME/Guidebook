package com.mcmiddleearth.guidebook.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AreaRegistryTest {

    private static InfoArea area(String name) {
        return area(name, "Title of " + name, true, Shape.CUBOID);
    }

    private static InfoArea area(String name, String title, boolean enabled, Shape shape) {
        AreaGeometry geometry =
                switch (shape) {
                    case SPHERE -> new AreaGeometry.Sphere(0, 64, 0, 10);
                    case CUBOID -> new AreaGeometry.Cuboid(0, 60, 0, 10, 70, 10);
                    case PRISM -> new AreaGeometry.Prism(List.of(0, 10, 0), List.of(0, 0, 10), 60, 70);
                };
        return TestAreas.area(name, "world", geometry, new AreaDetails(false, title, "", false, "", enabled));
    }

    private static AreaRegistry registryOf(InfoArea... areas) {
        return new AreaRegistry(List.of(areas));
    }

    @Test
    void resolvesANameIgnoringCase() {
        InfoArea minas = area("minas");
        AreaRegistry registry = registryOf(area("edoras"), minas);

        assertEquals(minas, registry.resolve("Minas").orElseThrow());
    }

    @Test
    void anExactCaseMatchWinsOverAMatchIgnoringCase() {
        InfoArea lower = area("minas");
        InfoArea upper = area("Minas");

        assertEquals(upper, registryOf(lower, upper).resolve("Minas").orElseThrow());
        assertEquals(lower, registryOf(upper, lower).resolve("minas").orElseThrow());
    }

    @Test
    void anUnknownNameIsNotFound() {
        assertTrue(registryOf(area("minas")).resolve("edoras").isEmpty());
    }

    @Test
    void resolvingExactlyFindsOnlyTheAreaWithThatExactName() {
        InfoArea lower = area("minas");
        InfoArea upper = area("Minas");

        assertEquals(upper, registryOf(lower, upper).resolveExact("Minas").orElseThrow());
        assertTrue(registryOf(lower).resolveExact("MINAS").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"helms-deep", "Minas_Tirith", "v1.2+old", "0"})
    void acceptsNamesMadeOfLettersDigitsAndUnderscoreHyphenDotPlus(String name) {
        assertTrue(AreaRegistry.isValidName(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {"helm's-deep", "helms deep", "", "minas/tirith", "caf\u00e9"})
    void rejectsNamesWithOtherCharacters(String name) {
        assertFalse(AreaRegistry.isValidName(name));
    }

    @Test
    void aNameInUseIgnoringCaseIsNotAvailable() {
        AreaRegistry registry = registryOf(area("minas"), area("edoras"));

        assertFalse(registry.isAvailable("minas"));
        assertFalse(registry.isAvailable("MINAS"));
    }

    @Test
    void anUnusedNameIsAvailable() {
        assertTrue(registryOf(area("minas")).isAvailable("minas-tirith"));
    }

    @Test
    void aNewNameIsUsableWhenValidAndUnused() {
        assertEquals(Optional.empty(), registryOf(area("minas")).newNameProblem("minas-tirith"));
    }

    @Test
    void aNewNameWithInvalidCharactersIsRefusedNamingTheAllowedCharacters() {
        assertEquals(
                Optional.of("'helm's-deep' isn't a valid Area name. Use only letters, digits and _ - . +"),
                registryOf().newNameProblem("helm's-deep"));
    }

    @Test
    void aNewNameInUseIgnoringCaseIsRefusedNamingTheAreaThatHasIt() {
        assertEquals(
                Optional.of("Area minas already has that name"),
                registryOf(area("minas")).newNameProblem("MINAS"));
    }

    private static Set<String> suggestedNames(AreaRegistry registry, String typed) {
        return registry.suggest(typed).stream()
                .map(AreaRegistry.Suggestion::name)
                .collect(Collectors.toSet());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "world", "world-gondor-minas", "gondor", "gondor-mi", "minas", "MINAS", "m"})
    void suggestsANameWhenAnyHyphenSegmentStartsWithTheTypedText(String typed) {
        AreaRegistry registry = registryOf(area("world-gondor-minas"));

        assertEquals(Set.of("world-gondor-minas"), suggestedNames(registry, typed));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ondor", "inas", "gondor-minas-x", "rohan"})
    void doesNotSuggestANameWhenTheTypedTextStartsMidSegment(String typed) {
        AreaRegistry registry = registryOf(area("world-gondor-minas"));

        assertEquals(Set.of(), suggestedNames(registry, typed));
    }

    @Test
    void suggestsOnlyTheMatchingNames() {
        AreaRegistry registry =
                registryOf(area("world-gondor-minas"), area("world-rohan-edoras"), area("world-gondor-osgiliath"));

        assertEquals(Set.of("world-gondor-minas", "world-gondor-osgiliath"), suggestedNames(registry, "gondor"));
    }

    @Test
    void aSuggestionCarriesTheAreasTitleAndShape() {
        AreaRegistry registry = registryOf(area("world-gondor-minas", "Minas Tirith", true, Shape.PRISM));

        assertEquals(
                List.of(new AreaRegistry.Suggestion("world-gondor-minas", "Minas Tirith", Shape.PRISM, false)),
                registry.suggest("minas"));
    }

    @Test
    void aDisabledAreaIsStillSuggestedAndMarkedDisabled() {
        AreaRegistry registry = registryOf(area("world-gondor-minas", "Minas Tirith", false, Shape.SPHERE));

        assertEquals(
                List.of(new AreaRegistry.Suggestion("world-gondor-minas", "Minas Tirith", Shape.SPHERE, true)),
                registry.suggest("minas"));
    }
}
