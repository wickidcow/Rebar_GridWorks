package io.github.wickidcow.gridworks.api.control;

import java.util.Objects;

/**
 * One channel/value pair in a batched Control Bus publication.
 */
public record ControlPublication(
        ControlChannel channel,
        ControlValue value
) {
    public ControlPublication {
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(value, "value");
    }

    public static ControlPublication of(
            ControlChannel channel,
            ControlValue value
    ) {
        return new ControlPublication(channel, value);
    }
}
