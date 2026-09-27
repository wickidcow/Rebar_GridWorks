package io.github.wickidcow.gridworks.api.control;

import java.util.Locale;

public enum BooleanInputMode {
    LEGACY("Legacy: Redstone + Default"),
    REDSTONE("Redstone only"),
    DEFAULT("Command: Default"),
    A("Command: A"),
    B("Command: B"),
    C("Command: C"),
    D("Command: D");

    private final String displayName;

    BooleanInputMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean accepts(ControlChannel channel) {
        return switch (this) {
            case LEGACY -> GridWorksChannels.REDSTONE_POWERED.equals(channel)
                    || GridWorksChannels.CONTROL_ENABLED.equals(channel);
            case REDSTONE -> GridWorksChannels.REDSTONE_POWERED.equals(channel);
            case DEFAULT -> GridWorksChannels.CONTROL_ENABLED.equals(channel);
            case A -> GridWorksChannels.CONTROL_A.equals(channel);
            case B -> GridWorksChannels.CONTROL_B.equals(channel);
            case C -> GridWorksChannels.CONTROL_C.equals(channel);
            case D -> GridWorksChannels.CONTROL_D.equals(channel);
        };
    }

    public BooleanInputMode cycle(int direction) {
        BooleanInputMode[] values = values();
        int step = direction >= 0 ? 1 : -1;
        return values[Math.floorMod(ordinal() + step, values.length)];
    }

    public static BooleanInputMode fromStored(String stored) {
        if (stored == null) {
            return LEGACY;
        }

        try {
            return valueOf(stored.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return LEGACY;
        }
    }
}
