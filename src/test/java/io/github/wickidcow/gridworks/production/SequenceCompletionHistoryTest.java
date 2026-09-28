package io.github.wickidcow.gridworks.production;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SequenceCompletionHistoryTest {
    @Test
    void recordsCompletionAndTimestamp() {
        SequenceCompletionHistory history = new SequenceCompletionHistory();

        history.recordCompletion(1_000L);
        history.recordCompletion(2_000L);

        assertEquals(2L, history.completedRuns());
        assertEquals(2_000L, history.lastCompletionEpochMillis());
    }

    @Test
    void timestampNeverMovesBackward() {
        SequenceCompletionHistory history =
                new SequenceCompletionHistory(5L, 5_000L);

        history.recordCompletion(4_000L);

        assertEquals(6L, history.completedRuns());
        assertEquals(5_000L, history.lastCompletionEpochMillis());
    }

    @Test
    void countSaturatesAtExactControlBusLimit() {
        SequenceCompletionHistory history =
                new SequenceCompletionHistory(
                        SequenceCompletionHistory.MAX_EXACT_COUNT,
                        10L
                );

        history.recordCompletion(20L);

        assertEquals(
                SequenceCompletionHistory.MAX_EXACT_COUNT,
                history.completedRuns()
        );
        assertEquals(20L, history.lastCompletionEpochMillis());
    }

    @Test
    void storedStateIsSanitizedBeforeConstruction() {
        SequenceCompletionHistory missing =
                SequenceCompletionHistory.fromStored(null, null);
        assertEquals(0L, missing.completedRuns());
        assertEquals(0L, missing.lastCompletionEpochMillis());

        SequenceCompletionHistory corrupt =
                SequenceCompletionHistory.fromStored(Long.MAX_VALUE, -10L);
        assertEquals(
                SequenceCompletionHistory.MAX_EXACT_COUNT,
                corrupt.completedRuns()
        );
        assertEquals(0L, corrupt.lastCompletionEpochMillis());
    }

    @Test
    void rejectsInvalidDirectConstruction() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SequenceCompletionHistory(-1L, 0L)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new SequenceCompletionHistory(
                        SequenceCompletionHistory.MAX_EXACT_COUNT + 1L,
                        0L
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new SequenceCompletionHistory(0L, -1L)
        );
    }
}
