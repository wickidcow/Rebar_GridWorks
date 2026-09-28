package io.github.wickidcow.gridworks.docs;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class GridWorksWikiConsistencyTest {
    private static final Path WIKI = Path.of("docs/wiki");
    private static final Pattern WIKI_LINK = Pattern.compile("\\[\\[([^\\]]+)]]");

    @Test
    void internalWikiLinksResolveToVersionedPages() throws Exception {
        assertTrue(Files.isDirectory(WIKI), "docs/wiki directory is missing");

        List<Path> pages;
        try (var stream = Files.list(WIKI)) {
            pages = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".md"))
                    .sorted()
                    .toList();
        }

        assertTrue(
                pages.stream().anyMatch(path -> path.getFileName().toString().equals("Home.md")),
                "docs/wiki/Home.md is required"
        );

        Set<String> pageNames = new HashSet<>();
        List<String> duplicateNames = new ArrayList<>();
        for (Path page : pages) {
            String normalized = normalizePageName(stem(page));
            if (!pageNames.add(normalized)) {
                duplicateNames.add(page.getFileName().toString());
            }
        }
        assertTrue(
                duplicateNames.isEmpty(),
                () -> "Duplicate normalized wiki page names: " + duplicateNames
        );

        List<String> broken = new ArrayList<>();
        for (Path page : pages) {
            String source = Files.readString(page);
            Matcher matcher = WIKI_LINK.matcher(source);
            while (matcher.find()) {
                String target = linkTarget(matcher.group(1));
                if (target.isEmpty() || target.startsWith("#")) {
                    continue;
                }

                int anchor = target.indexOf('#');
                if (anchor >= 0) {
                    target = target.substring(0, anchor);
                }

                String normalized = normalizePageName(target);
                if (!normalized.isEmpty() && !pageNames.contains(normalized)) {
                    broken.add(
                            page.getFileName() + " -> [[" + matcher.group(1) + "]]"
                    );
                }
            }
        }

        assertTrue(
                broken.isEmpty(),
                () -> "Broken internal GridWorks wiki links: " + broken
        );
    }

    private static String linkTarget(String raw) {
        String value = raw == null ? "" : raw.trim();
        int separator = value.lastIndexOf('|');
        return (separator >= 0 ? value.substring(separator + 1) : value).trim();
    }

    private static String stem(Path page) {
        String name = page.getFileName().toString();
        return name.substring(0, name.length() - ".md".length());
    }

    private static String normalizePageName(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.toLowerCase(Locale.ROOT).endsWith(".md")) {
            value = value.substring(0, value.length() - 3);
        }

        return value
                .replace(' ', '-')
                .replaceAll("-+", "-")
                .toLowerCase(Locale.ROOT);
    }
}
