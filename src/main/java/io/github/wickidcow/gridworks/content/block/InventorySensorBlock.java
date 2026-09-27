package io.github.wickidcow.gridworks.content.block;

import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.api.control.ControlStateSource;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.api.control.GridWorksChannels;
import io.github.wickidcow.gridworks.inventory.InventoryProbe;
import io.github.wickidcow.gridworks.inventory.InventorySnapshot;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

public final class InventorySensorBlock extends PhysicalControlNodeBlock implements ControlStateSource {
    private InventorySnapshot lastSnapshot;

    public InventorySensorBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
        setFacing(context.getFacing());
    }

    public InventorySensorBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);
    }

    @Override
    protected void beforeActivated() {
        lastSnapshot = null;
    }

    @Override
    protected void afterActivated() {
        GridWorks.getInstance().getInventorySensorManager().register(this);
    }

    @Override
    protected void afterDeactivated() {
        GridWorks.getInstance().getInventorySensorManager().unregister(this);
    }

    @Override
    protected void afterRemoved() {
        GridWorks.getInstance().getInventorySensorManager().unregister(this);
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
        InventorySnapshot snapshot = readTarget();
        if (snapshot.equals(lastSnapshot)) {
            return;
        }

        lastSnapshot = snapshot;
        publish(snapshot);
    }

    public @NotNull String describeSnapshot() {
        InventorySnapshot snapshot = lastSnapshot;
        if (snapshot == null || !snapshot.available()) {
            return "no inventory target";
        }

        return snapshot.items()
                + " items, "
                + snapshot.occupiedSlots()
                + "/"
                + snapshot.totalSlots()
                + " slots occupied";
    }

    private InventorySnapshot readTarget() {
        BlockFace facing = getFacing();
        Block source = getBlock();
        int targetX = source.getX() + facing.getModX();
        int targetY = source.getY() + facing.getModY();
        int targetZ = source.getZ() + facing.getModZ();

        World world = source.getWorld();
        if (!world.isChunkLoaded(targetX >> 4, targetZ >> 4)) {
            return InventorySnapshot.unavailable();
        }

        return InventoryProbe.snapshot(world.getBlockAt(targetX, targetY, targetZ));
    }

    private BlockFace getFacing() {
        BlockData data = getBlock().getBlockData();
        if (!(data instanceof Directional directional)) {
            throw new IllegalStateException(
                    "Inventory Sensor block material no longer provides Directional block data: "
                            + data.getMaterial()
            );
        }
        return directional.getFacing();
    }

    private void setFacing(BlockFace facing) {
        BlockData data = getBlock().getBlockData();
        if (!(data instanceof Directional directional)) {
            throw new IllegalStateException(
                    "Inventory Sensor block material no longer provides Directional block data: "
                            + data.getMaterial()
            );
        }

        if (!directional.getFaces().contains(facing)) {
            throw new IllegalArgumentException(
                    "Inventory Sensor material cannot face " + facing
            );
        }

        directional.setFacing(facing);
        getBlock().setBlockData(directional);
    }

    private void publish(InventorySnapshot snapshot) {
        var bus = GridWorks.getInstance().getControlBus();

        bus.publish(
                getNodeId(),
                GridWorksChannels.INVENTORY_AVAILABLE,
                ControlValue.of(snapshot.available())
        );

        if (!snapshot.available()) {
            return;
        }

        bus.publish(
                getNodeId(),
                GridWorksChannels.INVENTORY_ITEMS,
                ControlValue.of((double) snapshot.items())
        );
        bus.publish(
                getNodeId(),
                GridWorksChannels.INVENTORY_OCCUPIED_SLOTS,
                ControlValue.of((double) snapshot.occupiedSlots())
        );
        bus.publish(
                getNodeId(),
                GridWorksChannels.INVENTORY_TOTAL_SLOTS,
                ControlValue.of((double) snapshot.totalSlots())
        );
        bus.publish(
                getNodeId(),
                GridWorksChannels.INVENTORY_OCCUPIED_RATIO,
                ControlValue.of(snapshot.occupiedRatio())
        );
    }
}
