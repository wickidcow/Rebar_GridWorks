package io.github.wickidcow.gridworks.content.block;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FactoryControllerPresetContractTest {
    private static final Path SOURCE = Path.of(
            "src/main/java/io/github/wickidcow/gridworks/content/block/FactoryControllerBlock.java"
    );

    @Test
    void productionPacePresetsKeepTheirMetricOperatorAndThresholdContracts()
            throws Exception {
        String source = Files.readString(SOURCE);

        assertTrue(
                source.contains(
                        """
                        BATCH_RATE_LOW(
                                "Batch Rate <= 30/min",
                                new NumericControlRule(
                                        GridWorksChannels.BATCH_RATE_PER_MINUTE,
                                        ComparisonOperator.LESS_OR_EQUAL,
                                        30.0
                                )
                        """
                ),
                "Batch-rate preset drifted from <= 30 cycles/min"
        );
        assertTrue(
                source.contains(
                        """
                        BATCH_ETA_HIGH(
                                "Batch ETA >= 60s",
                                new NumericControlRule(
                                        GridWorksChannels.BATCH_ETA_SECONDS,
                                        ComparisonOperator.GREATER_OR_EQUAL,
                                        60.0
                                )
                        """
                ),
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

        assertTrue(loadShed >= 0 && batchRate > loadShed && batchEta > batchRate,
                "Production presets must remain appended to preserve existing preset ordering");
    }
}
