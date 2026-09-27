package io.github.wickidcow.gridworks.alarm;

/**
 * Small persisted history summary for one alarm.
 *
 * <p>GridWorks deliberately stores only aggregate occurrence count and the most
 * recent real trigger time. This avoids an unbounded event log while still
 * giving operators useful incident context.</p>
 */
public final class AlarmHistoryState {
    private long occurrenceCount;
    private long lastTriggeredEpochMillis;

    public AlarmHistoryState(long occurrenceCount, long lastTriggeredEpochMillis) {
        this.occurrenceCount = Math.max(0L, occurrenceCount);
        this.lastTriggeredEpochMillis = Math.max(0L, lastTriggeredEpochMillis);
    }

    public void recordTrigger(long epochMillis) {
        if (epochMillis < 0L) {
            throw new IllegalArgumentException("epochMillis must be non-negative");
        }

        if (occurrenceCount < Long.MAX_VALUE) {
            occurrenceCount++;
        }
        lastTriggeredEpochMillis = Math.max(lastTriggeredEpochMillis, epochMillis);
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
}
