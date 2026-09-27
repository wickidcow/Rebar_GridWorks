package io.github.wickidcow.gridworks.power;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.api.control.GridWorksChannels;
import io.github.wickidcow.gridworks.api.power.PowerGridSnapshot;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PowerGridTelemetryTest {
    @Test
    void mapsSnapshotToStableControlChannels() {
        PowerGridSnapshot snapshot = new PowerGridSnapshot(
                12,
                2,
                8,
                7,
                1000.0,
                750.0
        );

        Map<?, ControlValue> values = PowerGridTelemetry.fromSnapshot(snapshot);

        assertEquals(11, values.size());
        assertEquals(
                new ControlValue.BooleanValue(true),
                values.get(GridWorksChannels.POWER_AVAILABLE)
        );
        assertEquals(
                new ControlValue.NumberValue(250.0),
                values.get(GridWorksChannels.POWER_RESERVE_WATTS)
        );
        assertEquals(
                new ControlValue.NumberValue(1.0),
                values.get(GridWorksChannels.POWER_UNPOWERED_CONSUMERS)
        );
        assertEquals(
                new ControlValue.NumberValue(0.75),
                values.get(GridWorksChannels.POWER_LOAD_RATIO)
        );
    }

    @Test
    void unavailableStatePublishesOnlyAvailability() {
        Map<?, ControlValue> values = PowerGridTelemetry.unavailable();

        assertEquals(1, values.size());
        ControlValue value = values.get(GridWorksChannels.POWER_AVAILABLE);
        assertInstanceOf(ControlValue.BooleanValue.class, value);
        assertFalse(((ControlValue.BooleanValue) value).value());
    }

    @Test
    void zeroProductionLoadRemainsFiniteForControlBus() {
        PowerGridSnapshot snapshot = new PowerGridSnapshot(
                1,
                0,
                1,
                0,
                0.0,
                10.0
        );

        ControlValue.NumberValue value = (ControlValue.NumberValue)
                PowerGridTelemetry.fromSnapshot(snapshot)
                        .get(GridWorksChannels.POWER_LOAD_RATIO);

        assertTrue(Double.isFinite(value.value()));
        assertEquals(Double.MAX_VALUE, value.value());
    }
}
