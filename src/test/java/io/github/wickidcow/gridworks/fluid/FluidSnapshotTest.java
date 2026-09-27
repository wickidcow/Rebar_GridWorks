package io.github.wickidcow.gridworks.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FluidSnapshotTest {
    @Test
    void calculatesFillRatioAndFluidPresence() {
        FluidSnapshot snapshot = new FluidSnapshot(
                true,
                "pylon:plant_oil",
                250.0,
                1000.0
        );

        assertTrue(snapshot.hasFluid());
        assertEquals(0.25, snapshot.fillRatio());
    }

    @Test
    void emptyTankIsAvailableButHasNoFluid() {
        FluidSnapshot snapshot = new FluidSnapshot(true, "", 0.0, 1000.0);

        assertFalse(snapshot.hasFluid());
        assertEquals(0.0, snapshot.fillRatio());
    }

    @Test
    void unavailableSnapshotContainsNoMeasurements() {
        FluidSnapshot snapshot = FluidSnapshot.unavailable();

        assertFalse(snapshot.available());
        assertEquals("", snapshot.fluidKey());
        assertEquals(0.0, snapshot.amount());
        assertEquals(0.0, snapshot.capacity());
    }

    @Test
    void rejectsInvalidMeasurements() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FluidSnapshot(true, "", -1.0, 1000.0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new FluidSnapshot(true, "", 0.0, Double.NaN)
        );
    }
}
