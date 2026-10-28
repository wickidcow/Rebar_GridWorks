package io.github.wickidcow.gridworks.control;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TimerCycleEngineTest {
    @Test
    void firstReplayedOnDoesNotStartOneShotOrFakeAnEdge() {
        TimerCycleEngine timer = new TimerCycleEngine(
                TimerCycleEngine.Mode.ONE_SHOT, 5, 20, 1);
        assertEquals(TimerCycleEngine.Phase.IDLE, timer.observeInput(true, 100).phase());
        assertFalse(timer.advance(200).output());

        timer.observeInput(false, 201);
        assertEquals(TimerCycleEngine.Phase.WAITING, timer.observeInput(true, 202).phase());
        assertEquals(207, timer.state().nextDueTick().orElseThrow());
        assertFalse(timer.advance(206).output());
        assertTrue(timer.advance(207).output());
        assertEquals(227, timer.state().nextDueTick().orElseThrow());

        assertFalse(timer.advance(227).output());
        assertEquals(TimerCycleEngine.Phase.COMPLETE, timer.state().phase());
        assertTrue(timer.state().nextDueTick().isEmpty());
        assertEquals(1, timer.state().completedCycles());

        timer.observeInput(true, 400);
        assertEquals(TimerCycleEngine.Phase.COMPLETE, timer.state().phase());
    }

    @Test
    void repeatedPulseIsOneTickWideWithNoPollingOrCatchupStorm() {
        TimerCycleEngine timer = new TimerCycleEngine(
                TimerCycleEngine.Mode.REPEATING_PULSE, 0, 1, 19);
        timer.observeInput(false, 0);
        timer.observeInput(true, 1);

        assertTrue(timer.advance(1).output());
        assertFalse(timer.advance(2).output());
        assertEquals(21, timer.state().nextDueTick().orElseThrow());

        // Millions of missed ticks may produce one transition only.
        assertTrue(timer.advance(1_000_000).output());
        assertEquals(1_000_001, timer.state().nextDueTick().orElseThrow());
        assertTrue(timer.advance(1_000_000).output());
        assertFalse(timer.advance(1_000_001).output());
        assertEquals(2, timer.state().completedCycles());
    }

    @Test
    void dutyCyclePreservesOnAndOffTiming() {
        TimerCycleEngine timer = new TimerCycleEngine(
                TimerCycleEngine.Mode.DUTY_CYCLE, 10, 6, 14);
        timer.manualStart(100);

        assertFalse(timer.advance(109).output());
        assertTrue(timer.advance(110).output());
        assertFalse(timer.advance(116).output());
        assertTrue(timer.advance(130).output());
        assertEquals(136, timer.state().nextDueTick().orElseThrow());
    }

    @Test
    void stopOrLoadNeverResumesWorkOrKeepsAnOnOutput() {
        TimerCycleEngine timer = new TimerCycleEngine(
                TimerCycleEngine.Mode.DUTY_CYCLE, 0, 5, 5);
        timer.manualStart(0);
        timer.advance(0);
        assertTrue(timer.state().output());

        timer.stop();
        assertFalse(timer.state().output());
        assertTrue(timer.state().nextDueTick().isEmpty());
        assertFalse(timer.advance(100).output());

        timer.observeInput(false, 101);
        timer.observeInput(true, 102);
        timer.advance(102);
        assertTrue(timer.state().output());

        timer.resetAfterLoad();
        assertFalse(timer.state().output());
        assertTrue(timer.state().nextDueTick().isEmpty());
        assertEquals(TimerCycleEngine.Phase.IDLE, timer.observeInput(true, 103).phase());
    }

    @Test
    void changingConfigurationCancelsOldDeadlinesAndRebaselinesInput() {
        TimerCycleEngine timer = new TimerCycleEngine(
                TimerCycleEngine.Mode.ONE_SHOT, 5, 5, 1);
        timer.observeInput(false, 0);
        timer.observeInput(true, 10);
        timer.configure(TimerCycleEngine.Mode.REPEATING_PULSE, 0, 1, 20);

        assertTrue(timer.state().nextDueTick().isEmpty());
        assertEquals(TimerCycleEngine.Phase.IDLE, timer.observeInput(true, 15).phase());
        timer.observeInput(false, 16);
        assertEquals(TimerCycleEngine.Phase.WAITING, timer.observeInput(true, 17).phase());
    }

    @Test
    void overflowDeadlinesClampInsteadOfWrapping() {
        TimerCycleEngine timer = new TimerCycleEngine(
                TimerCycleEngine.Mode.ONE_SHOT, 60, 5, 1);
        timer.manualStart(Long.MAX_VALUE - 5);
        assertEquals(Long.MAX_VALUE, timer.state().nextDueTick().orElseThrow());
        assertFalse(timer.advance(Long.MAX_VALUE - 1).output());
        assertTrue(timer.advance(Long.MAX_VALUE).output());
    }

    @Test
    void invalidSchedulesNeverSilentlyCreateFastLoops() {
        assertThrows(IllegalArgumentException.class,
                () -> new TimerCycleEngine(TimerCycleEngine.Mode.REPEATING_PULSE, 0, 2, 20));
        assertThrows(IllegalArgumentException.class,
                () -> new TimerCycleEngine(TimerCycleEngine.Mode.DUTY_CYCLE, 0, 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new TimerCycleEngine(TimerCycleEngine.Mode.ONE_SHOT, -1, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new TimerCycleEngine(TimerCycleEngine.Mode.ONE_SHOT,
                        TimerCycleEngine.MAX_DURATION_TICKS + 1, 1, 1));
        TimerCycleEngine timer = new TimerCycleEngine(
                TimerCycleEngine.Mode.ONE_SHOT, 1, 1, 1);
        assertThrows(IllegalArgumentException.class, () -> timer.manualStart(-1));
        assertThrows(NullPointerException.class, () -> timer.configure(null, 0, 1, 1));
    }
}
