package io.github.wickidcow.gridworks.control;

import java.util.Objects;
import java.util.OptionalLong;

/**
 * Pure, edge-triggered scheduling logic for a future physical Timer/Clock Controller.
 *
 * <p>The block adapter supplies server ticks and schedules only the next due
 * transition. This engine does not tick, access Bukkit, load chunks, or create
 * tasks. A missed deadline causes at most one transition; overdue intervals are
 * never replayed as a catch-up burst.</p>
 */
public final class TimerCycleEngine {
    public enum Mode {
        ONE_SHOT,
        REPEATING_PULSE,
        DUTY_CYCLE
    }

    public enum Phase {
        IDLE,
        WAITING,
        ON,
        OFF,
        COMPLETE
    }

    public record State(Phase phase, boolean output, OptionalLong nextDueTick,
                        long completedCycles) { }

    /** 60 minutes at 20 TPS; runtime adapters may impose a smaller GUI range. */
    public static final long MAX_DURATION_TICKS = 72_000L;

    private final RisingEdgeTrigger inputEdge = new RisingEdgeTrigger();

    private Mode mode;
    private long initialDelayTicks;
    private long onTicks;
    private long offTicks;

    private Phase phase = Phase.IDLE;
    private boolean output;
    private long dueTick = -1L;
    private long completedCycles;

    public TimerCycleEngine(Mode mode, long initialDelayTicks, long onTicks, long offTicks) {
        configure(mode, initialDelayTicks, onTicks, offTicks);
    }

    /**
     * Applying a different configuration cancels pending work and re-baselines
     * the input. A new real OFF-to-ON edge is required before it will run.
     */
    public void configure(Mode mode, long initialDelayTicks, long onTicks, long offTicks) {
        Objects.requireNonNull(mode, "mode");
        requireDuration("initialDelayTicks", initialDelayTicks, 0L);
        requireDuration("onTicks", onTicks, 1L);
        requireDuration("offTicks", offTicks, 1L);
        if (mode == Mode.REPEATING_PULSE && onTicks != 1L) {
            throw new IllegalArgumentException("Repeating pulses must be one tick wide");
        }
        this.mode = mode;
        this.initialDelayTicks = initialDelayTicks;
        this.onTicks = onTicks;
        this.offTicks = offTicks;
        resetAfterLoad();
    }

    /**
     * A first observation (including replayed ON) only sets an input baseline.
     * A real false-to-true transition starts the schedule; OFF cancels it.
     */
    public State observeInput(boolean enabled, long nowTick) {
        requireTick(nowTick);
        boolean started = inputEdge.observe(enabled);
        if (!enabled) {
            stop();
        } else if (started) {
            startAt(nowTick);
        }
        return state();
    }

    /** Explicit operator start, independent of Control Bus input-edge replay. */
    public State manualStart(long nowTick) {
        requireTick(nowTick);
        startAt(nowTick);
        return state();
    }

    public State stop() {
        phase = Phase.IDLE;
        output = false;
        dueTick = -1L;
        return state();
    }

    /** Called before restoring saved settings on load or rejoining topology. */
    public State resetAfterLoad() {
        inputEdge.reset();
        stop();
        completedCycles = 0L;
        return state();
    }

    /**
     * Process no more than one pending transition, even after heavy lag.
     * Deadlines for a subsequent phase are based on the actual wake-up tick.
     */
    public State advance(long nowTick) {
        requireTick(nowTick);
        if (dueTick < 0L || nowTick < dueTick) {
            return state();
        }

        switch (phase) {
            case WAITING, OFF -> {
                phase = Phase.ON;
                output = true;
                dueTick = plusClamped(nowTick, onTicks);
            }
            case ON -> {
                output = false;
                if (completedCycles < Long.MAX_VALUE) {
                    completedCycles++;
                }
                if (mode == Mode.ONE_SHOT) {
                    phase = Phase.COMPLETE;
                    dueTick = -1L;
                } else {
                    phase = Phase.OFF;
                    dueTick = plusClamped(nowTick, offTicks);
                }
            }
            case IDLE, COMPLETE -> dueTick = -1L;
        }
        return state();
    }

    public State state() {
        return new State(phase, output,
                dueTick < 0L ? OptionalLong.empty() : OptionalLong.of(dueTick),
                completedCycles);
    }

    public Mode mode() {
        return mode;
    }

    private void startAt(long nowTick) {
        output = false;
        phase = Phase.WAITING;
        dueTick = plusClamped(nowTick, initialDelayTicks);
    }

    private static long plusClamped(long tick, long duration) {
        return tick > Long.MAX_VALUE - duration ? Long.MAX_VALUE : tick + duration;
    }

    private static void requireTick(long tick) {
        if (tick < 0L) {
            throw new IllegalArgumentException("nowTick must not be negative");
        }
    }

    private static void requireDuration(String name, long value, long minimum) {
        if (value < minimum || value > MAX_DURATION_TICKS) {
            throw new IllegalArgumentException(name + " must be between "
                    + minimum + " and " + MAX_DURATION_TICKS);
        }
    }
}
