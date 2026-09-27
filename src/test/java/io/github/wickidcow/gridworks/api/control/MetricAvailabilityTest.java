package io.github.wickidcow.gridworks.api.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MetricAvailabilityTest {
    @Test
    void mapsInventoryMetricsToInventoryAvailability() {
        assertEquals(
                GridWorksChannels.INVENTORY_AVAILABLE,
                MetricAvailability.channelFor(GridWorksChannels.INVENTORY_ITEMS)
                        .orElseThrow()
        );
        assertEquals(
                GridWorksChannels.INVENTORY_AVAILABLE,
                MetricAvailability.channelFor(GridWorksChannels.INVENTORY_OCCUPIED_RATIO)
                        .orElseThrow()
        );
    }

    @Test
    void mapsFluidMachineAndPowerMetricsToTheirAvailabilityChannels() {
        assertEquals(
                GridWorksChannels.FLUID_AVAILABLE,
                MetricAvailability.channelFor(GridWorksChannels.FLUID_FILL_RATIO)
                        .orElseThrow()
        );
        assertEquals(
                GridWorksChannels.MACHINE_AVAILABLE,
                MetricAvailability.channelFor(GridWorksChannels.MACHINE_PROGRESS)
                        .orElseThrow()
        );
        assertEquals(
                GridWorksChannels.POWER_AVAILABLE,
                MetricAvailability.channelFor(GridWorksChannels.POWER_LOAD_RATIO)
                        .orElseThrow()
        );
    }

    @Test
    void eventStyleMetricsWithoutAvailabilityRemainUnmapped() {
        assertTrue(
                MetricAvailability.channelFor(GridWorksChannels.REDSTONE_STRENGTH)
                        .isEmpty()
        );
    }
}
