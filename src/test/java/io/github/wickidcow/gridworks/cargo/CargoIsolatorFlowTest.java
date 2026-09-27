package io.github.wickidcow.gridworks.cargo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CargoIsolatorFlowTest {
    @Test
    void closedStateStopsBothDirections() {
        assertFalse(CargoIsolatorFlow.acceptsIncoming(false));
        assertEquals(0, CargoIsolatorFlow.transferRate(false, 4));
    }

    @Test
    void openStateRestoresConfiguredThroughput() {
        assertTrue(CargoIsolatorFlow.acceptsIncoming(true));
        assertEquals(4, CargoIsolatorFlow.transferRate(true, 4));
    }

    @Test
    void rejectsInvalidConfiguredRate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CargoIsolatorFlow.transferRate(true, 0)
        );
    }
}
