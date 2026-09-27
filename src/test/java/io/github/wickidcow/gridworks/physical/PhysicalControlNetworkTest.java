package io.github.wickidcow.gridworks.physical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.wickidcow.gridworks.api.control.ControlChannel;
import io.github.wickidcow.gridworks.api.control.ControlSignal;
import io.github.wickidcow.gridworks.api.control.ControlStateSource;
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
        List<RuntimeException> callbackFailures = new ArrayList<>();
        PhysicalControlNetwork network = new PhysicalControlNetwork(bus, store, callbackFailures::add);

        TestNode a = new TestNode();
        TestNode b = new TestNode();
        network.activate(a);
        network.activate(b);
        assertTrue(network.toggleLink(a.id(), b.id()));
        assertTrue(bus.isConnected(a.id(), b.id()));
        assertEquals(List.of(b.id()), a.availablePeers);
        assertEquals(List.of(a.id()), b.availablePeers);

        network.deactivate(b.id(), b);
        assertFalse(bus.isConnected(a.id(), b.id()));
        assertTrue(network.isLinked(a.id(), b.id()));
        assertEquals(List.of(b.id()), a.unavailablePeers);

        TestNode reloadedB = new TestNode(b.id());
        network.activate(reloadedB);
        assertTrue(bus.isConnected(a.id(), b.id()));
        assertEquals(List.of(a.id()), reloadedB.availablePeers);
        assertEquals(List.of(b.id(), b.id()), a.availablePeers);
        assertTrue(callbackFailures.isEmpty());
    }

    @Test
    void unlinkNotifiesBothLoadedPeersUnavailable() throws Exception {
        GraphControlBus bus = new GraphControlBus(32);
        PersistentConnectionStore store = new PersistentConnectionStore(tempDir.resolve("network.txt"));
        PhysicalControlNetwork network = new PhysicalControlNetwork(bus, store, ignored -> {});

        TestNode a = new TestNode();
        TestNode b = new TestNode();
        network.activate(a);
        network.activate(b);
        network.toggleLink(a.id(), b.id());

        assertFalse(network.toggleLink(a.id(), b.id()));

        assertEquals(List.of(b.id()), a.unavailablePeers);
        assertEquals(List.of(a.id()), b.unavailablePeers);
    }

    @Test
    void snapshotCountsPersistentAndLoadedTopologySeparately() throws Exception {
        GraphControlBus bus = new GraphControlBus(32);
        PersistentConnectionStore store = new PersistentConnectionStore(tempDir.resolve("network.txt"));
        PhysicalControlNetwork network = new PhysicalControlNetwork(bus, store, ignored -> {});

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
        PhysicalControlNetwork network = new PhysicalControlNetwork(bus, store, ignored -> {});

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

    @Test
    void stateSourcesReplayAcrossLoadedComponentWhenTopologyExpands() throws Exception {
        GraphControlBus bus = new GraphControlBus(32);
        PersistentConnectionStore store = new PersistentConnectionStore(tempDir.resolve("network.txt"));
        PhysicalControlNetwork network = new PhysicalControlNetwork(bus, store, ignored -> {});

        StatefulTestNode source = new StatefulTestNode(bus, true);
        TestNode middle = new TestNode();
        TestNode receiver = new TestNode();

        network.activate(source);
        network.activate(middle);
        network.activate(receiver);

        network.toggleLink(source.id(), middle.id());
        receiver.received.clear();

        // The source is not directly linked to receiver. Expanding the loaded
        // component must still replay source state through the whole bus.
        network.toggleLink(middle.id(), receiver.id());

        assertEquals(1, receiver.received.size());
        assertEquals(ControlValue.of(true), receiver.received.getFirst().value());
    }

    @Test
    void peerCallbackFailuresDoNotBreakTopologyChanges() throws Exception {
        GraphControlBus bus = new GraphControlBus(32);
        PersistentConnectionStore store = new PersistentConnectionStore(tempDir.resolve("network.txt"));
        List<RuntimeException> failures = new ArrayList<>();
        PhysicalControlNetwork network = new PhysicalControlNetwork(bus, store, failures::add);

        TestNode a = new TestNode();
        TestNode b = new TestNode();
        a.throwOnPeerAvailable = true;

        network.activate(a);
        network.activate(b);
        assertTrue(network.toggleLink(a.id(), b.id()));

        assertTrue(bus.isConnected(a.id(), b.id()));
        assertTrue(network.isLinked(a.id(), b.id()));
        assertEquals(1, failures.size());
        assertEquals(List.of(a.id()), b.availablePeers);

        b.throwOnPeerUnavailable = true;
        network.deactivate(a.id(), a);

        assertEquals(2, failures.size());
        assertFalse(network.isActive(a.id()));
        assertTrue(network.isActive(b.id()));
    }

    private static final class StatefulTestNode implements ControlStateSource {
        private final UUID id = UUID.randomUUID();
        private final GraphControlBus bus;
        private final boolean state;

        private StatefulTestNode(GraphControlBus bus, boolean state) {
            this.bus = bus;
            this.state = state;
        }

        @Override
        public UUID id() {
            return id;
        }

        @Override
        public void onSignal(ControlSignal signal) {
        }

        @Override
        public void publishCurrentState() {
            bus.publish(
                    id,
                    ControlChannel.of("gridworks", "test/state"),
                    ControlValue.of(state)
            );
        }
    }

    private static final class TestNode implements PhysicalControlEndpoint {
        private final UUID id;
        private final List<ControlSignal> received = new ArrayList<>();
        private final List<UUID> availablePeers = new ArrayList<>();
        private final List<UUID> unavailablePeers = new ArrayList<>();
        private boolean throwOnPeerAvailable;
        private boolean throwOnPeerUnavailable;

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

        @Override
        public void onControlPeerAvailable(UUID peerId) {
            if (throwOnPeerAvailable) {
                throw new IllegalStateException("test failure");
            }
            availablePeers.add(peerId);
        }

        @Override
        public void onControlPeerUnavailable(UUID peerId) {
            if (throwOnPeerUnavailable) {
                throw new IllegalStateException("test failure");
            }
            unavailablePeers.add(peerId);
        }
    }
}
