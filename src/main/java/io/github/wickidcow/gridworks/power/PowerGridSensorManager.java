package io.github.wickidcow.gridworks.power;

import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.content.block.PowerGridSensorBlock;
import io.github.wickidcow.gridworks.sampling.RoundRobinSamplePlanner;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.scheduler.BukkitTask;

public final class PowerGridSensorManager implements AutoCloseable {
    private final GridWorks plugin;
    private final RoundRobinSamplePlanner<PowerGridSensorBlock> planner;
    private final BukkitTask task;

    private boolean providerAvailable;
    private int unavailableSweepRemaining;

    public PowerGridSensorManager(
            GridWorks plugin,
            long intervalTicks,
            int maxSamplesPerTick
    ) {
        this.plugin = plugin;
        this.planner = new RoundRobinSamplePlanner<>(
                intervalTicks,
                maxSamplesPerTick
        );
        this.providerAvailable = plugin.getPowerGridBridge().isAvailable();
        this.task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::sampleTick,
                1L,
                1L
        );
    }

    public void register(PowerGridSensorBlock sensor) {
        if (!planner.add(sensor)) {
            return;
        }

        if (!providerAvailable) {
            unavailableSweepRemaining++;
        }
    }

    public void unregister(PowerGridSensorBlock sensor) {
        if (!planner.remove(sensor)) {
            return;
        }

        if (planner.size() == 0) {
            unavailableSweepRemaining = 0;
        }
    }

    public int loadedSensorCount() {
        return planner.size();
    }

    public long estimatedSweepTicks() {
        return planner.estimatedSweepTicks();
    }

    public int maxSamplesPerTick() {
        return planner.maxSamplesPerTick();
    }

    public boolean isScheduled() {
        return !task.isCancelled();
    }

    private void sampleTick() {
        boolean available = plugin.getPowerGridBridge().isAvailable();

        if (available != providerAvailable) {
            providerAvailable = available;
            planner.requestFullSweep();

            if (available) {
                unavailableSweepRemaining = 0;
            } else {
                unavailableSweepRemaining = planner.size();
            }
        }

        if (!available && unavailableSweepRemaining <= 0) {
            return;
        }

        List<PowerGridSensorBlock> batch = planner.nextTickBatch();
        for (PowerGridSensorBlock sensor : batch) {
            sample(sensor);
        }

        if (!available) {
            unavailableSweepRemaining = Math.max(
                    0,
                    unavailableSweepRemaining - batch.size()
            );
        }
    }

    private void sample(PowerGridSensorBlock sensor) {
        try {
            sensor.sampleNow();
        } catch (RuntimeException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Power Grid Sensor "
                            + sensor.getNodeId()
                            + " failed to sample its target",
                    exception
            );
        }
    }

    @Override
    public void close() {
        task.cancel();
        planner.clear();
        unavailableSweepRemaining = 0;
    }
}
