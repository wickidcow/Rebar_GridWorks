package io.github.wickidcow.gridworks.api.power;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LoadSheddingPolicyTest {
    @Test
    void defaultsUseHysteresisInsteadOfFlapping() {
        LoadSheddingPolicy policy = LoadSheddingPolicy.defaults();

        assertEquals(LoadSheddingStage.NORMAL, policy.update(0.89, 0));
        assertEquals(LoadSheddingStage.SHED_OPTIONAL, policy.update(0.90, 0));

        // Still above the restore threshold, so optional loads stay shed.
        assertEquals(LoadSheddingStage.SHED_OPTIONAL, policy.update(0.85, 0));
        assertEquals(LoadSheddingStage.NORMAL, policy.update(0.80, 0));
    }

    @Test
    void severeLoadShedsNormalAndOptional() {
        LoadSheddingPolicy policy = LoadSheddingPolicy.defaults();

        assertEquals(
                LoadSheddingStage.SHED_NORMAL_AND_OPTIONAL,
                policy.update(1.0, 0)
        );
        assertTrue(policy.stage().allowsEssentialLoads());
        assertFalse(policy.stage().allowsNormalLoads());
        assertFalse(policy.stage().allowsOptionalLoads());
    }

    @Test
    void anyUnpoweredConsumerForcesSevereShedding() {
        LoadSheddingPolicy policy = LoadSheddingPolicy.defaults();

        assertEquals(
                LoadSheddingStage.SHED_NORMAL_AND_OPTIONAL,
                policy.update(0.50, 1)
        );

        // Unpowered consumers keep the severe stage latched regardless of ratio.
        assertEquals(
                LoadSheddingStage.SHED_NORMAL_AND_OPTIONAL,
                policy.update(0.10, 1)
        );
    }

    @Test
    void recoveryFromSevereStageIsGradual() {
        LoadSheddingPolicy policy = LoadSheddingPolicy.defaults();

        policy.update(1.10, 0);

        // Normal loads are not restored until normalRestoreAt.
        assertEquals(
                LoadSheddingStage.SHED_NORMAL_AND_OPTIONAL,
                policy.update(0.91, 0)
        );

        // At 0.90 normal loads return, but optional loads remain shed.
        assertEquals(
                LoadSheddingStage.SHED_OPTIONAL,
                policy.update(0.90, 0)
        );

        // Optional loads only return at their lower restore point.
        assertEquals(
                LoadSheddingStage.NORMAL,
                policy.update(0.80, 0)
        );
    }

    @Test
    void snapshotUpdateUsesDerivedGridValues() {
        LoadSheddingPolicy policy = LoadSheddingPolicy.defaults();
        PowerGridSnapshot snapshot = new PowerGridSnapshot(
                4,
                1,
                3,
                2,
                1000.0,
                600.0
        );

        assertEquals(
                LoadSheddingStage.SHED_NORMAL_AND_OPTIONAL,
                policy.update(snapshot)
        );
    }

    @Test
    void canRestorePersistedStage() {
        LoadSheddingPolicy policy = LoadSheddingPolicy.defaults(
                LoadSheddingStage.SHED_OPTIONAL
        );

        assertEquals(LoadSheddingStage.SHED_OPTIONAL, policy.stage());
        assertEquals(
                LoadSheddingStage.SHED_OPTIONAL,
                policy.update(0.85, 0)
        );
    }

    @Test
    void resetReturnsToNormal() {
        LoadSheddingPolicy policy = LoadSheddingPolicy.defaults();
        policy.update(1.0, 0);

        policy.reset();

        assertEquals(LoadSheddingStage.NORMAL, policy.stage());
    }

    @Test
    void rejectsInvalidThresholdOrderingAndInputs() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new LoadSheddingPolicy(0.8, 0.9, 1.0, 0.9)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new LoadSheddingPolicy(0.9, 0.8, 0.85, 0.8)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new LoadSheddingPolicy(0.9, 0.8, 1.0, 0.7)
        );

        LoadSheddingPolicy policy = LoadSheddingPolicy.defaults();
        assertThrows(
                IllegalArgumentException.class,
                () -> policy.update(Double.NaN, 0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> policy.update(0.5, -1)
        );
    }
}
