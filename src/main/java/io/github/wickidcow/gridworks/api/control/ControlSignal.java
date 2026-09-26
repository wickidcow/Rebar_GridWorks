package io.github.wickidcow.gridworks.api.control;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable signal emitted onto a control bus.
 */
public record ControlSignal(UUID source, ControlChannel channel, ControlValue value, long sequence) {
    public ControlSignal {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(value, "value");
    }
}
