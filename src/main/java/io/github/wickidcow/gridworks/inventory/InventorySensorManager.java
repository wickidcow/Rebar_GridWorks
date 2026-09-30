package io.github.wickidcow.gridworks.inventory;

import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.content.block.InventorySensorBlock;
import io.github.wickidcow.gridworks.sampling.ScheduledSensorSampler;

public final class InventorySensorManager implements AutoCloseable {
    private final ScheduledSensorSampler<InventorySensorBlock> sampler;

    public InventorySensorManager(
            GridWorks plugin,
            long intervalTicks,
            int maxSamplesPerTick
    ) {
        sampler = new ScheduledSensorSampler<>(
                plugin,
                intervalTicks,
                maxSamplesPerTick,
                InventorySensorBlock::sampleNow,
                sensor -> "Inventory Sensor " + sensor.getNodeId()
        );
    }

    public void register(InventorySensorBlock sensor) {
        sampler.register(sensor);
    }

    public void unregister(InventorySensorBlock sensor) {
        sampler.unregister(sensor);
    }

    public int loadedSensorCount() {
        return sampler.loadedCount();
    }

    public long estimatedSweepTicks() {
        return sampler.estimatedSweepTicks();
    }

    public int maxSamplesPerTick() {
        return sampler.maxSamplesPerTick();
    }

    public boolean isScheduled() {
        return sampler.isScheduled();
    }

    @Override
    public void close() {
        sampler.close();
    }
}
