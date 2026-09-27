package io.github.wickidcow.gridworks.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class GridWorksContentConsistencyTest {
    private static final Path CONTENT_SOURCE = Path.of(
            "src/main/java/io/github/wickidcow/gridworks/content/GridWorksContent.java"
    );
    private static final Path RECIPE_SOURCE = Path.of(
            "src/main/java/io/github/wickidcow/gridworks/content/GridWorksRecipes.java"
    );
    private static final Path ENGLISH_LANG = Path.of(
            "src/main/resources/lang/en.yml"
    );

    private static final Pattern KEY_ASSIGNMENT = Pattern.compile(
            "\\b[A-Z][A-Z0-9_]*\\s*=\\s*new NamespacedKey\\(plugin, \"([a-z0-9_]+)\"\\);"
    );
    private static final Pattern RECIPE_DECLARATION = Pattern.compile(
            "recipe\\(plugin, \"([a-z0-9_]+)\""
    );

    @Test
    void contentKeysRecipesAndEnglishMetadataMatchCanonicalCatalog()
            throws Exception {
        Set<String> catalog = GridWorksContentCatalog.ALL_ID_SET;
        Set<String> contentIds = findAll(
                Files.readString(CONTENT_SOURCE),
                KEY_ASSIGNMENT
        );
        Set<String> recipeIds = findAll(
                Files.readString(RECIPE_SOURCE),
                RECIPE_DECLARATION
        );
        Set<String> englishIds = GridWorksContentValidator.extractEnglishItemIds(
                new ByteArrayInputStream(Files.readAllBytes(ENGLISH_LANG))
        );

        assertEquals(catalog, contentIds, "NamespacedKey registrations drifted");
        assertEquals(catalog, recipeIds, "Survival recipe catalog drifted");
        assertEquals(catalog, englishIds, "English item metadata drifted");
    }

    @Test
    void catalogIdentifiersAreUniqueAndNormalized() {
        GridWorksContentCatalog.validateCatalog();

        assertEquals(22, GridWorksContentCatalog.ALL_IDS.size());
        assertTrue(
                GridWorksContentCatalog.ALL_IDS.stream()
                        .allMatch(id -> id.matches("[a-z0-9_]+"))
        );
    }

    @Test
    void englishParserRejectsDuplicateTopLevelItemEntries()
            throws IOException {
        String duplicate = """
                addon: "GridWorks"
                item:
                  test:
                    name: "One"
                  test:
                    name: "Two"
                """;

        try {
            GridWorksContentValidator.extractEnglishItemIds(
                    new ByteArrayInputStream(
                            duplicate.getBytes(StandardCharsets.UTF_8)
                    )
            );
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("Duplicate"));
            return;
        }

        throw new AssertionError("Duplicate item metadata was accepted");
    }

    private static Set<String> findAll(String source, Pattern pattern) {
        Set<String> values = new LinkedHashSet<>();
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            if (!values.add(matcher.group(1))) {
                throw new AssertionError(
                        "Duplicate source identifier: " + matcher.group(1)
                );
            }
        }
        return values;
    }
}
