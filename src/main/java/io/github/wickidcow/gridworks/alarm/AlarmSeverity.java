package io.github.wickidcow.gridworks.alarm;

import java.util.Locale;

public enum AlarmSeverity {
    CRITICAL("Critical", 0),
    WARNING("Warning", 1),
    INFO("Info", 2);

    private final String displayName;
    private final int priority;

    AlarmSeverity(String displayName, int priority) {
        this.displayName = displayName;
        this.priority = priority;
    }

    public String displayName() {
        return displayName;
    }

    public int priority() {
        return priority;
    }

    public AlarmSeverity cycle(int direction) {
        AlarmSeverity[] values = values();
        int step = direction >= 0 ? 1 : -1;
        return values[Math.floorMod(ordinal() + step, values.length)];
    }

    public static AlarmSeverity fromStored(String stored) {
        if (stored == null) {
            return WARNING;
        }

        try {
            return valueOf(stored.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return WARNING;
        }
    }
}
