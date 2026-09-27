package io.github.wickidcow.gridworks.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FluidValveFlowTest {
    @Test
    void closedValveRequestsAndSuppliesNothing() {
        assertEquals(
                0.0,
                FluidValveFlow.requestedAmount(false, true, 1000.0),
                0.0
        );
        assertEquals(
                0.0,
                FluidValveFlow.suppliedAmount(false, 500.0),
                0.0
        );
    }

    @Test
    void openValveUsesAvailableTankSpaceAndStoredFluid() {
        assertEquals(
                750.0,
                FluidValveFlow.requestedAmount(true, true, 750.0),
                0.0
        );
        assertEquals(
                250.0,
                FluidValveFlow.suppliedAmount(true, 250.0),
                0.0
        );
    }

    @Test
    void incompatibleFluidIsNotRequested() {
        assertEquals(
                0.0,
                FluidValveFlow.requestedAmount(true, false, 1000.0),
                0.0
        );
    }

    @Test
    void rejectsInvalidAmounts() {
        assertThrows(
                IllegalArgumentException.class,
                () -> FluidValveFlow.requestedAmount(true, true, -1.0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> FluidValveFlow.suppliedAmount(true, Double.NaN)
        );
    }
}
