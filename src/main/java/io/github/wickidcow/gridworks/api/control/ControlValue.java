package io.github.wickidcow.gridworks.api.control;

import java.util.Objects;

/**
 * A value carried by a GridWorks control signal.
 *
 * <p>The built-in values cover the common automation cases. The interface is
 * intentionally open so addons can define domain-specific values when needed.</p>
 */
public interface ControlValue {

    record BooleanValue(boolean value) implements ControlValue {}

    record NumberValue(double value) implements ControlValue {
        public NumberValue {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Control numbers must be finite");
            }
        }
    }

    record TextValue(String value) implements ControlValue {
        public TextValue {
            Objects.requireNonNull(value, "value");
        }
    }

    static BooleanValue of(boolean value) {
        return new BooleanValue(value);
    }

    static NumberValue of(double value) {
        return new NumberValue(value);
    }

    static TextValue of(String value) {
        return new TextValue(value);
    }
}
