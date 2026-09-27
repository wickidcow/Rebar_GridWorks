package io.github.wickidcow.gridworks.control;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RisingEdgeTriggerTest {
    @Test
    void firstObservationNeverTriggers() {
        RisingEdgeTrigger trigger = new RisingEdgeTrigger();

        assertFalse(trigger.observe(true));
        assertTrue(trigger.isInitialized());
        assertTrue(trigger.currentState());
    }

    @Test
    void onlyFalseToTrueTriggers() {
        RisingEdgeTrigger trigger = new RisingEdgeTrigger();

        assertFalse(trigger.observe(false));
        assertFalse(trigger.observe(false));
        assertTrue(trigger.observe(true));
        assertFalse(trigger.observe(true));
        assertFalse(trigger.observe(false));
        assertTrue(trigger.observe(true));
    }

    @Test
    void resetRequiresFreshBaseline() {
        RisingEdgeTrigger trigger = new RisingEdgeTrigger();

        trigger.observe(false);
        assertTrue(trigger.observe(true));

        trigger.reset();
        assertFalse(trigger.isInitialized());
        assertFalse(trigger.observe(true));
    }
}
