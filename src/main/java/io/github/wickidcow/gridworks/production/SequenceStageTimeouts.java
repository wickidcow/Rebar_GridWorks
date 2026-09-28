package io.github.wickidcow.gridworks.production;

import java.util.Arrays;

/**
 * Per-stage Sequence Controller timeout configuration.
 *
 * <p>Timeout 0 disables the deadline for that stage. Values loaded from storage
 * are clamped so corrupt/legacy data cannot schedule an excessive task delay.</p>
 */
public final class SequenceStageTimeouts {
    public static final long DEFAULT_TIMEOUT_TICKS = 0L;
    public static final long MAX_TIMEOUT_TICKS = 72_000L;

    private final long[] ticks;

    public SequenceStageTimeouts() {
        this(new long[SequenceStateMachine.STAGE_COUNT]);
    }

    private SequenceStageTimeouts(long[] ticks) {
        this.ticks = ticks;
    }

    public static SequenceStageTimeouts fromStored(
            Long legacyTimeoutTicks,
            Long... stageTimeoutTicks
    ) {
        if (stageTimeoutTicks.length != SequenceStateMachine.STAGE_COUNT) {
            throw new IllegalArgumentException(
                    "Expected " + SequenceStateMachine.STAGE_COUNT
                            + " stage timeout values"
            );
        }

        long legacy = clamp(
                legacyTimeoutTicks == null
                        ? DEFAULT_TIMEOUT_TICKS
                        : legacyTimeoutTicks
        );
        long[] values = new long[SequenceStateMachine.STAGE_COUNT];

        for (int index = 0; index < values.length; index++) {
            Long stored = stageTimeoutTicks[index];
            values[index] = clamp(stored == null ? legacy : stored);
        }

        return new SequenceStageTimeouts(values);
    }

    public long get(int stage) {
        return ticks[index(stage)];
    }

    public void set(int stage, long timeoutTicks) {
        ticks[index(stage)] = clamp(timeoutTicks);
    }

    public long max() {
        return Arrays.stream(ticks).max().orElse(DEFAULT_TIMEOUT_TICKS);
    }

    public static long clamp(long timeoutTicks) {
        return Math.max(
                DEFAULT_TIMEOUT_TICKS,
                Math.min(MAX_TIMEOUT_TICKS, timeoutTicks)
        );
    }

    private static int index(int stage) {
        if (stage < 1 || stage > SequenceStateMachine.STAGE_COUNT) {
            throw new IllegalArgumentException(
                    "stage must be between 1 and " + SequenceStateMachine.STAGE_COUNT
            );
        }
        return stage - 1;
    }
}
