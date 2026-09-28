package io.github.wickidcow.gridworks.production;

import java.util.OptionalDouble;

/**
 * Runtime-only pace estimate for positive Batch Controller progress events.
 *
 * <p>Progress events arriving within one short burst window are coalesced so
 * parallel Machine Sensors sampled back-to-back do not create an artificial
 * microsecond interval. The next distinct burst closes the prior interval and
 * produces the observed cycles-per-minute rate.</p>
 */
public final class BatchPaceTracker {
    static final long BURST_WINDOW_NANOS = 50_000_000L;

    private boolean burstActive;
    private long burstStartNanos;
    private long burstDelta;
    private double ratePerMinute;

    public synchronized void observeProgress(long appliedDelta, long nowNanos) {
        if (appliedDelta <= 0L) {
            throw new IllegalArgumentException("appliedDelta must be positive");
        }

        if (!burstActive) {
            startBurst(appliedDelta, nowNanos);
            ratePerMinute = 0.0;
            return;
        }

        long elapsedNanos = nowNanos - burstStartNanos;
        if (elapsedNanos <= BURST_WINDOW_NANOS) {
            addToBurst(appliedDelta);
            return;
        }

        ratePerMinute = burstDelta * 60_000_000_000.0 / elapsedNanos;
        if (!Double.isFinite(ratePerMinute) || ratePerMinute <= 0.0) {
            ratePerMinute = 0.0;
        }

        startBurst(appliedDelta, nowNanos);
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
        burstActive = false;
        burstStartNanos = 0L;
        burstDelta = 0L;
        ratePerMinute = 0.0;
    }

    private void startBurst(long appliedDelta, long nowNanos) {
        burstActive = true;
        burstStartNanos = nowNanos;
        burstDelta = Math.min(
                BatchProgressTracker.MAX_EXACT_COUNT,
                appliedDelta
        );
    }

    private void addToBurst(long appliedDelta) {
        long max = BatchProgressTracker.MAX_EXACT_COUNT;
        long safeDelta = Math.min(max, appliedDelta);
        burstDelta = burstDelta > max - safeDelta
                ? max
                : burstDelta + safeDelta;
    }
}
