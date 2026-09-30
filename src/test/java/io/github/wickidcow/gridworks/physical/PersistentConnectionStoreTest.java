package io.github.wickidcow.gridworks.physical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PersistentConnectionStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void linksSurviveReloadAndStayCanonical() throws Exception {
        Path file = tempDir.resolve("network.txt");
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        PersistentConnectionStore store = new PersistentConnectionStore(file);
        assertTrue(store.toggle(a, b));
        assertTrue(store.contains(b, a));

        PersistentConnectionStore reloaded = new PersistentConnectionStore(file);
        assertTrue(reloaded.contains(a, b));
        assertEquals(Set.of(b), reloaded.neighbors(a));
    }

    @Test
    void togglingExistingLinkRemovesIt() throws Exception {
        PersistentConnectionStore store = new PersistentConnectionStore(tempDir.resolve("network.txt"));
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        assertTrue(store.toggle(a, b));
        assertFalse(store.toggle(a, b));
        assertFalse(store.contains(a, b));
    }

    @Test
    void removingNodeRemovesEveryAdjacentEdge() throws Exception {
        PersistentConnectionStore store = new PersistentConnectionStore(tempDir.resolve("network.txt"));
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();

        store.toggle(a, b);
        store.toggle(b, c);

        assertEquals(2, store.removeNode(b));
        assertTrue(store.links().isEmpty());
    }


    @Test
    void malformedPersistedEntryFailsLoudly() throws Exception {
        Path file = tempDir.resolve("network.txt");
        Files.writeString(
                file,
                "# GridWorks persistent Control Interface links\nnot-a-link\n",
                StandardCharsets.UTF_8
        );

        IOException failure = assertThrows(
                IOException.class,
                () -> new PersistentConnectionStore(file)
        );

        assertTrue(failure.getMessage().contains("line 2"));
    }

    @Test
    void malformedOrSelfLinkedUuidFailsLoudly() throws Exception {
        Path invalidUuid = tempDir.resolve("invalid-uuid.txt");
        Files.writeString(
                invalidUuid,
                "not-a-uuid,00000000-0000-0000-0000-000000000001\n",
                StandardCharsets.UTF_8
        );
        assertThrows(
                IOException.class,
                () -> new PersistentConnectionStore(invalidUuid)
        );

        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Path selfLink = tempDir.resolve("self-link.txt");
        Files.writeString(
                selfLink,
                id + "," + id + "\n",
                StandardCharsets.UTF_8
        );
        assertThrows(
                IOException.class,
                () -> new PersistentConnectionStore(selfLink)
        );
    }

    @Test
    void duplicateAndReversedPersistedLinksCanonicalize() throws Exception {
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Path file = tempDir.resolve("network.txt");
        Files.writeString(
                file,
                first + "," + second + "\n"
                        + second + "," + first + "\n",
                StandardCharsets.UTF_8
        );

        PersistentConnectionStore store = new PersistentConnectionStore(file);

        assertEquals(1, store.links().size());
        assertTrue(store.contains(first, second));
    }

    @Test
    void savedFileIsDeterministicAndLeavesNoTempFile() throws Exception {
        UUID a = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID b = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID c = UUID.fromString("00000000-0000-0000-0000-000000000003");
        Path file = tempDir.resolve("network.txt");

        PersistentConnectionStore store = new PersistentConnectionStore(file);
        store.toggle(c, b);
        store.toggle(b, a);

        assertEquals(
                java.util.List.of(
                        "# GridWorks persistent Control Interface links",
                        a + "," + b,
                        b + "," + c
                ),
                Files.readAllLines(file, StandardCharsets.UTF_8)
        );
        assertFalse(Files.exists(tempDir.resolve("network.txt.tmp")));
    }

    @Test
    void failedDiskSaveRollsBackInMemoryMutation() throws Exception {
        Path blockedParent = tempDir.resolve("blocked");
        Path file = blockedParent.resolve("network.txt");

        PersistentConnectionStore store = new PersistentConnectionStore(file);
        Files.writeString(blockedParent, "not a directory", StandardCharsets.UTF_8);

        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        assertThrows(IOException.class, () -> store.toggle(a, b));
        assertFalse(store.contains(a, b));
        assertTrue(store.neighbors(a).isEmpty());
        assertTrue(store.neighbors(b).isEmpty());
        assertTrue(store.links().isEmpty());
    }

    @Test
    void largePersistedChainBuildsIndexedAdjacencyCorrectly() throws Exception {
        Path file = tempDir.resolve("large-network.txt");
        java.util.List<UUID> nodes = new java.util.ArrayList<>();
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add("# GridWorks persistent Control Interface links");

        for (int i = 0; i < 2000; i++) {
            nodes.add(new UUID(0L, i + 1L));
        }
        for (int i = 0; i < nodes.size() - 1; i++) {
            lines.add(nodes.get(i) + "," + nodes.get(i + 1));
        }
        Files.write(file, lines, StandardCharsets.UTF_8);

        PersistentConnectionStore store = new PersistentConnectionStore(file);

        assertEquals(Set.of(nodes.get(1)), store.neighbors(nodes.getFirst()));
        assertEquals(
                Set.of(nodes.get(998), nodes.get(1000)),
                store.neighbors(nodes.get(999))
        );

        Set<UUID> component = store.componentOf(nodes.getFirst());
        assertEquals(2000, component.size());
        assertEquals(1999, store.edgeCount(component));
    }

    @Test
    void componentTraversalIncludesDisconnectedSource() throws Exception {
        PersistentConnectionStore store = new PersistentConnectionStore(tempDir.resolve("network.txt"));
        UUID isolated = UUID.randomUUID();

        assertEquals(Set.of(isolated), store.componentOf(isolated));
    }
}
