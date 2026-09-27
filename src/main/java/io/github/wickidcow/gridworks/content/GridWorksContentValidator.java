package io.github.wickidcow.gridworks.content;

import io.github.wickidcow.gridworks.GridWorks;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.NamespacedKey;

/**
 * Startup integrity checks for player-facing GridWorks content.
 */
public final class GridWorksContentValidator {
    private GridWorksContentValidator() {
        throw new AssertionError("Utility class");
    }

    public static void validate(GridWorks plugin) {
        GridWorksContentCatalog.validateCatalog();

        Set<String> recipeIds = GridWorksRecipes.registeredKeys()
                .stream()
                .map(NamespacedKey::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        requireExact(
                "registered survival recipes",
                GridWorksContentCatalog.ALL_ID_SET,
                recipeIds
        );

        try (InputStream stream = plugin.getResource("lang/en.yml")) {
            if (stream == null) {
                throw new IllegalStateException(
                        "GridWorks is missing bundled lang/en.yml"
                );
            }

            requireExact(
                    "English item metadata",
                    GridWorksContentCatalog.ALL_ID_SET,
                    extractEnglishItemIds(stream)
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not validate bundled GridWorks English metadata",
                    exception
            );
        }

        plugin.getLogger().info(
                "Validated "
                        + GridWorksContentCatalog.ALL_IDS.size()
                        + " GridWorks content entries and survival recipes."
        );
    }

    static Set<String> extractEnglishItemIds(InputStream stream)
            throws IOException {
        Set<String> ids = new LinkedHashSet<>();
        boolean inItemSection = false;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!inItemSection) {
                    if (line.equals("item:")) {
                        inItemSection = true;
                    }
                    continue;
                }

                if (!line.isBlank()
                        && !Character.isWhitespace(line.charAt(0))) {
                    break;
                }

                if (line.startsWith("  ")
                        && !line.startsWith("    ")
                        && line.endsWith(":")) {
                    String id = line.substring(2, line.length() - 1).trim();
                    if (!id.isEmpty() && !ids.add(id)) {
                        throw new IllegalStateException(
                                "Duplicate English item metadata entry: " + id
                        );
                    }
                }
            }
        }

        return ids;
    }

    private static void requireExact(
            String label,
            Set<String> expected,
            Set<String> actual
    ) {
        if (expected.equals(actual)) {
            return;
        }

        List<String> missing = expected.stream()
                .filter(id -> !actual.contains(id))
                .sorted()
                .toList();
        List<String> extra = actual.stream()
                .filter(id -> !expected.contains(id))
                .sorted()
                .toList();

        throw new IllegalStateException(
                "GridWorks "
                        + label
                        + " do not match the content catalog. Missing="
                        + missing
                        + ", extra="
                        + extra
        );
    }
}
