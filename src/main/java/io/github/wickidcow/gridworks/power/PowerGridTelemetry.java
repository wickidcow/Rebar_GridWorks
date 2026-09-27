package io.github.wickidcow.gridworks.power;

import io.github.wickidcow.gridworks.api.control.ControlChannel;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.api.control.GridWorksChannels;
import io.github.wickidcow.gridworks.api.power.PowerGridSnapshot;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Stable mapping from provider-neutral power snapshots to Control Bus values.
 */
public final class PowerGridTelemetry {
    private PowerGridTelemetry() {
        throw new AssertionError("Utility class");
    }

    public static Map<ControlChannel, ControlValue> fromSnapshot(
            PowerGridSnapshot snapshot
    ) {
        Objects.requireNonNull(snapshot, "snapshot");

        Map<ControlChannel, ControlValue> values = new LinkedHashMap<>();
        values.put(GridWorksChannels.POWER_AVAILABLE, ControlValue.of(true));
        values.put(
                GridWorksChannels.POWER_NODE_COUNT,
                ControlValue.of((double) snapshot.nodeCount())
        );
        values.put(
                GridWorksChannels.POWER_PRODUCER_COUNT,
                ControlValue.of((double) snapshot.producerCount())
        );
        values.put(
                GridWorksChannels.POWER_CONSUMER_COUNT,
                ControlValue.of((double) snapshot.consumerCount())
        );
        values.put(
                GridWorksChannels.POWER_POWERED_CONSUMERS,
                ControlValue.of((double) snapshot.poweredConsumerCount())
        );
        values.put(
                GridWorksChannels.POWER_UNPOWERED_CONSUMERS,
                ControlValue.of((double) snapshot.unpoweredConsumerCount())
        );
        values.put(
                GridWorksChannels.POWER_PRODUCTION_CAPACITY_WATTS,
                ControlValue.of(snapshot.productionCapacityWatts())
        );
        values.put(
                GridWorksChannels.POWER_DEMAND_WATTS,
                ControlValue.of(snapshot.demandWatts())
        );
        values.put(
                GridWorksChannels.POWER_RESERVE_WATTS,
                ControlValue.of(snapshot.reserveWatts())
        );
        values.put(
                GridWorksChannels.POWER_LOAD_RATIO,
                ControlValue.of(snapshot.loadRatio())
        );
        values.put(
                GridWorksChannels.POWER_POWERED_CONSUMER_RATIO,
                ControlValue.of(snapshot.poweredConsumerRatio())
        );

        return Collections.unmodifiableMap(values);
    }

    public static Map<ControlChannel, ControlValue> unavailable() {
        return Map.of(
                GridWorksChannels.POWER_AVAILABLE,
                ControlValue.of(false)
        );
    }
}
