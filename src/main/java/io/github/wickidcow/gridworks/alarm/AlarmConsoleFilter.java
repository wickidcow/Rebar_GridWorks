package io.github.wickidcow.gridworks.alarm;

import java.util.Locale;

public enum AlarmConsoleFilter {
    ALL("All"),
    WARNING_PLUS("Warning+"),
    CRITICAL_ONLY("Critical only"),
    LATCHED_ONLY("Latched only"),
    UNACKNOWLEDGED_ONLY("Unacknowledged only");

    private final String displayName;

    AlarmConsoleFilter(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean accepts(AlarmTelemetryState.Snapshot snapshot) {
        return switch (this) {
            case ALL -> true;
            case WARNING_PLUS -> snapshot.severity().priority() <= AlarmSeverity.WARNING.priority();
            case CRITICAL_ONLY -> snapshot.severity() == AlarmSeverity.CRITICAL;
            case LATCHED_ONLY -> snapshot.isLatched();
            case UNACKNOWLEDGED_ONLY -> snapshot.isLatched() && !snapshot.isAcknowledged();
        };
    }

    public AlarmConsoleFilter cycle(int direction) {
        AlarmConsoleFilter[] values = values();
        int step = direction >= 0 ? 1 : -1;
        return values[Math.floorMod(ordinal() + step, values.length)];
    }

    public static AlarmConsoleFilter fromStored(String stored) {
        if (stored == null) {
            return ALL;
        }

        try {
            return valueOf(stored.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ALL;
        }
    }
}
