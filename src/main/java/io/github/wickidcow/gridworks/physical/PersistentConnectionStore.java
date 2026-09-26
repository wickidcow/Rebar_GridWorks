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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public final class PersistentConnectionStore {
    private static final String HEADER = "# GridWorks persistent Control Interface links";

    private final Path path;
    private final Set<ControlLink> links = new LinkedHashSet<>();

    public PersistentConnectionStore(Path path) throws IOException {
        this.path = path;
        load();
    }

    public synchronized boolean contains(UUID first, UUID second) {
        return links.contains(new ControlLink(first, second));
    }

    public synchronized Set<UUID> neighbors(UUID nodeId) {
        Set<UUID> neighbors = new LinkedHashSet<>();
        for (ControlLink link : links) {
            if (link.contains(nodeId)) {
                neighbors.add(link.other(nodeId));
            }
        }
        return Collections.unmodifiableSet(neighbors);
    }

    public synchronized Set<UUID> componentOf(UUID nodeId) {
        Set<UUID> visited = new LinkedHashSet<>();
        Queue<UUID> queue = new ArrayDeque<>();

        visited.add(nodeId);
        queue.add(nodeId);

        while (!queue.isEmpty()) {
            UUID current = queue.remove();
            for (ControlLink link : links) {
                if (!link.contains(current)) {
                    continue;
                }

                UUID next = link.other(current);
                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }

        return Collections.unmodifiableSet(visited);
    }

    public synchronized int edgeCount(Set<UUID> nodes) {
        int count = 0;
        for (ControlLink link : links) {
            if (nodes.contains(link.first()) && nodes.contains(link.second())) {
                count++;
            }
        }
        return count;
    }

    public synchronized boolean toggle(UUID first, UUID second) throws IOException {
        ControlLink link = new ControlLink(first, second);
        boolean added;

        if (links.remove(link)) {
            added = false;
        } else {
            links.add(link);
            added = true;
        }

        try {
            save();
            return added;
        } catch (IOException exception) {
            if (added) {
                links.remove(link);
            } else {
                links.add(link);
            }
            throw exception;
        }
    }

    public synchronized int removeNode(UUID nodeId) throws IOException {
        List<ControlLink> removed = links.stream()
                .filter(link -> link.contains(nodeId))
                .toList();

        if (removed.isEmpty()) {
            return 0;
        }

        links.removeAll(removed);
        try {
            save();
            return removed.size();
        } catch (IOException exception) {
            links.addAll(removed);
            throw exception;
        }
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
                throw new IOException("Invalid GridWorks control-network entry on line " + lineNumber);
            }

            try {
                links.add(new ControlLink(
                        UUID.fromString(parts[0].trim()),
                        UUID.fromString(parts[1].trim())
                ));
            } catch (IllegalArgumentException exception) {
                throw new IOException(
                        "Invalid GridWorks control-network UUID on line " + lineNumber,
                        exception
                );
            }
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
                    return first != 0 ? first : left.second().compareTo(right.second());
                })
                .forEach(link -> lines.add(link.first() + "," + link.second()));

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
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
