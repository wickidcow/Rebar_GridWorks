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

    private GridWorksChannels() {
        throw new AssertionError("Utility class");
    }
}
