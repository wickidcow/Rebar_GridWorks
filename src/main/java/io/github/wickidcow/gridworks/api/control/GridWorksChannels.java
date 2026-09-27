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

    private GridWorksChannels() {
        throw new AssertionError("Utility class");
    }
}
