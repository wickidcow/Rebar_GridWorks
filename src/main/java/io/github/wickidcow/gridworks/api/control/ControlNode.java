package io.github.wickidcow.gridworks.api.control;

import java.util.UUID;

/**
 * Endpoint attached to a GridWorks control bus.
 *
 * <p>Callbacks run on the same thread that publishes the signal. Implementations
 * must not assume that GridWorks has moved execution to the server thread.</p>
 */
public interface ControlNode {
    UUID id();

    default boolean accepts(ControlChannel channel) {
        return true;
    }

    void onSignal(ControlSignal signal);
}
