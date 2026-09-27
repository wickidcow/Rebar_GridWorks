package io.github.wickidcow.gridworks.api.power;

public record PowerLimitSetting(boolean enabled, double limitWatts) {
    public PowerLimitSetting {
        if (!Double.isFinite(limitWatts) || limitWatts <= 0.0) {
            throw new IllegalArgumentException(
                    "limitWatts must be finite and greater than zero"
            );
        }
    }

    /**
     * Double.MAX_VALUE is the provider-neutral representation of bypass/no cap.
     */
    public double effectiveLimitWatts() {
        return enabled ? limitWatts : Double.MAX_VALUE;
    }

    public PowerLimitSetting withEnabled(boolean next) {
        return new PowerLimitSetting(next, limitWatts);
    }

    public PowerLimitSetting withLimitWatts(double next) {
        return new PowerLimitSetting(enabled, next);
    }
}
