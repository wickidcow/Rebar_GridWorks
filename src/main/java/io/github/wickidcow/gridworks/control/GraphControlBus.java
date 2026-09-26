package io.github.wickidcow.gridworks.control;

import io.github.wickidcow.gridworks.api.control.ControlBus;
import io.github.wickidcow.gridworks.api.control.ControlChannel;
import io.github.wickidcow.gridworks.api.control.ControlDispatchResult;
import io.github.wickidcow.gridworks.api.control.ControlNode;
import io.github.wickidcow.gridworks.api.control.ControlSignal;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * In-memory, thread-safe graph implementation of the GridWorks control bus.
 *
 * <p>The topology lock is never held while addon callbacks execute. A dispatch
 * therefore observes a stable snapshot of its route without allowing a slow or
 * re-entrant receiver to block topology updates.</p>
 */
public final class GraphControlBus implements ControlBus {
    private final Map<UUID, ControlNode> nodes = new HashMap<>();
    private final Map<UUID, Set<UUID>> edges = new HashMap<>();
    private final ReentrantReadWriteLock topologyLock = new ReentrantReadWriteLock();
    private final AtomicLong sequence = new AtomicLong();
    private final int maxPropagationNodes;

    public GraphControlBus(int maxPropagationNodes) {
        if (maxPropagationNodes < 1) {
            throw new IllegalArgumentException("maxPropagationNodes must be at least 1");
        }
        this.maxPropagationNodes = maxPropagationNodes;
    }

    public int maxPropagationNodes() {
        return maxPropagationNodes;
    }

    @Override
    public void register(ControlNode node) {
        Objects.requireNonNull(node, "node");
        UUID id = Objects.requireNonNull(node.id(), "node.id()");

        topologyLock.writeLock().lock();
        try {
            ControlNode existing = nodes.putIfAbsent(id, node);
            if (existing != null && existing != node) {
                throw new IllegalArgumentException("A control node with id " + id + " is already registered");
            }
            edges.computeIfAbsent(id, ignored -> new LinkedHashSet<>());
        } finally {
            topologyLock.writeLock().unlock();
        }
    }

    @Override
    public boolean unregister(UUID nodeId) {
        Objects.requireNonNull(nodeId, "nodeId");

        topologyLock.writeLock().lock();
        try {
            if (nodes.remove(nodeId) == null) {
                return false;
            }

            Set<UUID> neighbors = edges.remove(nodeId);
            if (neighbors != null) {
                for (UUID neighbor : neighbors) {
                    Set<UUID> neighborEdges = edges.get(neighbor);
                    if (neighborEdges != null) {
                        neighborEdges.remove(nodeId);
                    }
                }
            }
            return true;
        } finally {
            topologyLock.writeLock().unlock();
        }
    }

    @Override
    public void connect(UUID first, UUID second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (first.equals(second)) {
            throw new IllegalArgumentException("A control node cannot connect to itself");
        }

        topologyLock.writeLock().lock();
        try {
            requireRegistered(first);
            requireRegistered(second);
            edges.get(first).add(second);
            edges.get(second).add(first);
        } finally {
            topologyLock.writeLock().unlock();
        }
    }

    @Override
    public boolean disconnect(UUID first, UUID second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");

        topologyLock.writeLock().lock();
        try {
            Set<UUID> firstEdges = edges.get(first);
            Set<UUID> secondEdges = edges.get(second);
            boolean changed = firstEdges != null && firstEdges.remove(second);
            if (secondEdges != null) {
                changed |= secondEdges.remove(first);
            }
            return changed;
        } finally {
            topologyLock.writeLock().unlock();
        }
    }

    @Override
    public boolean isConnected(UUID first, UUID second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");

        topologyLock.readLock().lock();
        try {
            Set<UUID> firstEdges = edges.get(first);
            return firstEdges != null && firstEdges.contains(second);
        } finally {
            topologyLock.readLock().unlock();
        }
    }

    @Override
    public Set<UUID> neighbors(UUID nodeId) {
        Objects.requireNonNull(nodeId, "nodeId");

        topologyLock.readLock().lock();
        try {
            Set<UUID> neighbors = edges.get(nodeId);
            return neighbors == null ? Set.of() : Set.copyOf(neighbors);
        } finally {
            topologyLock.readLock().unlock();
        }
    }

    @Override
    public Set<UUID> componentOf(UUID nodeId) {
        Objects.requireNonNull(nodeId, "nodeId");

        topologyLock.readLock().lock();
        try {
            requireRegistered(nodeId);
            return Set.copyOf(walkComponent(nodeId, Integer.MAX_VALUE));
        } finally {
            topologyLock.readLock().unlock();
        }
    }

    @Override
    public int nodeCount() {
        topologyLock.readLock().lock();
        try {
            return nodes.size();
        } finally {
            topologyLock.readLock().unlock();
        }
    }

    @Override
    public int connectionCount() {
        topologyLock.readLock().lock();
        try {
            long total = 0;
            for (Set<UUID> neighbors : edges.values()) {
                total += neighbors.size();
            }
            return Math.toIntExact(total / 2);
        } finally {
            topologyLock.readLock().unlock();
        }
    }

    @Override
    public ControlDispatchResult publish(UUID source, ControlChannel channel, ControlValue value) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(value, "value");

        List<ControlNode> candidates = new ArrayList<>();
        boolean truncated;

        topologyLock.readLock().lock();
        try {
            requireRegistered(source);

            int traversalLimit = (int) Math.min(Integer.MAX_VALUE, (long) maxPropagationNodes + 2L);
            List<UUID> route = walkComponent(source, traversalLimit);
            int reachableRecipients = Math.max(0, route.size() - 1);
            truncated = reachableRecipients > maxPropagationNodes;

            int recipientLimit = Math.min(reachableRecipients, maxPropagationNodes);
            for (int i = 1; i <= recipientLimit; i++) {
                ControlNode node = nodes.get(route.get(i));
                if (node != null) {
                    candidates.add(node);
                }
            }
        } finally {
            topologyLock.readLock().unlock();
        }

        ControlSignal signal = new ControlSignal(source, channel, value, sequence.getAndIncrement());
        List<ControlDispatchResult.DeliveryFailure> failures = new ArrayList<>();
        int delivered = 0;

        for (ControlNode candidate : candidates) {
            try {
                if (candidate.accepts(channel)) {
                    candidate.onSignal(signal);
                    delivered++;
                }
            } catch (RuntimeException ex) {
                failures.add(new ControlDispatchResult.DeliveryFailure(candidate.id(), ex));
            }
        }

        return new ControlDispatchResult(delivered, truncated, failures);
    }

    @Override
    public void clear() {
        topologyLock.writeLock().lock();
        try {
            nodes.clear();
            edges.clear();
            sequence.set(0);
        } finally {
            topologyLock.writeLock().unlock();
        }
    }

    private List<UUID> walkComponent(UUID source, int limit) {
        List<UUID> route = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        Queue<UUID> queue = new ArrayDeque<>();
        visited.add(source);
        queue.add(source);

        while (!queue.isEmpty() && route.size() < limit) {
            UUID current = queue.remove();
            route.add(current);

            for (UUID neighbor : edges.getOrDefault(current, Set.of())) {
                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        return route;
    }

    private void requireRegistered(UUID nodeId) {
        if (!nodes.containsKey(nodeId)) {
            throw new IllegalArgumentException("Unknown control node: " + nodeId);
        }
    }
}
