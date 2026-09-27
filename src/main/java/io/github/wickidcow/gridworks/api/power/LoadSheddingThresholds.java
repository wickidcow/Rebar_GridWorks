package io.github.wickidcow.gridworks.api.power;

public record LoadSheddingThresholds(
        double optionalShedAt,
        double optionalRestoreAt,
        double normalShedAt,
        double normalRestoreAt
) {
    public static final double MAX_LOAD_RATIO = 10.0;

    public LoadSheddingThresholds {
        requireRange(optionalShedAt, "optionalShedAt");
        requireRange(optionalRestoreAt, "optionalRestoreAt");
        requireRange(normalShedAt, "normalShedAt");
        requireRange(normalRestoreAt, "normalRestoreAt");

        if (optionalRestoreAt > optionalShedAt) {
            throw new IllegalArgumentException(
                    "optionalRestoreAt must be <= optionalShedAt"
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
        if (normalRestoreAt > normalShedAt) {
            throw new IllegalArgumentException(
                    "normalRestoreAt must be <= normalShedAt"
            );
        }
    }

    public static LoadSheddingThresholds defaults() {
        return new LoadSheddingThresholds(0.90, 0.80, 1.00, 0.90);
    }

    public static LoadSheddingThresholds fromStoredOrDefault(
            Double optionalShedAt,
            Double optionalRestoreAt,
            Double normalShedAt,
            Double normalRestoreAt
    ) {
        if (optionalShedAt == null
                || optionalRestoreAt == null
                || normalShedAt == null
                || normalRestoreAt == null) {
            return defaults();
        }

        try {
            return new LoadSheddingThresholds(
                    optionalShedAt,
                    optionalRestoreAt,
                    normalShedAt,
                    normalRestoreAt
            );
        } catch (IllegalArgumentException ignored) {
            return defaults();
        }
    }

    public LoadSheddingPolicy createPolicy(LoadSheddingStage initialStage) {
        return new LoadSheddingPolicy(
                optionalShedAt,
                optionalRestoreAt,
                normalShedAt,
                normalRestoreAt,
                initialStage
        );
    }

    public LoadSheddingThresholds withOptionalShedAt(double value) {
        return new LoadSheddingThresholds(
                Math.clamp(value, optionalRestoreAt, normalShedAt),
                optionalRestoreAt,
                normalShedAt,
                normalRestoreAt
        );
    }

    public LoadSheddingThresholds withOptionalRestoreAt(double value) {
        return new LoadSheddingThresholds(
                optionalShedAt,
                Math.clamp(
                        value,
                        0.0,
                        Math.min(optionalShedAt, normalRestoreAt)
                ),
                normalShedAt,
                normalRestoreAt
        );
    }

    public LoadSheddingThresholds withNormalShedAt(double value) {
        return new LoadSheddingThresholds(
                optionalShedAt,
                optionalRestoreAt,
                Math.clamp(
                        value,
                        Math.max(optionalShedAt, normalRestoreAt),
                        MAX_LOAD_RATIO
                ),
                normalRestoreAt
        );
    }

    public LoadSheddingThresholds withNormalRestoreAt(double value) {
        return new LoadSheddingThresholds(
                optionalShedAt,
                optionalRestoreAt,
                normalShedAt,
                Math.clamp(value, optionalRestoreAt, normalShedAt)
        );
    }

    private static void requireRange(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0 || value > MAX_LOAD_RATIO) {
            throw new IllegalArgumentException(
                    name + " must be finite and between 0 and " + MAX_LOAD_RATIO
            );
        }
    }
}
