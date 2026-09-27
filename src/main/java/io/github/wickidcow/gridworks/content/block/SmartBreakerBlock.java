package io.github.wickidcow.gridworks.content.block;

import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.block.interfaces.GuiRebarBlock;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.pylonmc.rebar.util.gui.GuiItems;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.api.control.ControlAddress;
import io.github.wickidcow.gridworks.api.control.ControlChannel;
import io.github.wickidcow.gridworks.api.control.ControlCommandChannel;
import io.github.wickidcow.gridworks.api.control.ControlInputRouteMode;
import io.github.wickidcow.gridworks.api.control.ControlSignal;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.api.power.PowerBranchControlResult;
import io.github.wickidcow.gridworks.api.power.PowerBranchSnapshot;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.window.AnvilWindow;

public final class SmartBreakerBlock extends PhysicalControlNodeBlock
        implements GuiRebarBlock {

    private static final NamespacedKey ROUTE_MODE_KEY = key("smart_breaker_route_mode");
    private static final NamespacedKey CIRCUIT_KEY = key("smart_breaker_circuit");
    private static final NamespacedKey ADDRESS_KEY = key("smart_breaker_address");
    private static final NamespacedKey DESIRED_ENABLED_KEY = key("smart_breaker_desired_enabled");

    private ControlInputRouteMode routeMode;
    private ControlCommandChannel circuit;
    private ControlAddress address;
    private boolean desiredEnabled;

    private Optional<PowerBranchSnapshot> observedBranch = Optional.empty();
    private String lastStatus = "Not reconciled";

    private final StatusItem statusItem = new StatusItem();
    private final RouteModeItem routeModeItem = new RouteModeItem();
    private final CircuitItem circuitItem = new CircuitItem();
    private final AddressItem addressItem = new AddressItem();
    private final CommandItem commandItem = new CommandItem();

    public SmartBreakerBlock(
            @NotNull Block block,
            @NotNull BlockCreateContext context
    ) {
        super(block, context);
        setFacing(context.getFacing());

        this.routeMode = ControlInputRouteMode.CIRCUIT;
        this.circuit = ControlCommandChannel.DEFAULT;
        this.address = ControlAddress.defaultFor(getNodeId(), "breaker");
        this.desiredEnabled = true;
    }

    public SmartBreakerBlock(
            @NotNull Block block,
            @NotNull PersistentDataContainer pdc
    ) {
        super(block, pdc);

        this.routeMode = ControlInputRouteMode.fromStored(
                pdc.get(ROUTE_MODE_KEY, PersistentDataType.STRING)
        );
        this.circuit = ControlCommandChannel.fromStored(
                pdc.get(CIRCUIT_KEY, PersistentDataType.STRING)
        );
        this.address = ControlAddress.fromStoredOrDefault(
                pdc.get(ADDRESS_KEY, PersistentDataType.STRING),
                ControlAddress.defaultFor(getNodeId(), "breaker")
        );

        Byte storedDesired = pdc.get(DESIRED_ENABLED_KEY, PersistentDataType.BYTE);
        this.desiredEnabled = storedDesired == null || storedDesired != 0;
    }

    @Override
    protected void afterActivated() {
        GridWorks.getInstance().getSmartBreakerManager().register(this);
    }

    @Override
    protected void afterDeactivated() {
        GridWorks.getInstance().getSmartBreakerManager().unregister(this);
    }

    @Override
    protected void afterRemoved() {
        GridWorks.getInstance().getSmartBreakerManager().unregister(this);
    }

    @Override
    public boolean accepts(@NotNull ControlChannel channel) {
        return activeInputChannel().equals(channel);
    }

    @Override
    protected void handleSignal(@NotNull ControlSignal signal) {
        if (!(signal.value() instanceof ControlValue.BooleanValue booleanValue)) {
            return;
        }

        runOnServerThreadIfActive(() -> {
            desiredEnabled = booleanValue.value();
            reconcileBranch();
        });
    }

    @Override
    protected void writeNodeData(@NotNull PersistentDataContainer pdc) {
        pdc.set(ROUTE_MODE_KEY, PersistentDataType.STRING, routeMode.name());
        pdc.set(CIRCUIT_KEY, PersistentDataType.STRING, circuit.name());
        pdc.set(ADDRESS_KEY, PersistentDataType.STRING, address.value());
        pdc.set(
                DESIRED_ENABLED_KEY,
                PersistentDataType.BYTE,
                desiredEnabled ? (byte) 1 : (byte) 0
        );
    }

    @Override
    public @NotNull Gui createGui() {
        reconcileBranch();

        return Gui.builder()
                .setStructure("s # m c a # t # x")
                .addIngredient('#', GuiItems.background())
                .addIngredient('s', statusItem)
                .addIngredient('m', routeModeItem)
                .addIngredient('c', circuitItem)
                .addIngredient('a', addressItem)
                .addIngredient('t', commandItem)
                .build();
    }

    @Override
    public @NotNull Component getGuiTitle() {
        return Component.text("Smart Breaker", NamedTextColor.GOLD);
    }

    public @NotNull ControlInputRouteMode getRouteMode() {
        return routeMode;
    }

    public @NotNull ControlCommandChannel getCircuit() {
        return circuit;
    }

    public @NotNull ControlAddress getAddress() {
        return address;
    }

    public boolean isDesiredEnabled() {
        return desiredEnabled;
    }

    public @NotNull String describeBranch() {
        PowerBranchSnapshot snapshot = observedBranch.orElse(null);
        if (snapshot == null) {
            return lastStatus;
        }

        String limit = snapshot.powerLimitSupported()
                ? ", limit " + formatWatts(snapshot.powerLimitWatts()) + " W"
                : "";

        return snapshot.displayName()
                + " / "
                + (snapshot.enabled() ? "CLOSED" : "OPEN")
                + limit
                + " / desired "
                + (desiredEnabled ? "CLOSED" : "OPEN");
    }

    public boolean targetsChunk(UUID worldId, int chunkX, int chunkZ) {
        Target target = targetCoordinates();
        return target.world().getUID().equals(worldId)
                && (target.x() >> 4) == chunkX
                && (target.z() >> 4) == chunkZ;
    }

    public void markTargetUnavailable(String reason) {
        observedBranch = Optional.empty();
        lastStatus = reason;
        notifyItems();
    }

    public void reconcileBranch() {
        var bridge = GridWorks.getInstance().getPowerBranchBridge();
        if (!bridge.isAvailable()) {
            observedBranch = Optional.empty();
            lastStatus = bridge.status();
            notifyItems();
            return;
        }

        Target target = targetCoordinates();
        if (!target.world().isChunkLoaded(target.x() >> 4, target.z() >> 4)) {
            observedBranch = Optional.empty();
            lastStatus = "Target chunk is not loaded";
            notifyItems();
            return;
        }

        Block targetBlock = target.world().getBlockAt(
                target.x(),
                target.y(),
                target.z()
        );

        try {
            Optional<PowerBranchSnapshot> snapshot = bridge.snapshotFor(
                    targetBlock,
                    target.side()
            );

            if (snapshot.isEmpty()) {
                observedBranch = Optional.empty();
                lastStatus = "No controllable power branch on target face";
                notifyItems();
                return;
            }

            PowerBranchSnapshot current = snapshot.orElseThrow();
            observedBranch = snapshot;

            if (current.enabled() == desiredEnabled) {
                lastStatus = "Synchronized";
                notifyItems();
                return;
            }

            PowerBranchControlResult result = bridge.setEnabled(
                    targetBlock,
                    target.side(),
                    desiredEnabled
            );
            lastStatus = result.status() + (
                    result.message().isEmpty() ? "" : ": " + result.message()
            );

            if (result.applied()) {
                observedBranch = bridge.snapshotFor(targetBlock, target.side());
                PowerBranchSnapshot verified = observedBranch.orElse(null);
                if (verified == null) {
                    lastStatus = "Provider applied command but target disappeared";
                } else if (verified.enabled() != desiredEnabled) {
                    lastStatus = "Provider applied command but readback did not match";
                } else {
                    lastStatus = "Synchronized";
                }
            }
        } catch (RuntimeException exception) {
            observedBranch = Optional.empty();
            lastStatus = "Provider error";
            GridWorks.getInstance().getLogger().log(
                    java.util.logging.Level.SEVERE,
                    "Smart Breaker " + getNodeId() + " provider operation failed",
                    exception
            );
        }

        notifyItems();
    }

    private void setDesiredEnabled(boolean enabled) {
        desiredEnabled = enabled;
        reconcileBranch();
    }

    private void toggleRouteMode() {
        routeMode = routeMode.toggle();
        routeModeItem.notifyWindows();
        circuitItem.notifyWindows();
        addressItem.notifyWindows();

        GridWorks.getInstance()
                .getPhysicalControlNetwork()
                .replayStateSources(getNodeId());
    }

    private void changeCircuit(int direction) {
        if (routeMode != ControlInputRouteMode.CIRCUIT) {
            return;
        }

        circuit = circuit.cycle(direction);
        circuitItem.notifyWindows();

        GridWorks.getInstance()
                .getPhysicalControlNetwork()
                .replayStateSources(getNodeId());
    }

    private void setAddress(ControlAddress next) {
        address = next;
        addressItem.notifyWindows();

        if (routeMode == ControlInputRouteMode.ADDRESS) {
            GridWorks.getInstance()
                    .getPhysicalControlNetwork()
                    .replayStateSources(getNodeId());
        }
    }

    private ControlChannel activeInputChannel() {
        return routeMode == ControlInputRouteMode.ADDRESS
                ? address.channel()
                : circuit.channel();
    }

    private Target targetCoordinates() {
        BlockFace facing = getFacing();
        Block source = getBlock();
        return new Target(
                source.getWorld(),
                source.getX() + facing.getModX(),
                source.getY() + facing.getModY(),
                source.getZ() + facing.getModZ(),
                facing.getOppositeFace()
        );
    }

    private BlockFace getFacing() {
        BlockData data = getBlock().getBlockData();
        if (!(data instanceof Directional directional)) {
            throw new IllegalStateException(
                    "Smart Breaker material no longer provides Directional block data: "
                            + data.getMaterial()
            );
        }
        return directional.getFacing();
    }

    private void setFacing(BlockFace facing) {
        BlockData data = getBlock().getBlockData();
        if (!(data instanceof Directional directional)) {
            throw new IllegalStateException(
                    "Smart Breaker material no longer provides Directional block data: "
                            + data.getMaterial()
            );
        }

        if (!directional.getFaces().contains(facing)) {
            throw new IllegalArgumentException(
                    "Smart Breaker material cannot face " + facing
            );
        }

        directional.setFacing(facing);
        getBlock().setBlockData(directional);
    }

    private void openAddressWindow(Player player) {
        final boolean[] firstRename = {true};

        Gui upperGui = Gui.builder()
                .setStructure("# a #")
                .addIngredient('#', GuiItems.background())
                .addIngredient(
                        'a',
                        ItemStackBuilder.of(Material.NAME_TAG)
                                .name(Component.text(address.value(), NamedTextColor.GOLD))
                )
                .build();

        Gui lowerGui = Gui.builder()
                .setStructure(
                        "# # # # # # # # #",
                        "# # # # i # # # #",
                        "# # # # # # # # #",
                        "# # # # # # # # #"
                )
                .addIngredient('#', GuiItems.background())
                .addIngredient(
                        'i',
                        ItemStackBuilder.of(Material.PAPER)
                                .name(Component.text("Set Breaker Address", NamedTextColor.GOLD))
                                .lore(Component.text(
                                        "Use the same address as a load-shedding tier.",
                                        NamedTextColor.GRAY
                                ))
                )
                .build();

        try {
            AnvilWindow window = AnvilWindow.builder()
                    .setViewer(player)
                    .setUpperGui(upperGui)
                    .setLowerGui(lowerGui)
                    .setTitle(Component.text("Smart Breaker Address"))
                    .addRenameHandler(raw -> {
                        if (firstRename[0]) {
                            firstRename[0] = false;
                            return;
                        }

                        try {
                            setAddress(ControlAddress.fromUserInput(raw));
                        } catch (IllegalArgumentException exception) {
                            player.sendMessage(Component.text(
                                    exception.getMessage(),
                                    NamedTextColor.RED
                            ));
                        }
                    })
                    .build(player);
            window.open();
        } catch (RuntimeException exception) {
            GridWorks.getInstance().getLogger().log(
                    java.util.logging.Level.SEVERE,
                    "Could not open Smart Breaker address window",
                    exception
            );
            player.sendMessage(Component.text(
                    "GridWorks could not open the address window.",
                    NamedTextColor.RED
            ));
        }
    }

    private void notifyItems() {
        statusItem.notifyWindows();
        routeModeItem.notifyWindows();
        circuitItem.notifyWindows();
        addressItem.notifyWindows();
        commandItem.notifyWindows();
    }

    private static String formatWatts(double watts) {
        if (watts == Double.MAX_VALUE) {
            return "unlimited";
        }
        return String.format(Locale.ROOT, "%.1f", watts);
    }

    private static NamespacedKey key(String value) {
        return Objects.requireNonNull(
                NamespacedKey.fromString("gridworks:" + value)
        );
    }

    private abstract class BreakerItem extends AbstractItem {
        protected ItemStackBuilder item(Material material, String name) {
            return ItemStackBuilder.of(material)
                    .name(Component.text(name, NamedTextColor.GOLD));
        }
    }

    private final class StatusItem extends BreakerItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            PowerBranchSnapshot snapshot = observedBranch.orElse(null);
            Material material = snapshot == null
                    ? Material.GRAY_DYE
                    : (snapshot.enabled() ? Material.LIME_DYE : Material.RED_DYE);

            ItemStackBuilder builder = item(material, "Branch Status")
                    .lore(
                            Component.text(lastStatus, NamedTextColor.GRAY),
                            Component.text(
                                    "Desired: " + (desiredEnabled ? "CLOSED" : "OPEN"),
                                    desiredEnabled
                                            ? NamedTextColor.GREEN
                                            : NamedTextColor.RED
                            )
                    );

            if (snapshot != null) {
                builder.lore(
                        Component.text(
                                "Target: " + snapshot.displayName(),
                                NamedTextColor.WHITE
                        ),
                        Component.text(
                                "Observed: " + (snapshot.enabled() ? "CLOSED" : "OPEN"),
                                snapshot.enabled()
                                        ? NamedTextColor.GREEN
                                        : NamedTextColor.RED
                        )
                );
                if (snapshot.powerLimitSupported()) {
                    builder.lore(Component.text(
                            "Limit: " + formatWatts(snapshot.powerLimitWatts()) + " W",
                            NamedTextColor.GRAY
                    ));
                }
            }
            return builder;
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            reconcileBranch();
        }
    }

    private final class RouteModeItem extends BreakerItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(
                    routeMode == ControlInputRouteMode.ADDRESS
                            ? Material.ENDER_EYE
                            : Material.REDSTONE,
                    "Input route: " + routeMode.displayName()
            ).lore(Component.text(
                    "Click to switch Circuit / Address",
                    NamedTextColor.YELLOW
            ));
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            toggleRouteMode();
        }
    }

    private final class CircuitItem extends BreakerItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            ItemStackBuilder builder = item(
                    routeMode == ControlInputRouteMode.CIRCUIT
                            ? Material.REDSTONE_TORCH
                            : Material.GRAY_DYE,
                    "Circuit: " + circuit.displayName()
            ).lore(Component.text(
                    circuit.channel().toString(),
                    NamedTextColor.GRAY
            ));

            if (routeMode == ControlInputRouteMode.CIRCUIT) {
                builder.lore(Component.text(
                        "Left/right click to cycle Default / A / B / C / D",
                        NamedTextColor.YELLOW
                ));
            }
            return builder;
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            if (clickType.isLeftClick()) {
                changeCircuit(1);
            } else if (clickType.isRightClick()) {
                changeCircuit(-1);
            }
        }
    }

    private final class AddressItem extends BreakerItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            ItemStackBuilder builder = item(
                    routeMode == ControlInputRouteMode.ADDRESS
                            ? Material.NAME_TAG
                            : Material.GRAY_DYE,
                    "Address: " + address.value()
            ).lore(Component.text(
                    address.channel().toString(),
                    NamedTextColor.GRAY
            ));

            if (routeMode == ControlInputRouteMode.ADDRESS) {
                builder.lore(Component.text(
                        "Click to edit address",
                        NamedTextColor.YELLOW
                ));
            }
            return builder;
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            if (routeMode != ControlInputRouteMode.ADDRESS) {
                return;
            }

            player.closeInventory();
            GridWorks.getInstance().getServer().getScheduler().runTask(
                    GridWorks.getInstance(),
                    () -> openAddressWindow(player)
            );
        }
    }

    private final class CommandItem extends BreakerItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(
                    desiredEnabled ? Material.LEVER : Material.BARRIER,
                    "Command: " + (desiredEnabled ? "CLOSED" : "OPEN")
            ).lore(Component.text(
                    "Click to toggle the desired branch state",
                    NamedTextColor.YELLOW
            ));
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            setDesiredEnabled(!desiredEnabled);
        }
    }

    private record Target(
            World world,
            int x,
            int y,
            int z,
            BlockFace side
    ) {
    }
}
