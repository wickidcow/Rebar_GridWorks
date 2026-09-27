package io.github.wickidcow.gridworks.api.power;

import java.util.Locale;

public enum LoadSheddingStage {
    NORMAL,
    SHED_OPTIONAL,
    SHED_NORMAL_AND_OPTIONAL;

    public boolean allowsEssentialLoads() {
        return true;
    }

    public boolean allowsNormalLoads() {
        return this != SHED_NORMAL_AND_OPTIONAL;
    }

    public boolean allowsOptionalLoads() {
        return this == NORMAL;
    }

    public static LoadSheddingStage fromStored(String stored) {
        if (stored == null) {
            return NORMAL;
        }

        try {
            return valueOf(stored.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return NORMAL;
        }
    }
}
