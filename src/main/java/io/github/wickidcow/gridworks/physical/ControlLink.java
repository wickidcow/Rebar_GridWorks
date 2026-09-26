package io.github.wickidcow.gridworks.physical;

import java.util.Objects;
import java.util.UUID;

public record ControlLink(UUID first, UUID second) {
    public ControlLink {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");

        if (first.equals(second)) {
            throw new IllegalArgumentException("A control link cannot connect a node to itself");
        }

        if (first.compareTo(second) > 0) {
            UUID swap = first;
            first = second;
            second = swap;
        }
    }

    public boolean contains(UUID nodeId) {
        return first.equals(nodeId) || second.equals(nodeId);
    }

    public UUID other(UUID nodeId) {
        if (first.equals(nodeId)) {
            return second;
        }
        if (second.equals(nodeId)) {
            return first;
        }
        throw new IllegalArgumentException("Node " + nodeId + " is not part of this control link");
    }
}
