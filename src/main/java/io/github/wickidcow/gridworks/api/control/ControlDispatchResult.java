package io.github.wickidcow.gridworks.api.control;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Result of one control-bus publication.
 */
public record ControlDispatchResult(int delivered, boolean truncated, List<DeliveryFailure> failures) {
    public ControlDispatchResult {
        if (delivered < 0) {
            throw new IllegalArgumentException("delivered must be non-negative");
        }
        failures = List.copyOf(Objects.requireNonNull(failures, "failures"));
    }

    public boolean successful() {
        return failures.isEmpty() && !truncated;
    }

    public record DeliveryFailure(UUID nodeId, RuntimeException cause) {
        public DeliveryFailure {
            Objects.requireNonNull(nodeId, "nodeId");
            Objects.requireNonNull(cause, "cause");
        }
    }
}
