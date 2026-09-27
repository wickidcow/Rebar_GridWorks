package io.github.wickidcow.gridworks.api.control;

public final class GridWorksChannels {
    public static final ControlChannel REDSTONE_POWERED =
            ControlChannel.of("gridworks", "redstone/powered");

    public static final ControlChannel REDSTONE_STRENGTH =
            ControlChannel.of("gridworks", "redstone/strength");

    public static final ControlChannel INVENTORY_AVAILABLE =
            ControlChannel.of("gridworks", "inventory/available");

    public static final ControlChannel INVENTORY_ITEMS =
            ControlChannel.of("gridworks", "inventory/items");

    public static final ControlChannel INVENTORY_OCCUPIED_SLOTS =
            ControlChannel.of("gridworks", "inventory/occupied_slots");

    public static final ControlChannel INVENTORY_TOTAL_SLOTS =
            ControlChannel.of("gridworks", "inventory/total_slots");

    public static final ControlChannel INVENTORY_OCCUPIED_RATIO =
            ControlChannel.of("gridworks", "inventory/occupied_ratio");

    public static final ControlChannel FLUID_AVAILABLE =
            ControlChannel.of("gridworks", "fluid/available");

    public static final ControlChannel FLUID_PRESENT =
            ControlChannel.of("gridworks", "fluid/present");

    public static final ControlChannel FLUID_TYPE =
            ControlChannel.of("gridworks", "fluid/type");

    public static final ControlChannel FLUID_AMOUNT =
            ControlChannel.of("gridworks", "fluid/amount");

    public static final ControlChannel FLUID_CAPACITY =
            ControlChannel.of("gridworks", "fluid/capacity");

    public static final ControlChannel FLUID_FILL_RATIO =
            ControlChannel.of("gridworks", "fluid/fill_ratio");

    /**
     * Backward-compatible default boolean command circuit.
     */
    public static final ControlChannel CONTROL_ENABLED =
            ControlChannel.of("gridworks", "control/enabled");

    public static final ControlChannel CONTROL_A =
            ControlChannel.of("gridworks", "control/a");

    public static final ControlChannel CONTROL_B =
            ControlChannel.of("gridworks", "control/b");

    public static final ControlChannel CONTROL_C =
            ControlChannel.of("gridworks", "control/c");

    public static final ControlChannel CONTROL_D =
            ControlChannel.of("gridworks", "control/d");

    public static final ControlChannel POWER_AVAILABLE =
            ControlChannel.of("gridworks", "power/available");

    public static final ControlChannel POWER_NODE_COUNT =
            ControlChannel.of("gridworks", "power/node_count");

    public static final ControlChannel POWER_PRODUCER_COUNT =
            ControlChannel.of("gridworks", "power/producer_count");

    public static final ControlChannel POWER_CONSUMER_COUNT =
            ControlChannel.of("gridworks", "power/consumer_count");

    public static final ControlChannel POWER_POWERED_CONSUMERS =
            ControlChannel.of("gridworks", "power/powered_consumers");

    public static final ControlChannel POWER_UNPOWERED_CONSUMERS =
            ControlChannel.of("gridworks", "power/unpowered_consumers");

    public static final ControlChannel POWER_PRODUCTION_CAPACITY_WATTS =
            ControlChannel.of("gridworks", "power/production_capacity_watts");

    public static final ControlChannel POWER_DEMAND_WATTS =
            ControlChannel.of("gridworks", "power/demand_watts");

    public static final ControlChannel POWER_RESERVE_WATTS =
            ControlChannel.of("gridworks", "power/reserve_watts");

    public static final ControlChannel POWER_LOAD_RATIO =
            ControlChannel.of("gridworks", "power/load_ratio");

    public static final ControlChannel POWER_POWERED_CONSUMER_RATIO =
            ControlChannel.of("gridworks", "power/powered_consumer_ratio");

    public static final ControlChannel ALARM_NAME =
            ControlChannel.of("gridworks", "alarm/name");

    public static final ControlChannel ALARM_SEVERITY =
            ControlChannel.of("gridworks", "alarm/severity");

    public static final ControlChannel ALARM_CONDITION_ACTIVE =
            ControlChannel.of("gridworks", "alarm/condition_active");

    public static final ControlChannel ALARM_LATCHED =
            ControlChannel.of("gridworks", "alarm/latched");

    public static final ControlChannel ALARM_ACKNOWLEDGED =
            ControlChannel.of("gridworks", "alarm/acknowledged");

    public static final ControlChannel ALARM_OCCURRENCES =
            ControlChannel.of("gridworks", "alarm/occurrences");

    public static final ControlChannel ALARM_LAST_TRIGGERED_EPOCH_MS =
            ControlChannel.of("gridworks", "alarm/last_triggered_epoch_ms");

    /**
     * Text command. Value is either a target node UUID or "*" for every alarm
     * in the current Control Bus component.
     */
    public static final ControlChannel ALARM_ACKNOWLEDGE =
            ControlChannel.of("gridworks", "alarm/acknowledge");

    private GridWorksChannels() {
        throw new AssertionError("Utility class");
    }
}
