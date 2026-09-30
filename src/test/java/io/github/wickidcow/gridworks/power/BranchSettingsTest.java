package io.github.wickidcow.gridworks.power;

import io.github.wickidcow.gridworks.power.nativeapi.BranchSettings;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BranchSettingsTest {
    @Test void outageAndReclosePreserveTheConfiguredLimit() {
        var state = new BranchSettings(true, 80);
        state = state.withEnabled(false).withLimit(40);
        assertEquals(0, state.effectiveWatts());
        assertEquals(40, state.withEnabled(true).effectiveWatts());
    }
    @Test void bypassNeverClosesAnOpenBreaker() {
        var state = new BranchSettings(false, 80).withLimit(Double.MAX_VALUE);
        assertEquals(0, state.effectiveWatts());
        assertFalse(state.enabled());
    }
    @Test void rejectsInvalidPowerAndSupportsZero() {
        for (double value : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new BranchSettings(true, value));
        }
        assertEquals(0, new BranchSettings(true, 0).effectiveWatts());
    }
}
