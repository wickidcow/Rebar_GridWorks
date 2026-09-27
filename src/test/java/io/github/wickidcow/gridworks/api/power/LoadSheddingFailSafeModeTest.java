package io.github.wickidcow.gridworks.api.power;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LoadSheddingFailSafeModeTest {
    @Test
    void essentialOnlyKeepsOnlyPriorityOneOnline() {
        assertEquals(
                new LoadSheddingOutputState(true, false, false),
                LoadSheddingFailSafeMode.ESSENTIAL_ONLY.resolve(
                        LoadSheddingStage.NORMAL
                )
        );
    }

    @Test
    void allowAllIgnoresLastStageWhenTelemetryIsMissing() {
        assertEquals(
                new LoadSheddingOutputState(true, true, true),
                LoadSheddingFailSafeMode.ALLOW_ALL.resolve(
                        LoadSheddingStage.SHED_NORMAL_AND_OPTIONAL
                )
        );
    }

    @Test
    void holdLastUsesLastKnownStage() {
        assertEquals(
                new LoadSheddingOutputState(true, true, false),
                LoadSheddingFailSafeMode.HOLD_LAST.resolve(
                        LoadSheddingStage.SHED_OPTIONAL
                )
        );
    }

    @Test
    void storedModeDefaultsSafely() {
        assertEquals(
                LoadSheddingFailSafeMode.ESSENTIAL_ONLY,
                LoadSheddingFailSafeMode.fromStored(null)
        );
        assertEquals(
                LoadSheddingFailSafeMode.ESSENTIAL_ONLY,
                LoadSheddingFailSafeMode.fromStored("bad")
        );
    }
}
