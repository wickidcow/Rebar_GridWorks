package io.github.wickidcow.gridworks.content.block;

import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.wickidcow.gridworks.api.control.ControlChannel;
import io.github.wickidcow.gridworks.api.control.ControlSignal;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.api.control.GridWorksChannels;
import java.util.Objects;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public final class StatusLightBlock extends PhysicalControlNodeBlock {
    private static final NamespacedKey LIT_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:status_light_lit")
    );

    private boolean lit;

    public StatusLightBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
        this.lit = false;
    }

    public StatusLightBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);
        Byte stored = pdc.get(LIT_KEY, PersistentDataType.BYTE);
        this.lit = stored != null && stored != 0;
    }

    @Override
    public boolean accepts(@NotNull ControlChannel channel) {
        return GridWorksChannels.REDSTONE_POWERED.equals(channel);
    }

    @Override
    protected void afterActivated() {
        applyVisualState();
    }

    @Override
    protected void handleSignal(@NotNull ControlSignal signal) {
        if (!(signal.value() instanceof ControlValue.BooleanValue booleanValue)) {
            return;
        }

        setLit(booleanValue.value());
    }

    @Override
    protected void writeNodeData(@NotNull PersistentDataContainer pdc) {
        pdc.set(LIT_KEY, PersistentDataType.BYTE, lit ? (byte) 1 : (byte) 0);
    }

    public boolean isLit() {
        return lit;
    }

    public void setLit(boolean lit) {
        if (this.lit == lit) {
            applyVisualState();
            return;
        }

        this.lit = lit;
        applyVisualState();
    }

    public void applyVisualState() {
        BlockData data = getBlock().getBlockData();
        if (!(data instanceof Lightable lightable)) {
            throw new IllegalStateException(
                    "Status Light block material no longer provides Lightable block data: " + data.getMaterial()
            );
        }

        if (lightable.isLit() == lit) {
            return;
        }

        lightable.setLit(lit);
        getBlock().setBlockData(lightable);
    }
}
