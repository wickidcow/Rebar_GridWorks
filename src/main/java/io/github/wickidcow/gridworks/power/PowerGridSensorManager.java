package io.github.wickidcow.gridworks.power;

import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.content.block.PowerGridSensorBlock;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.scheduler.BukkitTask;

public final class PowerGridSensorManager implements AutoCloseable {
    private final GridWorks plugin;
    private final Set<PowerGridSensorBlock> sensors =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private final BukkitTask task;

    private boolean providerAvailable;

    public PowerGridSensorManager(GridWorks plugin, long intervalTicks) {
        this.plugin = plugin;
        this.providerAvailable = plugin.getPowerGridBridge().isAvailable();
        this.task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::sampleAll,
                intervalTicks,
                intervalTicks
        );
    }

    public void register(PowerGridSensorBlock sensor) {
        sensors.add(sensor);
        sample(sensor);
    }

    public void unregister(PowerGridSensorBlock sensor) {
        sensors.remove(sensor);
    }

    public int loadedSensorCount() {
        return sensors.size();
    }

    private void sampleAll() {
        boolean available = plugin.getPowerGridBridge().isAvailable();

        if (!available) {
            if (providerAvailable) {
                // Publish one transition to unavailable, then stop touching each
                // sensor until a provider returns.
                for (PowerGridSensorBlock sensor : List.copyOf(sensors)) {
                    sample(sensor);
                }
            }
            providerAvailable = false;
            return;
        }

        providerAvailable = true;
        for (PowerGridSensorBlock sensor : List.copyOf(sensors)) {
            sample(sensor);
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
        sensors.clear();
    }
}
