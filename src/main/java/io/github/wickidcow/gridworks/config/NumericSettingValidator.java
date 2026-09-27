package io.github.wickidcow.gridworks.config;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Pure numeric configuration validation shared by Bukkit-backed settings.
 */
public final class NumericSettingValidator {
    private NumericSettingValidator() {
        throw new AssertionError("Utility class");
    }

    public static int positiveInt(
            Object rawValue,
            int defaultValue,
            String path
    ) {
        if (defaultValue <= 0) {
            throw new IllegalArgumentException("defaultValue must be positive");
        }
        if (rawValue == null) {
            return defaultValue;
        }

        BigDecimal value = decimal(rawValue, path);
        final int parsed;
        try {
            parsed = value.intValueExact();
        } catch (ArithmeticException exception) {
            throw invalid(path, rawValue, "must be a whole number within integer range");
        }

        if (parsed <= 0) {
            throw invalid(path, rawValue, "must be greater than zero");
        }
        return parsed;
    }

    public static long positiveLong(
            Object rawValue,
            long defaultValue,
            String path
    ) {
        if (defaultValue <= 0L) {
            throw new IllegalArgumentException("defaultValue must be positive");
        }
        if (rawValue == null) {
            return defaultValue;
        }

        BigDecimal value = decimal(rawValue, path);
        final long parsed;
        try {
            parsed = value.longValueExact();
        } catch (ArithmeticException exception) {
            throw invalid(path, rawValue, "must be a whole number within long range");
        }

        if (parsed <= 0L) {
            throw invalid(path, rawValue, "must be greater than zero");
        }
        return parsed;
    }

    private static BigDecimal decimal(Object rawValue, String path) {
        Objects.requireNonNull(path, "path");

        if (!(rawValue instanceof Number number)) {
            throw invalid(path, rawValue, "must be numeric");
        }

        try {
            return new BigDecimal(number.toString());
        } catch (NumberFormatException exception) {
            throw invalid(path, rawValue, "must be a finite numeric value");
        }
    }

    private static IllegalArgumentException invalid(
            String path,
            Object rawValue,
            String reason
    ) {
        return new IllegalArgumentException(
                "Invalid GridWorks config '" + path + "' value '"
                        + rawValue + "': " + reason
        );
    }
}
