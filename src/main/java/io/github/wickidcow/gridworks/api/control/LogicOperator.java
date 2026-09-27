package io.github.wickidcow.gridworks.api.control;

import java.util.Objects;
import java.util.Optional;

/**
 * Boolean combination for controller conditions with three-state support.
 *
 * <p>An empty Optional represents a condition whose current value is not yet
 * known. AND/OR short-circuit when a known value is sufficient to determine
 * the result.</p>
 */
public enum LogicOperator {
    AND {
        @Override
        public Optional<Boolean> combine(Optional<Boolean> first, Optional<Boolean> second) {
            Objects.requireNonNull(first, "first");
            Objects.requireNonNull(second, "second");

            if (first.isPresent() && !first.orElseThrow()) {
                return Optional.of(false);
            }
            if (second.isPresent() && !second.orElseThrow()) {
                return Optional.of(false);
            }
            if (first.isPresent() && second.isPresent()) {
                return Optional.of(true);
            }
            return Optional.empty();
        }
    },
    OR {
        @Override
        public Optional<Boolean> combine(Optional<Boolean> first, Optional<Boolean> second) {
            Objects.requireNonNull(first, "first");
            Objects.requireNonNull(second, "second");

            if (first.isPresent() && first.orElseThrow()) {
                return Optional.of(true);
            }
            if (second.isPresent() && second.orElseThrow()) {
                return Optional.of(true);
            }
            if (first.isPresent() && second.isPresent()) {
                return Optional.of(false);
            }
            return Optional.empty();
        }
    };

    public abstract Optional<Boolean> combine(
            Optional<Boolean> first,
            Optional<Boolean> second
    );
}
