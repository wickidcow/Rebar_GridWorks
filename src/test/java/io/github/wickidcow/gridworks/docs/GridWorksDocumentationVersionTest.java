package io.github.wickidcow.gridworks.docs;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class GridWorksDocumentationVersionTest {
    private static final Path PROPERTIES = Path.of("gradle.properties");
    private static final Path BUILD = Path.of("build.gradle.kts");
    private static final Path HOME = Path.of("docs/wiki/Home.md");
    private static final Path ROADMAP =
            Path.of("docs/wiki/Roadmap-and-Compatibility.md");

    private static final Pattern JAVA_TOOLCHAIN = Pattern.compile(
            "JavaLanguageVersion\\.of\\((\\d+)\\)"
    );

    @Test
    void compatibilityDocsMatchBuildTargets() throws Exception {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(PROPERTIES)) {
            properties.load(reader);
        }

        String version = required(properties, "version");
        String minecraft = required(properties, "minecraft.version");
        String rebar = required(properties, "rebar.version");

        Matcher javaMatcher = JAVA_TOOLCHAIN.matcher(Files.readString(BUILD));
        assertTrue(javaMatcher.find(), "Java toolchain version is missing");
        String javaVersion = javaMatcher.group(1);

        String developmentLine = developmentLine(version);
        String home = Files.readString(HOME);
        String roadmap = Files.readString(ROADMAP);

        assertTrue(
                home.contains("> **Current development line:** " + developmentLine),
                "Wiki Home development line is stale"
        );
        assertTrue(
                home.contains("> **Minecraft / Paper:** " + minecraft),
                "Wiki Home Minecraft/Paper target is stale"
        );
        assertTrue(
                home.contains("> **Java:** " + javaVersion),
                "Wiki Home Java target is stale"
        );
        assertTrue(
                home.contains("> **Rebar:** " + rebar),
                "Wiki Home Rebar target is stale"
        );

        assertTrue(
                roadmap.contains("| GridWorks | " + version + " |"),
                "Roadmap GridWorks version is stale"
        );
        assertTrue(
                roadmap.contains("| Minecraft / Paper | " + minecraft + " |"),
                "Roadmap Minecraft/Paper target is stale"
        );
        assertTrue(
                roadmap.contains("| Java | " + javaVersion + " |"),
                "Roadmap Java target is stale"
        );
        assertTrue(
                roadmap.contains("| Rebar | " + rebar + " |"),
                "Roadmap Rebar target is stale"
        );
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing Gradle property: " + key);
        }
        return value.trim();
    }

    private static String developmentLine(String version) {
        String base = version;
        int suffix = base.indexOf('-');
        if (suffix >= 0) {
            base = base.substring(0, suffix);
        }

        String[] parts = base.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException(
                    "Expected semantic project version, got " + version
            );
        }
        return parts[0] + "." + parts[1] + ".x";
    }
}
