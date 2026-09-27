package io.github.wickidcow.gridworks.api.power;

/**
 * Provider-neutral summary of one loaded electricity network.
 *
 * <p>The model intentionally contains only measurements GridWorks needs for
 * monitoring and control. It does not expose Rebar node classes, graph edges,
 * handlers, or other upstream implementation details.</p>
 */
public record PowerGridSnapshot(
        int nodeCount,
        int producerCount,
        int consumerCount,
        int poweredConsumerCount,
        double productionCapacityWatts,
        double demandWatts
) {
    public PowerGridSnapshot {
        if (nodeCount < 0
                || producerCount < 0
                || consumerCount < 0
                || poweredConsumerCount < 0) {
            throw new IllegalArgumentException("Power-grid counts must be non-negative");
        }
        if (producerCount > nodeCount || consumerCount > nodeCount) {
            throw new IllegalArgumentException(
                    "Producer/consumer counts cannot exceed total node count"
            );
        }
        if (poweredConsumerCount > consumerCount) {
            throw new IllegalArgumentException(
                    "Powered consumer count cannot exceed consumer count"
            );
        }
        if (!Double.isFinite(productionCapacityWatts)
                || productionCapacityWatts < 0.0
                || !Double.isFinite(demandWatts)
                || demandWatts < 0.0) {
            throw new IllegalArgumentException(
                    "Power values must be finite and non-negative"
            );
        }
    }

    public int unpoweredConsumerCount() {
        return consumerCount - poweredConsumerCount;
    }

    public boolean fullyPowered() {
        return unpoweredConsumerCount() == 0;
    }

    /**
     * Demand divided by production capacity.
     *
     * <p>0.0 means no demand. Positive infinity is represented as
     * {@link Double#MAX_VALUE} when demand exists but production is zero, so the
     * result remains a valid finite Control Bus number.</p>
     */
    public double loadRatio() {
        if (demandWatts == 0.0) {
            return 0.0;
        }
        if (productionCapacityWatts == 0.0) {
            return Double.MAX_VALUE;
        }
        return demandWatts / productionCapacityWatts;
    }

    /**
     * Simple capacity-minus-demand reserve.
     *
     * <p>This is not a promise that every consumer can be reached: an upstream
     * provider may still report unpowered consumers because of branch/edge
     * limits even when this number is positive.</p>
     */
    public double reserveWatts() {
        return productionCapacityWatts - demandWatts;
    }

    public double poweredConsumerRatio() {
        if (consumerCount == 0) {
            return 1.0;
        }
        return poweredConsumerCount / (double) consumerCount;
    }
}
