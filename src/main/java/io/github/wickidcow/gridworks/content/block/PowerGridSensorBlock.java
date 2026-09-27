package io.github.wickidcow.gridworks.content.block;

import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.api.control.ControlStateSource;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.api.control.GridWorksChannels;
import io.github.wickidcow.gridworks.api.power.PowerGridSnapshot;
import io.github.wickidcow.gridworks.power.PowerGridTelemetry;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

public final class PowerGridSensorBlock extends PhysicalControlNodeBlock
        implements ControlStateSource {

    private Optional<PowerGridSnapshot> lastSnapshot;
    private long sampleRevision;

    public PowerGridSensorBlock(
            @NotNull Block block,
            @NotNull BlockCreateContext context
    ) {
        super(block, context);
        setFacing(context.getFacing());
    }

    public PowerGridSensorBlock(
            @NotNull Block block,
            @NotNull PersistentDataContainer pdc
    ) {
        super(block, pdc);
    }

    @Override
    protected void afterActivated() {
        GridWorks.getInstance().getPowerGridSensorManager().register(this);
    }

    @Override
    protected void afterDeactivated() {
        GridWorks.getInstance().getPowerGridSensorManager().unregister(this);
    }

    @Override
    protected void afterRemoved() {
        GridWorks.getInstance().getPowerGridSensorManager().unregister(this);
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
        Optional<PowerGridSnapshot> snapshot = readTarget();
        if (Objects.equals(snapshot, lastSnapshot)) {
            return;
        }

        lastSnapshot = snapshot;
        sampleRevision = nextRevision(sampleRevision);
        publish(snapshot);
    }

    public @NotNull String describeSnapshot() {
        Optional<PowerGridSnapshot> snapshot = lastSnapshot;
        if (snapshot == null || snapshot.isEmpty()) {
            return GridWorks.getInstance().getPowerGridBridge().status();
        }

        PowerGridSnapshot value = snapshot.orElseThrow();
        return format(value.demandWatts())
                + " W demand / "
                + format(value.productionCapacityWatts())
                + " W capacity, "
                + format(value.reserveWatts())
                + " W reserve, "
                + value.unpoweredConsumerCount()
                + " unpowered";
    }

    private Optional<PowerGridSnapshot> readTarget() {
        var bridge = GridWorks.getInstance().getPowerGridBridge();
        if (!bridge.isAvailable()) {
            return Optional.empty();
        }

        BlockFace facing = getFacing();
        Block source = getBlock();
        int targetX = source.getX() + facing.getModX();
        int targetY = source.getY() + facing.getModY();
        int targetZ = source.getZ() + facing.getModZ();

        World world = source.getWorld();
        if (!world.isChunkLoaded(targetX >> 4, targetZ >> 4)) {
            return Optional.empty();
        }

        return bridge.snapshotFor(world.getBlockAt(targetX, targetY, targetZ));
    }

    private BlockFace getFacing() {
        BlockData data = getBlock().getBlockData();
        if (!(data instanceof Directional directional)) {
            throw new IllegalStateException(
                    "Power Grid Sensor material no longer provides Directional block data: "
                            + data.getMaterial()
            );
        }
        return directional.getFacing();
    }

    private void setFacing(BlockFace facing) {
        BlockData data = getBlock().getBlockData();
        if (!(data instanceof Directional directional)) {
            throw new IllegalStateException(
                    "Power Grid Sensor material no longer provides Directional block data: "
                            + data.getMaterial()
            );
        }

        if (!directional.getFaces().contains(facing)) {
            throw new IllegalArgumentException(
                    "Power Grid Sensor material cannot face " + facing
            );
        }

        directional.setFacing(facing);
        getBlock().setBlockData(directional);
    }

    private void publish(Optional<PowerGridSnapshot> snapshot) {
        var bus = GridWorks.getInstance().getControlBus();
        var values = snapshot
                .map(PowerGridTelemetry::fromSnapshot)
                .orElseGet(PowerGridTelemetry::unavailable);

        for (var entry : values.entrySet()) {
            bus.publish(getNodeId(), entry.getKey(), entry.getValue());
        }

        bus.publish(
                getNodeId(),
                GridWorksChannels.POWER_SAMPLE_REVISION,
                ControlValue.of((double) sampleRevision)
        );
    }

    private static long nextRevision(long current) {
        // Keep the numeric ControlValue exactly representable by IEEE-754.
        return current >= 9_007_199_254_740_991L ? 1L : current + 1L;
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
