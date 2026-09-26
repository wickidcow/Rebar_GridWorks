package io.github.wickidcow.gridworks.content.block;

import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.block.context.BlockBreakContext;
import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.block.interfaces.BlockBreakRebarBlockHandler;
import io.github.pylonmc.rebar.block.interfaces.UnloadRebarBlockHandler;
import io.github.pylonmc.rebar.event.RebarBlockUnloadEvent;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.api.control.ControlNode;
import io.github.wickidcow.gridworks.api.control.ControlSignal;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.event.EventPriority;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public final class ControlInterfaceBlock extends RebarBlock
        implements ControlNode, UnloadRebarBlockHandler, BlockBreakRebarBlockHandler {

    private static final NamespacedKey NODE_ID_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:node_id")
    );

    private final UUID nodeId;
    private volatile ControlSignal lastSignal;

    public ControlInterfaceBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
        this.nodeId = UUID.randomUUID();
    }

    public ControlInterfaceBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);
        this.nodeId = loadNodeId(pdc);
    }

    private static UUID loadNodeId(PersistentDataContainer pdc) {
        String stored = pdc.get(NODE_ID_KEY, PersistentDataType.STRING);
        if (stored == null) {
            UUID migrated = UUID.randomUUID();
            GridWorks.getInstance().getLogger().warning(
                    "Control Interface was missing a node id; generated " + migrated
            );
            return migrated;
        }

        try {
            return UUID.fromString(stored);
        } catch (IllegalArgumentException exception) {
            UUID migrated = UUID.randomUUID();
            GridWorks.getInstance().getLogger().warning(
                    "Control Interface had an invalid node id '" + stored + "'; generated " + migrated
            );
            return migrated;
        }
    }

    @Override
    public void postInitialise() {
        super.postInitialise();
        GridWorks.getInstance().getPhysicalControlNetwork().activate(this);
    }

    @Override
    public void write(@NotNull PersistentDataContainer pdc) {
        pdc.set(NODE_ID_KEY, PersistentDataType.STRING, nodeId.toString());
    }

    @Override
    public @NotNull UUID id() {
        return nodeId;
    }

    public @NotNull UUID getNodeId() {
        return nodeId;
    }

    public Optional<ControlSignal> getLastSignal() {
        return Optional.ofNullable(lastSignal);
    }

    @Override
    public void onSignal(@NotNull ControlSignal signal) {
        lastSignal = signal;
    }

    @Override
    public void onUnload(@NotNull RebarBlockUnloadEvent event, @NotNull EventPriority priority) {
        GridWorks.getInstance().getPhysicalControlNetwork().deactivate(nodeId, this);
    }

    @Override
    public void onPostBlockBreak(@NotNull BlockBreakContext context) {
        try {
            GridWorks.getInstance().getPhysicalControlNetwork().remove(nodeId, this);
        } catch (IOException exception) {
            GridWorks.getInstance().getLogger().severe(
                    "Failed to persist removal of Control Interface " + nodeId + ": " + exception.getMessage()
            );
        }
    }
}
