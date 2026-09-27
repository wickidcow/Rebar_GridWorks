package io.github.wickidcow.gridworks.control;

/**
 * Industrial-style alarm latch with acknowledgement.
 *
 * <p>An unacknowledged alarm remains latched after the fault clears. An
 * acknowledged alarm clears automatically once the fault is no longer active.
 * The first observed active state after load establishes state silently rather
 * than manufacturing a rising-edge notification.</p>
 */
public final class AlarmLatch {
    private boolean initialized;
    private boolean conditionActive;
    private boolean latched;
    private boolean acknowledged;

    public AlarmLatch(boolean latched, boolean acknowledged) {
        this.latched = latched;
        this.acknowledged = latched && acknowledged;
    }

    /**
     * Updates the current fault condition.
     *
     * @return true when this observation is a real false-to-true edge and should
     *         produce an audible notification.
     */
    public boolean observe(boolean active) {
        boolean rising = initialized && !conditionActive && active;
        boolean firstActive = !initialized && active;

        initialized = true;
        conditionActive = active;

        if (rising) {
            latched = true;
            acknowledged = false;
            return true;
        }

        if (firstActive && !latched) {
            // Restore the visible alarm state silently.
            latched = true;
            acknowledged = false;
        }

        if (!active && acknowledged) {
            latched = false;
            acknowledged = false;
        }

        return false;
    }

    public void acknowledge() {
        if (!latched) {
            return;
        }

        if (conditionActive) {
            acknowledged = true;
        } else {
            latched = false;
            acknowledged = false;
        }
    }

    public void resetObservation() {
        initialized = false;
        conditionActive = false;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isConditionActive() {
        return conditionActive;
    }

    public boolean isLatched() {
        return latched;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }
}
