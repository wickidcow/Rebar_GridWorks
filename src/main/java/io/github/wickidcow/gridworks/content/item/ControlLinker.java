package io.github.wickidcow.gridworks.content.item;

import io.github.pylonmc.rebar.block.BlockStorage;
import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler;
import io.github.pylonmc.rebar.item.RebarItem;
import io.github.pylonmc.rebar.item.interfaces.BlockInteractRebarItemHandler;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.api.control.ControlSignal;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.content.block.PhysicalControlNodeBlock;
import io.github.wickidcow.gridworks.content.block.RedstoneSensorBlock;
import io.github.wickidcow.gridworks.content.block.StatusLightBlock;
import io.github.wickidcow.gridworks.physical.ControlNetworkSnapshot;
import io.github.wickidcow.gridworks.physical.PhysicalControlNetwork;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public final class ControlLinker extends RebarItem implements BlockInteractRebarItemHandler {
    private static final NamespacedKey SELECTED_NODE_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:selected_node")
    );

    public ControlLinker(@NotNull ItemStack stack) {
        super(stack);
    }

    @Override
    @MultiHandler(priorities = EventPriority.NORMAL)
    public void onInteractWithBlock(@NotNull PlayerInteractEvent event, @NotNull EventPriority priority) {
        if (!event.getAction().isRightClick() || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        RebarBlock rebarBlock = BlockStorage.get(event.getClickedBlock());
        if (!(rebarBlock instanceof PhysicalControlNodeBlock controlNode)) {
            return;
        }

        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);

        PhysicalControlNetwork network = GridWorks.getInstance().getPhysicalControlNetwork();
        UUID clickedNode = controlNode.getNodeId();

        if (event.getPlayer().isSneaking()) {
            showNetworkInfo(event, network, controlNode);
            return;
        }

        UUID selectedNode = getSelectedNode();
        if (selectedNode == null) {
            setSelectedNode(clickedNode);
            event.getPlayer().sendMessage(
                    Component.text("Selected GridWorks node ", NamedTextColor.GRAY)
                            .append(Component.text(shortNodeId(clickedNode), NamedTextColor.AQUA))
                            .append(Component.text(". Right-click another node to link or unlink.", NamedTextColor.GRAY))
            );
            return;
        }

        if (selectedNode.equals(clickedNode)) {
            clearSelectedNode();
            event.getPlayer().sendMessage(Component.text("Linker selection cleared.", NamedTextColor.YELLOW));
            return;
        }

        if (!network.isActive(selectedNode)) {
            clearSelectedNode();
            event.getPlayer().sendMessage(
                    Component.text("The selected node is no longer loaded. Select it again.", NamedTextColor.RED)
            );
            return;
        }

        try {
            boolean connected = network.toggleLink(selectedNode, clickedNode);
            clearSelectedNode();

            event.getPlayer().sendMessage(
                    connected
                            ? Component.text("GridWorks nodes linked.", NamedTextColor.GREEN)
                            : Component.text("GridWorks nodes unlinked.", NamedTextColor.YELLOW)
            );
        } catch (IOException exception) {
            event.getPlayer().sendMessage(
                    Component.text("GridWorks could not save that connection. No topology change was kept.", NamedTextColor.RED)
            );
            GridWorks.getInstance().getLogger().severe(
                    "Failed to persist GridWorks link change: " + exception.getMessage()
            );
        }
    }

    private void showNetworkInfo(
            PlayerInteractEvent event,
            PhysicalControlNetwork network,
            PhysicalControlNodeBlock controlNode
    ) {
        ControlNetworkSnapshot snapshot = network.snapshot(controlNode.getNodeId());

        event.getPlayer().sendMessage(Component.text("GridWorks Control Network", NamedTextColor.GOLD));
        event.getPlayer().sendMessage(
                Component.text("Network: ", NamedTextColor.GRAY)
                        .append(Component.text(snapshot.networkId(), NamedTextColor.AQUA))
        );
        event.getPlayer().sendMessage(
                Component.text("Nodes: ", NamedTextColor.GRAY)
                        .append(Component.text(
                                snapshot.totalNodes() + " (" + snapshot.loadedNodes() + " loaded)",
                                NamedTextColor.WHITE
                        ))
        );
        event.getPlayer().sendMessage(
                Component.text("Connections: ", NamedTextColor.GRAY)
                        .append(Component.text(Integer.toString(snapshot.connections()), NamedTextColor.WHITE))
        );
        event.getPlayer().sendMessage(
                Component.text("Node: ", NamedTextColor.GRAY)
                        .append(Component.text(shortNodeId(controlNode.getNodeId()), NamedTextColor.WHITE))
        );

        if (controlNode instanceof RedstoneSensorBlock sensor) {
            event.getPlayer().sendMessage(
                    Component.text("Redstone: ", NamedTextColor.GRAY)
                            .append(Component.text(Integer.toString(sensor.getLastPower()), NamedTextColor.RED))
            );
        } else if (controlNode instanceof StatusLightBlock light) {
            event.getPlayer().sendMessage(
                    Component.text("Light: ", NamedTextColor.GRAY)
                            .append(Component.text(light.isActive() ? "ON" : "OFF",
                                    light.isActive() ? NamedTextColor.GREEN : NamedTextColor.RED))
            );
        }

        controlNode.getLastSignal().ifPresent(signal ->
                event.getPlayer().sendMessage(
                        Component.text("Last signal: ", NamedTextColor.GRAY)
                                .append(Component.text(
                                        signal.channel() + " = " + displayValue(signal),
                                        NamedTextColor.WHITE
                                ))
                )
        );
    }

    private static String displayValue(ControlSignal signal) {
        ControlValue value = signal.value();
        if (value instanceof ControlValue.BooleanValue booleanValue) {
            return Boolean.toString(booleanValue.value());
        }
        if (value instanceof ControlValue.NumberValue numberValue) {
            return Double.toString(numberValue.value());
        }
        if (value instanceof ControlValue.TextValue textValue) {
            return textValue.value();
        }
        return value.toString();
    }

    private UUID getSelectedNode() {
        String raw = getStack().getPersistentDataContainer().get(SELECTED_NODE_KEY, PersistentDataType.STRING);
        if (raw == null) {
            return null;
        }

        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException exception) {
            clearSelectedNode();
            return null;
        }
    }

    private void setSelectedNode(UUID nodeId) {
        getStack().editPersistentDataContainer(
                pdc -> pdc.set(SELECTED_NODE_KEY, PersistentDataType.STRING, nodeId.toString())
        );
    }

    private void clearSelectedNode() {
        getStack().editPersistentDataContainer(pdc -> pdc.remove(SELECTED_NODE_KEY));
    }

    private static String shortNodeId(UUID nodeId) {
        return nodeId.toString().substring(0, 8).toUpperCase();
    }
}
