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

public final class PhysicalControlNetwork implements AutoCloseable {
    private final ControlBus controlBus;
    private final PersistentConnectionStore connectionStore;
    private final Map<UUID, ControlNode> activeNodes = new HashMap<>();

    public PhysicalControlNetwork(
            ControlBus controlBus,
            PersistentConnectionStore connectionStore
    ) {
        this.controlBus = Objects.requireNonNull(controlBus, "controlBus");
        this.connectionStore = Objects.requireNonNull(connectionStore, "connectionStore");
    }

    public void activate(ControlNode node) {
        Objects.requireNonNull(node, "node");
        UUID nodeId = Objects.requireNonNull(node.id(), "node.id()");
        boolean changed = false;

        synchronized (this) {
            ControlNode existing = activeNodes.get(nodeId);
            if (existing != null && existing != node) {
                throw new IllegalStateException(
                        "Duplicate loaded GridWorks node id " + nodeId
                                + ". This usually means persistent block data was duplicated."
                );
            }

            if (existing != node) {
                activeNodes.put(nodeId, node);
                controlBus.register(node);

                for (UUID neighborId : connectionStore.neighbors(nodeId)) {
                    if (activeNodes.containsKey(neighborId)) {
                        controlBus.connect(nodeId, neighborId);
                    }
                }
                changed = true;
            }
        }

        if (changed) {
            resyncComponent(nodeId);
        }
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
        boolean connected;

        synchronized (this) {
            requireActive(first);
            requireActive(second);

            connected = connectionStore.toggle(first, second);
            if (connected) {
                controlBus.connect(first, second);
            } else {
                controlBus.disconnect(first, second);
            }
        }

        if (connected) {
            resyncComponent(first);
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

    public void resyncComponent(UUID nodeId) {
        List<ControlStateSource> stateSources = new ArrayList<>();

        synchronized (this) {
            if (!activeNodes.containsKey(nodeId)) {
                return;
            }

            for (UUID componentNode : controlBus.componentOf(nodeId)) {
                ControlNode active = activeNodes.get(componentNode);
                if (active instanceof ControlStateSource stateSource) {
                    stateSources.add(stateSource);
                }
            }
        }

        for (ControlStateSource stateSource : stateSources) {
            stateSource.publishCurrentState();
        }
    }

    private void requireActive(UUID nodeId) {
        if (!activeNodes.containsKey(nodeId)) {
            throw new IllegalArgumentException("Control node is not loaded: " + nodeId);
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
