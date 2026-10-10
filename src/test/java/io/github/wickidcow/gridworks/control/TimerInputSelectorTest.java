package io.github.wickidcow.gridworks.control;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TimerInputSelectorTest {
    @Test void autoBindsOnlyTheFirstEligibleLinkedPeer() {
        TimerInputSelector selector = new TimerInputSelector(null);
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        assertFalse(selector.accept(first, false));
        assertNull(selector.activeSource());
        assertTrue(selector.accept(first, true));
        assertFalse(selector.accept(second, true));
        assertEquals(first, selector.activeSource());
        assertFalse(selector.peerUnavailable(second));
        assertTrue(selector.peerUnavailable(first));
        assertTrue(selector.accept(second, true));
        assertEquals(second, selector.activeSource());
    }

    @Test void explicitPreferenceRejectsOtherPeersAndSurvivesUnload() {
        UUID selected = UUID.randomUUID(), unrelated = UUID.randomUUID();
        TimerInputSelector selector = new TimerInputSelector(selected);
        assertFalse(selector.accept(unrelated, true));
        assertFalse(selector.accept(selected, false));
        assertTrue(selector.accept(selected, true));
        assertTrue(selector.peerUnavailable(selected));
        assertEquals(selected, selector.preferredSource());
        assertFalse(selector.accept(unrelated, true));
        assertNull(selector.activeSource());
    }

    @Test void transientResetRetainsSavedPreference() {
        UUID selected = UUID.randomUUID();
        TimerInputSelector selector = new TimerInputSelector(selected);
        selector.accept(selected, true);
        selector.resetActive();
        assertNull(selector.activeSource());
        assertEquals(selected, selector.preferredSource());
        assertTrue(selector.accept(selected, true));
    }

    @Test void preferenceChangesClearPreviousObservation() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        TimerInputSelector selector = new TimerInputSelector(a);
        assertTrue(selector.accept(a, true));
        selector.select(b);
        assertNull(selector.activeSource());
        assertFalse(selector.accept(a, true));
        assertTrue(selector.accept(b, true));
        selector.select(null);
        assertNull(selector.preferredSource());
        assertTrue(selector.accept(a, true));
    }

    @Test void nullSignalSourceIsInvalid() {
        TimerInputSelector selector = new TimerInputSelector(null);
        assertThrows(NullPointerException.class, () -> selector.accept(null, true));
        assertThrows(NullPointerException.class, () -> selector.peerUnavailable(null));
    }
}
