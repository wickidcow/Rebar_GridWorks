package io.github.wickidcow.gridworks.control;

import java.util.Objects;
import java.util.UUID;

/** One preferred and one currently active directly linked Timer input source. */
public final class TimerInputSelector {
    private UUID preferred;
    private UUID active;

    public TimerInputSelector(UUID preferred) {
        this.preferred = preferred;
    }

    public boolean accept(UUID source, boolean directlyLinked) {
        Objects.requireNonNull(source, "source");
        if (!directlyLinked || (preferred != null && !preferred.equals(source))) {
            return false;
        }
        if (active == null) {
            active = source;
        }
        return active.equals(source);
    }

    public boolean peerUnavailable(UUID source) {
        Objects.requireNonNull(source, "source");
        if (!source.equals(active)) {
            return false;
        }
        active = null;
        return true;
    }

    public void select(UUID preferred) {
        this.preferred = preferred;
        active = null;
    }

    public void resetActive() {
        active = null;
    }

    public UUID preferredSource() {
        return preferred;
    }

    public UUID activeSource() {
        return active;
    }
}
