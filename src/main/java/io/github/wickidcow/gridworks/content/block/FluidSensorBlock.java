package io.github.wickidcow.gridworks.content.block;

import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.api.control.ControlStateSource;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.api.control.GridWorksChannels;
import io.github.wickidcow.gridworks.fluid.FluidProbe;
import io.github.wickidcow.gridworks.fluid.FluidSnapshot;
import java.util.Locale;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

public final class FluidSensorBlock extends PhysicalControlNodeBlock implements ControlStateSource {
    private FluidSnapshot lastSnapshot;

    public FluidSensorBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
        setFacing(context.getFacing());
    }

    public FluidSensorBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);
    }

    @Override
    protected void beforeActivated() {
        lastSnapshot = null;
    }

    @Override
    protected void afterActivated() {
        GridWorks.getInstance().getFluidSensorManager().register(this);
    }

    @Override
    protected void afterDeactivated() {
        GridWorks.getInstance().getFluidSensorManager().unregister(this);
    }

    @Override
    protected void afterRemoved() {
        GridWorks.getInstance().getFluidSensorManager().unregister(this);
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
        FluidSnapshot snapshot = readTarget();
        if (snapshot.equals(lastSnapshot)) {
            return;
        }

        lastSnapshot = snapshot;
        publish(snapshot);
    }

    public @NotNull String describeSnapshot() {
        FluidSnapshot snapshot = lastSnapshot;
        if (snapshot == null || !snapshot.available()) {
            return "no Rebar fluid tank target";
        }

        String fluid = snapshot.fluidKey().isEmpty() ? "empty" : snapshot.fluidKey();
        return fluid + ", "
                + format(snapshot.amount())
                + " / "
                + format(snapshot.capacity())
                + " mB ("
                + format(snapshot.fillRatio() * 100.0)
                + "%)";
    }

    private FluidSnapshot readTarget() {
        BlockFace facing = getFacing();
        Block source = getBlock();
        int targetX = source.getX() + facing.getModX();
        int targetY = source.getY() + facing.getModY();
        int targetZ = source.getZ() + facing.getModZ();

        World world = source.getWorld();
        if (!world.isChunkLoaded(targetX >> 4, targetZ >> 4)) {
            return FluidSnapshot.unavailable();
        }

        return FluidProbe.snapshot(world.getBlockAt(targetX, targetY, targetZ));
    }

    private BlockFace getFacing() {
        BlockData data = getBlock().getBlockData();
        if (!(data instanceof Directional directional)) {
            throw new IllegalStateException(
                    "Fluid Sensor block material no longer provides Directional block data: "
                            + data.getMaterial()
            );
        }
        return directional.getFacing();
    }

    private void setFacing(BlockFace facing) {
        BlockData data = getBlock().getBlockData();
        if (!(data instanceof Directional directional)) {
            throw new IllegalStateException(
                    "Fluid Sensor block material no longer provides Directional block data: "
                            + data.getMaterial()
            );
        }

        if (!directional.getFaces().contains(facing)) {
            throw new IllegalArgumentException(
                    "Fluid Sensor material cannot face " + facing
            );
        }

        directional.setFacing(facing);
        getBlock().setBlockData(directional);
    }

    private void publish(FluidSnapshot snapshot) {
        var bus = GridWorks.getInstance().getControlBus();

        bus.publish(
                getNodeId(),
                GridWorksChannels.FLUID_AVAILABLE,
                ControlValue.of(snapshot.available())
        );

        if (!snapshot.available()) {
            return;
        }

        bus.publish(
                getNodeId(),
                GridWorksChannels.FLUID_PRESENT,
                ControlValue.of(snapshot.hasFluid())
        );
        bus.publish(
                getNodeId(),
                GridWorksChannels.FLUID_TYPE,
                ControlValue.of(snapshot.fluidKey())
        );
        bus.publish(
                getNodeId(),
                GridWorksChannels.FLUID_AMOUNT,
                ControlValue.of(snapshot.amount())
        );
        bus.publish(
                getNodeId(),
                GridWorksChannels.FLUID_CAPACITY,
                ControlValue.of(snapshot.capacity())
        );
        bus.publish(
                getNodeId(),
                GridWorksChannels.FLUID_FILL_RATIO,
                ControlValue.of(snapshot.fillRatio())
        );
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
