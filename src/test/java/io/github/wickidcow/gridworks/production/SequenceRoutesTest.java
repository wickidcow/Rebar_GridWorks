package io.github.wickidcow.gridworks.production;

import static org.junit.jupiter.api.Assertions.*;

import io.github.wickidcow.gridworks.api.control.ControlAddress;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SequenceRoutesTest {
    @Test
    void defaultsAreDistinctAndDeterministic() {
        UUID nodeId = UUID.randomUUID();

        SequenceRoutes first = SequenceRoutes.defaults(nodeId);
        SequenceRoutes second = SequenceRoutes.defaults(nodeId);

        assertEquals(first, second);
        assertEquals(4, first.triggers().size());
        assertEquals(4, first.outputs().size());

        assertNotEquals(first.start(), first.complete());
        for (int stage = 1; stage <= 4; stage++) {
            assertTrue(first.isInputAddress(first.trigger(stage)));
            assertTrue(first.isOutputAddress(first.output(stage)));
        }
    }

    @Test
    void rejectsCollisionsAcrossInputsAndOutputs() {
        SequenceRoutes routes = SequenceRoutes.defaults(UUID.randomUUID());

        assertThrows(
                IllegalArgumentException.class,
                () -> routes.withOutput(1, routes.start())
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> routes.withTrigger(2, routes.output(3))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> routes.withComplete(routes.trigger(4))
        );
    }

    @Test
    void routeEditsPreserveUnrelatedAddresses() {
        SequenceRoutes routes = SequenceRoutes.defaults(UUID.randomUUID());
        ControlAddress changed = ControlAddress.fromUserInput("mixer_stage_2");

        SequenceRoutes edited = routes.withOutput(2, changed);

        assertEquals(changed, edited.output(2));
        assertEquals(routes.output(1), edited.output(1));
        assertEquals(routes.trigger(2), edited.trigger(2));
        assertEquals(routes.start(), edited.start());
        assertEquals(routes.complete(), edited.complete());
    }

    @Test
    void stageAccessIsValidated() {
        SequenceRoutes routes = SequenceRoutes.defaults(UUID.randomUUID());

        assertThrows(IllegalArgumentException.class, () -> routes.trigger(0));
        assertThrows(IllegalArgumentException.class, () -> routes.output(5));
    }
}
