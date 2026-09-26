package io.github.wickidcow.gridworks.physical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.wickidcow.gridworks.api.control.ControlChannel;
import io.github.wickidcow.gridworks.api.control.ControlNode;
import io.github.wickidcow.gridworks.api.control.ControlSignal;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.control.GraphControlBus;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PhysicalControlNetworkTest {
    @TempDir
    Path tempDir;

    @Test
    void unloadedNodesDoNotStayInLiveGraphButReconnectOnActivation() throws Exception {
        GraphControlBus bus = new GraphControlBus(32);
        PersistentConnectionStore store = new PersistentConnectionStore(tempDir.resolve("network.txt"));
        PhysicalControlNetwork network = new PhysicalControlNetwork(bus, store);

        TestNode a = new TestNode();
        TestNode b = new TestNode();
        network.activate(a);
        network.activate(b);
        assertTrue(network.toggleLink(a.id(), b.id()));
        assertTrue(bus.isConnected(a.id(), b.id()));

        network.deactivate(b.id(), b);
        assertFalse(bus.isConnected(a.id(), b.id()));
        assertTrue(network.isLinked(a.id(), b.id()));

        TestNode reloadedB = new TestNode(b.id());
        network.activate(reloadedB);
        assertTrue(bus.isConnected(a.id(), b.id()));
    }

    @Test
    void snapshotCountsPersistentAndLoadedTopologySeparately() throws Exception {
        GraphControlBus bus = new GraphControlBus(32);
        PersistentConnectionStore store = new PersistentConnectionStore(tempDir.resolve("network.txt"));
        PhysicalControlNetwork network = new PhysicalControlNetwork(bus, store);

        TestNode a = new TestNode();
        TestNode b = new TestNode();
        TestNode c = new TestNode();
        network.activate(a);
        network.activate(b);
        network.activate(c);
        network.toggleLink(a.id(), b.id());
        network.toggleLink(b.id(), c.id());
        network.deactivate(c.id(), c);

        ControlNetworkSnapshot snapshot = network.snapshot(a.id());
        assertEquals(3, snapshot.totalNodes());
        assertEquals(2, snapshot.loadedNodes());
        assertEquals(2, snapshot.connections());
    }

    @Test
    void signalDeliveryResumesWhenPeerReloads() throws Exception {
        GraphControlBus bus = new GraphControlBus(32);
        PersistentConnectionStore store = new PersistentConnectionStore(tempDir.resolve("network.txt"));
        PhysicalControlNetwork network = new PhysicalControlNetwork(bus, store);

        TestNode a = new TestNode();
        TestNode b = new TestNode();
        network.activate(a);
        network.activate(b);
        network.toggleLink(a.id(), b.id());

        bus.publish(a.id(), ControlChannel.of("gridworks", "test"), ControlValue.of(true));
        assertEquals(1, b.received.size());

        network.deactivate(b.id(), b);
        TestNode reloadedB = new TestNode(b.id());
        network.activate(reloadedB);

        bus.publish(a.id(), ControlChannel.of("gridworks", "test"), ControlValue.of(false));
        assertEquals(1, reloadedB.received.size());
    }

    private static final class TestNode implements ControlNode {
        private final UUID id;
        private final List<ControlSignal> received = new ArrayList<>();

        private TestNode() {
            this(UUID.randomUUID());
        }

        private TestNode(UUID id) {
            this.id = id;
        }

        @Override
        public UUID id() {
            return id;
        }

        @Override
        public void onSignal(ControlSignal signal) {
            received.add(signal);
        }
    }
}
