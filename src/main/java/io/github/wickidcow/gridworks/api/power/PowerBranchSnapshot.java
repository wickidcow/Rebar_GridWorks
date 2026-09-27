package io.github.wickidcow.gridworks.api.power;

import java.util.Objects;

/**
 * Provider-neutral state for one controllable electrical branch.
 */
public record PowerBranchSnapshot(
        String branchId,
        String displayName,
        boolean enabled,
        boolean powerLimitSupported,
        double powerLimitWatts
) {
    public PowerBranchSnapshot {
        branchId = requireText(branchId, "branchId");
        displayName = requireText(displayName, "displayName");

        if (!Double.isFinite(powerLimitWatts) || powerLimitWatts < 0.0) {
            throw new IllegalArgumentException(
                    "powerLimitWatts must be finite and non-negative"
            );
        }
        if (!powerLimitSupported && powerLimitWatts != 0.0) {
            throw new IllegalArgumentException(
                    "unsupported power limits must report 0 watts"
            );
        }
    }

    public static PowerBranchSnapshot switchOnly(
            String branchId,
            String displayName,
            boolean enabled
    ) {
        return new PowerBranchSnapshot(
                branchId,
                displayName,
                enabled,
                false,
                0.0
        );
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }
}
