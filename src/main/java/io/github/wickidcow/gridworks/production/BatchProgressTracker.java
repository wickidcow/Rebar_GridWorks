package io.github.wickidcow.gridworks.production;

import io.github.wickidcow.gridworks.machine.ObservedMachineCycleCounter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Event-driven batch progress derived from cumulative source cycle counters.
 *
 * <p>The first value from a source establishes a baseline. Later increases add
 * only the delta to batch progress. A source counter decrease re-baselines
 * safely, and forgetting a source ensures work performed while disconnected is
 * not counted when it reconnects.</p>
 */
public final class BatchProgressTracker {
    public static final long DEFAULT_TARGET = 64L;
    public static final long MAX_EXACT_COUNT =
            ObservedMachineCycleCounter.MAX_EXACT_COUNT;

    private final Map<UUID, Long> lastSourceCounts = new HashMap<>();
    private long target;
    private long progress;

    public BatchProgressTracker() {
        this(DEFAULT_TARGET, 0L);
    }

    public BatchProgressTracker(long target, long progress) {
        validateTarget(target);
        validateProgress(progress);
        this.target = target;
        this.progress = progress;
    }

    public synchronized Observation observe(UUID source, long sourceCount) {
        Objects.requireNonNull(source, "source");
        validateSourceCount(sourceCount);

        Long previous = lastSourceCounts.put(source, sourceCount);
        if (previous == null || sourceCount <= previous) {
            return snapshot(0L);
        }

        long delta = sourceCount - previous;
        long room = MAX_EXACT_COUNT - progress;
        long applied = Math.min(delta, room);
        progress += applied;
        return snapshot(applied);
    }

    public synchronized void forgetSource(UUID source) {
        lastSourceCounts.remove(Objects.requireNonNull(source, "source"));
    }

    public synchronized void resetProgress() {
        progress = 0L;
    }

    public synchronized void setTarget(long target) {
        validateTarget(target);
        this.target = target;
    }

    public synchronized long target() {
        return target;
    }

    public synchronized long progress() {
        return progress;
    }

    public synchronized long remaining() {
        return Math.max(0L, target - progress);
    }

    public synchronized boolean isComplete() {
        return progress >= target;
    }

    public synchronized int trackedSourceCount() {
        return lastSourceCounts.size();
    }

    private Observation snapshot(long appliedDelta) {
        return new Observation(
                appliedDelta,
                progress,
                target,
                Math.max(0L, target - progress),
                progress >= target
        );
    }

    private static void validateTarget(long target) {
        if (target < 1L || target > MAX_EXACT_COUNT) {
            throw new IllegalArgumentException(
                    "target must be between 1 and " + MAX_EXACT_COUNT
            );
        }
    }

    private static void validateProgress(long progress) {
        if (progress < 0L || progress > MAX_EXACT_COUNT) {
            throw new IllegalArgumentException(
                    "progress must be between 0 and " + MAX_EXACT_COUNT
            );
        }
    }

    private static void validateSourceCount(long sourceCount) {
        if (sourceCount < 0L || sourceCount > MAX_EXACT_COUNT) {
            throw new IllegalArgumentException(
                    "sourceCount must be between 0 and " + MAX_EXACT_COUNT
            );
        }
    }

    public record Observation(
            long appliedDelta,
            long progress,
            long target,
            long remaining,
            boolean complete
    ) {
    }
}
