package io.github.wickidcow.gridworks.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class NumericSettingValidatorTest {
    @Test
    void missingValuesUseBackwardCompatibleDefaults() {
        assertEquals(
                4096,
                NumericSettingValidator.positiveInt(
                        null,
                        4096,
                        "control-bus.max-propagation-nodes"
                )
        );
        assertEquals(
                20L,
                NumericSettingValidator.positiveLong(
                        null,
                        20L,
                        "sensors.power.sample-interval-ticks"
                )
        );
    }

    @Test
    void acceptsPositiveWholeNumbersAcrossNumberTypes() {
        assertEquals(
                5,
                NumericSettingValidator.positiveInt(
                        5L,
                        1,
                        "test.int"
                )
        );
        assertEquals(
                25L,
                NumericSettingValidator.positiveLong(
                        new BigInteger("25"),
                        1L,
                        "test.long"
                )
        );
        assertEquals(
                10,
                NumericSettingValidator.positiveInt(
                        new BigDecimal("10.0"),
                        1,
                        "test.decimal"
                )
        );
    }

    @Test
    void rejectsZeroNegativeFractionalAndNonNumericValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> NumericSettingValidator.positiveInt(0, 1, "zero")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> NumericSettingValidator.positiveLong(-1L, 1L, "negative")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> NumericSettingValidator.positiveInt(1.5, 1, "fractional")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> NumericSettingValidator.positiveLong("20", 1L, "string")
        );
    }

    @Test
    void rejectsOverflowAndNonFiniteNumbersWithKeyInMessage() {
        IllegalArgumentException overflow = assertThrows(
                IllegalArgumentException.class,
                () -> NumericSettingValidator.positiveInt(
                        Long.MAX_VALUE,
                        1,
                        "cargo.isolator.transfer-rate"
                )
        );
        assertTrue(
                overflow.getMessage().contains("cargo.isolator.transfer-rate")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> NumericSettingValidator.positiveLong(
                        Double.NaN,
                        1L,
                        "nan"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> NumericSettingValidator.positiveLong(
                        Double.POSITIVE_INFINITY,
                        1L,
                        "infinity"
                )
        );
    }
}
