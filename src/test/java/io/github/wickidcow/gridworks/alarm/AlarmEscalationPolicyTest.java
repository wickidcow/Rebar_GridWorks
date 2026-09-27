package io.github.wickidcow.gridworks.alarm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AlarmEscalationPolicyTest {
    @Test
    void warningEscalatesToCriticalAfterDelay() {
        AlarmEscalationPolicy policy = new AlarmEscalationPolicy(true, 60_000L);

        assertEquals(
                AlarmSeverity.WARNING,
                policy.effectiveSeverity(
                        AlarmSeverity.WARNING,
                        true,
                        false,
                        100_000L,
                        159_999L
                )
        );
        assertEquals(
                AlarmSeverity.CRITICAL,
                policy.effectiveSeverity(
                        AlarmSeverity.WARNING,
                        true,
                        false,
                        100_000L,
                        160_000L
                )
        );
    }

    @Test
    void infoEscalatesOnlyOneLevel() {
        AlarmEscalationPolicy policy = new AlarmEscalationPolicy(true, 1_000L);

        assertEquals(
                AlarmSeverity.WARNING,
                policy.effectiveSeverity(
                        AlarmSeverity.INFO,
                        true,
                        false,
                        10_000L,
                        50_000L
                )
        );
    }

    @Test
    void acknowledgementOrClearCancelsEscalation() {
        AlarmEscalationPolicy policy = new AlarmEscalationPolicy(true, 1_000L);

        assertEquals(
                -1L,
                policy.remainingMillis(
                        AlarmSeverity.WARNING,
                        true,
                        true,
                        10_000L,
                        20_000L
                )
        );
        assertEquals(
                -1L,
                policy.remainingMillis(
                        AlarmSeverity.WARNING,
                        false,
                        false,
                        10_000L,
                        20_000L
                )
        );
    }

    @Test
    void remainingDelayCanBeReconstructedAfterRestart() {
        AlarmEscalationPolicy policy = new AlarmEscalationPolicy(true, 60_000L);

        assertEquals(
                25_000L,
                policy.remainingMillis(
                        AlarmSeverity.WARNING,
                        true,
                        false,
                        100_000L,
                        135_000L
                )
        );
        assertEquals(
                0L,
                policy.remainingMillis(
                        AlarmSeverity.WARNING,
                        true,
                        false,
                        100_000L,
                        200_000L
                )
        );
    }

    @Test
    void criticalNeverSchedulesAnotherEscalation() {
        AlarmEscalationPolicy policy = new AlarmEscalationPolicy(true, 1_000L);

        assertEquals(
                -1L,
                policy.remainingMillis(
                        AlarmSeverity.CRITICAL,
                        true,
                        false,
                        10_000L,
                        20_000L
                )
        );
        assertEquals(
                AlarmSeverity.CRITICAL,
                policy.effectiveSeverity(
                        AlarmSeverity.CRITICAL,
                        true,
                        false,
                        10_000L,
                        20_000L
                )
        );
    }

    @Test
    void rejectsNegativeDelay() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AlarmEscalationPolicy(true, -1L)
        );
    }
}
