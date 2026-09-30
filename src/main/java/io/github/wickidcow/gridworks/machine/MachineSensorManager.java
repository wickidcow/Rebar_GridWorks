package io.github.wickidcow.gridworks.machine;

import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.content.block.MachineSensorBlock;
import io.github.wickidcow.gridworks.sampling.ScheduledSensorSampler;

public final class MachineSensorManager implements AutoCloseable {
    private final ScheduledSensorSampler<MachineSensorBlock> sampler;

    public MachineSensorManager(
            GridWorks plugin,
            long intervalTicks,
            int maxSamplesPerTick
    ) {
        sampler = new ScheduledSensorSampler<>(
                plugin,
                intervalTicks,
                maxSamplesPerTick,
                MachineSensorBlock::sampleNow,
                sensor -> "Machine Sensor " + sensor.getNodeId()
        );
    }

    public void register(MachineSensorBlock sensor) {
        sampler.register(sensor);
    }

    public void unregister(MachineSensorBlock sensor) {
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
