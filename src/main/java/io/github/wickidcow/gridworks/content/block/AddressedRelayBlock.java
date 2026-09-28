package io.github.wickidcow.gridworks.content.block;

import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.block.interfaces.GuiRebarBlock;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.pylonmc.rebar.util.gui.GuiItems;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.api.control.ControlAddress;
import io.github.wickidcow.gridworks.api.control.ControlChannel;
import io.github.wickidcow.gridworks.api.control.ControlSignal;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Switch;
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

public final class AddressedRelayBlock extends PhysicalControlNodeBlock implements GuiRebarBlock {
    private static final NamespacedKey ADDRESS_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:addressed_relay_address")
    );

    private volatile ControlAddress address;
    private volatile boolean powered;

    private final AddressItem addressItem = new AddressItem();
    private final StatusItem statusItem = new StatusItem();

    public AddressedRelayBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
        this.address = ControlAddress.defaultFor(getNodeId(), "relay");
        this.powered = false;
    }

    public AddressedRelayBlock(
            @NotNull Block block,
            @NotNull PersistentDataContainer pdc
    ) {
        super(block, pdc);
        this.address = ControlAddress.fromStoredOrDefault(
                pdc.get(ADDRESS_KEY, PersistentDataType.STRING),
                ControlAddress.defaultFor(getNodeId(), "relay")
        );
        this.powered = false;
    }

    @Override
    public boolean accepts(@NotNull ControlChannel channel) {
        return address.channel().equals(channel);
    }

    @Override
    protected void beforeActivated() {
        powered = false;
    }

    @Override
    protected void afterActivated() {
        applyOutputState();
    }

    @Override
    protected void handleSignal(@NotNull ControlSignal signal) {
        if (signal.value() instanceof ControlValue.BooleanValue booleanValue) {
            powered = booleanValue.value();
            runOnServerThreadIfActive(() -> {
                applyOutputState();
                statusItem.notifyWindows();
            });
        }
    }

    @Override
    protected void writeNodeData(@NotNull PersistentDataContainer pdc) {
        pdc.set(ADDRESS_KEY, PersistentDataType.STRING, address.value());
    }

    @Override
    public @NotNull Gui createGui() {
        return Gui.builder()
                .setStructure("a # x")
                .addIngredient('#', GuiItems.background())
                .addIngredient('a', addressItem)
                .addIngredient('x', statusItem)
                .build();
    }

    @Override
    public @NotNull Component getGuiTitle() {
        return Component.text("Addressed Relay", NamedTextColor.GOLD);
    }

    public @NotNull ControlAddress getAddress() {
        return address;
    }

    public boolean isPowered() {
        return powered;
    }

    private void setAddress(ControlAddress next) {
        if (address.equals(next)) {
            return;
        }

        address = next;
        powered = false;
        applyOutputState();
        addressItem.notifyWindows();
        statusItem.notifyWindows();

        GridWorks.getInstance()
                .getPhysicalControlNetwork()
                .replayStateSources(getNodeId());
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
                                .name(Component.text("Set Relay Address", NamedTextColor.GOLD))
                                .lore(
                                        Component.text(
                                                "Spaces become underscores.",
                                                NamedTextColor.GRAY
                                        ),
                                        Component.text(
                                                "Shared addresses intentionally control groups.",
                                                NamedTextColor.GRAY
                                        )
                                )
                )
                .build();

        try {
            AnvilWindow window = AnvilWindow.builder()
                    .setViewer(player)
                    .setUpperGui(upperGui)
                    .setLowerGui(lowerGui)
                    .setTitle(Component.text("Addressed Relay"))
                    .addRenameHandler(raw -> {
                        if (firstRename[0]) {
                            firstRename[0] = false;
                            return;
                        }

                        try {
                            setAddress(ControlAddress.fromUserInput(raw));
                        } catch (IllegalArgumentException ignored) {
                            player.sendMessage(Component.text(
                                    "Address must contain letters or numbers.",
                                    NamedTextColor.RED
                            ));
                        }
                    })
                    .build(player);
            window.open();
        } catch (RuntimeException exception) {
            GridWorks.getInstance().getLogger().log(
                    java.util.logging.Level.SEVERE,
                    "Could not open Addressed Relay address window",
                    exception
            );
            player.sendMessage(Component.text(
                    "GridWorks could not open the address window.",
                    NamedTextColor.RED
            ));
        }
    }

    private void applyOutputState() {
        BlockData blockData = getBlock().getBlockData();
        if (!(blockData instanceof Switch relaySwitch)) {
            throw new IllegalStateException(
                    "Addressed Relay material no longer provides Switch block data: "
                            + blockData.getMaterial()
            );
        }

        if (relaySwitch.isPowered() == powered) {
            return;
        }

        relaySwitch.setPowered(powered);
        getBlock().setBlockData(relaySwitch);
    }

    private abstract class RelayItem extends AbstractItem {
        protected ItemStackBuilder item(Material material, String name) {
            return ItemStackBuilder.of(material)
                    .name(Component.text(name, NamedTextColor.GOLD));
        }
    }

    private final class AddressItem extends RelayItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(Material.NAME_TAG, "Address: " + address.value())
                    .lore(
                            Component.text(address.channel().toString(), NamedTextColor.GRAY),
                            Component.text("Click to edit address", NamedTextColor.YELLOW),
                            Component.text(
                                    "Relays sharing an address act as a group",
                                    NamedTextColor.DARK_GRAY
                            )
                    );
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            player.closeInventory();
            GridWorks.getInstance().getServer().getScheduler().runTask(
                    GridWorks.getInstance(),
                    () -> openAddressWindow(player)
            );
        }
    }

    private final class StatusItem extends RelayItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(
                    powered ? Material.LIME_DYE : Material.GRAY_DYE,
                    "Output: " + (powered ? "ON" : "OFF")
            ).lore(Component.text(
                    "Only accepts " + address.channel(),
                    NamedTextColor.GRAY
            ));
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
        }
    }
}
