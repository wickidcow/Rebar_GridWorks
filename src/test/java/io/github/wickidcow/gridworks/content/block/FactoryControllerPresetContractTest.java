package io.github.wickidcow.gridworks.content.block;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class FactoryControllerPresetContractTest {
    private static final Path SOURCE = Path.of(
            "src/main/java/io/github/wickidcow/gridworks/content/block/FactoryControllerBlock.java"
    );

    private static final Pattern BATCH_RATE_PRESET = Pattern.compile(
            "BATCH_RATE_LOW\\(\\s*"
                    + "\"Batch Rate <= 30/min\",\\s*"
                    + "new NumericControlRule\\(\\s*"
                    + "GridWorksChannels\\.BATCH_RATE_PER_MINUTE,\\s*"
                    + "ComparisonOperator\\.LESS_OR_EQUAL,\\s*"
                    + "30\\.0\\s*\\)",
            Pattern.DOTALL
    );

    private static final Pattern BATCH_ETA_PRESET = Pattern.compile(
            "BATCH_ETA_HIGH\\(\\s*"
                    + "\"Batch ETA >= 60s\",\\s*"
                    + "new NumericControlRule\\(\\s*"
                    + "GridWorksChannels\\.BATCH_ETA_SECONDS,\\s*"
                    + "ComparisonOperator\\.GREATER_OR_EQUAL,\\s*"
                    + "60\\.0\\s*\\)",
            Pattern.DOTALL
    );

    @Test
    void productionPacePresetsKeepTheirMetricOperatorAndThresholdContracts()
            throws Exception {
        String source = Files.readString(SOURCE);

        assertTrue(
                BATCH_RATE_PRESET.matcher(source).find(),
                "Batch-rate preset drifted from <= 30 cycles/min"
        );
        assertTrue(
                BATCH_ETA_PRESET.matcher(source).find(),
                "Batch-ETA preset drifted from >= 60 seconds"
        );
    }

    @Test
    void productionPresetsRemainAppendedAfterExistingPowerPresets()
            throws Exception {
        String source = Files.readString(SOURCE);

        int loadShed = source.indexOf("LOAD_SHED_TRIGGER(");
        int batchRate = source.indexOf("BATCH_RATE_LOW(");
        int batchEta = source.indexOf("BATCH_ETA_HIGH(");

        assertTrue(
                loadShed >= 0 && batchRate > loadShed && batchEta > batchRate,
                "Production presets must remain appended to preserve existing preset ordering"
        );
    }
}
