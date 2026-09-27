package io.github.wickidcow.gridworks.control;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AlarmLatchTest {
    @Test
    void firstActiveReplayLatchesSilently() {
        AlarmLatch latch = new AlarmLatch(false, false);

        assertFalse(latch.observe(true));
        assertTrue(latch.isConditionActive());
        assertTrue(latch.isLatched());
        assertFalse(latch.isAcknowledged());
    }

    @Test
    void realRisingEdgeRingsAndResetsAcknowledgement() {
        AlarmLatch latch = new AlarmLatch(false, false);

        latch.observe(false);
        assertTrue(latch.observe(true));
        latch.acknowledge();
        assertTrue(latch.isAcknowledged());

        latch.observe(false);
        assertFalse(latch.isLatched());

        assertTrue(latch.observe(true));
        assertTrue(latch.isLatched());
        assertFalse(latch.isAcknowledged());
    }

    @Test
    void unacknowledgedFaultStaysLatchedAfterConditionClears() {
        AlarmLatch latch = new AlarmLatch(false, false);

        latch.observe(false);
        latch.observe(true);
        latch.observe(false);

        assertFalse(latch.isConditionActive());
        assertTrue(latch.isLatched());
        assertFalse(latch.isAcknowledged());

        latch.acknowledge();
        assertFalse(latch.isLatched());
    }

    @Test
    void acknowledgedActiveFaultClearsWhenConditionClears() {
        AlarmLatch latch = new AlarmLatch(false, false);

        latch.observe(true);
        latch.acknowledge();

        assertTrue(latch.isLatched());
        assertTrue(latch.isAcknowledged());

        latch.observe(false);

        assertFalse(latch.isLatched());
        assertFalse(latch.isAcknowledged());
    }

    @Test
    void restoredAcknowledgedAlarmDoesNotRingAgain() {
        AlarmLatch latch = new AlarmLatch(true, true);

        latch.resetObservation();
        assertFalse(latch.observe(true));
        assertTrue(latch.isLatched());
        assertTrue(latch.isAcknowledged());
    }
}
