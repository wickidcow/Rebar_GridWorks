package io.github.wickidcow.gridworks.alarm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AlarmHistoryStateTest {
    @Test
    void recordsOnlyExplicitTriggerCalls() {
        AlarmHistoryState history = new AlarmHistoryState(0, 0);

        assertFalse(history.hasTriggered());

        history.recordTrigger(1_000L);
        history.recordTrigger(2_000L);

        assertEquals(2L, history.occurrenceCount());
        assertEquals(2_000L, history.lastTriggeredEpochMillis());
        assertTrue(history.hasTriggered());
    }

    @Test
    void keepsNewestWallClockValueIfClockMovesBackward() {
        AlarmHistoryState history = new AlarmHistoryState(4, 5_000L);

        history.recordTrigger(4_000L);

        assertEquals(5L, history.occurrenceCount());
        assertEquals(5_000L, history.lastTriggeredEpochMillis());
    }

    @Test
    void sanitizesStoredValuesAndRejectsNegativeNewTime() {
        AlarmHistoryState history = new AlarmHistoryState(-4, -10);

        assertEquals(0L, history.occurrenceCount());
        assertEquals(0L, history.lastTriggeredEpochMillis());
        assertThrows(IllegalArgumentException.class, () -> history.recordTrigger(-1L));
    }

    @Test
    void occurrenceCounterSaturatesInsteadOfOverflowing() {
        AlarmHistoryState history = new AlarmHistoryState(Long.MAX_VALUE, 1L);

        history.recordTrigger(2L);

        assertEquals(Long.MAX_VALUE, history.occurrenceCount());
        assertEquals(2L, history.lastTriggeredEpochMillis());
    }
}
