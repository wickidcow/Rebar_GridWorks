package io.github.wickidcow.gridworks.api.control;

import java.util.Locale;

public enum ControlOutputMode {
    CIRCUIT("Circuit"),
    ADDRESS("Address");

    private final String displayName;

    ControlOutputMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public ControlOutputMode toggle() {
        return this == CIRCUIT ? ADDRESS : CIRCUIT;
    }

    public static ControlOutputMode fromStored(String stored) {
        if (stored == null) {
            return CIRCUIT;
        }

        try {
            return valueOf(stored.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return CIRCUIT;
        }
    }
}
