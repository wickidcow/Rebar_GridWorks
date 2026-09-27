package io.github.wickidcow.gridworks.fluid;

import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.content.block.FluidSensorBlock;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.scheduler.BukkitTask;

public final class FluidSensorManager implements AutoCloseable {
    private final GridWorks plugin;
    private final Set<FluidSensorBlock> sensors =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private final BukkitTask task;

    public FluidSensorManager(GridWorks plugin, long intervalTicks) {
        this.plugin = plugin;
        this.task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::sampleAll,
                intervalTicks,
                intervalTicks
        );
    }

    public void register(FluidSensorBlock sensor) {
        sensors.add(sensor);
        sample(sensor);
    }

    public void unregister(FluidSensorBlock sensor) {
        sensors.remove(sensor);
    }

    public int loadedSensorCount() {
        return sensors.size();
    }

    public boolean isScheduled() {
        return !task.isCancelled();
    }

    private void sampleAll() {
        for (FluidSensorBlock sensor : List.copyOf(sensors)) {
            sample(sensor);
        }
    }

    private void sample(FluidSensorBlock sensor) {
        try {
            sensor.sampleNow();
        } catch (RuntimeException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Fluid Sensor " + sensor.getNodeId() + " failed to sample its target",
                    exception
            );
        }
    }

    @Override
    public void close() {
        task.cancel();
        sensors.clear();
    }
}
