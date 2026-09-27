package io.github.wickidcow.gridworks.production;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SequenceFaultReasonTest {
    @Test
    void nonFaultedStateAlwaysLoadsNone() {
        assertEquals(SequenceFaultReason.NONE, SequenceFaultReason.fromStored("timeout", false));
        assertEquals(SequenceFaultReason.NONE, SequenceFaultReason.fromStored(null, false));
    }

    @Test
    void faultedStateRestoresKnownReasons() {
        assertEquals(SequenceFaultReason.TIMEOUT, SequenceFaultReason.fromStored("timeout", true));
        assertEquals(SequenceFaultReason.INTERLOCK, SequenceFaultReason.fromStored("INTERLOCK", true));
    }

    @Test
    void missingInvalidOrNoneFaultReasonFallsBackToUnknown() {
        assertEquals(SequenceFaultReason.UNKNOWN, SequenceFaultReason.fromStored(null, true));
        assertEquals(SequenceFaultReason.UNKNOWN, SequenceFaultReason.fromStored("garbage", true));
        assertEquals(SequenceFaultReason.UNKNOWN, SequenceFaultReason.fromStored("none", true));
    }
}
