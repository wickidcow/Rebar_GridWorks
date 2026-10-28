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
import java.util.Properties;
import org.junit.jupiter.api.Test;

class RebarPublicApiBoundaryTest {
    private static final Path MAIN_SOURCE = Path.of("src/main/java");

    private static final List<String> FORBIDDEN_PRODUCTION_REFERENCES = List.of(
            "io.github.pylonmc.rebar.fluid.FluidManager",
            "io.github.pylonmc.rebar.logistics.CargoRoutes",
            "TickingRebarBlock.isTicking"
    );

    @Test
    void productionCodeDoesNotUseKnownRebarInternals()
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
                () -> "GridWorks crossed the public Rebar API boundary: "
                        + violations
        );
    }

    @Test
    void rebarDependencyPinsReviewedElectricityRelease()
            throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(Path.of("gradle.properties"))) {
            properties.load(reader);
        }

        String rebarVersion = properties.getProperty("rebar.version");
        String minecraftVersion = properties.getProperty("minecraft.version");

        assertTrue(rebarVersion != null && !rebarVersion.isBlank());
        assertTrue(minecraftVersion != null && !minecraftVersion.isBlank());

        assertEquals("0.44.4-26.2", rebarVersion,
                "Electricity builds pin the verified published Rebar release");
        assertEquals("26.2", minecraftVersion);

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
