package io.github.wickidcow.gridworks.physical;

import io.github.wickidcow.gridworks.api.control.ControlBus;
import io.github.wickidcow.gridworks.api.control.ControlNode;
import io.github.wickidcow.gridworks.api.control.ControlStateSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class PhysicalControlNetwork implements AutoCloseable {
    private final ControlBus controlBus;
    private final PersistentConnectionStore connectionStore;
    private final Consumer<RuntimeException> callbackFailureHandler;
    private final Map<UUID, ControlNode> activeNodes = new HashMap<>();

    public PhysicalControlNetwork(
            ControlBus controlBus,
            PersistentConnectionStore connectionStore,
            Consumer<RuntimeException> callbackFailureHandler
    ) {
        this.controlBus = Objects.requireNonNull(controlBus, "controlBus");
        this.connectionStore = Objects.requireNonNull(connectionStore, "connectionStore");
        this.callbackFailureHandler = Objects.requireNonNull(callbackFailureHandler, "callbackFailureHandler");
    }

    public void activate(ControlNode node) {
        Objects.requireNonNull(node, "node");
        UUID nodeId = Objects.requireNonNull(node.id(), "node.id()");
        List<ControlNode> availablePeers = new ArrayList<>();

        synchronized (this) {
            ControlNode existing = activeNodes.get(nodeId);
            if (existing != null && existing != node) {
                throw new IllegalStateException(
                        "Duplicate loaded GridWorks node id " + nodeId
                                + ". This usually means persistent block data was duplicated."
                );
            }

            if (existing == node) {
                return;
            }

            activeNodes.put(nodeId, node);
            controlBus.register(node);

            for (UUID neighborId : connectionStore.neighbors(nodeId)) {
                ControlNode neighbor = activeNodes.get(neighborId);
                if (neighbor != null) {
                    controlBus.connect(nodeId, neighborId);
                    availablePeers.add(neighbor);
                }
            }
        }

        notifyPeersAvailable(node, availablePeers);
        replayStateSources(nodeId);
    }

    public synchronized void deactivate(UUID nodeId, ControlNode expectedNode) {
        ControlNode active = activeNodes.get(nodeId);
        if (active != expectedNode) {
            return;
        }

        activeNodes.remove(nodeId);
        controlBus.unregister(nodeId);
    }

    public synchronized void remove(UUID nodeId, ControlNode expectedNode) throws IOException {
        IOException failure = null;

        try {
            connectionStore.removeNode(nodeId);
        } catch (IOException exception) {
            failure = exception;
        } finally {
            deactivate(nodeId, expectedNode);
        }

        if (failure != null) {
            throw failure;
        }
    }

    public boolean toggleLink(UUID first, UUID second) throws IOException {
        ControlNode firstNode;
        ControlNode secondNode;
        boolean connected;

        synchronized (this) {
            firstNode = requireActive(first);
            secondNode = requireActive(second);

            connected = connectionStore.toggle(first, second);
            if (connected) {
                controlBus.connect(first, second);
            } else {
                controlBus.disconnect(first, second);
            }
        }

        if (connected) {
            notifyPeerAvailable(firstNode, second);
            notifyPeerAvailable(secondNode, first);
            replayStateSources(first);
        }
        return connected;
    }

    public synchronized boolean isLinked(UUID first, UUID second) {
        return connectionStore.contains(first, second);
    }

    public synchronized boolean isActive(UUID nodeId) {
        return activeNodes.containsKey(nodeId);
    }

    public synchronized int activeNodeCount() {
        return activeNodes.size();
    }

    /**
     * Returns directly linked nodes that are currently loaded.
     *
     * <p>This never loads chunks or scans the world. It intersects the persisted
     * direct-link set with the live-node registry.</p>
     */
    public synchronized List<UUID> activeLinkedNodes(UUID nodeId) {
        requireActive(nodeId);

        return connectionStore.neighbors(nodeId).stream()
                .filter(activeNodes::containsKey)
                .sorted()
                .toList();
    }

    public synchronized Set<UUID> activeComponentNodes(UUID nodeId) {
        requireActive(nodeId);
        return controlBus.componentOf(nodeId);
    }

    public synchronized ControlNetworkSnapshot snapshot(UUID nodeId) {
        Set<UUID> component = connectionStore.componentOf(nodeId);
        int loaded = 0;
        for (UUID id : component) {
            if (activeNodes.containsKey(id)) {
                loaded++;
            }
        }

        return new ControlNetworkSnapshot(
                networkId(component),
                component.size(),
                loaded,
                connectionStore.edgeCount(component)
        );
    }

    /**
     * Replays all loaded state sources in the live component containing nodeId.
     * No chunks are loaded and callbacks execute outside the topology monitor.
     */
    public void replayStateSources(UUID nodeId) {
        List<ControlStateSource> sources = new ArrayList<>();

        synchronized (this) {
            if (!activeNodes.containsKey(nodeId)) {
                return;
            }

            for (UUID componentNode : controlBus.componentOf(nodeId)) {
                ControlNode node = activeNodes.get(componentNode);
                if (node instanceof ControlStateSource stateSource) {
                    sources.add(stateSource);
                }
            }
        }

        for (ControlStateSource source : sources) {
            try {
                source.publishCurrentState();
            } catch (RuntimeException exception) {
                callbackFailureHandler.accept(exception);
            }
        }
    }

    private ControlNode requireActive(UUID nodeId) {
        ControlNode node = activeNodes.get(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Control node is not loaded: " + nodeId);
        }
        return node;
    }

    private void notifyPeersAvailable(ControlNode node, List<ControlNode> peers) {
        for (ControlNode peer : peers) {
            notifyPeerAvailable(node, peer.id());
            notifyPeerAvailable(peer, node.id());
        }
    }

    private void notifyPeerAvailable(ControlNode node, UUID peerId) {
        if (!(node instanceof PhysicalControlEndpoint endpoint)) {
            return;
        }

        try {
            endpoint.onControlPeerAvailable(peerId);
        } catch (RuntimeException exception) {
            callbackFailureHandler.accept(exception);
        }
    }

    private static String networkId(Set<UUID> component) {
        List<UUID> sorted = new ArrayList<>(component);
        sorted.sort(Comparator.naturalOrder());

        StringBuilder identity = new StringBuilder();
        for (UUID nodeId : sorted) {
            identity.append(nodeId).append(';');
        }

        UUID networkUuid = UUID.nameUUIDFromBytes(
                identity.toString().getBytes(StandardCharsets.UTF_8)
        );
        return networkUuid.toString().substring(0, 4).toUpperCase();
    }

    @Override
    public synchronized void close() {
        for (UUID nodeId : List.copyOf(activeNodes.keySet())) {
            controlBus.unregister(nodeId);
        }
        activeNodes.clear();
    }
}
