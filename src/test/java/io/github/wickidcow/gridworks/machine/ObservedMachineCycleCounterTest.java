package io.github.wickidcow.gridworks.machine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ObservedMachineCycleCounterTest {
    @Test
    void countsOnlyAvailableProcessingToIdleTransitions() {
        ObservedMachineCycleCounter counter = new ObservedMachineCycleCounter();
        assertFalse(counter.observe(MachineSnapshot.idle("processor"), 100L));
        assertFalse(counter.observe(MachineSnapshot.processing("processor", 200, 150), 200L));
        assertTrue(counter.observe(MachineSnapshot.idle("processor"), 300L));
        assertEquals(1L, counter.observedCycles());
        assertEquals(300L, counter.lastCycleEpochMillis());
    }

    @Test
    void unavailableTargetClearsTheCompletionBaseline() {
        ObservedMachineCycleCounter counter = new ObservedMachineCycleCounter();
        counter.observe(MachineSnapshot.processing("processor", 200, 100), 100L);
        counter.observe(MachineSnapshot.unavailable(), 200L);
        assertFalse(counter.observe(MachineSnapshot.idle("processor"), 300L));
        assertEquals(0L, counter.observedCycles());
    }

    @Test
    void initialAndRepeatedIdleNeverManufactureCycles() {
        ObservedMachineCycleCounter counter = new ObservedMachineCycleCounter();
        assertFalse(counter.observe(MachineSnapshot.idle("processor"), 100L));
        assertFalse(counter.observe(MachineSnapshot.idle("processor"), 200L));
        assertEquals(0L, counter.observedCycles());
    }

    @Test
    void persistedTotalsContinueAndTimestampNeverMovesBackward() {
        ObservedMachineCycleCounter counter = new ObservedMachineCycleCounter(12L, 1_000L);
        counter.observe(MachineSnapshot.processing("processor", 100, 50), 900L);
        assertTrue(counter.observe(MachineSnapshot.idle("processor"), 950L));
        assertEquals(13L, counter.observedCycles());
        assertEquals(1_000L, counter.lastCycleEpochMillis());
    }

    @Test
    void exactTransportCountSaturatesWithoutOverflow() {
        ObservedMachineCycleCounter counter = new ObservedMachineCycleCounter(
                ObservedMachineCycleCounter.MAX_EXACT_COUNT, 10L
        );
        counter.observe(MachineSnapshot.processing("processor", 100, 50), 20L);
        assertTrue(counter.observe(MachineSnapshot.idle("processor"), 30L));
        assertEquals(ObservedMachineCycleCounter.MAX_EXACT_COUNT, counter.observedCycles());
        assertEquals(30L, counter.lastCycleEpochMillis());
    }

    @Test
    void rejectsInvalidPersistedState() {
        assertThrows(IllegalArgumentException.class, () -> new ObservedMachineCycleCounter(-1L, 0L));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ObservedMachineCycleCounter(
                        ObservedMachineCycleCounter.MAX_EXACT_COUNT + 1L, 0L
                )
        );
        assertThrows(IllegalArgumentException.class, () -> new ObservedMachineCycleCounter(0L, -1L));
    }
}
