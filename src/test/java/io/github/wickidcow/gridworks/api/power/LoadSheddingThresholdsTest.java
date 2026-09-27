package io.github.wickidcow.gridworks.api.power;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LoadSheddingThresholdsTest {
    @Test
    void defaultsMatchPolicyDefaults() {
        LoadSheddingThresholds thresholds = LoadSheddingThresholds.defaults();

        assertEquals(0.90, thresholds.optionalShedAt(), 0.0);
        assertEquals(0.80, thresholds.optionalRestoreAt(), 0.0);
        assertEquals(1.00, thresholds.normalShedAt(), 0.0);
        assertEquals(0.90, thresholds.normalRestoreAt(), 0.0);
    }

    @Test
    void editsClampWithoutBreakingHysteresisOrdering() {
        LoadSheddingThresholds thresholds = LoadSheddingThresholds.defaults();

        thresholds = thresholds.withOptionalShedAt(1.50);
        assertEquals(1.00, thresholds.optionalShedAt(), 0.0);

        thresholds = thresholds.withOptionalRestoreAt(0.95);
        assertEquals(0.90, thresholds.optionalRestoreAt(), 0.0);

        thresholds = thresholds.withNormalShedAt(0.25);
        assertEquals(1.00, thresholds.normalShedAt(), 0.0);

        thresholds = thresholds.withNormalRestoreAt(0.10);
        assertEquals(0.90, thresholds.normalRestoreAt(), 0.0);
    }

    @Test
    void persistedInvalidCombinationFallsBackAsOneAtomicConfiguration() {
        LoadSheddingThresholds thresholds =
                LoadSheddingThresholds.fromStoredOrDefault(
                        0.95,
                        0.90,
                        0.80,
                        0.85
                );

        assertEquals(LoadSheddingThresholds.defaults(), thresholds);
    }

    @Test
    void partialLegacyPersistenceFallsBackToDefaults() {
        LoadSheddingThresholds thresholds =
                LoadSheddingThresholds.fromStoredOrDefault(
                        null,
                        null,
                        null,
                        null
                );

        assertEquals(LoadSheddingThresholds.defaults(), thresholds);
    }

    @Test
    void createsPolicyAtPersistedStage() {
        LoadSheddingThresholds thresholds = new LoadSheddingThresholds(
                0.85,
                0.70,
                1.10,
                0.90
        );

        LoadSheddingPolicy policy = thresholds.createPolicy(
                LoadSheddingStage.SHED_OPTIONAL
        );

        assertEquals(LoadSheddingStage.SHED_OPTIONAL, policy.stage());
        assertEquals(0.85, policy.optionalShedAt(), 0.0);
        assertEquals(0.70, policy.optionalRestoreAt(), 0.0);
        assertEquals(1.10, policy.normalShedAt(), 0.0);
        assertEquals(0.90, policy.normalRestoreAt(), 0.0);
    }
}
