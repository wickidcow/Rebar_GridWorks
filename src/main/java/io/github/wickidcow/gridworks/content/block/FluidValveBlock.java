package io.github.wickidcow.gridworks.content.block;

import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.block.interfaces.FluidTankRebarBlock;
import io.github.pylonmc.rebar.block.interfaces.GuiRebarBlock;
import io.github.pylonmc.rebar.fluid.FluidPointType;
import io.github.pylonmc.rebar.fluid.RebarFluid;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.pylonmc.rebar.util.gui.GuiItems;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.api.control.ControlAddress;
import io.github.wickidcow.gridworks.api.control.ControlChannel;
import io.github.wickidcow.gridworks.api.control.ControlCommandChannel;
import io.github.wickidcow.gridworks.api.control.ControlInputRoute;
import io.github.wickidcow.gridworks.api.control.ControlInputRouteMode;
import io.github.wickidcow.gridworks.api.control.ControlSignal;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.fluid.FluidValveFlow;
import java.util.List;
import java.util.Objects;
import kotlin.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

public final class FluidValveBlock extends PhysicalControlNodeBlock
        implements FluidTankRebarBlock, GuiRebarBlock {

    private static final NamespacedKey ROUTE_MODE_KEY = key("fluid_valve_route_mode");
    private static final NamespacedKey CIRCUIT_KEY = key("fluid_valve_circuit");
    private static final NamespacedKey ADDRESS_KEY = key("fluid_valve_address");
    private static final NamespacedKey OPEN_KEY = key("fluid_valve_open");

    private static final double TRANSIT_CAPACITY_MB = 1000.0;

    private ControlInputRoute inputRoute;
    private boolean open;

    private final StatusItem statusItem = new StatusItem();
    private final RouteModeItem routeModeItem = new RouteModeItem();
    private final CircuitItem circuitItem = new CircuitItem();
    private final AddressItem addressItem = new AddressItem();
    private final ToggleItem toggleItem = new ToggleItem();

    public FluidValveBlock(
            @NotNull Block block,
            @NotNull BlockCreateContext context
    ) {
        super(block, context);

        BlockFace facing = context.getFacing();
        setFacing(facing);
        setCapacity(TRANSIT_CAPACITY_MB);
        createFluidPoint(FluidPointType.INPUT, facing.getOppositeFace());
        createFluidPoint(FluidPointType.OUTPUT, facing);

        this.inputRoute = ControlInputRoute.defaults(
                ControlAddress.defaultFor(getNodeId(), "fluid_valve")
        );
        this.open = false;
    }

    public FluidValveBlock(
            @NotNull Block block,
            @NotNull PersistentDataContainer pdc
    ) {
        super(block, pdc);

        this.inputRoute = ControlInputRoute.fromStored(
                pdc.get(ROUTE_MODE_KEY, PersistentDataType.STRING),
                pdc.get(CIRCUIT_KEY, PersistentDataType.STRING),
                pdc.get(ADDRESS_KEY, PersistentDataType.STRING),
                ControlAddress.defaultFor(getNodeId(), "fluid_valve")
        );

        Byte storedOpen = pdc.get(OPEN_KEY, PersistentDataType.BYTE);
        this.open = storedOpen != null && storedOpen != 0;
    }

    @Override
    public boolean accepts(@NotNull ControlChannel channel) {
        return activeInputChannel().equals(channel);
    }

    @Override
    protected void handleSignal(@NotNull ControlSignal signal) {
        if (signal.value() instanceof ControlValue.BooleanValue booleanValue) {
            runOnServerThreadIfActive(() -> setOpen(booleanValue.value()));
        }
    }

    @Override
    protected void writeNodeData(@NotNull PersistentDataContainer pdc) {
        pdc.set(ROUTE_MODE_KEY, PersistentDataType.STRING, inputRoute.mode().name());
        pdc.set(CIRCUIT_KEY, PersistentDataType.STRING, inputRoute.circuit().name());
        pdc.set(ADDRESS_KEY, PersistentDataType.STRING, inputRoute.address().value());
        pdc.set(OPEN_KEY, PersistentDataType.BYTE, open ? (byte) 1 : (byte) 0);
    }

    @Override
    public boolean isAllowedFluid(@NotNull RebarFluid fluid) {
        return true;
    }

    @Override
    public double fluidAmountRequested(@NotNull RebarFluid fluid) {
        RebarFluid current = getFluidType();
        boolean compatible = current == null || current.equals(fluid);

        return FluidValveFlow.requestedAmount(
                open,
                compatible,
                getFluidSpaceRemaining()
        );
    }

    @Override
    public @NotNull List<Pair<RebarFluid, Double>> getSuppliedFluids() {
        RebarFluid current = getFluidType();
        double supplied = FluidValveFlow.suppliedAmount(open, getFluidAmount());

        if (current == null || supplied <= 0.0) {
            return List.of();
        }

        return List.of(new Pair<>(current, supplied));
    }

    @Override
    public @NotNull Gui createGui() {
        return Gui.builder()
                .setStructure("s # m c a # t # x")
                .addIngredient('#', GuiItems.background())
                .addIngredient('s', statusItem)
                .addIngredient('m', routeModeItem)
                .addIngredient('c', circuitItem)
                .addIngredient('a', addressItem)
                .addIngredient('t', toggleItem)
                .build();
    }

    @Override
    public @NotNull Component getGuiTitle() {
        return Component.text("Fluid Valve", NamedTextColor.GOLD);
    }

    public boolean isOpen() {
        return open;
    }

    public @NotNull ControlInputRouteMode getRouteMode() {
        return inputRoute.mode();
    }

    public @NotNull ControlCommandChannel getCircuit() {
        return inputRoute.circuit();
    }

    public @NotNull ControlAddress getAddress() {
        return inputRoute.address();
    }

    public @NotNull String describeValve() {
        String fluid = getFluidType() == null
                ? "empty"
                : getFluidType().getKey().toString();

        return (open ? "OPEN" : "CLOSED")
                + " / "
                + fluid
                + " / "
                + format(getFluidAmount())
                + " / "
                + format(getFluidCapacity())
                + " mB";
    }

    private void setOpen(boolean next) {
        if (open == next) {
            return;
        }
        open = next;
        notifyItems();
    }

    private void toggleRouteMode() {
        applyRouteChange(inputRoute.toggleMode());
    }

    private void changeCircuit(int direction) {
        applyRouteChange(inputRoute.cycleCircuit(direction));
    }

    private void setAddress(ControlAddress next) {
        applyRouteChange(inputRoute.withAddress(next));
    }

    private void applyRouteChange(ControlInputRoute.RouteChange change) {
        if (!change.changed()) {
            return;
        }

        if (change.activeRouteChanged()) {
            open = false;
        }

        inputRoute = change.route();
        notifyItems();

        if (change.activeRouteChanged()) {
            requestStateReplay();
        }
    }

    private void requestStateReplay() {
        GridWorks.getInstance()
                .getPhysicalControlNetwork()
                .replayStateSources(getNodeId());
    }

    private ControlChannel activeInputChannel() {
        return inputRoute.activeChannel();
    }

    private BlockFace getFacing() {
        BlockData data = getBlock().getBlockData();
        if (!(data instanceof Directional directional)) {
            throw new IllegalStateException(
                    "Fluid Valve material no longer provides Directional block data: "
                            + data.getMaterial()
            );
        }
        return directional.getFacing();
    }

    private void setFacing(BlockFace facing) {
        BlockData data = getBlock().getBlockData();
        if (!(data instanceof Directional directional)) {
            throw new IllegalStateException(
                    "Fluid Valve material no longer provides Directional block data: "
                            + data.getMaterial()
            );
        }

        if (!directional.getFaces().contains(facing)) {
            throw new IllegalArgumentException(
                    "Fluid Valve material cannot face " + facing
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
                                .name(Component.text(inputRoute.address().value(), NamedTextColor.GOLD))
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
                                .name(Component.text("Set Fluid Valve Address", NamedTextColor.GOLD))
                                .lore(Component.text(
                                        "Boolean true opens flow; false closes it.",
                                        NamedTextColor.GRAY
                                ))
                )
                .build();

        try {
            AnvilWindow window = AnvilWindow.builder()
                    .setViewer(player)
                    .setUpperGui(upperGui)
                    .setLowerGui(lowerGui)
                    .setTitle(Component.text("Fluid Valve Address"))
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
                    "Could not open Fluid Valve address window",
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
        toggleItem.notifyWindows();
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static NamespacedKey key(String value) {
        return Objects.requireNonNull(
                NamespacedKey.fromString("gridworks:" + value)
        );
    }

    private abstract class ValveItem extends AbstractItem {
        protected ItemStackBuilder item(Material material, String name) {
            return ItemStackBuilder.of(material)
                    .name(Component.text(name, NamedTextColor.GOLD));
        }
    }

    private final class StatusItem extends ValveItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            String fluid = getFluidType() == null
                    ? "empty"
                    : getFluidType().getKey().toString();

            return item(
                    open ? Material.LIME_DYE : Material.RED_DYE,
                    "Valve: " + (open ? "OPEN" : "CLOSED")
            ).lore(
                    Component.text("Fluid: " + fluid, NamedTextColor.WHITE),
                    Component.text(
                            "Buffer: "
                                    + format(getFluidAmount())
                                    + " / "
                                    + format(getFluidCapacity())
                                    + " mB",
                            NamedTextColor.GRAY
                    )
            );
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
        }
    }

    private final class RouteModeItem extends ValveItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(
                    inputRoute.mode() == ControlInputRouteMode.ADDRESS
                            ? Material.ENDER_EYE
                            : Material.REDSTONE,
                    "Input route: " + inputRoute.mode().displayName()
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

    private final class CircuitItem extends ValveItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            ItemStackBuilder builder = item(
                    inputRoute.mode() == ControlInputRouteMode.CIRCUIT
                            ? Material.REDSTONE_TORCH
                            : Material.GRAY_DYE,
                    "Circuit: " + inputRoute.circuit().displayName()
            ).lore(Component.text(inputRoute.circuit().channel().toString(), NamedTextColor.GRAY));

            if (inputRoute.mode() == ControlInputRouteMode.CIRCUIT) {
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

    private final class AddressItem extends ValveItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            ItemStackBuilder builder = item(
                    inputRoute.mode() == ControlInputRouteMode.ADDRESS
                            ? Material.NAME_TAG
                            : Material.GRAY_DYE,
                    "Address: " + inputRoute.address().value()
            ).lore(Component.text(inputRoute.address().channel().toString(), NamedTextColor.GRAY));

            if (inputRoute.mode() == ControlInputRouteMode.ADDRESS) {
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

    private final class ToggleItem extends ValveItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(
                    open ? Material.LIME_DYE : Material.GRAY_DYE,
                    "Manual state: " + (open ? "OPEN" : "CLOSED")
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
            setOpen(!open);
        }
    }
}
