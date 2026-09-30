package io.github.wickidcow.gridworks.fluid;

import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.content.block.FluidSensorBlock;
import io.github.wickidcow.gridworks.sampling.ScheduledSensorSampler;

public final class FluidSensorManager implements AutoCloseable {
    private final ScheduledSensorSampler<FluidSensorBlock> sampler;

    public FluidSensorManager(
            GridWorks plugin,
            long intervalTicks,
            int maxSamplesPerTick
    ) {
        sampler = new ScheduledSensorSampler<>(
                plugin,
                intervalTicks,
                maxSamplesPerTick,
                FluidSensorBlock::sampleNow,
                sensor -> "Fluid Sensor " + sensor.getNodeId()
        );
    }

    public void register(FluidSensorBlock sensor) {
        sampler.register(sensor);
    }

    public void unregister(FluidSensorBlock sensor) {
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
