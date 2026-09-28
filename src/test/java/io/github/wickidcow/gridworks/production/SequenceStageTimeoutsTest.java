package io.github.wickidcow.gridworks.production;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SequenceStageTimeoutsTest {
    @Test
    void defaultsEveryStageToOff() {
        SequenceStageTimeouts timeouts = new SequenceStageTimeouts();

        for (int stage = 1; stage <= SequenceStateMachine.STAGE_COUNT; stage++) {
            assertEquals(0L, timeouts.get(stage));
        }
        assertEquals(0L, timeouts.max());
    }

    @Test
    void legacySingleTimeoutMigratesToEveryMissingStage() {
        SequenceStageTimeouts timeouts = SequenceStageTimeouts.fromStored(
                600L,
                null,
                null,
                null,
                null
        );

        for (int stage = 1; stage <= SequenceStateMachine.STAGE_COUNT; stage++) {
            assertEquals(600L, timeouts.get(stage));
        }
    }

    @Test
    void stageSpecificValuesOverrideLegacyFallback() {
        SequenceStageTimeouts timeouts = SequenceStageTimeouts.fromStored(
                600L,
                100L,
                null,
                0L,
                900L
        );

        assertEquals(100L, timeouts.get(1));
        assertEquals(600L, timeouts.get(2));
        assertEquals(0L, timeouts.get(3));
        assertEquals(900L, timeouts.get(4));
        assertEquals(900L, timeouts.max());
    }

    @Test
    void storageAndEditsAreClampedToSupportedRange() {
        SequenceStageTimeouts timeouts = SequenceStageTimeouts.fromStored(
                -5L,
                null,
                100_000L,
                null,
                null
        );

        assertEquals(0L, timeouts.get(1));
        assertEquals(72_000L, timeouts.get(2));

        timeouts.set(3, -10L);
        timeouts.set(4, 90_000L);
        assertEquals(0L, timeouts.get(3));
        assertEquals(72_000L, timeouts.get(4));
    }

    @Test
    void rejectsInvalidStageAndStorageArity() {
        SequenceStageTimeouts timeouts = new SequenceStageTimeouts();

        assertThrows(IllegalArgumentException.class, () -> timeouts.get(0));
        assertThrows(IllegalArgumentException.class, () -> timeouts.set(5, 20L));
        assertThrows(
                IllegalArgumentException.class,
                () -> SequenceStageTimeouts.fromStored(10L, 1L, 2L)
        );
    }
}
