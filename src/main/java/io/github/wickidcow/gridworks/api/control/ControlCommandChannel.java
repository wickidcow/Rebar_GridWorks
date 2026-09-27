package io.github.wickidcow.gridworks.api.control;

import java.util.Locale;

public enum ControlCommandChannel {
    DEFAULT("Default", GridWorksChannels.CONTROL_ENABLED),
    A("A", GridWorksChannels.CONTROL_A),
    B("B", GridWorksChannels.CONTROL_B),
    C("C", GridWorksChannels.CONTROL_C),
    D("D", GridWorksChannels.CONTROL_D);

    private final String displayName;
    private final ControlChannel channel;

    ControlCommandChannel(String displayName, ControlChannel channel) {
        this.displayName = displayName;
        this.channel = channel;
    }

    public String displayName() {
        return displayName;
    }

    public ControlChannel channel() {
        return channel;
    }

    public ControlCommandChannel cycle(int direction) {
        ControlCommandChannel[] values = values();
        int step = direction >= 0 ? 1 : -1;
        return values[Math.floorMod(ordinal() + step, values.length)];
    }

    public static ControlCommandChannel fromStored(String stored) {
        if (stored == null) {
            return DEFAULT;
        }

        try {
            return valueOf(stored.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return DEFAULT;
        }
    }
}
