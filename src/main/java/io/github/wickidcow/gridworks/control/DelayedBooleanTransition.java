package io.github.wickidcow.gridworks.control;

/**
 * Pure state machine for a delayed boolean output.
 *
 * <p>It decides whether a caller should schedule, retain, or cancel a pending
 * transition. The caller owns the actual scheduler.</p>
 */
public final class DelayedBooleanTransition {
    public enum Action {
        NONE,
        CANCEL_PENDING,
        SCHEDULE_ON,
        SCHEDULE_OFF
    }

    private boolean output;
    private Boolean input;
    private Boolean pendingTarget;

    public DelayedBooleanTransition(boolean initialOutput) {
        this.output = initialOutput;
    }

    public Action observe(boolean newInput) {
        input = newInput;

        if (output == newInput) {
            if (pendingTarget != null) {
                pendingTarget = null;
                return Action.CANCEL_PENDING;
            }
            return Action.NONE;
        }

        if (pendingTarget != null && pendingTarget == newInput) {
            return Action.NONE;
        }

        pendingTarget = newInput;
        return newInput ? Action.SCHEDULE_ON : Action.SCHEDULE_OFF;
    }

    /**
     * Commits a scheduled transition only if the input still wants that target.
     */
    public boolean commit(boolean target) {
        if (pendingTarget == null
                || pendingTarget != target
                || input == null
                || input != target) {
            return false;
        }

        output = target;
        pendingTarget = null;
        return true;
    }

    public boolean output() {
        return output;
    }

    public Boolean input() {
        return input;
    }

    public Boolean pendingTarget() {
        return pendingTarget;
    }

    public void reset(boolean initialOutput) {
        output = initialOutput;
        input = null;
        pendingTarget = null;
    }
}
