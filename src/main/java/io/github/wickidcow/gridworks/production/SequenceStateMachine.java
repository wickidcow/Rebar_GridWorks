package io.github.wickidcow.gridworks.production;

import java.util.Locale;

/**
 * Persistent four-stage production sequence state.
 */
public final class SequenceStateMachine {
    public static final int STAGE_COUNT = 4;

    private Phase phase;
    private int currentStage;

    public SequenceStateMachine() {
        this(Phase.IDLE, 0);
    }

    public SequenceStateMachine(Phase phase, int currentStage) {
        if (phase == null) {
            throw new NullPointerException("phase");
        }
        if (phase == Phase.RUNNING) {
            if (currentStage < 1 || currentStage > STAGE_COUNT) {
                throw new IllegalArgumentException(
                        "running sequence stage must be between 1 and " + STAGE_COUNT
                );
            }
        } else if (currentStage != 0) {
            throw new IllegalArgumentException(
                    "idle/complete sequence must use currentStage=0"
            );
        }

        this.phase = phase;
        this.currentStage = currentStage;
    }

    public static SequenceStateMachine fromStored(String storedPhase, Integer storedStage) {
        Phase phase = Phase.fromStored(storedPhase);
        int stage = storedStage == null ? 0 : storedStage;

        if (phase == Phase.RUNNING && stage >= 1 && stage <= STAGE_COUNT) {
            return new SequenceStateMachine(phase, stage);
        }
        if (phase == Phase.COMPLETE) {
            return new SequenceStateMachine(Phase.COMPLETE, 0);
        }
        return new SequenceStateMachine();
    }

    public synchronized Transition start() {
        Phase previousPhase = phase;
        int previousStage = currentStage;

        phase = Phase.RUNNING;
        currentStage = 1;
        return transition(previousPhase, previousStage);
    }

    public synchronized Transition advance() {
        Phase previousPhase = phase;
        int previousStage = currentStage;

        if (phase != Phase.RUNNING) {
            return transition(previousPhase, previousStage);
        }

        if (currentStage < STAGE_COUNT) {
            currentStage++;
        } else {
            phase = Phase.COMPLETE;
            currentStage = 0;
        }

        return transition(previousPhase, previousStage);
    }

    public synchronized Transition abort() {
        Phase previousPhase = phase;
        int previousStage = currentStage;

        phase = Phase.IDLE;
        currentStage = 0;
        return transition(previousPhase, previousStage);
    }

    public synchronized Phase phase() {
        return phase;
    }

    public synchronized int currentStage() {
        return currentStage;
    }

    public synchronized boolean isRunning() {
        return phase == Phase.RUNNING;
    }

    public synchronized boolean isComplete() {
        return phase == Phase.COMPLETE;
    }

    private Transition transition(Phase previousPhase, int previousStage) {
        return new Transition(
                previousPhase,
                previousStage,
                phase,
                currentStage,
                previousPhase != phase || previousStage != currentStage
        );
    }

    public enum Phase {
        IDLE,
        RUNNING,
        COMPLETE;

        public static Phase fromStored(String stored) {
            if (stored == null) {
                return IDLE;
            }
            try {
                return valueOf(stored.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return IDLE;
            }
        }
    }

    public record Transition(
            Phase previousPhase,
            int previousStage,
            Phase phase,
            int currentStage,
            boolean changed
    ) {
    }
}
