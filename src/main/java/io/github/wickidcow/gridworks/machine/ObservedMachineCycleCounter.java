package io.github.wickidcow.gridworks.machine;

import java.util.Objects;

/**
 * Counts observed machine work cycles from sampled Machine Sensor state.
 *
 * <p>A cycle is counted only when one continuously available target moves from
 * processing to idle. The first observation is a baseline, and an unavailable
 * observation clears that baseline. This avoids manufacturing a cycle after
 * chunk unload, target replacement, sensor reload, or state replay.</p>
 *
 * <p>This is activity telemetry, not a guaranteed recipe-output counter.
 * Released Rebar exposes common processing state but no universal completion
 * event across every processor contract GridWorks supports.</p>
 */
public final class ObservedMachineCycleCounter {
    public static final long MAX_EXACT_COUNT = 9_007_199_254_740_991L;

    private long observedCycles;
    private long lastCycleEpochMillis;
    private MachineSnapshot previousSnapshot;

    public ObservedMachineCycleCounter() {
        this(0L, 0L);
    }

    public ObservedMachineCycleCounter(long observedCycles, long lastCycleEpochMillis) {
        if (observedCycles < 0L || observedCycles > MAX_EXACT_COUNT) {
            throw new IllegalArgumentException(
                    "observedCycles must be between 0 and " + MAX_EXACT_COUNT
            );
        }
        if (lastCycleEpochMillis < 0L) {
            throw new IllegalArgumentException("lastCycleEpochMillis must be non-negative");
        }
        this.observedCycles = observedCycles;
        this.lastCycleEpochMillis = lastCycleEpochMillis;
    }

    public static ObservedMachineCycleCounter fromStored(
            Long observedCycles,
            Long lastCycleEpochMillis
    ) {
        long safeCycles = observedCycles == null
                ? 0L
                : Math.max(0L, Math.min(MAX_EXACT_COUNT, observedCycles));
        long safeTimestamp = lastCycleEpochMillis == null
                ? 0L
                : Math.max(0L, lastCycleEpochMillis);
        return new ObservedMachineCycleCounter(safeCycles, safeTimestamp);
    }

    public synchronized boolean observe(MachineSnapshot snapshot, long nowEpochMillis) {
        Objects.requireNonNull(snapshot, "snapshot");

        if (!snapshot.available()) {
            previousSnapshot = null;
            return false;
        }

        MachineSnapshot previous = previousSnapshot;
        previousSnapshot = snapshot;

        if (previous == null
                || !previous.available()
                || !previous.processing()
                || snapshot.processing()) {
            return false;
        }

        if (observedCycles < MAX_EXACT_COUNT) {
            observedCycles++;
        }
        lastCycleEpochMillis = Math.max(lastCycleEpochMillis, Math.max(0L, nowEpochMillis));
        return true;
    }

    public synchronized void resetCount() {
        observedCycles = 0L;
        lastCycleEpochMillis = 0L;
    }

    /**
     * Clears only the transient processing baseline.
     *
     * <p>Persisted totals/history remain intact. This is used when a sensor
     * leaves and later re-enters the live topology so an old processing state
     * cannot be paired with a new idle sample and counted as a completion.</p>
     */
    public synchronized void resetObservation() {
        previousSnapshot = null;
    }

    public synchronized long observedCycles() {
        return observedCycles;
    }

    public synchronized long lastCycleEpochMillis() {
        return lastCycleEpochMillis;
    }
}
