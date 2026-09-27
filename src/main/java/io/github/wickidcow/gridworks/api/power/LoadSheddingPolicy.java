package io.github.wickidcow.gridworks.api.power;

import java.util.Objects;

/**
 * Stateful load-shedding policy with hysteresis.
 *
 * <p>The policy consumes power telemetry snapshots only when new telemetry
 * arrives. It does not poll or schedule work.</p>
 */
public final class LoadSheddingPolicy {
    private final double optionalShedAt;
    private final double optionalRestoreAt;
    private final double normalShedAt;
    private final double normalRestoreAt;

    private LoadSheddingStage stage;

    public LoadSheddingPolicy(
            double optionalShedAt,
            double optionalRestoreAt,
            double normalShedAt,
            double normalRestoreAt
    ) {
        this(
                optionalShedAt,
                optionalRestoreAt,
                normalShedAt,
                normalRestoreAt,
                LoadSheddingStage.NORMAL
        );
    }

    public LoadSheddingPolicy(
            double optionalShedAt,
            double optionalRestoreAt,
            double normalShedAt,
            double normalRestoreAt,
            LoadSheddingStage initialStage
    ) {
        requireFiniteNonNegative(optionalShedAt, "optionalShedAt");
        requireFiniteNonNegative(optionalRestoreAt, "optionalRestoreAt");
        requireFiniteNonNegative(normalShedAt, "normalShedAt");
        requireFiniteNonNegative(normalRestoreAt, "normalRestoreAt");

        if (optionalRestoreAt > optionalShedAt) {
            throw new IllegalArgumentException(
                    "optionalRestoreAt must be <= optionalShedAt"
            );
        }
        if (normalRestoreAt > normalShedAt) {
            throw new IllegalArgumentException(
                    "normalRestoreAt must be <= normalShedAt"
            );
        }
        if (normalShedAt < optionalShedAt) {
            throw new IllegalArgumentException(
                    "normalShedAt must be >= optionalShedAt"
            );
        }
        if (normalRestoreAt < optionalRestoreAt) {
            throw new IllegalArgumentException(
                    "normalRestoreAt must be >= optionalRestoreAt"
            );
        }

        this.optionalShedAt = optionalShedAt;
        this.optionalRestoreAt = optionalRestoreAt;
        this.normalShedAt = normalShedAt;
        this.normalRestoreAt = normalRestoreAt;
        this.stage = Objects.requireNonNull(initialStage, "initialStage");
    }

    public static LoadSheddingPolicy defaults() {
        return defaults(LoadSheddingStage.NORMAL);
    }

    public static LoadSheddingPolicy defaults(LoadSheddingStage initialStage) {
        return new LoadSheddingPolicy(
                0.90,
                0.80,
                1.00,
                0.90,
                initialStage
        );
    }

    public LoadSheddingStage stage() {
        return stage;
    }

    public LoadSheddingStage update(PowerGridSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return update(snapshot.loadRatio(), snapshot.unpoweredConsumerCount());
    }

    public LoadSheddingStage update(
            double loadRatio,
            int unpoweredConsumers
    ) {
        requireFiniteNonNegative(loadRatio, "loadRatio");
        if (unpoweredConsumers < 0) {
            throw new IllegalArgumentException(
                    "unpoweredConsumers must be non-negative"
            );
        }

        stage = switch (stage) {
            case NORMAL -> {
                if (mustShedNormal(loadRatio, unpoweredConsumers)) {
                    yield LoadSheddingStage.SHED_NORMAL_AND_OPTIONAL;
                }
                if (loadRatio >= optionalShedAt) {
                    yield LoadSheddingStage.SHED_OPTIONAL;
                }
                yield LoadSheddingStage.NORMAL;
            }
            case SHED_OPTIONAL -> {
                if (mustShedNormal(loadRatio, unpoweredConsumers)) {
                    yield LoadSheddingStage.SHED_NORMAL_AND_OPTIONAL;
                }
                if (loadRatio <= optionalRestoreAt) {
                    yield LoadSheddingStage.NORMAL;
                }
                yield LoadSheddingStage.SHED_OPTIONAL;
            }
            case SHED_NORMAL_AND_OPTIONAL -> {
                if (unpoweredConsumers > 0 || loadRatio > normalRestoreAt) {
                    yield LoadSheddingStage.SHED_NORMAL_AND_OPTIONAL;
                }
                if (loadRatio <= optionalRestoreAt) {
                    yield LoadSheddingStage.NORMAL;
                }
                yield LoadSheddingStage.SHED_OPTIONAL;
            }
        };

        return stage;
    }

    public void reset() {
        stage = LoadSheddingStage.NORMAL;
    }

    public double optionalShedAt() {
        return optionalShedAt;
    }

    public double optionalRestoreAt() {
        return optionalRestoreAt;
    }

    public double normalShedAt() {
        return normalShedAt;
    }

    public double normalRestoreAt() {
        return normalRestoreAt;
    }

    private boolean mustShedNormal(double loadRatio, int unpoweredConsumers) {
        return unpoweredConsumers > 0 || loadRatio >= normalShedAt;
    }

    private static void requireFiniteNonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(
                    name + " must be finite and non-negative"
            );
        }
    }
}
