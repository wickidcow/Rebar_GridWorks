package io.github.wickidcow.gridworks.api.power;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PowerGridSnapshotTest {
    @Test
    void derivesUsefulControlMeasurements() {
        PowerGridSnapshot snapshot = new PowerGridSnapshot(
                12,
                2,
                8,
                7,
                1000.0,
                750.0
        );

        assertEquals(1, snapshot.unpoweredConsumerCount());
        assertFalse(snapshot.fullyPowered());
        assertEquals(0.75, snapshot.loadRatio(), 1e-9);
        assertEquals(250.0, snapshot.reserveWatts(), 1e-9);
        assertEquals(0.875, snapshot.poweredConsumerRatio(), 1e-9);
    }

    @Test
    void zeroDemandAndZeroConsumersHaveStableRatios() {
        PowerGridSnapshot snapshot = new PowerGridSnapshot(
                1,
                1,
                0,
                0,
                500.0,
                0.0
        );

        assertTrue(snapshot.fullyPowered());
        assertEquals(0.0, snapshot.loadRatio(), 0.0);
        assertEquals(1.0, snapshot.poweredConsumerRatio(), 0.0);
    }

    @Test
    void demandWithoutProductionStillReturnsFiniteLoadRatio() {
        PowerGridSnapshot snapshot = new PowerGridSnapshot(
                1,
                0,
                1,
                0,
                0.0,
                100.0
        );

        assertEquals(Double.MAX_VALUE, snapshot.loadRatio());
        assertEquals(-100.0, snapshot.reserveWatts(), 0.0);
    }

    @Test
    void rejectsImpossibleCountsAndPowerValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PowerGridSnapshot(1, 2, 0, 0, 1.0, 0.0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new PowerGridSnapshot(1, 0, 1, 2, 0.0, 0.0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new PowerGridSnapshot(1, 0, 0, 0, Double.NaN, 0.0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new PowerGridSnapshot(1, 0, 0, 0, -1.0, 0.0)
        );
    }
}
