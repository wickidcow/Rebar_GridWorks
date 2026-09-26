package io.github.wickidcow.gridworks.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.wickidcow.gridworks.api.control.ControlChannel;
import io.github.wickidcow.gridworks.api.control.ControlDispatchResult;
import io.github.wickidcow.gridworks.api.control.ControlNode;
import io.github.wickidcow.gridworks.api.control.ControlSignal;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GraphControlBusTest {
    private static final ControlChannel CHANNEL = ControlChannel.of("gridworks", "test/signal");

    @Test
    void cyclesDeliverAtMostOncePerRecipient() {
        GraphControlBus bus = new GraphControlBus(16);
        TestNode a = new TestNode();
        TestNode b = new TestNode();
        TestNode c = new TestNode();
        register(bus, a, b, c);

        bus.connect(a.id(), b.id());
        bus.connect(b.id(), c.id());
        bus.connect(c.id(), a.id());

        ControlDispatchResult result = bus.publish(a.id(), CHANNEL, ControlValue.of(true));

        assertEquals(2, result.delivered());
        assertFalse(result.truncated());
        assertTrue(result.failures().isEmpty());
        assertEquals(0, a.received.size());
        assertEquals(1, b.received.size());
        assertEquals(1, c.received.size());
        assertEquals(b.received.getFirst().sequence(), c.received.getFirst().sequence());
    }

    @Test
    void propagationCapDoesNotCountTheSourceNode() {
        GraphControlBus bus = new GraphControlBus(2);
        TestNode a = new TestNode();
        TestNode b = new TestNode();
        TestNode c = new TestNode();
        register(bus, a, b, c);
        bus.connect(a.id(), b.id());
        bus.connect(b.id(), c.id());

        ControlDispatchResult exact = bus.publish(a.id(), CHANNEL, ControlValue.of(1.0));
        assertEquals(2, exact.delivered());
        assertFalse(exact.truncated());

        TestNode d = new TestNode();
        bus.register(d);
        bus.connect(c.id(), d.id());

        ControlDispatchResult capped = bus.publish(a.id(), CHANNEL, ControlValue.of(2.0));
        assertEquals(2, capped.delivered());
        assertTrue(capped.truncated());
        assertEquals(0, d.received.size());
    }

    @Test
    void unregisterRemovesItsConnections() {
        GraphControlBus bus = new GraphControlBus(16);
        TestNode a = new TestNode();
        TestNode b = new TestNode();
        TestNode c = new TestNode();
        register(bus, a, b, c);
        bus.connect(a.id(), b.id());
        bus.connect(b.id(), c.id());

        assertEquals(2, bus.connectionCount());
        assertTrue(bus.unregister(b.id()));

        assertEquals(0, bus.connectionCount());
        assertEquals(Set.of(a.id()), bus.componentOf(a.id()));
        assertTrue(bus.neighbors(c.id()).isEmpty());
    }

    @Test
    void receiverFailureDoesNotStopOtherReceivers() {
        GraphControlBus bus = new GraphControlBus(16);
        TestNode source = new TestNode();
        TestNode broken = new TestNode();
        broken.throwOnSignal = true;
        TestNode healthy = new TestNode();
        register(bus, source, broken, healthy);
        bus.connect(source.id(), broken.id());
        bus.connect(source.id(), healthy.id());

        ControlDispatchResult result = bus.publish(source.id(), CHANNEL, ControlValue.of("running"));

        assertEquals(1, result.delivered());
        assertEquals(1, result.failures().size());
        assertEquals(broken.id(), result.failures().getFirst().nodeId());
        assertEquals(1, healthy.received.size());
        assertFalse(result.successful());
    }

    @Test
    void channelFilteringHappensBeforeDelivery() {
        GraphControlBus bus = new GraphControlBus(16);
        TestNode source = new TestNode();
        TestNode receiver = new TestNode();
        receiver.acceptedChannel = ControlChannel.of("gridworks", "other");
        register(bus, source, receiver);
        bus.connect(source.id(), receiver.id());

        ControlDispatchResult result = bus.publish(source.id(), CHANNEL, ControlValue.of(true));

        assertEquals(0, result.delivered());
        assertTrue(receiver.received.isEmpty());
    }

    @Test
    void rejectsInvalidValuesAndChannels() {
        assertThrows(IllegalArgumentException.class, () -> ControlValue.of(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> ControlValue.of(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> ControlChannel.parse("MissingNamespace"));
        assertThrows(IllegalArgumentException.class, () -> ControlChannel.of("GridWorks", "bad"));
        assertEquals("gridworks:machine/running", ControlChannel.parse("gridworks:machine/running").toString());
    }

    private static void register(GraphControlBus bus, TestNode... nodes) {
        for (TestNode node : nodes) {
            bus.register(node);
        }
    }

    private static final class TestNode implements ControlNode {
        private final UUID id = UUID.randomUUID();
        private final List<ControlSignal> received = new ArrayList<>();
        private ControlChannel acceptedChannel;
        private boolean throwOnSignal;

        @Override
        public UUID id() {
            return id;
        }

        @Override
        public boolean accepts(ControlChannel channel) {
            return acceptedChannel == null || acceptedChannel.equals(channel);
        }

        @Override
        public void onSignal(ControlSignal signal) {
            if (throwOnSignal) {
                throw new IllegalStateException("test failure");
            }
            received.add(signal);
        }
    }
}
