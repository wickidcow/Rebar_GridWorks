package io.github.wickidcow.gridworks.api.control;

import java.util.Optional;

/**
 * Maps numeric measurement channels to the boolean availability channel that
 * determines whether those measurements are currently meaningful.
 */
public final class MetricAvailability {
    private MetricAvailability() {
        throw new AssertionError("Utility class");
    }

    public static Optional<ControlChannel> channelFor(ControlChannel metric) {
        if (GridWorksChannels.INVENTORY_ITEMS.equals(metric)
                || GridWorksChannels.INVENTORY_OCCUPIED_SLOTS.equals(metric)
                || GridWorksChannels.INVENTORY_TOTAL_SLOTS.equals(metric)
                || GridWorksChannels.INVENTORY_OCCUPIED_RATIO.equals(metric)) {
            return Optional.of(GridWorksChannels.INVENTORY_AVAILABLE);
        }

        if (GridWorksChannels.FLUID_AMOUNT.equals(metric)
                || GridWorksChannels.FLUID_CAPACITY.equals(metric)
                || GridWorksChannels.FLUID_FILL_RATIO.equals(metric)) {
            return Optional.of(GridWorksChannels.FLUID_AVAILABLE);
        }

        if (GridWorksChannels.MACHINE_PROGRESS.equals(metric)
                || GridWorksChannels.MACHINE_PROCESS_TIME_TICKS.equals(metric)
                || GridWorksChannels.MACHINE_TICKS_REMAINING.equals(metric)) {
            return Optional.of(GridWorksChannels.MACHINE_AVAILABLE);
        }

        if (GridWorksChannels.POWER_NODE_COUNT.equals(metric)
                || GridWorksChannels.POWER_PRODUCER_COUNT.equals(metric)
                || GridWorksChannels.POWER_CONSUMER_COUNT.equals(metric)
                || GridWorksChannels.POWER_POWERED_CONSUMERS.equals(metric)
                || GridWorksChannels.POWER_UNPOWERED_CONSUMERS.equals(metric)
                || GridWorksChannels.POWER_PRODUCTION_CAPACITY_WATTS.equals(metric)
                || GridWorksChannels.POWER_DEMAND_WATTS.equals(metric)
                || GridWorksChannels.POWER_RESERVE_WATTS.equals(metric)
                || GridWorksChannels.POWER_LOAD_RATIO.equals(metric)
                || GridWorksChannels.POWER_POWERED_CONSUMER_RATIO.equals(metric)) {
            return Optional.of(GridWorksChannels.POWER_AVAILABLE);
        }

        return Optional.empty();
    }
}
