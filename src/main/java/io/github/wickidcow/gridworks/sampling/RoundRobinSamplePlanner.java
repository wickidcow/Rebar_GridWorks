package io.github.wickidcow.gridworks.sampling;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Identity-based round-robin sample planner that spreads a sensor population
 * across ticks instead of waking every sensor on the same scheduler tick.
 *
 * <p>Scheduling uses integer "sensor-tick" credits rather than floating point,
 * so long-running servers do not accumulate interval drift. Each registration
 * contributes one full sample credit so newly loaded sensors are serviced
 * promptly, while {@code maxSamplesPerTick} bounds the amount of probing work
 * a single server tick can receive.</p>
 */
public final class RoundRobinSamplePlanner<T> {
    private final long targetIntervalTicks;
    private final int maxSamplesPerTick;
    private final Set<T> members =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private final ArrayDeque<T> queue = new ArrayDeque<>();

    private long sampleCreditUnits;

    public RoundRobinSamplePlanner(
            long targetIntervalTicks,
            int maxSamplesPerTick
    ) {
        if (targetIntervalTicks <= 0L) {
            throw new IllegalArgumentException(
                    "targetIntervalTicks must be positive"
            );
        }
        if (maxSamplesPerTick <= 0) {
            throw new IllegalArgumentException(
                    "maxSamplesPerTick must be positive"
            );
        }

        this.targetIntervalTicks = targetIntervalTicks;
        this.maxSamplesPerTick = maxSamplesPerTick;
    }

    public synchronized boolean add(T value) {
        Objects.requireNonNull(value, "value");
        if (!members.add(value)) {
            return false;
        }

        queue.addLast(value);
        sampleCreditUnits = Math.min(
                maxCreditUnits(),
                saturatingAdd(sampleCreditUnits, targetIntervalTicks)
        );
        return true;
    }

    public synchronized boolean remove(T value) {
        Objects.requireNonNull(value, "value");
        if (!members.remove(value)) {
            return false;
        }

        queue.removeIf(candidate -> candidate == value);
        if (queue.isEmpty()) {
            sampleCreditUnits = 0L;
        } else {
            sampleCreditUnits = Math.min(
                    sampleCreditUnits,
                    maxCreditUnits()
            );
        }
        return true;
    }

    public synchronized List<T> nextTickBatch() {
        int size = members.size();
        if (size == 0) {
            sampleCreditUnits = 0L;
            return List.of();
        }

        sampleCreditUnits = Math.min(
                maxCreditUnits(),
                saturatingAdd(sampleCreditUnits, size)
        );

        long due = sampleCreditUnits / targetIntervalTicks;
        int count = (int) Math.min((long) maxSamplesPerTick, due);
        if (count <= 0) {
            return List.of();
        }

        List<T> batch = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            T next = queue.removeFirst();
            queue.addLast(next);
            batch.add(next);
        }

        sampleCreditUnits -= (long) count * targetIntervalTicks;
        return List.copyOf(batch);
    }

    public synchronized void requestFullSweep() {
        sampleCreditUnits = maxCreditUnits();
    }

    public synchronized int size() {
        return members.size();
    }

    public long targetIntervalTicks() {
        return targetIntervalTicks;
    }

    public int maxSamplesPerTick() {
        return maxSamplesPerTick;
    }

    /**
     * Conservative estimate of how many ticks a full population sweep needs.
     * Under the budget this remains the configured interval; above the budget
     * it reports the longer bounded-work interval.
     */
    public synchronized long estimatedSweepTicks() {
        if (members.isEmpty()) {
            return 0L;
        }

        long budgetLimited = (
                members.size() + (long) maxSamplesPerTick - 1L
        ) / maxSamplesPerTick;
        return Math.max(targetIntervalTicks, budgetLimited);
    }

    public synchronized void clear() {
        members.clear();
        queue.clear();
        sampleCreditUnits = 0L;
    }

    private long maxCreditUnits() {
        return saturatingMultiply(members.size(), targetIntervalTicks);
    }

    private static long saturatingAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private static long saturatingMultiply(long left, long right) {
        if (left == 0L || right == 0L) {
            return 0L;
        }
        if (left > Long.MAX_VALUE / right) {
            return Long.MAX_VALUE;
        }
        return left * right;
    }
}
