package io.github.wickidcow.gridworks.alarm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlarmSeverityAndFilterTest {
    @Test
    void oldOrInvalidSeverityDefaultsToWarning() {
        assertEquals(AlarmSeverity.WARNING, AlarmSeverity.fromStored(null));
        assertEquals(AlarmSeverity.WARNING, AlarmSeverity.fromStored("not-a-severity"));
    }

    @Test
    void severityCyclesBothDirections() {
        assertEquals(AlarmSeverity.WARNING, AlarmSeverity.CRITICAL.cycle(1));
        assertEquals(AlarmSeverity.CRITICAL, AlarmSeverity.INFO.cycle(1));
        assertEquals(AlarmSeverity.INFO, AlarmSeverity.CRITICAL.cycle(-1));
    }

    @Test
    void consoleFiltersMatchOperationalMeaning() {
        AlarmTelemetryState.Snapshot criticalUnack = snapshot(
                AlarmSeverity.CRITICAL, true, true, false
        );
        AlarmTelemetryState.Snapshot warningClear = snapshot(
                AlarmSeverity.WARNING, false, false, false
        );
        AlarmTelemetryState.Snapshot infoLatchedAck = snapshot(
                AlarmSeverity.INFO, false, true, true
        );

        assertTrue(AlarmConsoleFilter.ALL.accepts(infoLatchedAck));

        assertTrue(AlarmConsoleFilter.WARNING_PLUS.accepts(criticalUnack));
        assertTrue(AlarmConsoleFilter.WARNING_PLUS.accepts(warningClear));
        assertFalse(AlarmConsoleFilter.WARNING_PLUS.accepts(infoLatchedAck));

        assertTrue(AlarmConsoleFilter.CRITICAL_ONLY.accepts(criticalUnack));
        assertFalse(AlarmConsoleFilter.CRITICAL_ONLY.accepts(warningClear));

        assertTrue(AlarmConsoleFilter.LATCHED_ONLY.accepts(criticalUnack));
        assertTrue(AlarmConsoleFilter.LATCHED_ONLY.accepts(infoLatchedAck));
        assertFalse(AlarmConsoleFilter.LATCHED_ONLY.accepts(warningClear));

        assertTrue(AlarmConsoleFilter.UNACKNOWLEDGED_ONLY.accepts(criticalUnack));
        assertFalse(AlarmConsoleFilter.UNACKNOWLEDGED_ONLY.accepts(infoLatchedAck));
    }

    private static AlarmTelemetryState.Snapshot snapshot(
            AlarmSeverity severity,
            boolean condition,
            boolean latched,
            boolean acknowledged
    ) {
        return new AlarmTelemetryState.Snapshot(
                UUID.randomUUID(),
                "Test",
                severity,
                condition,
                latched,
                acknowledged,
                1
        );
    }
}
