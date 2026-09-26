package io.github.wickidcow.gridworks.physical;

public record ControlNetworkSnapshot(
        String networkId,
        int totalNodes,
        int loadedNodes,
        int connections
) {
    public ControlNetworkSnapshot {
        if (totalNodes < 1) {
            throw new IllegalArgumentException("totalNodes must be at least 1");
        }
        if (loadedNodes < 0 || loadedNodes > totalNodes) {
            throw new IllegalArgumentException("loadedNodes must be between 0 and totalNodes");
        }
        if (connections < 0) {
            throw new IllegalArgumentException("connections must be non-negative");
        }
    }
}
