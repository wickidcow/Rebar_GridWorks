package io.github.wickidcow.gridworks.alarm;

/**
 * Small persisted history summary for one alarm.
 *
 * <p>GridWorks deliberately stores only aggregate occurrence count and the most
 * recent real trigger time. This avoids an unbounded event log while still
 * giving operators useful incident context.</p>
 */
public final class AlarmHistoryState {
    public static final long MAX_EXACT_COUNT = 9_007_199_254_740_991L;

    private long occurrenceCount;
    private long lastTriggeredEpochMillis;

    public AlarmHistoryState(long occurrenceCount, long lastTriggeredEpochMillis) {
        this.occurrenceCount = Math.max(
                0L,
                Math.min(MAX_EXACT_COUNT, occurrenceCount)
        );
        this.lastTriggeredEpochMillis = exactNonNegative(lastTriggeredEpochMillis);
    }

    public void recordTrigger(long epochMillis) {
        if (epochMillis < 0L) {
            throw new IllegalArgumentException("epochMillis must be non-negative");
        }

        if (occurrenceCount < MAX_EXACT_COUNT) {
            occurrenceCount++;
        }
        lastTriggeredEpochMillis = Math.max(
                lastTriggeredEpochMillis,
                exactNonNegative(epochMillis)
        );
    }

    public long occurrenceCount() {
        return occurrenceCount;
    }

    public long lastTriggeredEpochMillis() {
        return lastTriggeredEpochMillis;
    }

    public boolean hasTriggered() {
        return occurrenceCount > 0L && lastTriggeredEpochMillis > 0L;
    }

    private static long exactNonNegative(long value) {
        return Math.max(0L, Math.min(MAX_EXACT_COUNT, value));
    }
}
