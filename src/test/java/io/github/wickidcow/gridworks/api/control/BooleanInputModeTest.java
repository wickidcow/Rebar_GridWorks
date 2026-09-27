package io.github.wickidcow.gridworks.api.control;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BooleanInputModeTest {
    @Test
    void legacyModePreservesOriginalInputs() {
        assertTrue(BooleanInputMode.LEGACY.accepts(GridWorksChannels.REDSTONE_POWERED));
        assertTrue(BooleanInputMode.LEGACY.accepts(GridWorksChannels.CONTROL_ENABLED));
        assertFalse(BooleanInputMode.LEGACY.accepts(GridWorksChannels.CONTROL_A));
    }

    @Test
    void namedModesAreIsolated() {
        assertTrue(BooleanInputMode.A.accepts(GridWorksChannels.CONTROL_A));
        assertFalse(BooleanInputMode.A.accepts(GridWorksChannels.CONTROL_B));
        assertFalse(BooleanInputMode.A.accepts(GridWorksChannels.CONTROL_ENABLED));

        assertTrue(BooleanInputMode.D.accepts(GridWorksChannels.CONTROL_D));
        assertFalse(BooleanInputMode.D.accepts(GridWorksChannels.REDSTONE_POWERED));
    }

    @Test
    void commandChannelsCycleAndDefaultSafely() {
        assertSame(ControlCommandChannel.A, ControlCommandChannel.DEFAULT.cycle(1));
        assertSame(ControlCommandChannel.DEFAULT, ControlCommandChannel.D.cycle(1));
        assertSame(ControlCommandChannel.D, ControlCommandChannel.DEFAULT.cycle(-1));
        assertSame(ControlCommandChannel.DEFAULT, ControlCommandChannel.fromStored("unknown"));
        assertSame(BooleanInputMode.LEGACY, BooleanInputMode.fromStored("unknown"));
    }
}
