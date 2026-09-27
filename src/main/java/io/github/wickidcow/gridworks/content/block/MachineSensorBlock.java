package io.github.wickidcow.gridworks.content.block;

import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.api.control.ControlStateSource;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.api.control.GridWorksChannels;
import io.github.wickidcow.gridworks.machine.MachineProbe;
import io.github.wickidcow.gridworks.machine.MachineSnapshot;
import io.github.wickidcow.gridworks.machine.ObservedMachineCycleCounter;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public final class MachineSensorBlock extends PhysicalControlNodeBlock
        implements ControlStateSource {
    private static final NamespacedKey OBSERVED_CYCLES_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:machine_sensor_observed_cycles")
    );
    private static final NamespacedKey LAST_CYCLE_EPOCH_MS_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:machine_sensor_last_cycle_epoch_ms")
    );

    private final ObservedMachineCycleCounter cycleCounter;
    private MachineSnapshot lastSnapshot;

    public MachineSensorBlock(
            @NotNull Block block,
            @NotNull BlockCreateContext context
    ) {
        super(block, context);
        this.cycleCounter = new ObservedMachineCycleCounter();
        setFacing(context.getFacing());
    }

    public MachineSensorBlock(
            @NotNull Block block,
            @NotNull PersistentDataContainer pdc
    ) {
        super(block, pdc);
        Long storedCycles = pdc.get(OBSERVED_CYCLES_KEY, PersistentDataType.LONG);
        Long storedLastCycle = pdc.get(LAST_CYCLE_EPOCH_MS_KEY, PersistentDataType.LONG);
        this.cycleCounter = new ObservedMachineCycleCounter(
                storedCycles == null ? 0L : storedCycles,
                storedLastCycle == null ? 0L : storedLastCycle
        );
    }

    @Override
    protected void afterActivated() {
        GridWorks.getInstance().getMachineSensorManager().register(this);
    }

    @Override
    protected void afterDeactivated() {
        GridWorks.getInstance().getMachineSensorManager().unregister(this);
    }

    @Override
    protected void afterRemoved() {
        GridWorks.getInstance().getMachineSensorManager().unregister(this);
    }

    @Override
    protected void writeNodeData(@NotNull PersistentDataContainer pdc) {
        pdc.set(OBSERVED_CYCLES_KEY, PersistentDataType.LONG, cycleCounter.observedCycles());
        pdc.set(LAST_CYCLE_EPOCH_MS_KEY, PersistentDataType.LONG, cycleCounter.lastCycleEpochMillis());
    }

    @Override
    public void publishCurrentState() {
        if (lastSnapshot == null) {
            sampleNow();
        } else {
            publish(lastSnapshot);
        }
    }

    public void sampleNow() {
        MachineSnapshot snapshot = readTarget();
        boolean completedObservedCycle = cycleCounter.observe(
                snapshot,
                System.currentTimeMillis()
        );

        if (snapshot.equals(lastSnapshot) && !completedObservedCycle) {
            return;
        }

        lastSnapshot = snapshot;
        publish(snapshot);
    }

    public @NotNull String describeSnapshot() {
        MachineSnapshot snapshot = lastSnapshot;
        long observedCycles = cycleCounter.observedCycles();
        if (snapshot == null || !snapshot.available()) {
            return "no supported Rebar processor target, "
                    + observedCycles
                    + " observed cycle(s)";
        }
        if (!snapshot.processing()) {
            return snapshot.kind()
                    + ", idle, "
                    + observedCycles
                    + " observed cycle(s)";
        }

        return snapshot.kind()
                + ", processing "
                + String.format(Locale.ROOT, "%.0f%%", snapshot.progress() * 100.0)
                + ", "
                + snapshot.ticksRemaining()
                + " ticks remaining, "
                + observedCycles
                + " observed cycle(s)";
    }

    private MachineSnapshot readTarget() {
        BlockFace facing = getFacing();
        Block source = getBlock();
        int targetX = source.getX() + facing.getModX();
        int targetY = source.getY() + facing.getModY();
        int targetZ = source.getZ() + facing.getModZ();

        World world = source.getWorld();
        if (!world.isChunkLoaded(targetX >> 4, targetZ >> 4)) {
            return MachineSnapshot.unavailable();
        }

        return MachineProbe.snapshot(world.getBlockAt(targetX, targetY, targetZ));
    }

    private BlockFace getFacing() {
        BlockData data = getBlock().getBlockData();
        if (!(data instanceof Directional directional)) {
            throw new IllegalStateException(
                    "Machine Sensor material no longer provides Directional block data: "
                            + data.getMaterial()
            );
        }
        return directional.getFacing();
    }

    private void setFacing(BlockFace facing) {
        BlockData data = getBlock().getBlockData();
        if (!(data instanceof Directional directional)) {
            throw new IllegalStateException(
                    "Machine Sensor material no longer provides Directional block data: "
                            + data.getMaterial()
            );
        }

        if (!directional.getFaces().contains(facing)) {
            throw new IllegalArgumentException(
                    "Machine Sensor material cannot face " + facing
            );
        }

        directional.setFacing(facing);
        getBlock().setBlockData(directional);
    }

    private void publish(MachineSnapshot snapshot) {
        var bus = GridWorks.getInstance().getControlBus();

        bus.publish(
                getNodeId(),
                GridWorksChannels.MACHINE_AVAILABLE,
                ControlValue.of(snapshot.available())
        );

        if (snapshot.available()) {
            bus.publish(getNodeId(), GridWorksChannels.MACHINE_KIND, ControlValue.of(snapshot.kind()));
            bus.publish(getNodeId(), GridWorksChannels.MACHINE_PROCESSING, ControlValue.of(snapshot.processing()));
            bus.publish(getNodeId(), GridWorksChannels.MACHINE_PROGRESS, ControlValue.of(snapshot.progress()));
            bus.publish(getNodeId(), GridWorksChannels.MACHINE_PROCESS_TIME_TICKS, ControlValue.of((double) snapshot.processTimeTicks()));
            bus.publish(getNodeId(), GridWorksChannels.MACHINE_TICKS_REMAINING, ControlValue.of((double) snapshot.ticksRemaining()));
        }

        bus.publish(
                getNodeId(),
                GridWorksChannels.MACHINE_OBSERVED_CYCLES,
                ControlValue.of((double) cycleCounter.observedCycles())
        );
        bus.publish(
                getNodeId(),
                GridWorksChannels.MACHINE_LAST_CYCLE_EPOCH_MS,
                ControlValue.of((double) cycleCounter.lastCycleEpochMillis())
        );
    }
}
