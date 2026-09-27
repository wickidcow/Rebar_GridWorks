package io.github.wickidcow.gridworks.api.power;

import java.util.Locale;

public enum LoadSheddingFailSafeMode {
    ESSENTIAL_ONLY("Essential Only"),
    ALLOW_ALL("Allow All"),
    HOLD_LAST("Hold Last");

    private final String displayName;

    LoadSheddingFailSafeMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public LoadSheddingFailSafeMode cycle(int direction) {
        LoadSheddingFailSafeMode[] values = values();
        int step = direction >= 0 ? 1 : -1;
        return values[Math.floorMod(ordinal() + step, values.length)];
    }

    public LoadSheddingOutputState resolve(LoadSheddingStage lastKnownStage) {
        return switch (this) {
            case ESSENTIAL_ONLY -> new LoadSheddingOutputState(true, false, false);
            case ALLOW_ALL -> new LoadSheddingOutputState(true, true, true);
            case HOLD_LAST -> LoadSheddingOutputState.fromStage(lastKnownStage);
        };
    }

    public static LoadSheddingFailSafeMode fromStored(String stored) {
        if (stored == null) {
            return ESSENTIAL_ONLY;
        }

        try {
            return valueOf(stored.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ESSENTIAL_ONLY;
        }
    }
}
