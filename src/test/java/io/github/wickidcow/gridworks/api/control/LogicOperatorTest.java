package io.github.wickidcow.gridworks.api.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class LogicOperatorTest {
    @Test
    void andUsesThreeStateShortCircuiting() {
        assertEquals(
                Optional.of(false),
                LogicOperator.AND.combine(Optional.of(false), Optional.empty())
        );
        assertEquals(
                Optional.of(false),
                LogicOperator.AND.combine(Optional.empty(), Optional.of(false))
        );
        assertEquals(
                Optional.of(true),
                LogicOperator.AND.combine(Optional.of(true), Optional.of(true))
        );
        assertTrue(
                LogicOperator.AND.combine(Optional.of(true), Optional.empty()).isEmpty()
        );
    }

    @Test
    void orUsesThreeStateShortCircuiting() {
        assertEquals(
                Optional.of(true),
                LogicOperator.OR.combine(Optional.of(true), Optional.empty())
        );
        assertEquals(
                Optional.of(true),
                LogicOperator.OR.combine(Optional.empty(), Optional.of(true))
        );
        assertEquals(
                Optional.of(false),
                LogicOperator.OR.combine(Optional.of(false), Optional.of(false))
        );
        assertTrue(
                LogicOperator.OR.combine(Optional.of(false), Optional.empty()).isEmpty()
        );
    }
}
