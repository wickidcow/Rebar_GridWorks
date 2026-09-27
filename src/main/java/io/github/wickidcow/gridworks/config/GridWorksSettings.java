package io.github.wickidcow.gridworks.config;

import io.github.wickidcow.gridworks.GridWorks;
import org.bukkit.configuration.file.FileConfiguration;

public record GridWorksSettings(
        int maxPropagationNodes,
        long inventorySampleIntervalTicks,
        long fluidSampleIntervalTicks,
        long machineSampleIntervalTicks,
        long powerSampleIntervalTicks,
        int cargoIsolatorTransferRate
) {
    private static final int DEFAULT_MAX_PROPAGATION_NODES = 4096;
    private static final long DEFAULT_INVENTORY_SAMPLE_INTERVAL_TICKS = 10L;
    private static final long DEFAULT_FLUID_SAMPLE_INTERVAL_TICKS = 10L;
    private static final long DEFAULT_MACHINE_SAMPLE_INTERVAL_TICKS = 20L;
    private static final long DEFAULT_POWER_SAMPLE_INTERVAL_TICKS = 20L;
    private static final int DEFAULT_CARGO_ISOLATOR_TRANSFER_RATE = 1;

    public GridWorksSettings {
        if (maxPropagationNodes <= 0
                || inventorySampleIntervalTicks <= 0L
                || fluidSampleIntervalTicks <= 0L
                || machineSampleIntervalTicks <= 0L
                || powerSampleIntervalTicks <= 0L
                || cargoIsolatorTransferRate <= 0) {
            throw new IllegalArgumentException(
                    "GridWorksSettings values must all be positive"
            );
        }
    }

    public static GridWorksSettings load(GridWorks plugin) {
        FileConfiguration config = plugin.getConfig();

        return new GridWorksSettings(
                NumericSettingValidator.positiveInt(
                        config.get("control-bus.max-propagation-nodes"),
                        DEFAULT_MAX_PROPAGATION_NODES,
                        "control-bus.max-propagation-nodes"
                ),
                NumericSettingValidator.positiveLong(
                        config.get("sensors.inventory.sample-interval-ticks"),
                        DEFAULT_INVENTORY_SAMPLE_INTERVAL_TICKS,
                        "sensors.inventory.sample-interval-ticks"
                ),
                NumericSettingValidator.positiveLong(
                        config.get("sensors.fluid.sample-interval-ticks"),
                        DEFAULT_FLUID_SAMPLE_INTERVAL_TICKS,
                        "sensors.fluid.sample-interval-ticks"
                ),
                NumericSettingValidator.positiveLong(
                        config.get("sensors.machine.sample-interval-ticks"),
                        DEFAULT_MACHINE_SAMPLE_INTERVAL_TICKS,
                        "sensors.machine.sample-interval-ticks"
                ),
                NumericSettingValidator.positiveLong(
                        config.get("sensors.power.sample-interval-ticks"),
                        DEFAULT_POWER_SAMPLE_INTERVAL_TICKS,
                        "sensors.power.sample-interval-ticks"
                ),
                NumericSettingValidator.positiveInt(
                        config.get("cargo.isolator.transfer-rate"),
                        DEFAULT_CARGO_ISOLATOR_TRANSFER_RATE,
                        "cargo.isolator.transfer-rate"
                )
        );
    }

    public String describe() {
        return "bus cap "
                + maxPropagationNodes
                + "; sensor ticks inventory/fluid/machine/power "
                + inventorySampleIntervalTicks
                + "/"
                + fluidSampleIntervalTicks
                + "/"
                + machineSampleIntervalTicks
                + "/"
                + powerSampleIntervalTicks
                + "; cargo isolator rate "
                + cargoIsolatorTransferRate;
    }
}
