package io.github.wickidcow.gridworks.api.power;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.wickidcow.gridworks.api.control.ControlInputRouteMode;
import org.junit.jupiter.api.Test;

class PowerBranchApiTest {
    @Test
    void switchOnlySnapshotHasNoSyntheticLimit() {
        PowerBranchSnapshot snapshot = PowerBranchSnapshot.switchOnly(
                "edge-1",
                "Ore Line",
                true
        );

        assertTrue(snapshot.enabled());
        assertFalse(snapshot.powerLimitSupported());
        assertEquals(0.0, snapshot.powerLimitWatts(), 0.0);
    }

    @Test
    void rejectsInvalidBranchSnapshots() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PowerBranchSnapshot("", "Branch", true, false, 0.0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new PowerBranchSnapshot("id", "Branch", true, false, 10.0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new PowerBranchSnapshot("id", "Branch", true, true, Double.NaN)
        );
    }

    @Test
    void controlResultsExposeAppliedState() {
        assertTrue(PowerBranchControlResult.applied("ok").applied());
        assertFalse(PowerBranchControlResult.rejected("no").applied());
    }

    @Test
    void inputRouteModeDefaultsAndTogglesSafely() {
        assertEquals(
                ControlInputRouteMode.CIRCUIT,
                ControlInputRouteMode.fromStored(null)
        );
        assertEquals(
                ControlInputRouteMode.CIRCUIT,
                ControlInputRouteMode.fromStored("bad")
        );
        assertEquals(
                ControlInputRouteMode.ADDRESS,
                ControlInputRouteMode.CIRCUIT.toggle()
        );
    }
}
