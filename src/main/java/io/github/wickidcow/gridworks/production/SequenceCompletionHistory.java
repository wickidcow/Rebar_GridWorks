package io.github.wickidcow.gridworks.production;

/**
 * Persistent completion history for a Sequence Controller.
 */
public final class SequenceCompletionHistory {
    public static final long MAX_EXACT_COUNT = 9_007_199_254_740_991L;

    private long completedRuns;
    private long lastCompletionEpochMillis;

    public SequenceCompletionHistory() {
        this(0L, 0L);
    }

    public SequenceCompletionHistory(
            long completedRuns,
            long lastCompletionEpochMillis
    ) {
        if (completedRuns < 0L || completedRuns > MAX_EXACT_COUNT) {
            throw new IllegalArgumentException(
                    "completedRuns must be between 0 and " + MAX_EXACT_COUNT
            );
        }
        if (lastCompletionEpochMillis < 0L) {
            throw new IllegalArgumentException(
                    "lastCompletionEpochMillis must be non-negative"
            );
        }

        this.completedRuns = completedRuns;
        this.lastCompletionEpochMillis = lastCompletionEpochMillis;
    }

    public static SequenceCompletionHistory fromStored(
            Long completedRuns,
            Long lastCompletionEpochMillis
    ) {
        long safeRuns = completedRuns == null
                ? 0L
                : Math.max(0L, Math.min(MAX_EXACT_COUNT, completedRuns));
        long safeTimestamp = lastCompletionEpochMillis == null
                ? 0L
                : Math.max(0L, lastCompletionEpochMillis);
        return new SequenceCompletionHistory(safeRuns, safeTimestamp);
    }

    public synchronized void recordCompletion(long nowEpochMillis) {
        if (completedRuns < MAX_EXACT_COUNT) {
            completedRuns++;
        }
        lastCompletionEpochMillis = Math.max(
                lastCompletionEpochMillis,
                Math.max(0L, nowEpochMillis)
        );
    }

    public synchronized long completedRuns() {
        return completedRuns;
    }

    public synchronized long lastCompletionEpochMillis() {
        return lastCompletionEpochMillis;
    }
}
