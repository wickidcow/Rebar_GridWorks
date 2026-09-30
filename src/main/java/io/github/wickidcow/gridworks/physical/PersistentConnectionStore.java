package io.github.wickidcow.gridworks.physical;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public final class PersistentConnectionStore {
    private static final String HEADER =
            "# GridWorks persistent Control Interface links";

    private final Path path;
    private final Set<ControlLink> links = new LinkedHashSet<>();
    private final Map<UUID, LinkedHashSet<UUID>> adjacency = new HashMap<>();

    public PersistentConnectionStore(Path path) throws IOException {
        this.path = path;
        load();
    }

    public synchronized boolean contains(UUID first, UUID second) {
        return links.contains(new ControlLink(first, second));
    }

    public synchronized Set<UUID> neighbors(UUID nodeId) {
        Set<UUID> neighbors = adjacency.get(nodeId);
        if (neighbors == null || neighbors.isEmpty()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(neighbors));
    }

    public synchronized Set<UUID> componentOf(UUID nodeId) {
        Set<UUID> visited = new LinkedHashSet<>();
        Queue<UUID> queue = new ArrayDeque<>();

        visited.add(nodeId);
        queue.add(nodeId);

        while (!queue.isEmpty()) {
            UUID current = queue.remove();
            Set<UUID> currentNeighbors = adjacency.get(current);
            if (currentNeighbors == null) {
                continue;
            }

            for (UUID next : currentNeighbors) {
                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }

        return Collections.unmodifiableSet(visited);
    }

    public synchronized int edgeCount(Set<UUID> nodes) {
        long total = 0L;
        for (UUID nodeId : nodes) {
            Set<UUID> currentNeighbors = adjacency.get(nodeId);
            if (currentNeighbors == null) {
                continue;
            }

            for (UUID neighbor : currentNeighbors) {
                if (nodes.contains(neighbor)) {
                    total++;
                }
            }
        }
        return Math.toIntExact(total / 2L);
    }

    public synchronized boolean toggle(UUID first, UUID second)
            throws IOException {
        ControlLink link = new ControlLink(first, second);
        boolean added;

        if (links.contains(link)) {
            removeIndexed(link);
            added = false;
        } else {
            addIndexed(link);
            added = true;
        }

        try {
            save();
            return added;
        } catch (IOException exception) {
            if (added) {
                removeIndexed(link);
            } else {
                addIndexed(link);
            }
            throw exception;
        }
    }

    public synchronized int removeNode(UUID nodeId) throws IOException {
        Set<UUID> neighbors = adjacency.get(nodeId);
        if (neighbors == null || neighbors.isEmpty()) {
            return 0;
        }

        List<ControlLink> removed = neighbors.stream()
                .map(neighbor -> new ControlLink(nodeId, neighbor))
                .toList();

        for (ControlLink link : removed) {
            removeIndexed(link);
        }

        try {
            save();
            return removed.size();
        } catch (IOException exception) {
            for (ControlLink link : removed) {
                addIndexed(link);
            }
            throw exception;
        }
    }

    public synchronized int linkCount() {
        return links.size();
    }

    public synchronized Set<ControlLink> links() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(links));
    }

    private void load() throws IOException {
        if (!Files.exists(path)) {
            return;
        }

        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        int lineNumber = 0;

        for (String rawLine : lines) {
            lineNumber++;
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split(",", -1);
            if (parts.length != 2) {
                throw new IOException(
                        "Invalid GridWorks control-network entry on line "
                                + lineNumber
                );
            }

            try {
                ControlLink link = new ControlLink(
                        UUID.fromString(parts[0].trim()),
                        UUID.fromString(parts[1].trim())
                );
                if (!links.contains(link)) {
                    addIndexed(link);
                }
            } catch (IllegalArgumentException exception) {
                throw new IOException(
                        "Invalid GridWorks control-network UUID on line "
                                + lineNumber,
                        exception
                );
            }
        }
    }

    private void addIndexed(ControlLink link) {
        if (!links.add(link)) {
            return;
        }

        adjacency.computeIfAbsent(
                link.first(),
                ignored -> new LinkedHashSet<>()
        ).add(link.second());
        adjacency.computeIfAbsent(
                link.second(),
                ignored -> new LinkedHashSet<>()
        ).add(link.first());
    }

    private void removeIndexed(ControlLink link) {
        if (!links.remove(link)) {
            return;
        }

        removeNeighbor(link.first(), link.second());
        removeNeighbor(link.second(), link.first());
    }

    private void removeNeighbor(UUID nodeId, UUID neighbor) {
        LinkedHashSet<UUID> neighbors = adjacency.get(nodeId);
        if (neighbors == null) {
            return;
        }

        neighbors.remove(neighbor);
        if (neighbors.isEmpty()) {
            adjacency.remove(nodeId);
        }
    }

    private void save() throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        List<String> lines = new ArrayList<>(links.size() + 1);
        lines.add(HEADER);
        links.stream()
                .sorted((left, right) -> {
                    int first = left.first().compareTo(right.first());
                    return first != 0
                            ? first
                            : left.second().compareTo(right.second());
                })
                .forEach(link -> lines.add(
                        link.first() + "," + link.second()
                ));

        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        Files.write(temp, lines, StandardCharsets.UTF_8);

        try {
            Files.move(
                    temp,
                    path,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(
                    temp,
                    path,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }
}
