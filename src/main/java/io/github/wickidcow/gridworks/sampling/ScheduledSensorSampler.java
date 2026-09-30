package io.github.wickidcow.gridworks.sampling;

import io.github.wickidcow.gridworks.GridWorks;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import org.bukkit.scheduler.BukkitTask;

/**
 * One bounded scheduler for a homogeneous sensor population.
 */
public final class ScheduledSensorSampler<T> implements AutoCloseable {
    private final GridWorks plugin;
    private final RoundRobinSamplePlanner<T> planner;
    private final Consumer<T> sampleAction;
    private final Function<T, String> description;
    private final BukkitTask task;

    public ScheduledSensorSampler(
            GridWorks plugin,
            long targetIntervalTicks,
            int maxSamplesPerTick,
            Consumer<T> sampleAction,
            Function<T, String> description
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.planner = new RoundRobinSamplePlanner<>(
                targetIntervalTicks,
                maxSamplesPerTick
        );
        this.sampleAction = Objects.requireNonNull(sampleAction, "sampleAction");
        this.description = Objects.requireNonNull(description, "description");

        this.task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::sampleTick,
                1L,
                1L
        );
    }

    public void register(T sensor) {
        planner.add(sensor);
    }

    public void unregister(T sensor) {
        planner.remove(sensor);
    }

    public int loadedCount() {
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
        List<T> batch = planner.nextTickBatch();
        for (T sensor : batch) {
            try {
                sampleAction.accept(sensor);
            } catch (RuntimeException exception) {
                plugin.getLogger().log(
                        Level.SEVERE,
                        description.apply(sensor) + " failed to sample its target",
                        exception
                );
            }
        }
    }

    @Override
    public void close() {
        task.cancel();
        planner.clear();
    }
}
