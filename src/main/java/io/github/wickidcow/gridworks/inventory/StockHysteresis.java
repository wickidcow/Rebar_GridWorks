package io.github.wickidcow.gridworks.inventory;

public final class StockHysteresis {
    private StockHysteresis() {
        throw new AssertionError("Utility class");
    }

    public static boolean nextDemand(
            boolean currentDemand,
            double value,
            double lowThreshold,
            double highThreshold
    ) {
        validateThresholds(lowThreshold, highThreshold);
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException("stock value must be finite and non-negative");
        }

        if (currentDemand) {
            return value < highThreshold;
        }
        return value <= lowThreshold;
    }

    public static void validateThresholds(
            double lowThreshold,
            double highThreshold
    ) {
        if (!Double.isFinite(lowThreshold)
                || !Double.isFinite(highThreshold)
                || lowThreshold < 0.0
                || highThreshold <= lowThreshold) {
            throw new IllegalArgumentException(
                    "stock thresholds require 0 <= low < high"
            );
        }
    }

    public static double clampLow(
            double candidate,
            double highThreshold,
            double minimumGap
    ) {
        if (!Double.isFinite(candidate)
                || !Double.isFinite(highThreshold)
                || !Double.isFinite(minimumGap)
                || minimumGap <= 0.0) {
            throw new IllegalArgumentException("stock threshold inputs must be finite");
        }
        return Math.clamp(candidate, 0.0, Math.max(0.0, highThreshold - minimumGap));
    }

    public static double clampHigh(
            double candidate,
            double lowThreshold,
            double minimumGap,
            double maximum
    ) {
        if (!Double.isFinite(candidate)
                || !Double.isFinite(lowThreshold)
                || !Double.isFinite(minimumGap)
                || !Double.isFinite(maximum)
                || minimumGap <= 0.0
                || maximum <= 0.0) {
            throw new IllegalArgumentException("stock threshold inputs must be finite");
        }

        double minimum = Math.min(maximum, lowThreshold + minimumGap);
        return Math.clamp(candidate, minimum, maximum);
    }
}
