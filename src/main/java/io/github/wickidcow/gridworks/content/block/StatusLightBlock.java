package io.github.wickidcow.gridworks.content.block;

import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.wickidcow.gridworks.api.control.ControlChannel;
import io.github.wickidcow.gridworks.api.control.ControlSignal;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.api.control.GridWorksChannels;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

public final class StatusLightBlock extends PhysicalControlNodeBlock {
    private boolean active;

    public StatusLightBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
    }

    public StatusLightBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);
    }

    @Override
    public boolean accepts(@NotNull ControlChannel channel) {
        return GridWorksChannels.REDSTONE_POWERED.equals(channel)
                || GridWorksChannels.CONTROL_ENABLED.equals(channel);
    }

    @Override
    protected void handleSignal(@NotNull ControlSignal signal) {
        if (signal.value() instanceof ControlValue.BooleanValue booleanValue) {
            setActive(booleanValue.value());
        }
    }

    public boolean isActive() {
        return active;
    }

    private void setActive(boolean newActive) {
        active = newActive;

        if (!(getBlock().getBlockData() instanceof Lightable lightable)) {
            return;
        }
        if (lightable.isLit() == newActive) {
            return;
        }

        lightable.setLit(newActive);
        getBlock().setBlockData(lightable, false);
    }
}
