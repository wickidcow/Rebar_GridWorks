package io.github.wickidcow.gridworks.content.block;

import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.api.control.GridWorksChannels;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

public final class RedstoneSensorBlock extends PhysicalControlNodeBlock {
    private int lastPower = -1;

    public RedstoneSensorBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
    }

    public RedstoneSensorBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);
    }

    @Override
    protected void afterActivated() {
        updatePower(getBlock().getBlockPower());
    }

    public void updatePower(int newPower) {
        int power = Math.clamp(newPower, 0, 15);
        if (power == lastPower) {
            return;
        }

        lastPower = power;
        GridWorks.getInstance().getControlBus().publish(
                getNodeId(),
                GridWorksChannels.REDSTONE_STRENGTH,
                ControlValue.of((double) power)
        );
        GridWorks.getInstance().getControlBus().publish(
                getNodeId(),
                GridWorksChannels.REDSTONE_POWERED,
                ControlValue.of(power > 0)
        );
    }

    public int getLastPower() {
        return lastPower;
    }
}
