package io.github.wickidcow.gridworks.api.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class NumericControlRuleTest {
    private static final ControlChannel CHANNEL =
            ControlChannel.of("gridworks", "fluid/fill_ratio");

    @Test
    void evaluatesMatchingNumericSignals() {
        NumericControlRule rule = new NumericControlRule(
                CHANNEL,
                ComparisonOperator.LESS_OR_EQUAL,
                0.25
        );

        ControlSignal low = new ControlSignal(
                UUID.randomUUID(),
                CHANNEL,
                ControlValue.of(0.20),
                1
        );
        ControlSignal high = new ControlSignal(
                UUID.randomUUID(),
                CHANNEL,
                ControlValue.of(0.50),
                2
        );

        assertEquals(true, rule.evaluate(low).orElseThrow());
        assertEquals(false, rule.evaluate(high).orElseThrow());
    }

    @Test
    void ignoresWrongChannelAndWrongValueType() {
        NumericControlRule rule = new NumericControlRule(
                CHANNEL,
                ComparisonOperator.GREATER_OR_EQUAL,
                0.75
        );

        ControlSignal wrongChannel = new ControlSignal(
                UUID.randomUUID(),
                ControlChannel.of("gridworks", "inventory/occupied_ratio"),
                ControlValue.of(0.9),
                1
        );
        ControlSignal wrongType = new ControlSignal(
                UUID.randomUUID(),
                CHANNEL,
                ControlValue.of(true),
                2
        );

        assertTrue(rule.evaluate(wrongChannel).isEmpty());
        assertTrue(rule.evaluate(wrongType).isEmpty());
    }

    @Test
    void supportsAllComparisonDirections() {
        assertTrue(ComparisonOperator.GREATER_THAN.test(2.0, 1.0));
        assertTrue(ComparisonOperator.GREATER_OR_EQUAL.test(1.0, 1.0));
        assertTrue(ComparisonOperator.LESS_THAN.test(1.0, 2.0));
        assertTrue(ComparisonOperator.LESS_OR_EQUAL.test(1.0, 1.0));
        assertTrue(ComparisonOperator.EQUAL.test(1.0, 1.0));
        assertTrue(ComparisonOperator.NOT_EQUAL.test(1.0, 2.0));

        assertFalse(ComparisonOperator.GREATER_THAN.test(1.0, 1.0));
        assertFalse(ComparisonOperator.LESS_THAN.test(1.0, 1.0));
    }

    @Test
    void rejectsNonFiniteThresholdsAndObservations() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new NumericControlRule(CHANNEL, ComparisonOperator.EQUAL, Double.NaN)
        );

        NumericControlRule rule = new NumericControlRule(
                CHANNEL,
                ComparisonOperator.EQUAL,
                1.0
        );
        assertThrows(IllegalArgumentException.class, () -> rule.test(Double.POSITIVE_INFINITY));
    }
}
