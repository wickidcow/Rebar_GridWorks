package io.github.wickidcow.gridworks.production;

import java.util.Locale;

/**
 * Persisted cause for a Sequence Controller FAULT state.
 */
public enum SequenceFaultReason {
    NONE("None"),
    TIMEOUT("Stage timeout"),
    INTERLOCK("Fault interlock"),
    UNKNOWN("Unknown");

    private final String displayName;

    SequenceFaultReason(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public String telemetryValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static SequenceFaultReason fromStored(String stored, boolean faulted) {
        if (!faulted) {
            return NONE;
        }

        if (stored == null || stored.isBlank()) {
            return UNKNOWN;
        }

        try {
            SequenceFaultReason parsed = valueOf(stored.trim().toUpperCase(Locale.ROOT));
            return parsed == NONE ? UNKNOWN : parsed;
        } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
        }
    }
}
