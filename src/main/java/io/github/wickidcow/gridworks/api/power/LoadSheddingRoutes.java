package io.github.wickidcow.gridworks.api.power;

import io.github.wickidcow.gridworks.api.control.ControlAddress;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record LoadSheddingRoutes(
        ControlAddress essential,
        ControlAddress normal,
        ControlAddress optional
) {
    public LoadSheddingRoutes {
        Objects.requireNonNull(essential, "essential");
        Objects.requireNonNull(normal, "normal");
        Objects.requireNonNull(optional, "optional");

        if (essential.equals(normal)
                || essential.equals(optional)
                || normal.equals(optional)) {
            throw new IllegalArgumentException(
                    "Load-shedding tier addresses must be distinct"
            );
        }
    }

    public static LoadSheddingRoutes repaired(
            UUID nodeId,
            ControlAddress essential,
            ControlAddress normal,
            ControlAddress optional
    ) {
        Objects.requireNonNull(nodeId, "nodeId");
        Objects.requireNonNull(essential, "essential");
        Objects.requireNonNull(normal, "normal");
        Objects.requireNonNull(optional, "optional");

        Set<ControlAddress> used = new HashSet<>();
        used.add(essential);

        ControlAddress repairedNormal = ensureUnique(
                nodeId,
                "normal",
                normal,
                used
        );
        used.add(repairedNormal);

        ControlAddress repairedOptional = ensureUnique(
                nodeId,
                "optional",
                optional,
                used
        );

        return new LoadSheddingRoutes(
                essential,
                repairedNormal,
                repairedOptional
        );
    }

    private static ControlAddress ensureUnique(
            UUID nodeId,
            String prefix,
            ControlAddress requested,
            Set<ControlAddress> used
    ) {
        if (!used.contains(requested)) {
            return requested;
        }

        for (int suffix = 0; suffix < 10_000; suffix++) {
            String candidatePrefix = suffix == 0
                    ? prefix
                    : prefix + "_" + suffix;
            ControlAddress candidate = ControlAddress.defaultFor(
                    nodeId,
                    candidatePrefix
            );
            if (!used.contains(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException(
                "Could not generate a unique load-shedding address"
        );
    }
}
