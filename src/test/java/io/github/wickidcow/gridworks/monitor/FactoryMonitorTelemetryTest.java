package io.github.wickidcow.gridworks.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.wickidcow.gridworks.api.control.ControlAddress;
import io.github.wickidcow.gridworks.api.control.ControlSignal;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.api.control.GridWorksChannels;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FactoryMonitorTelemetryTest {
    @Test
    void keepsSameChannelSignalsIndependentBySource() {
        FactoryMonitorTelemetry telemetry = new FactoryMonitorTelemetry();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        assertTrue(telemetry.observe(new ControlSignal(
                first,
                GridWorksChannels.INVENTORY_ITEMS,
                ControlValue.of(32.0),
                10
        )));
        assertTrue(telemetry.observe(new ControlSignal(
                second,
                GridWorksChannels.INVENTORY_ITEMS,
                ControlValue.of(64.0),
                20
        )));

        assertEquals(
                first,
                telemetry.latest(first, GridWorksChannels.INVENTORY_ITEMS)
                        .orElseThrow()
                        .source()
        );
        assertEquals(
                second,
                telemetry.latest(second, GridWorksChannels.INVENTORY_ITEMS)
                        .orElseThrow()
                        .source()
        );
        assertEquals(
                second,
                telemetry.latest(GridWorksChannels.INVENTORY_ITEMS)
                        .orElseThrow()
                        .source()
        );
        assertEquals(Set.of(first, second), telemetry.sourceIds());
        assertEquals(2, telemetry.signalCount());
    }

    @Test
    void rejectsOlderAsynchronousUpdateForSameSourceAndChannel() {
        FactoryMonitorTelemetry telemetry = new FactoryMonitorTelemetry();
        UUID source = UUID.randomUUID();

        assertTrue(telemetry.observe(new ControlSignal(
                source,
                GridWorksChannels.MACHINE_PROCESSING,
                ControlValue.of(true),
                40
        )));
        assertFalse(telemetry.observe(new ControlSignal(
                source,
                GridWorksChannels.MACHINE_PROCESSING,
                ControlValue.of(false),
                30
        )));

        ControlSignal latest = telemetry
                .latest(source, GridWorksChannels.MACHINE_PROCESSING)
                .orElseThrow();
        assertEquals(40, latest.sequence());
        assertEquals(ControlValue.of(true), latest.value());
    }

    @Test
    void addressedCommandsRemainBoundedPerSource() {
        FactoryMonitorTelemetry telemetry = new FactoryMonitorTelemetry();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        assertTrue(telemetry.observe(new ControlSignal(
                first,
                ControlAddress.fromUserInput("ore_line").channel(),
                ControlValue.of(true),
                5
        )));
        assertTrue(telemetry.observe(new ControlSignal(
                first,
                ControlAddress.fromUserInput("smelter").channel(),
                ControlValue.of(false),
                6
        )));
        assertTrue(telemetry.observe(new ControlSignal(
                second,
                ControlAddress.fromUserInput("farm").channel(),
                ControlValue.of(true),
                7
        )));

        assertEquals(2, telemetry.signalCount());
        assertEquals(
                "smelter",
                ControlAddress.fromChannel(
                        telemetry.latestAddressed(first).orElseThrow().channel()
                ).value()
        );
        assertEquals(second, telemetry.latestAddressed().orElseThrow().source());
    }

    @Test
    void clearDropsAllTelemetry() {
        FactoryMonitorTelemetry telemetry = new FactoryMonitorTelemetry();
        UUID source = UUID.randomUUID();

        telemetry.observe(new ControlSignal(
                source,
                GridWorksChannels.REDSTONE_POWERED,
                ControlValue.of(true),
                1
        ));
        telemetry.observe(new ControlSignal(
                source,
                ControlAddress.fromUserInput("line").channel(),
                ControlValue.of(false),
                2
        ));

        telemetry.clear();

        assertTrue(telemetry.sourceIds().isEmpty());
        assertEquals(0, telemetry.signalCount());
        assertTrue(telemetry.latest(source, GridWorksChannels.REDSTONE_POWERED).isEmpty());
        assertTrue(telemetry.latestAddressed(source).isEmpty());
    }

    @Test
    void retainSourcesDropsDisconnectedTelemetry() {
        FactoryMonitorTelemetry telemetry = new FactoryMonitorTelemetry();
        UUID keep = UUID.randomUUID();
        UUID remove = UUID.randomUUID();

        telemetry.observe(new ControlSignal(
                keep,
                GridWorksChannels.REDSTONE_POWERED,
                ControlValue.of(true),
                1
        ));
        telemetry.observe(new ControlSignal(
                remove,
                GridWorksChannels.REDSTONE_POWERED,
                ControlValue.of(false),
                2
        ));

        telemetry.retainSources(Set.of(keep));

        assertEquals(Set.of(keep), telemetry.sourceIds());
        assertEquals(1, telemetry.signalCount());
        assertTrue(telemetry.latest(keep, GridWorksChannels.REDSTONE_POWERED).isPresent());
        assertTrue(telemetry.latest(remove, GridWorksChannels.REDSTONE_POWERED).isEmpty());
    }
}
