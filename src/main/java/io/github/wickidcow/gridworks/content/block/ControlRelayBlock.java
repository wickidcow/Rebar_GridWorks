package io.github.wickidcow.gridworks.content.block;

import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.block.interfaces.InteractRebarBlockHandler;
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.api.control.BooleanInputConfigurable;
import io.github.wickidcow.gridworks.api.control.BooleanInputMode;
import io.github.wickidcow.gridworks.api.control.ControlChannel;
import io.github.wickidcow.gridworks.api.control.ControlSignal;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import java.util.Objects;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Switch;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public final class ControlRelayBlock extends PhysicalControlNodeBlock
        implements InteractRebarBlockHandler, BooleanInputConfigurable {

    private static final NamespacedKey POWERED_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:relay_powered")
    );
    private static final NamespacedKey INPUT_MODE_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:relay_input_mode")
    );

    private volatile boolean powered;
    private volatile BooleanInputMode inputMode;

    public ControlRelayBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
        this.powered = false;
        this.inputMode = BooleanInputMode.LEGACY;
    }

    public ControlRelayBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);

        Byte stored = pdc.get(POWERED_KEY, PersistentDataType.BYTE);
        this.powered = stored != null && stored != 0;
        this.inputMode = BooleanInputMode.fromStored(
                pdc.get(INPUT_MODE_KEY, PersistentDataType.STRING)
        );
    }

    @Override
    public boolean accepts(@NotNull ControlChannel channel) {
        return inputMode.accepts(channel);
    }

    @Override
    protected void afterActivated() {
        applyOutputState();
    }

    @Override
    protected void handleSignal(@NotNull ControlSignal signal) {
        if (signal.value() instanceof ControlValue.BooleanValue booleanValue) {
            setPowered(booleanValue.value());
        }
    }

    @Override
    protected void writeNodeData(@NotNull PersistentDataContainer pdc) {
        pdc.set(POWERED_KEY, PersistentDataType.BYTE, powered ? (byte) 1 : (byte) 0);
        pdc.set(INPUT_MODE_KEY, PersistentDataType.STRING, inputMode.name());
    }

    @Override
    @MultiHandler(priorities = EventPriority.LOWEST)
    public void onInteractedWith(@NotNull PlayerInteractEvent event, @NotNull EventPriority priority) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getHand() == EquipmentSlot.HAND) {
            event.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    @Override
    public @NotNull BooleanInputMode getBooleanInputMode() {
        return inputMode;
    }

    @Override
    public void setBooleanInputMode(@NotNull BooleanInputMode mode) {
        inputMode = Objects.requireNonNull(mode, "mode");
        powered = false;

        runOnServerThreadIfActive(() -> {
            applyOutputState();
            GridWorks.getInstance().getPhysicalControlNetwork().replayStateSources(getNodeId());
        });
    }

    public boolean isPowered() {
        return powered;
    }

    public void setPowered(boolean powered) {
        this.powered = powered;
        runOnServerThreadIfActive(this::applyOutputState);
    }

    private void applyOutputState() {
        BlockData blockData = getBlock().getBlockData();
        if (!(blockData instanceof Switch relaySwitch)) {
            throw new IllegalStateException(
                    "Control Relay block material no longer provides Switch block data: "
                            + blockData.getMaterial()
            );
        }

        if (relaySwitch.isPowered() == powered) {
            return;
        }

        relaySwitch.setPowered(powered);
        getBlock().setBlockData(relaySwitch);
    }
}
