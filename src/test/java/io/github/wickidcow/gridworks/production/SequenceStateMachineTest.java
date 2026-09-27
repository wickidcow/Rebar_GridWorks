package io.github.wickidcow.gridworks.production;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SequenceStateMachineTest {
    @Test
    void sequenceRunsFourStagesThenCompletes() {
        SequenceStateMachine sequence = new SequenceStateMachine();

        assertEquals(SequenceStateMachine.Phase.IDLE, sequence.phase());
        assertEquals(1, sequence.start().currentStage());

        assertEquals(2, sequence.advance().currentStage());
        assertEquals(3, sequence.advance().currentStage());
        assertEquals(4, sequence.advance().currentStage());

        SequenceStateMachine.Transition completed = sequence.advance();
        assertEquals(SequenceStateMachine.Phase.COMPLETE, completed.phase());
        assertEquals(0, completed.currentStage());
        assertTrue(sequence.isComplete());
    }

    @Test
    void advanceDoesNothingUnlessRunning() {
        SequenceStateMachine sequence = new SequenceStateMachine();

        assertFalse(sequence.advance().changed());
        sequence.start();
        sequence.abort();
        assertFalse(sequence.advance().changed());
    }

    @Test
    void startRestartsASequenceAtStageOne() {
        SequenceStateMachine sequence = new SequenceStateMachine();
        sequence.start();
        sequence.advance();
        assertEquals(2, sequence.currentStage());

        SequenceStateMachine.Transition restarted = sequence.start();

        assertEquals(1, restarted.currentStage());
        assertEquals(SequenceStateMachine.Phase.RUNNING, restarted.phase());
    }

    @Test
    void abortClearsRunningOrCompleteState() {
        SequenceStateMachine sequence = new SequenceStateMachine();
        sequence.start();
        sequence.advance();

        sequence.abort();

        assertEquals(SequenceStateMachine.Phase.IDLE, sequence.phase());
        assertEquals(0, sequence.currentStage());
        assertFalse(sequence.isRunning());
        assertFalse(sequence.isComplete());
    }

    @Test
    void storedStateUsesConservativeFallbacks() {
        assertEquals(
                SequenceStateMachine.Phase.RUNNING,
                SequenceStateMachine.fromStored("running", 3).phase()
        );
        assertEquals(
                3,
                SequenceStateMachine.fromStored("running", 3).currentStage()
        );
        assertEquals(
                SequenceStateMachine.Phase.IDLE,
                SequenceStateMachine.fromStored("running", 99).phase()
        );
        assertEquals(
                SequenceStateMachine.Phase.COMPLETE,
                SequenceStateMachine.fromStored("complete", 99).phase()
        );
        assertEquals(
                SequenceStateMachine.Phase.IDLE,
                SequenceStateMachine.fromStored("garbage", 2).phase()
        );
    }

    @Test
    void constructorRejectsInconsistentState() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SequenceStateMachine(SequenceStateMachine.Phase.RUNNING, 0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new SequenceStateMachine(SequenceStateMachine.Phase.IDLE, 1)
        );
    }
}
