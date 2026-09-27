package io.github.wickidcow.gridworks.machine;

import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.content.block.MachineSensorBlock;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.scheduler.BukkitTask;

public final class MachineSensorManager implements AutoCloseable {
    private final GridWorks plugin;
    private final Set<MachineSensorBlock> sensors =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private final BukkitTask task;

    public MachineSensorManager(GridWorks plugin, long intervalTicks) {
        this.plugin = plugin;
        this.task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::sampleAll,
                intervalTicks,
                intervalTicks
        );
    }

    public void register(MachineSensorBlock sensor) {
        sensors.add(sensor);
        sample(sensor);
    }

    public void unregister(MachineSensorBlock sensor) {
        sensors.remove(sensor);
    }

    public int loadedSensorCount() {
        return sensors.size();
    }

    private void sampleAll() {
        for (MachineSensorBlock sensor : List.copyOf(sensors)) {
            sample(sensor);
        }
    }

    private void sample(MachineSensorBlock sensor) {
        try {
            sensor.sampleNow();
        } catch (RuntimeException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Machine Sensor "
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
