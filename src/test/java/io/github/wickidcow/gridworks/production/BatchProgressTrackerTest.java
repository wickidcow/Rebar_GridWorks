package io.github.wickidcow.gridworks.production;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class BatchProgressTrackerTest {
    @Test
    void firstSourceValueIsBaselineAndLaterDeltasAccumulate() {
        BatchProgressTracker tracker = new BatchProgressTracker(10L, 0L);
        UUID source = UUID.randomUUID();

        assertEquals(0L, tracker.observe(source, 100L).appliedDelta());
        assertEquals(3L, tracker.observe(source, 103L).appliedDelta());
        assertEquals(3L, tracker.progress());
        assertFalse(tracker.isComplete());
        assertEquals(7L, tracker.remaining());
    }

    @Test
    void multipleSourcesContributeOnlyTheirOwnNewWork() {
        BatchProgressTracker tracker = new BatchProgressTracker(5L, 0L);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        tracker.observe(first, 40L);
        tracker.observe(second, 80L);
        tracker.observe(first, 42L);
        BatchProgressTracker.Observation result = tracker.observe(second, 83L);

        assertEquals(5L, tracker.progress());
        assertTrue(result.complete());
        assertEquals(0L, result.remaining());
        assertEquals(2, tracker.trackedSourceCount());
    }

    @Test
    void sourceCounterResetRebaselinesWithoutNegativeProgress() {
        BatchProgressTracker tracker = new BatchProgressTracker(10L, 4L);
        UUID source = UUID.randomUUID();

        tracker.observe(source, 20L);
        assertEquals(0L, tracker.observe(source, 2L).appliedDelta());
        assertEquals(4L, tracker.progress());
        assertEquals(2L, tracker.observe(source, 4L).appliedDelta());
        assertEquals(6L, tracker.progress());
    }

    @Test
    void forgottenSourceDoesNotBackfillDisconnectedCycles() {
        BatchProgressTracker tracker = new BatchProgressTracker(10L, 0L);
        UUID source = UUID.randomUUID();

        tracker.observe(source, 5L);
        tracker.observe(source, 7L);
        tracker.forgetSource(source);

        assertEquals(0L, tracker.observe(source, 20L).appliedDelta());
        assertEquals(2L, tracker.progress());
        assertEquals(1L, tracker.observe(source, 21L).appliedDelta());
        assertEquals(3L, tracker.progress());
    }

    @Test
    void rebaselineAdvancesSourceHistoryWithoutAddingProgress() {
        BatchProgressTracker tracker = new BatchProgressTracker(10L, 3L);
        UUID source = UUID.randomUUID();

        tracker.observe(source, 10L);
        tracker.rebaseline(source, 25L);

        assertEquals(3L, tracker.progress());
        assertEquals(1L, tracker.observe(source, 26L).appliedDelta());
        assertEquals(4L, tracker.progress());
    }

    @Test
    void resettingBatchKeepsLiveSourceBaselines() {
        BatchProgressTracker tracker = new BatchProgressTracker(4L, 0L);
        UUID source = UUID.randomUUID();

        tracker.observe(source, 10L);
        tracker.observe(source, 13L);
        tracker.resetProgress();

        assertEquals(0L, tracker.progress());
        assertEquals(1L, tracker.observe(source, 14L).appliedDelta());
        assertEquals(1L, tracker.progress());
    }

    @Test
    void targetChangesRecomputeCompletionWithoutChangingProgress() {
        BatchProgressTracker tracker = new BatchProgressTracker(10L, 7L);

        tracker.setTarget(5L);
        assertTrue(tracker.isComplete());
        assertEquals(7L, tracker.progress());

        tracker.setTarget(12L);
        assertFalse(tracker.isComplete());
        assertEquals(5L, tracker.remaining());
    }

    @Test
    void progressSaturatesAtExactControlNumberLimit() {
        BatchProgressTracker tracker = new BatchProgressTracker(
                BatchProgressTracker.MAX_EXACT_COUNT,
                BatchProgressTracker.MAX_EXACT_COUNT - 1L
        );
        UUID source = UUID.randomUUID();

        tracker.observe(source, 0L);
        tracker.observe(source, 100L);

        assertEquals(BatchProgressTracker.MAX_EXACT_COUNT, tracker.progress());
        assertTrue(tracker.isComplete());
    }

    @Test
    void rejectsInvalidPersistentAndSourceValues() {
        assertThrows(IllegalArgumentException.class, () -> new BatchProgressTracker(0L, 0L));
        assertThrows(IllegalArgumentException.class, () -> new BatchProgressTracker(1L, -1L));

        BatchProgressTracker tracker = new BatchProgressTracker();
        assertThrows(
                IllegalArgumentException.class,
                () -> tracker.observe(UUID.randomUUID(), -1L)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> tracker.rebaseline(UUID.randomUUID(), -1L)
        );
    }
}
