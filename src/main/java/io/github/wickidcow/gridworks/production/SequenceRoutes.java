package io.github.wickidcow.gridworks.production;

import io.github.wickidcow.gridworks.api.control.ControlAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Collision-safe addressed routes for a four-stage Sequence Controller.
 */
public record SequenceRoutes(
        ControlAddress start,
        ControlAddress complete,
        List<ControlAddress> triggers,
        List<ControlAddress> outputs
) {
    public SequenceRoutes {
        start = Objects.requireNonNull(start, "start");
        complete = Objects.requireNonNull(complete, "complete");
        triggers = List.copyOf(Objects.requireNonNull(triggers, "triggers"));
        outputs = List.copyOf(Objects.requireNonNull(outputs, "outputs"));

        if (triggers.size() != SequenceStateMachine.STAGE_COUNT
                || outputs.size() != SequenceStateMachine.STAGE_COUNT) {
            throw new IllegalArgumentException(
                    "Sequence Controller requires exactly "
                            + SequenceStateMachine.STAGE_COUNT
                            + " trigger and output addresses"
            );
        }

        Set<ControlAddress> unique = new HashSet<>();
        unique.add(start);
        if (!unique.add(complete)) {
            throw new IllegalArgumentException("Sequence addresses must be distinct");
        }
        for (ControlAddress trigger : triggers) {
            if (!unique.add(Objects.requireNonNull(trigger, "trigger"))) {
                throw new IllegalArgumentException("Sequence addresses must be distinct");
            }
        }
        for (ControlAddress output : outputs) {
            if (!unique.add(Objects.requireNonNull(output, "output"))) {
                throw new IllegalArgumentException("Sequence addresses must be distinct");
            }
        }
    }

    public static SequenceRoutes defaults(UUID nodeId) {
        Objects.requireNonNull(nodeId, "nodeId");

        List<ControlAddress> triggers = new ArrayList<>();
        List<ControlAddress> outputs = new ArrayList<>();
        for (int stage = 1; stage <= SequenceStateMachine.STAGE_COUNT; stage++) {
            triggers.add(ControlAddress.defaultFor(nodeId, "seq" + stage + "_next"));
            outputs.add(ControlAddress.defaultFor(nodeId, "seq" + stage + "_step"));
        }

        return new SequenceRoutes(
                ControlAddress.defaultFor(nodeId, "seq_start"),
                ControlAddress.defaultFor(nodeId, "seq_done"),
                triggers,
                outputs
        );
    }

    public ControlAddress trigger(int stage) {
        return triggers.get(index(stage));
    }

    public ControlAddress output(int stage) {
        return outputs.get(index(stage));
    }

    public SequenceRoutes withStart(ControlAddress next) {
        return new SequenceRoutes(next, complete, triggers, outputs);
    }

    public SequenceRoutes withComplete(ControlAddress next) {
        return new SequenceRoutes(start, next, triggers, outputs);
    }

    public SequenceRoutes withTrigger(int stage, ControlAddress next) {
        List<ControlAddress> changed = new ArrayList<>(triggers);
        changed.set(index(stage), Objects.requireNonNull(next, "next"));
        return new SequenceRoutes(start, complete, changed, outputs);
    }

    public SequenceRoutes withOutput(int stage, ControlAddress next) {
        List<ControlAddress> changed = new ArrayList<>(outputs);
        changed.set(index(stage), Objects.requireNonNull(next, "next"));
        return new SequenceRoutes(start, complete, triggers, changed);
    }

    public boolean isInputAddress(ControlAddress address) {
        return start.equals(address) || triggers.contains(address);
    }

    public boolean isOutputAddress(ControlAddress address) {
        return complete.equals(address) || outputs.contains(address);
    }

    private static int index(int stage) {
        if (stage < 1 || stage > SequenceStateMachine.STAGE_COUNT) {
            throw new IllegalArgumentException(
                    "stage must be between 1 and " + SequenceStateMachine.STAGE_COUNT
            );
        }
        return stage - 1;
    }
}
