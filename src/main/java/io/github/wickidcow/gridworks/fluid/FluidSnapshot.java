package io.github.wickidcow.gridworks.fluid;

import java.util.Objects;

public record FluidSnapshot(
        boolean available,
        String fluidKey,
        double amount,
        double capacity
) {
    public FluidSnapshot {
        fluidKey = Objects.requireNonNull(fluidKey, "fluidKey");
        if (!Double.isFinite(amount) || amount < 0.0) {
            throw new IllegalArgumentException("amount must be finite and non-negative");
        }
        if (!Double.isFinite(capacity) || capacity < 0.0) {
            throw new IllegalArgumentException("capacity must be finite and non-negative");
        }
        if (!available && (!fluidKey.isEmpty() || amount != 0.0 || capacity != 0.0)) {
            throw new IllegalArgumentException("unavailable snapshots must contain zero measurements");
        }
    }

    public static FluidSnapshot unavailable() {
        return new FluidSnapshot(false, "", 0.0, 0.0);
    }

    public boolean hasFluid() {
        return available && !fluidKey.isEmpty() && amount > 0.0;
    }

    public double fillRatio() {
        return capacity <= 0.0 ? 0.0 : amount / capacity;
    }
}
