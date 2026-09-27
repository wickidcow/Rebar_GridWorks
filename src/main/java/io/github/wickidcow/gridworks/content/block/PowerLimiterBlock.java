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
import io.github.wickidcow.gridworks.api.power.PowerLimitSetting;
import io.github.wickidcow.gridworks.power.PowerBranchDevice;
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

public final class PowerLimiterBlock extends PhysicalControlNodeBlock
        implements GuiRebarBlock, PowerBranchDevice {

    private static final NamespacedKey ROUTE_MODE_KEY = key("power_limiter_route_mode");
    private static final NamespacedKey CIRCUIT_KEY = key("power_limiter_circuit");
    private static final NamespacedKey ADDRESS_KEY = key("power_limiter_address");
    private static final NamespacedKey LIMIT_ENABLED_KEY = key("power_limiter_enabled");
    private static final NamespacedKey LIMIT_WATTS_KEY = key("power_limiter_watts");

    private static final double DEFAULT_LIMIT_WATTS = 1000.0;
    private static final double MIN_LIMIT_WATTS = 1.0;
    private static final double MAX_LIMIT_WATTS = 1_000_000_000_000.0;

    private ControlInputRouteMode routeMode;
    private ControlCommandChannel circuit;
    private ControlAddress address;
    private PowerLimitSetting setting;

    private Optional<PowerBranchSnapshot> observedBranch = Optional.empty();
    private String lastStatus = "Not reconciled";

    private final StatusItem statusItem = new StatusItem();
    private final RouteModeItem routeModeItem = new RouteModeItem();
    private final CircuitItem circuitItem = new CircuitItem();
    private final AddressItem addressItem = new AddressItem();
    private final LimitItem limitItem = new LimitItem();
    private final LimitToggleItem toggleItem = new LimitToggleItem();

    public PowerLimiterBlock(
            @NotNull Block block,
            @NotNull BlockCreateContext context
    ) {
        super(block, context);
        setFacing(context.getFacing());

        this.routeMode = ControlInputRouteMode.CIRCUIT;
        this.circuit = ControlCommandChannel.DEFAULT;
        this.address = ControlAddress.defaultFor(getNodeId(), "limiter");
        this.setting = new PowerLimitSetting(false, DEFAULT_LIMIT_WATTS);
    }

    public PowerLimiterBlock(
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
                ControlAddress.defaultFor(getNodeId(), "limiter")
        );

        Byte enabled = pdc.get(LIMIT_ENABLED_KEY, PersistentDataType.BYTE);
        Double storedLimit = pdc.get(LIMIT_WATTS_KEY, PersistentDataType.DOUBLE);
        this.setting = new PowerLimitSetting(
                enabled != null && enabled != 0,
                sanitizeLimit(storedLimit)
        );
    }

    @Override
    protected void afterActivated() {
        GridWorks.getInstance().getPowerBranchDeviceManager().register(this);
    }

    @Override
    protected void afterDeactivated() {
        GridWorks.getInstance().getPowerBranchDeviceManager().unregister(this);
    }

    @Override
    protected void afterRemoved() {
        GridWorks.getInstance().getPowerBranchDeviceManager().unregister(this);
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
            setting = setting.withEnabled(booleanValue.value());
            reconcileBranch();
        });
    }

    @Override
    protected void writeNodeData(@NotNull PersistentDataContainer pdc) {
        pdc.set(ROUTE_MODE_KEY, PersistentDataType.STRING, routeMode.name());
        pdc.set(CIRCUIT_KEY, PersistentDataType.STRING, circuit.name());
        pdc.set(ADDRESS_KEY, PersistentDataType.STRING, address.value());
        pdc.set(
                LIMIT_ENABLED_KEY,
                PersistentDataType.BYTE,
                setting.enabled() ? (byte) 1 : (byte) 0
        );
        pdc.set(
                LIMIT_WATTS_KEY,
                PersistentDataType.DOUBLE,
                setting.limitWatts()
        );
    }

    @Override
    public @NotNull Gui createGui() {
        reconcileBranch();

        return Gui.builder()
                .setStructure("s # m c a # l t x")
                .addIngredient('#', GuiItems.background())
                .addIngredient('s', statusItem)
                .addIngredient('m', routeModeItem)
                .addIngredient('c', circuitItem)
                .addIngredient('a', addressItem)
                .addIngredient('l', limitItem)
                .addIngredient('t', toggleItem)
                .build();
    }

    @Override
    public @NotNull Component getGuiTitle() {
        return Component.text("Power Limiter", NamedTextColor.GOLD);
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

    public @NotNull PowerLimitSetting getSetting() {
        return setting;
    }

    public @NotNull String describeBranch() {
        PowerBranchSnapshot snapshot = observedBranch.orElse(null);
        if (snapshot == null) {
            return lastStatus;
        }

        return snapshot.displayName()
                + " / observed "
                + (snapshot.powerLimitSupported()
                        ? formatWatts(snapshot.powerLimitWatts()) + " W"
                        : "limit unsupported")
                + " / desired "
                + (setting.enabled()
                        ? formatWatts(setting.limitWatts()) + " W"
                        : "BYPASS");
    }

    @Override
    public boolean targetsChunk(UUID worldId, int chunkX, int chunkZ) {
        Target target = targetCoordinates();
        return target.world().getUID().equals(worldId)
                && (target.x() >> 4) == chunkX
                && (target.z() >> 4) == chunkZ;
    }

    @Override
    public void markTargetUnavailable(String reason) {
        observedBranch = Optional.empty();
        lastStatus = reason;
        notifyItems();
    }

    @Override
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

            if (!current.powerLimitSupported()) {
                lastStatus = "Provider does not support branch power limits";
                notifyItems();
                return;
            }

            double desired = setting.effectiveLimitWatts();
            if (sameLimit(current.powerLimitWatts(), desired)) {
                lastStatus = "Synchronized";
                notifyItems();
                return;
            }

            PowerBranchControlResult result = bridge.setPowerLimitWatts(
                    targetBlock,
                    target.side(),
                    desired
            );
            lastStatus = result.status() + (
                    result.message().isEmpty() ? "" : ": " + result.message()
            );

            if (result.applied()) {
                observedBranch = bridge.snapshotFor(targetBlock, target.side());
                PowerBranchSnapshot verified = observedBranch.orElse(null);
                if (verified == null) {
                    lastStatus = "Provider applied limit but target disappeared";
                } else if (!verified.powerLimitSupported()) {
                    lastStatus = "Provider applied limit but readback lost limit support";
                } else if (!sameLimit(verified.powerLimitWatts(), desired)) {
                    lastStatus = "Provider applied a different limit than requested";
                } else {
                    lastStatus = "Synchronized";
                }
            }
        } catch (RuntimeException exception) {
            observedBranch = Optional.empty();
            lastStatus = "Provider error";
            GridWorks.getInstance().getLogger().log(
                    java.util.logging.Level.SEVERE,
                    "Power Limiter " + getNodeId() + " provider operation failed",
                    exception
            );
        }

        notifyItems();
    }

    private void toggleLimiter() {
        setting = setting.withEnabled(!setting.enabled());
        reconcileBranch();
    }

    private void changeLimit(double delta) {
        double next = Math.clamp(
                setting.limitWatts() + delta,
                MIN_LIMIT_WATTS,
                MAX_LIMIT_WATTS
        );
        setting = setting.withLimitWatts(next);
        reconcileBranch();
    }

    private void toggleRouteMode() {
        setting = setting.withEnabled(false);
        routeMode = routeMode.toggle();
        reconcileBranch();
        notifyItems();
        requestStateReplay();
    }

    private void changeCircuit(int direction) {
        if (routeMode != ControlInputRouteMode.CIRCUIT) {
            return;
        }

        setting = setting.withEnabled(false);
        circuit = circuit.cycle(direction);
        reconcileBranch();
        circuitItem.notifyWindows();
        toggleItem.notifyWindows();
        requestStateReplay();
    }

    private void setAddress(ControlAddress next) {
        if (address.equals(next)) {
            return;
        }

        boolean activeAddressRoute = routeMode == ControlInputRouteMode.ADDRESS;
        if (activeAddressRoute) {
            setting = setting.withEnabled(false);
        }

        address = next;
        addressItem.notifyWindows();

        if (activeAddressRoute) {
            reconcileBranch();
            toggleItem.notifyWindows();
            requestStateReplay();
        }
    }

    private void requestStateReplay() {
        GridWorks.getInstance()
                .getPhysicalControlNetwork()
                .replayStateSources(getNodeId());
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
                    "Power Limiter material no longer provides Directional block data: "
                            + data.getMaterial()
            );
        }
        return directional.getFacing();
    }

    private void setFacing(BlockFace facing) {
        BlockData data = getBlock().getBlockData();
        if (!(data instanceof Directional directional)) {
            throw new IllegalStateException(
                    "Power Limiter material no longer provides Directional block data: "
                            + data.getMaterial()
            );
        }

        if (!directional.getFaces().contains(facing)) {
            throw new IllegalArgumentException(
                    "Power Limiter material cannot face " + facing
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
                                .name(Component.text("Set Limiter Address", NamedTextColor.GOLD))
                                .lore(Component.text(
                                        "Boolean true enables the configured limit.",
                                        NamedTextColor.GRAY
                                ))
                )
                .build();

        try {
            AnvilWindow window = AnvilWindow.builder()
                    .setViewer(player)
                    .setUpperGui(upperGui)
                    .setLowerGui(lowerGui)
                    .setTitle(Component.text("Power Limiter Address"))
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
                    "Could not open Power Limiter address window",
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
        limitItem.notifyWindows();
        toggleItem.notifyWindows();
    }

    private static boolean sameLimit(double left, double right) {
        if (left == Double.MAX_VALUE || right == Double.MAX_VALUE) {
            return left == right;
        }
        double scale = Math.max(1.0, Math.max(Math.abs(left), Math.abs(right)));
        return Math.abs(left - right) <= scale * 1.0e-9;
    }

    private static double sanitizeLimit(Double stored) {
        if (stored == null
                || !Double.isFinite(stored)
                || stored <= 0.0) {
            return DEFAULT_LIMIT_WATTS;
        }
        return Math.clamp(stored, MIN_LIMIT_WATTS, MAX_LIMIT_WATTS);
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

    private abstract class LimiterItem extends AbstractItem {
        protected ItemStackBuilder item(Material material, String name) {
            return ItemStackBuilder.of(material)
                    .name(Component.text(name, NamedTextColor.GOLD));
        }
    }

    private final class StatusItem extends LimiterItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            PowerBranchSnapshot snapshot = observedBranch.orElse(null);
            Material material = snapshot == null
                    ? Material.GRAY_DYE
                    : (snapshot.powerLimitSupported()
                            ? Material.LIME_DYE
                            : Material.RED_DYE);

            ItemStackBuilder builder = item(material, "Limiter Status")
                    .lore(
                            Component.text(lastStatus, NamedTextColor.GRAY),
                            Component.text(
                                    "Desired: "
                                            + (setting.enabled()
                                            ? formatWatts(setting.limitWatts()) + " W"
                                            : "BYPASS"),
                                    NamedTextColor.WHITE
                            )
                    );

            if (snapshot != null) {
                builder.lore(Component.text(
                        "Target: " + snapshot.displayName(),
                        NamedTextColor.WHITE
                ));
                if (snapshot.powerLimitSupported()) {
                    builder.lore(Component.text(
                            "Observed: " + formatWatts(snapshot.powerLimitWatts()) + " W",
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

    private final class RouteModeItem extends LimiterItem {
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

    private final class CircuitItem extends LimiterItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            ItemStackBuilder builder = item(
                    routeMode == ControlInputRouteMode.CIRCUIT
                            ? Material.REDSTONE_TORCH
                            : Material.GRAY_DYE,
                    "Circuit: " + circuit.displayName()
            ).lore(Component.text(circuit.channel().toString(), NamedTextColor.GRAY));

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

    private final class AddressItem extends LimiterItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            ItemStackBuilder builder = item(
                    routeMode == ControlInputRouteMode.ADDRESS
                            ? Material.NAME_TAG
                            : Material.GRAY_DYE,
                    "Address: " + address.value()
            ).lore(Component.text(address.channel().toString(), NamedTextColor.GRAY));

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

    private final class LimitItem extends LimiterItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(
                    Material.COMPARATOR,
                    "Limit: " + formatWatts(setting.limitWatts()) + " W"
            ).lore(
                    Component.text(
                            "Left +100 W / Right -100 W",
                            NamedTextColor.YELLOW
                    ),
                    Component.text(
                            "Shift uses 1000 W",
                            NamedTextColor.YELLOW
                    )
            );
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            double step = clickType.isShiftClick() ? 1000.0 : 100.0;
            if (clickType.isLeftClick()) {
                changeLimit(step);
            } else if (clickType.isRightClick()) {
                changeLimit(-step);
            }
        }
    }

    private final class LimitToggleItem extends LimiterItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(
                    setting.enabled() ? Material.LIME_DYE : Material.GRAY_DYE,
                    "Limiter: " + (setting.enabled() ? "ENABLED" : "BYPASS")
            ).lore(Component.text(
                    "Click to toggle; Control Bus boolean does the same",
                    NamedTextColor.YELLOW
            ));
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            toggleLimiter();
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
