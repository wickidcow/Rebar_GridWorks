package io.github.wickidcow.gridworks.power.nativeapi;

/** Independent switch and limiter state; closing never discards the configured limit. */
public record BranchSettings(boolean enabled, double limitWatts) {
    public BranchSettings {
        if (!Double.isFinite(limitWatts) || limitWatts < 0) {
            throw new IllegalArgumentException("Power limit must be finite and non-negative");
        }
    }

    public double effectiveWatts() { return enabled ? limitWatts : 0; }
    public BranchSettings withEnabled(boolean value) { return new BranchSettings(value, limitWatts); }
    public BranchSettings withLimit(double value) { return new BranchSettings(enabled, value); }
}
