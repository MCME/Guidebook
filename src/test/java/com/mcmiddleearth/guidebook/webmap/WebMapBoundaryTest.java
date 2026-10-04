package com.mcmiddleearth.guidebook.webmap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** The Web map API is to move into a shared library, so it mustn't depend on the rest of Guidebook (ADR 0005). */
class WebMapBoundaryTest {

    private static final Path WEB_MAP_SOURCES = Path.of("src/main/java/com/mcmiddleearth/guidebook/webmap");

    @Test
    void theWebMapPackageImportsNothingElseFromGuidebook() throws IOException {
        List<String> guidebookImports;
        try (Stream<Path> files = Files.walk(WEB_MAP_SOURCES)) {
            List<Path> sources =
                    files.filter(file -> file.toString().endsWith(".java")).toList();
            assertFalse(sources.isEmpty(), "No Web map sources found in " + WEB_MAP_SOURCES.toAbsolutePath());
            guidebookImports = sources.stream()
                    .flatMap(WebMapBoundaryTest::lines)
                    .filter(line -> line.startsWith("import com.mcmiddleearth.guidebook."))
                    .filter(line -> !line.startsWith("import com.mcmiddleearth.guidebook.webmap."))
                    .toList();
        }

        assertEquals(List.of(), guidebookImports);
    }

    private static Stream<String> lines(Path file) {
        try {
            return Files.readAllLines(file).stream();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
