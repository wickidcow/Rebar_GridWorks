package io.github.wickidcow.gridworks.machine;

import java.util.Objects;

public record MachineSnapshot(
        boolean available,
        String kind,
        boolean processing,
        double progress,
        int processTimeTicks,
        int ticksRemaining
) {
    public MachineSnapshot {
        kind = Objects.requireNonNull(kind, "kind");

        if (!Double.isFinite(progress) || progress < 0.0 || progress > 1.0) {
            throw new IllegalArgumentException(
                    "progress must be finite and between 0 and 1"
            );
        }
        if (processTimeTicks < 0 || ticksRemaining < 0) {
            throw new IllegalArgumentException(
                    "machine tick values must be non-negative"
            );
        }
        if (!available) {
            if (!kind.isEmpty()
                    || processing
                    || progress != 0.0
                    || processTimeTicks != 0
                    || ticksRemaining != 0) {
                throw new IllegalArgumentException(
                        "unavailable machine snapshots must contain zero measurements"
                );
            }
        } else if (kind.isBlank()) {
            throw new IllegalArgumentException(
                    "available machine snapshots require a kind"
            );
        } else if (!processing
                && (progress != 0.0
                || processTimeTicks != 0
                || ticksRemaining != 0)) {
            throw new IllegalArgumentException(
                    "idle machine snapshots must contain zero progress measurements"
            );
        } else if (processing && processTimeTicks <= 0) {
            throw new IllegalArgumentException(
                    "processing machines require a positive process time"
            );
        }
    }

    public static MachineSnapshot unavailable() {
        return new MachineSnapshot(false, "", false, 0.0, 0, 0);
    }

    public static MachineSnapshot idle(String kind) {
        return new MachineSnapshot(true, kind, false, 0.0, 0, 0);
    }

    public static MachineSnapshot processing(
            String kind,
            Integer processTimeTicks,
            Integer ticksRemaining
    ) {
        Objects.requireNonNull(kind, "kind");

        int total = processTimeTicks == null ? 0 : Math.max(0, processTimeTicks);
        int remaining = ticksRemaining == null ? total : Math.max(0, ticksRemaining);

        if (total <= 0) {
            // A provider/interface claiming active processing without a usable
            // duration is not safe to expose as progress telemetry.
            return idle(kind);
        }

        remaining = Math.min(remaining, total);
        double progress = Math.clamp(
                1.0 - (remaining / (double) total),
                0.0,
                1.0
        );

        return new MachineSnapshot(
                true,
                kind,
                true,
                progress,
                total,
                remaining
        );
    }
}
