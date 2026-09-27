package io.github.wickidcow.gridworks.api.control;

import java.util.Locale;

public enum ControlInputRouteMode {
    CIRCUIT("Circuit"),
    ADDRESS("Address");

    private final String displayName;

    ControlInputRouteMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public ControlInputRouteMode toggle() {
        return this == CIRCUIT ? ADDRESS : CIRCUIT;
    }

    public static ControlInputRouteMode fromStored(String stored) {
        if (stored == null) {
            return CIRCUIT;
        }

        try {
            return valueOf(stored.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return CIRCUIT;
        }
    }
}
