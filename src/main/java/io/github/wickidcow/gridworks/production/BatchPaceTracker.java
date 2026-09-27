package io.github.wickidcow.gridworks.production;

import java.util.OptionalDouble;

/**
 * Runtime-only pace estimate for positive Batch Controller progress events.
 *
 * <p>The first event establishes a timing baseline. Each later event computes
 * the observed cycles-per-minute rate from that event's positive delta and the
 * monotonic time since the previous positive progress event.</p>
 */
public final class BatchPaceTracker {
    private boolean initialized;
    private long lastProgressNanos;
    private double ratePerMinute;

    public synchronized void observeProgress(long appliedDelta, long nowNanos) {
        if (appliedDelta <= 0L) {
            throw new IllegalArgumentException("appliedDelta must be positive");
        }

        if (!initialized) {
            initialized = true;
            lastProgressNanos = nowNanos;
            ratePerMinute = 0.0;
            return;
        }

        long elapsedNanos = nowNanos - lastProgressNanos;
        if (elapsedNanos <= 0L) {
            return;
        }

        lastProgressNanos = nowNanos;
        ratePerMinute = appliedDelta * 60_000_000_000.0 / elapsedNanos;
        if (!Double.isFinite(ratePerMinute) || ratePerMinute <= 0.0) {
            ratePerMinute = 0.0;
        }
    }

    public synchronized boolean isAvailable() {
        return ratePerMinute > 0.0;
    }

    public synchronized double ratePerMinute() {
        return ratePerMinute;
    }

    public synchronized OptionalDouble etaSeconds(long remainingCycles) {
        if (remainingCycles < 0L) {
            throw new IllegalArgumentException("remainingCycles must be non-negative");
        }
        if (remainingCycles == 0L) {
            return OptionalDouble.of(0.0);
        }
        if (!isAvailable()) {
            return OptionalDouble.empty();
        }

        double seconds = remainingCycles * 60.0 / ratePerMinute;
        return Double.isFinite(seconds) && seconds >= 0.0
                ? OptionalDouble.of(seconds)
                : OptionalDouble.empty();
    }

    public synchronized void reset() {
        initialized = false;
        lastProgressNanos = 0L;
        ratePerMinute = 0.0;
    }
}
