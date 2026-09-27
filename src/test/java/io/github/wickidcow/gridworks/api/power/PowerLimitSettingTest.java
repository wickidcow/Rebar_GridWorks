package io.github.wickidcow.gridworks.api.power;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PowerLimitSettingTest {
    @Test
    void enabledUsesConfiguredLimit() {
        PowerLimitSetting setting = new PowerLimitSetting(true, 1500.0);

        assertEquals(1500.0, setting.effectiveLimitWatts(), 0.0);
    }

    @Test
    void disabledMeansProviderNeutralUnlimited() {
        PowerLimitSetting setting = new PowerLimitSetting(false, 1500.0);

        assertEquals(Double.MAX_VALUE, setting.effectiveLimitWatts(), 0.0);
    }

    @Test
    void immutableUpdatesPreserveOtherState() {
        PowerLimitSetting setting = new PowerLimitSetting(false, 1000.0)
                .withEnabled(true)
                .withLimitWatts(2500.0);

        assertEquals(new PowerLimitSetting(true, 2500.0), setting);
    }

    @Test
    void rejectsInvalidLimits() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PowerLimitSetting(true, 0.0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new PowerLimitSetting(true, Double.NaN)
        );
    }
}
