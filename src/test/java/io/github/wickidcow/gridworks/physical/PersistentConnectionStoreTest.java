package io.github.wickidcow.gridworks.physical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void componentTraversalIncludesDisconnectedSource() throws Exception {
        PersistentConnectionStore store = new PersistentConnectionStore(tempDir.resolve("network.txt"));
        UUID isolated = UUID.randomUUID();

        assertEquals(Set.of(isolated), store.componentOf(isolated));
    }
}
