package io.github.wickidcow.gridworks.inventory;

import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.content.block.InventorySensorBlock;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.scheduler.BukkitTask;

public final class InventorySensorManager implements AutoCloseable {
    private final GridWorks plugin;
    private final Set<InventorySensorBlock> sensors =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private final BukkitTask task;

    public InventorySensorManager(GridWorks plugin, long intervalTicks) {
        this.plugin = plugin;
        this.task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::sampleAll,
                intervalTicks,
                intervalTicks
        );
    }

    public void register(InventorySensorBlock sensor) {
        sensors.add(sensor);
        sample(sensor);
    }

    public void unregister(InventorySensorBlock sensor) {
        sensors.remove(sensor);
    }

    public int loadedSensorCount() {
        return sensors.size();
    }

    private void sampleAll() {
        for (InventorySensorBlock sensor : List.copyOf(sensors)) {
            sample(sensor);
        }
    }

    private void sample(InventorySensorBlock sensor) {
        try {
            sensor.sampleNow();
        } catch (RuntimeException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Inventory Sensor " + sensor.getNodeId() + " failed to sample its target",
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
