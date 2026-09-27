package io.github.wickidcow.gridworks.alarm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.wickidcow.gridworks.api.control.ControlSignal;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.api.control.GridWorksChannels;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlarmTelemetryStateTest {
    @Test
    void aggregatesAlarmTelemetryBySource() {
        UUID source = UUID.randomUUID();
        AlarmTelemetryState state = new AlarmTelemetryState(source);

        state.apply(new ControlSignal(
                source,
                GridWorksChannels.ALARM_NAME,
                ControlValue.of("Low Diesel"),
                1
        ));
        state.apply(new ControlSignal(
                source,
                GridWorksChannels.ALARM_CONDITION_ACTIVE,
                ControlValue.of(true),
                2
        ));
        state.apply(new ControlSignal(
                source,
                GridWorksChannels.ALARM_LATCHED,
                ControlValue.of(true),
                3
        ));
        state.apply(new ControlSignal(
                source,
                GridWorksChannels.ALARM_ACKNOWLEDGED,
                ControlValue.of(false),
                4
        ));

        AlarmTelemetryState.Snapshot snapshot = state.snapshot();
        assertEquals("Low Diesel", snapshot.name());
        assertTrue(snapshot.isConditionActive());
        assertTrue(snapshot.isLatched());
        assertFalse(snapshot.isAcknowledged());
        assertEquals(4, snapshot.latestSequence());
    }

    @Test
    void ignoresOutOfOrderUpdatesForTheSameField() {
        UUID source = UUID.randomUUID();
        AlarmTelemetryState state = new AlarmTelemetryState(source);

        state.apply(new ControlSignal(
                source,
                GridWorksChannels.ALARM_LATCHED,
                ControlValue.of(true),
                20
        ));
        state.apply(new ControlSignal(
                source,
                GridWorksChannels.ALARM_LATCHED,
                ControlValue.of(false),
                10
        ));

        assertTrue(state.snapshot().isLatched());
    }

    @Test
    void rejectsDifferentSource() {
        UUID source = UUID.randomUUID();
        AlarmTelemetryState state = new AlarmTelemetryState(source);

        assertThrows(
                IllegalArgumentException.class,
                () -> state.apply(new ControlSignal(
                        UUID.randomUUID(),
                        GridWorksChannels.ALARM_LATCHED,
                        ControlValue.of(true),
                        1
                ))
        );
    }
}
