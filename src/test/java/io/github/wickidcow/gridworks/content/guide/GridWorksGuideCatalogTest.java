package io.github.wickidcow.gridworks.content.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.wickidcow.gridworks.content.GridWorksContentCatalog;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class GridWorksGuideCatalogTest {
    private static final Path GUIDE_SOURCE = Path.of(
            "src/main/java/io/github/wickidcow/gridworks/content/guide/GridWorksGuide.java"
    );
    private static final Path ENGLISH_LANG = Path.of(
            "src/main/resources/lang/en.yml"
    );

    private static final Pattern ITEM_SWITCH = Pattern.compile(
            "case \"([a-z0-9_]+)\"\\s*->\\s*GridWorksContent\\.[A-Z0-9_]+_ITEM"
    );

    @Test
    void guideCategoriesCoverEveryPlayerFacingItemExactlyOnce() {
        GridWorksGuideCatalog.validateCatalog();

        Set<String> itemIds = new LinkedHashSet<>();
        for (GridWorksGuideCatalog.Category category
                : GridWorksGuideCatalog.CATEGORIES) {
            for (String itemId : category.itemIds()) {
                assertTrue(
                        itemIds.add(itemId),
                        "Guide item appears more than once: " + itemId
                );
            }
        }

        assertEquals(GridWorksContentCatalog.ALL_ID_SET, itemIds);
        assertEquals(5, GridWorksGuideCatalog.CATEGORIES.size());
        assertEquals(26, itemIds.size());
    }

    @Test
    void runtimeItemResolverCoversTheCanonicalCatalog() throws Exception {
        Matcher matcher = ITEM_SWITCH.matcher(Files.readString(GUIDE_SOURCE));
        Set<String> resolved = new LinkedHashSet<>();
        while (matcher.find()) {
            assertTrue(
                    resolved.add(matcher.group(1)),
                    "Duplicate guide item resolver: " + matcher.group(1)
            );
        }

        assertEquals(GridWorksContentCatalog.ALL_ID_SET, resolved);
    }

    @Test
    void englishGuideTranslationsCoverLandingAndCategoryPages() throws Exception {
        String yaml = Files.readString(ENGLISH_LANG);
        int guideStart = yaml.indexOf("\nguide:\n");
        assertTrue(guideStart >= 0, "English lang is missing guide metadata");

        String guide = yaml.substring(guideStart);
        int pageStart = guide.indexOf("  page:\n");
        int buttonStart = guide.indexOf("  button:\n");
        assertTrue(pageStart >= 0 && buttonStart > pageStart);

        Set<String> expected = new LinkedHashSet<>();
        expected.add(GridWorksGuideCatalog.ROOT_PAGE_KEY);
        GridWorksGuideCatalog.CATEGORIES.forEach(
                category -> expected.add(category.key())
        );

        assertEquals(
                expected,
                sectionKeys(guide.substring(pageStart, buttonStart))
        );
        assertEquals(
                expected,
                sectionKeys(guide.substring(buttonStart))
        );
    }

    private static Set<String> sectionKeys(String section) {
        Pattern keys = Pattern.compile("(?m)^    ([a-z0-9_]+):");
        Matcher matcher = keys.matcher(section);
        Set<String> values = new LinkedHashSet<>();
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return values;
    }
}
