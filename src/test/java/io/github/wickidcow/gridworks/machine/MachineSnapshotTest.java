package io.github.wickidcow.gridworks.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MachineSnapshotTest {
    @Test
    void normalizesProcessingProgressFromRemainingTicks() {
        MachineSnapshot snapshot = MachineSnapshot.processing(
                "processor",
                200,
                50
        );

        assertTrue(snapshot.available());
        assertTrue(snapshot.processing());
        assertEquals(0.75, snapshot.progress(), 1e-9);
        assertEquals(200, snapshot.processTimeTicks());
        assertEquals(50, snapshot.ticksRemaining());
    }

    @Test
    void remainingTicksAreClampedToProcessDuration() {
        MachineSnapshot snapshot = MachineSnapshot.processing(
                "recipe_processor",
                100,
                250
        );

        assertEquals(0.0, snapshot.progress(), 0.0);
        assertEquals(100, snapshot.ticksRemaining());
    }

    @Test
    void missingDurationFallsBackToSafeIdleState() {
        MachineSnapshot snapshot = MachineSnapshot.processing(
                "processor",
                null,
                null
        );

        assertTrue(snapshot.available());
        assertFalse(snapshot.processing());
        assertEquals(0.0, snapshot.progress(), 0.0);
    }

    @Test
    void unavailableSnapshotHasNoSyntheticMeasurements() {
        assertEquals(
                new MachineSnapshot(false, "", false, 0.0, 0, 0),
                MachineSnapshot.unavailable()
        );
    }

    @Test
    void rejectsInconsistentSnapshots() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MachineSnapshot(
                        false,
                        "processor",
                        false,
                        0.0,
                        0,
                        0
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new MachineSnapshot(
                        true,
                        "processor",
                        false,
                        0.5,
                        100,
                        50
                )
        );
    }
}
