package io.github.wickidcow.gridworks.inventory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StockHysteresisTest {
    @Test
    void startsDemandAtOrBelowLowThreshold() {
        assertTrue(StockHysteresis.nextDemand(false, 256.0, 256.0, 1024.0));
        assertTrue(StockHysteresis.nextDemand(false, 100.0, 256.0, 1024.0));
        assertFalse(StockHysteresis.nextDemand(false, 257.0, 256.0, 1024.0));
    }

    @Test
    void activeDemandHoldsThroughDeadbandUntilHighThreshold() {
        assertTrue(StockHysteresis.nextDemand(true, 257.0, 256.0, 1024.0));
        assertTrue(StockHysteresis.nextDemand(true, 1023.0, 256.0, 1024.0));
        assertFalse(StockHysteresis.nextDemand(true, 1024.0, 256.0, 1024.0));
        assertFalse(StockHysteresis.nextDemand(true, 2048.0, 256.0, 1024.0));
    }

    @Test
    void inactiveDemandHoldsThroughDeadbandUntilLowThreshold() {
        assertFalse(StockHysteresis.nextDemand(false, 900.0, 256.0, 1024.0));
        assertFalse(StockHysteresis.nextDemand(false, 257.0, 256.0, 1024.0));
        assertTrue(StockHysteresis.nextDemand(false, 256.0, 256.0, 1024.0));
    }

    @Test
    void rejectsInvalidThresholdBands() {
        assertThrows(
                IllegalArgumentException.class,
                () -> StockHysteresis.nextDemand(false, 1.0, 5.0, 5.0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> StockHysteresis.nextDemand(false, 1.0, -1.0, 5.0)
        );
    }

    @Test
    void thresholdEditingPreservesGapAndBounds() {
        assertTrue(StockHysteresis.clampLow(990.0, 1024.0, 64.0) == 960.0);
        assertTrue(StockHysteresis.clampHigh(200.0, 256.0, 64.0, 4096.0) == 320.0);
        assertTrue(StockHysteresis.clampHigh(5000.0, 256.0, 64.0, 4096.0) == 4096.0);
    }
}
