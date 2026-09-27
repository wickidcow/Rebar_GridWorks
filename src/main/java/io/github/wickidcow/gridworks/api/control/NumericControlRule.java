package io.github.wickidcow.gridworks.api.control;

import java.util.Objects;
import java.util.Optional;

public record NumericControlRule(
        ControlChannel channel,
        ComparisonOperator operator,
        double threshold
) {
    public NumericControlRule {
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(operator, "operator");
        if (!Double.isFinite(threshold)) {
            throw new IllegalArgumentException("threshold must be finite");
        }
    }

    public Optional<Boolean> evaluate(ControlSignal signal) {
        Objects.requireNonNull(signal, "signal");

        if (!channel.equals(signal.channel())) {
            return Optional.empty();
        }
        if (!(signal.value() instanceof ControlValue.NumberValue numberValue)) {
            return Optional.empty();
        }

        return Optional.of(operator.test(numberValue.value(), threshold));
    }

    public boolean test(double observed) {
        if (!Double.isFinite(observed)) {
            throw new IllegalArgumentException("observed value must be finite");
        }
        return operator.test(observed, threshold);
    }

    public String describe() {
        return channel + " " + operator.symbol() + " " + threshold;
    }
}
