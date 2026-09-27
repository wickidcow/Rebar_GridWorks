package io.github.wickidcow.gridworks.alarm;

import io.github.wickidcow.gridworks.api.control.ControlSignal;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.api.control.GridWorksChannels;
import java.util.Objects;
import java.util.UUID;

/**
 * Thread-safe aggregation of one Alarm Indicator's telemetry.
 */
public final class AlarmTelemetryState {
    private final UUID source;

    private String name;
    private AlarmSeverity severity;
    private Boolean conditionActive;
    private Boolean latched;
    private Boolean acknowledged;
    private long occurrenceCount;
    private long lastTriggeredEpochMillis;

    private long nameSequence = -1;
    private long severitySequence = -1;
    private long conditionSequence = -1;
    private long latchedSequence = -1;
    private long acknowledgedSequence = -1;
    private long occurrenceSequence = -1;
    private long lastTriggeredSequence = -1;

    public AlarmTelemetryState(UUID source) {
        this.source = Objects.requireNonNull(source, "source");
        this.name = "Alarm " + shortId(source);
        this.severity = AlarmSeverity.WARNING;
    }

    public synchronized boolean apply(ControlSignal signal) {
        Objects.requireNonNull(signal, "signal");
        if (!source.equals(signal.source())) {
            throw new IllegalArgumentException("Signal source does not match alarm state");
        }

        if (GridWorksChannels.ALARM_NAME.equals(signal.channel())
                && signal.value() instanceof ControlValue.TextValue textValue) {
            if (signal.sequence() < nameSequence) {
                return false;
            }
            name = normalizeName(textValue.value());
            nameSequence = signal.sequence();
            return true;
        }

        if (GridWorksChannels.ALARM_SEVERITY.equals(signal.channel())
                && signal.value() instanceof ControlValue.TextValue textValue) {
            if (signal.sequence() < severitySequence) {
                return false;
            }
            severity = AlarmSeverity.fromStored(textValue.value());
            severitySequence = signal.sequence();
            return true;
        }

        if (GridWorksChannels.ALARM_OCCURRENCES.equals(signal.channel())
                && signal.value() instanceof ControlValue.NumberValue numberValue) {
            if (signal.sequence() < occurrenceSequence) {
                return false;
            }
            occurrenceCount = safeWholeNumber(numberValue.value());
            occurrenceSequence = signal.sequence();
            return true;
        }

        if (GridWorksChannels.ALARM_LAST_TRIGGERED_EPOCH_MS.equals(signal.channel())
                && signal.value() instanceof ControlValue.NumberValue numberValue) {
            if (signal.sequence() < lastTriggeredSequence) {
                return false;
            }
            lastTriggeredEpochMillis = safeWholeNumber(numberValue.value());
            lastTriggeredSequence = signal.sequence();
            return true;
        }

        if (!(signal.value() instanceof ControlValue.BooleanValue booleanValue)) {
            return false;
        }

        if (GridWorksChannels.ALARM_CONDITION_ACTIVE.equals(signal.channel())) {
            if (signal.sequence() < conditionSequence) {
                return false;
            }
            conditionActive = booleanValue.value();
            conditionSequence = signal.sequence();
            return true;
        }

        if (GridWorksChannels.ALARM_LATCHED.equals(signal.channel())) {
            if (signal.sequence() < latchedSequence) {
                return false;
            }
            latched = booleanValue.value();
            latchedSequence = signal.sequence();
            return true;
        }

        if (GridWorksChannels.ALARM_ACKNOWLEDGED.equals(signal.channel())) {
            if (signal.sequence() < acknowledgedSequence) {
                return false;
            }
            acknowledged = booleanValue.value();
            acknowledgedSequence = signal.sequence();
            return true;
        }

        return false;
    }

    public synchronized Snapshot snapshot() {
        long latest = Math.max(
                Math.max(nameSequence, severitySequence),
                Math.max(
                        Math.max(conditionSequence, latchedSequence),
                        Math.max(
                                acknowledgedSequence,
                                Math.max(occurrenceSequence, lastTriggeredSequence)
                        )
                )
        );

        return new Snapshot(
                source,
                name,
                severity,
                conditionActive,
                latched,
                acknowledged,
                occurrenceCount,
                lastTriggeredEpochMillis,
                latest
        );
    }

    private static long safeWholeNumber(double value) {
        if (!Double.isFinite(value) || value <= 0.0) {
            return 0L;
        }
        return (long) Math.min(value, Long.MAX_VALUE);
    }

    private static String normalizeName(String raw) {
        if (raw == null) {
            return "";
        }

        String normalized = raw
                .replaceAll("\\p{Cntrl}", "")
                .replaceAll("\\s+", " ")
                .trim();

        return normalized.isEmpty() ? "Alarm" : normalized;
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8).toUpperCase();
    }

    public record Snapshot(
            UUID source,
            String name,
            AlarmSeverity severity,
            Boolean conditionActive,
            Boolean latched,
            Boolean acknowledged,
            long occurrenceCount,
            long lastTriggeredEpochMillis,
            long latestSequence
    ) {
        public boolean isLatched() {
            return Boolean.TRUE.equals(latched);
        }

        public boolean isAcknowledged() {
            return Boolean.TRUE.equals(acknowledged);
        }

        public boolean isConditionActive() {
            return Boolean.TRUE.equals(conditionActive);
        }
    }
}
