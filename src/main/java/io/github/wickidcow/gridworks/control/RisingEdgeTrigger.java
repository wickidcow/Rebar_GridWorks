package io.github.wickidcow.gridworks.control;

/**
 * Detects rising boolean edges without treating the first observed state as an
 * edge. This makes one-shot devices safe across restarts and topology replay.
 */
public final class RisingEdgeTrigger {
    private Boolean previous;

    /**
     * @return true only when a known false state is followed by true.
     */
    public boolean observe(boolean current) {
        boolean triggered = previous != null && !previous && current;
        previous = current;
        return triggered;
    }

    public boolean isInitialized() {
        return previous != null;
    }

    public boolean currentState() {
        return Boolean.TRUE.equals(previous);
    }

    public void reset() {
        previous = null;
    }
}
