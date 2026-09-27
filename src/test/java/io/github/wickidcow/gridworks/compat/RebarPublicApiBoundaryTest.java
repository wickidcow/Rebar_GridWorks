package io.github.wickidcow.gridworks.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class RebarPublicApiBoundaryTest {
    private static final Path MAIN_SOURCE = Path.of("src/main/java");

    private static final List<String> FORBIDDEN_PRODUCTION_REFERENCES = List.of(
            "io.github.pylonmc.rebar.fluid.FluidManager",
            "io.github.pylonmc.rebar.logistics.CargoRoutes",
            "TickingRebarBlock.isTicking",
            "io.github.pylonmc.rebar.electricity."
    );

    @Test
    void productionCodeDoesNotUseKnownRebarInternalsOrUnreleasedElectricity()
            throws Exception {
        List<String> violations = new ArrayList<>();

        try (var paths = Files.walk(MAIN_SOURCE)) {
            for (Path path : paths
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".java"))
                    .toList()) {
                String source = Files.readString(path);
                for (String forbidden : FORBIDDEN_PRODUCTION_REFERENCES) {
                    if (source.contains(forbidden)) {
                        violations.add(
                                MAIN_SOURCE.relativize(path)
                                        + " references "
                                        + forbidden
                        );
                    }
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "GridWorks crossed the released Rebar API boundary: "
                        + violations
        );
    }

    @Test
    void rebarDependencyIsAReleasedBuildMatchingMinecraftLine()
            throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(Path.of("gradle.properties"))) {
            properties.load(reader);
        }

        String rebarVersion = properties.getProperty("rebar.version");
        String minecraftVersion = properties.getProperty("minecraft.version");

        assertTrue(rebarVersion != null && !rebarVersion.isBlank());
        assertTrue(minecraftVersion != null && !minecraftVersion.isBlank());

        String normalized = rebarVersion.toLowerCase(Locale.ROOT);
        for (String forbidden : List.of(
                "snapshot",
                "feature",
                "develop",
                "master",
                "main",
                "/",
                "\\"
        )) {
            assertFalse(
                    normalized.contains(forbidden),
                    () -> "rebar.version must reference a released artifact, not '"
                            + rebarVersion + "'"
            );
        }

        int separator = rebarVersion.lastIndexOf('-');
        assertTrue(
                separator > 0 && separator < rebarVersion.length() - 1,
                () -> "Expected released Rebar version '<version>-<minecraft>', got "
                        + rebarVersion
        );
        assertEquals(
                minecraftVersion,
                rebarVersion.substring(separator + 1),
                "Rebar and Paper/Minecraft target lines must match"
        );
    }

    @Test
    void rebarRemainsCompileOnly() throws IOException {
        String build = Files.readString(Path.of("build.gradle.kts"));

        assertTrue(
                build.contains(
                        "compileOnly(\"io.github.pylonmc:rebar:$rebarVersion\")"
                ),
                "Rebar must remain a provided/compileOnly server dependency"
        );
        assertFalse(
                build.contains(
                        "implementation(\"io.github.pylonmc:rebar:"
                ),
                "GridWorks must not shade/bundle Rebar"
        );
    }
}
