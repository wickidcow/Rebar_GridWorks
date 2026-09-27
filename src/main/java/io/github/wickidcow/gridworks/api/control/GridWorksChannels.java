package io.github.wickidcow.gridworks.api.control;

public final class GridWorksChannels {
    public static final ControlChannel REDSTONE_POWERED =
            ControlChannel.of("gridworks", "redstone/powered");

    public static final ControlChannel REDSTONE_STRENGTH =
            ControlChannel.of("gridworks", "redstone/strength");

    public static final ControlChannel CONTROL_ENABLED =
            ControlChannel.of("gridworks", "control/enabled");

    private GridWorksChannels() {
        throw new AssertionError("Utility class");
    }
}
