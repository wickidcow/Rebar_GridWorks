package io.github.wickidcow.gridworks.production;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BatchPaceTrackerTest {
    @Test
    void firstProgressEventOnlyEstablishesTimingBaseline() {
        BatchPaceTracker tracker = new BatchPaceTracker();

        tracker.observeProgress(1L, 1_000_000_000L);

        assertFalse(tracker.isAvailable());
        assertTrue(tracker.etaSeconds(10L).isEmpty());
    }

    @Test
    void laterProgressComputesObservedRateAndEta() {
        BatchPaceTracker tracker = new BatchPaceTracker();

        tracker.observeProgress(1L, 1_000_000_000L);
        tracker.observeProgress(4L, 3_000_000_000L);

        assertTrue(tracker.isAvailable());
        assertEquals(120.0, tracker.ratePerMinute(), 0.000001);
        assertEquals(5.0, tracker.etaSeconds(10L).orElseThrow(), 0.000001);
        assertEquals(0.0, tracker.etaSeconds(0L).orElseThrow(), 0.000001);
    }

    @Test
    void resetMakesPaceUnavailableAgain() {
        BatchPaceTracker tracker = new BatchPaceTracker();
        tracker.observeProgress(1L, 1_000_000_000L);
        tracker.observeProgress(1L, 2_000_000_000L);
        assertTrue(tracker.isAvailable());

        tracker.reset();

        assertFalse(tracker.isAvailable());
        assertTrue(tracker.etaSeconds(1L).isEmpty());
    }

    @Test
    void rejectsInvalidArguments() {
        BatchPaceTracker tracker = new BatchPaceTracker();

        assertThrows(IllegalArgumentException.class, () -> tracker.observeProgress(0L, 1L));
        assertThrows(IllegalArgumentException.class, () -> tracker.etaSeconds(-1L));
    }
}
