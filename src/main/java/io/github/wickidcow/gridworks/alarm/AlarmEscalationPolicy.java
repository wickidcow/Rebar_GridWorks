package io.github.wickidcow.gridworks.alarm;

import java.util.Objects;

/**
 * Stateless one-step alarm escalation policy.
 */
public record AlarmEscalationPolicy(boolean enabled, long delayMillis) {
    public AlarmEscalationPolicy {
        if (delayMillis < 0L) {
            throw new IllegalArgumentException("delayMillis must be non-negative");
        }
    }

    public AlarmSeverity effectiveSeverity(
            AlarmSeverity configuredSeverity,
            boolean latched,
            boolean acknowledged,
            long lastTriggeredEpochMillis,
            long nowEpochMillis
    ) {
        Objects.requireNonNull(configuredSeverity, "configuredSeverity");

        if (!shouldEscalate(
                configuredSeverity,
                latched,
                acknowledged,
                lastTriggeredEpochMillis
        )) {
            return configuredSeverity;
        }

        long elapsed = Math.max(0L, nowEpochMillis - lastTriggeredEpochMillis);
        if (elapsed < delayMillis) {
            return configuredSeverity;
        }

        return escalateOneLevel(configuredSeverity);
    }

    /**
     * @return -1 when no task is needed, 0 when escalation is already due, or
     *         the remaining delay in milliseconds.
     */
    public long remainingMillis(
            AlarmSeverity configuredSeverity,
            boolean latched,
            boolean acknowledged,
            long lastTriggeredEpochMillis,
            long nowEpochMillis
    ) {
        Objects.requireNonNull(configuredSeverity, "configuredSeverity");

        if (!shouldEscalate(
                configuredSeverity,
                latched,
                acknowledged,
                lastTriggeredEpochMillis
        )) {
            return -1L;
        }

        long elapsed = Math.max(0L, nowEpochMillis - lastTriggeredEpochMillis);
        return Math.max(0L, delayMillis - elapsed);
    }

    public static AlarmSeverity escalateOneLevel(AlarmSeverity severity) {
        Objects.requireNonNull(severity, "severity");
        return switch (severity) {
            case INFO -> AlarmSeverity.WARNING;
            case WARNING, CRITICAL -> AlarmSeverity.CRITICAL;
        };
    }

    private boolean shouldEscalate(
            AlarmSeverity configuredSeverity,
            boolean latched,
            boolean acknowledged,
            long lastTriggeredEpochMillis
    ) {
        return enabled
                && latched
                && !acknowledged
                && configuredSeverity != AlarmSeverity.CRITICAL
                && lastTriggeredEpochMillis > 0L;
    }
}
