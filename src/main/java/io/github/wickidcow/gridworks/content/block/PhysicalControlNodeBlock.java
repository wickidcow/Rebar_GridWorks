package io.github.wickidcow.gridworks.content.block;

import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.block.context.BlockBreakContext;
import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.block.interfaces.BlockBreakRebarBlockHandler;
import io.github.pylonmc.rebar.block.interfaces.UnloadRebarBlockHandler;
import io.github.pylonmc.rebar.event.RebarBlockUnloadEvent;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.api.control.ControlSignal;
import io.github.wickidcow.gridworks.physical.PhysicalControlEndpoint;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.event.EventPriority;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public abstract class PhysicalControlNodeBlock extends RebarBlock
        implements PhysicalControlEndpoint, UnloadRebarBlockHandler, BlockBreakRebarBlockHandler {

    private static final NamespacedKey NODE_ID_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:node_id")
    );

    private final UUID nodeId;
    private volatile ControlSignal lastSignal;

    protected PhysicalControlNodeBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
        this.nodeId = UUID.randomUUID();
    }

    protected PhysicalControlNodeBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);
        this.nodeId = loadNodeId(pdc);
    }

    private static UUID loadNodeId(PersistentDataContainer pdc) {
        String stored = pdc.get(NODE_ID_KEY, PersistentDataType.STRING);
        if (stored == null) {
            UUID migrated = UUID.randomUUID();
            GridWorks.getInstance().getLogger().warning(
                    "GridWorks control node was missing a node id; generated " + migrated
            );
            return migrated;
        }

        try {
            return UUID.fromString(stored);
        } catch (IllegalArgumentException exception) {
            UUID migrated = UUID.randomUUID();
            GridWorks.getInstance().getLogger().warning(
                    "GridWorks control node had an invalid node id '" + stored + "'; generated " + migrated
            );
            return migrated;
        }
    }

    @Override
    public void postInitialise() {
        super.postInitialise();
        beforeActivated();
        GridWorks.getInstance().getPhysicalControlNetwork().activate(this);
        afterActivated();
    }

    /**
     * Runs immediately before this node enters the live physical network.
     *
     * <p>Use this hook for transient fail-safe state and input-edge baselines
     * that must be prepared before activation can replay component state.</p>
     */
    protected void beforeActivated() {
    }

    /**
     * Runs after activation and component-wide state replay have completed.
     *
     * <p>Do not clear replay-derived input state here. This hook is intended
     * for work that requires the node to already be live in the topology.</p>
     */
    protected void afterActivated() {
    }

    @Override
    public void write(@NotNull PersistentDataContainer pdc) {
        pdc.set(NODE_ID_KEY, PersistentDataType.STRING, nodeId.toString());
        writeNodeData(pdc);
    }

    protected void writeNodeData(@NotNull PersistentDataContainer pdc) {
    }

    @Override
    public final @NotNull UUID id() {
        return nodeId;
    }

    public final @NotNull UUID getNodeId() {
        return nodeId;
    }

    public final Optional<ControlSignal> getLastSignal() {
        return Optional.ofNullable(lastSignal);
    }

    @Override
    public final void onSignal(@NotNull ControlSignal signal) {
        lastSignal = signal;
        handleSignal(signal);
    }

    protected void handleSignal(@NotNull ControlSignal signal) {
    }

    @Override
    public final void onUnload(@NotNull RebarBlockUnloadEvent event, @NotNull EventPriority priority) {
        GridWorks.getInstance().getPhysicalControlNetwork().deactivate(nodeId, this);
        afterDeactivated();
    }

    protected void afterDeactivated() {
    }

    @Override
    public final void onPostBlockBreak(@NotNull BlockBreakContext context) {
        try {
            GridWorks.getInstance().getPhysicalControlNetwork().remove(nodeId, this);
        } catch (IOException exception) {
            GridWorks.getInstance().getLogger().severe(
                    "Failed to persist removal of GridWorks control node " + nodeId + ": " + exception.getMessage()
            );
        }
        afterRemoved();
    }

    protected void afterRemoved() {
    }

    /**
     * Executes a Bukkit/world/GUI mutation on the primary server thread only
     * while this physical node is still part of the live GridWorks topology.
     *
     * <p>Control Bus publishers may call receivers from arbitrary threads. This
     * guard prevents physical blocks from touching Bukkit state asynchronously
     * and prevents delayed work from reviving an unloaded node or forcing its
     * chunk back into memory.</p>
     */
    protected final void runOnServerThreadIfActive(@NotNull Runnable action) {
        Objects.requireNonNull(action, "action");
        GridWorks plugin = GridWorks.getInstance();

        Runnable guarded = () -> {
            try {
                if (!plugin.getPhysicalControlNetwork().isActive(nodeId)) {
                    return;
                }
            } catch (IllegalStateException ignored) {
                return;
            }
            action.run();
        };

        if (Bukkit.isPrimaryThread()) {
            guarded.run();
        } else {
            plugin.getServer().getScheduler().runTask(plugin, guarded);
        }
    }
}
