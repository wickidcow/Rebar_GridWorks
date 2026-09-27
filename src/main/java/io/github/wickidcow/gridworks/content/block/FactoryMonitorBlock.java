package io.github.wickidcow.gridworks.content.block;

import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.block.interfaces.GuiRebarBlock;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.pylonmc.rebar.util.gui.GuiItems;
import io.github.wickidcow.gridworks.api.control.ControlAddress;
import io.github.wickidcow.gridworks.api.control.ControlChannel;
import io.github.wickidcow.gridworks.api.control.ControlSignal;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.api.control.GridWorksChannels;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemProvider;

public final class FactoryMonitorBlock extends PhysicalControlNodeBlock implements GuiRebarBlock {
    private static final List<SignalDefinition> DEFINITIONS = List.of(
            new SignalDefinition(GridWorksChannels.REDSTONE_POWERED, "Redstone Powered", Material.REDSTONE_TORCH),
            new SignalDefinition(GridWorksChannels.REDSTONE_STRENGTH, "Redstone Strength", Material.REDSTONE),
            new SignalDefinition(GridWorksChannels.CONTROL_ENABLED, "Control Default", Material.LEVER),
            new SignalDefinition(GridWorksChannels.CONTROL_A, "Control A", Material.LIME_DYE),
            new SignalDefinition(GridWorksChannels.CONTROL_B, "Control B", Material.CYAN_DYE),
            new SignalDefinition(GridWorksChannels.CONTROL_C, "Control C", Material.ORANGE_DYE),
            new SignalDefinition(GridWorksChannels.CONTROL_D, "Control D", Material.PURPLE_DYE),

            new SignalDefinition(GridWorksChannels.INVENTORY_AVAILABLE, "Inventory Available", Material.CHEST),
            new SignalDefinition(GridWorksChannels.INVENTORY_ITEMS, "Inventory Items", Material.CHEST),
            new SignalDefinition(GridWorksChannels.INVENTORY_OCCUPIED_SLOTS, "Occupied Slots", Material.HOPPER),
            new SignalDefinition(GridWorksChannels.INVENTORY_TOTAL_SLOTS, "Total Slots", Material.BARREL),
            new SignalDefinition(GridWorksChannels.INVENTORY_OCCUPIED_RATIO, "Inventory Fill", Material.COMPARATOR),

            new SignalDefinition(GridWorksChannels.FLUID_AVAILABLE, "Fluid Tank Available", Material.WATER_BUCKET),
            new SignalDefinition(GridWorksChannels.FLUID_PRESENT, "Fluid Present", Material.BLUE_DYE),
            new SignalDefinition(GridWorksChannels.FLUID_TYPE, "Fluid Type", Material.NAME_TAG),
            new SignalDefinition(GridWorksChannels.FLUID_AMOUNT, "Fluid Amount", Material.BUCKET),
            new SignalDefinition(GridWorksChannels.FLUID_CAPACITY, "Fluid Capacity", Material.CAULDRON),
            new SignalDefinition(GridWorksChannels.FLUID_FILL_RATIO, "Fluid Fill", Material.LIGHT_BLUE_STAINED_GLASS),

            new SignalDefinition(GridWorksChannels.ALARM_NAME, "Alarm Name", Material.NAME_TAG),
            new SignalDefinition(GridWorksChannels.ALARM_SEVERITY, "Alarm Severity", Material.YELLOW_DYE),
            new SignalDefinition(GridWorksChannels.ALARM_CONDITION_ACTIVE, "Alarm Condition", Material.REDSTONE_TORCH),
            new SignalDefinition(GridWorksChannels.ALARM_LATCHED, "Alarm Latched", Material.BELL),
            new SignalDefinition(GridWorksChannels.ALARM_ACKNOWLEDGED, "Alarm Acknowledged", Material.LIME_DYE),
            new SignalDefinition(GridWorksChannels.ALARM_OCCURRENCES, "Alarm Occurrences", Material.PAPER),
            new SignalDefinition(GridWorksChannels.ALARM_LAST_TRIGGERED_EPOCH_MS, "Alarm Last Triggered", Material.CLOCK)
    );

    private final Map<ControlChannel, ControlSignal> latestSignals = new ConcurrentHashMap<>();
    private volatile ControlSignal lastAddressedSignal;
    private final Map<ControlChannel, SignalValueItem> signalItems = createSignalItems();
    private final AddressedSignalItem addressedSignalItem = new AddressedSignalItem();

    public FactoryMonitorBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
    }

    public FactoryMonitorBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);
    }

    @Override
    public boolean accepts(@NotNull ControlChannel channel) {
        return signalItems.containsKey(channel)
                || ControlAddress.isAddressedChannel(channel);
    }

    @Override
    protected void handleSignal(@NotNull ControlSignal signal) {
        if (ControlAddress.isAddressedChannel(signal.channel())) {
            lastAddressedSignal = signal;
            runOnServerThreadIfActive(addressedSignalItem::notifyWindows);
            return;
        }

        latestSignals.put(signal.channel(), signal);

        SignalValueItem item = signalItems.get(signal.channel());
        if (item == null) {
            return;
        }

        runOnServerThreadIfActive(item::notifyWindows);
    }

    @Override
    public @NotNull Gui createGui() {
        return Gui.builder()
                .setStructure(
                        "r s 0 1 2 3 4 # #",
                        "a i o t f # # # #",
                        "v p y m k z # # #",
                        "w u g h j # # # #",
                        "l c d # # # # # #"
                )
                .addIngredient('#', GuiItems.background())
                .addIngredient('r', item(GridWorksChannels.REDSTONE_POWERED))
                .addIngredient('s', item(GridWorksChannels.REDSTONE_STRENGTH))
                .addIngredient('0', item(GridWorksChannels.CONTROL_ENABLED))
                .addIngredient('1', item(GridWorksChannels.CONTROL_A))
                .addIngredient('2', item(GridWorksChannels.CONTROL_B))
                .addIngredient('3', item(GridWorksChannels.CONTROL_C))
                .addIngredient('4', item(GridWorksChannels.CONTROL_D))
                .addIngredient('a', item(GridWorksChannels.INVENTORY_AVAILABLE))
                .addIngredient('i', item(GridWorksChannels.INVENTORY_ITEMS))
                .addIngredient('o', item(GridWorksChannels.INVENTORY_OCCUPIED_SLOTS))
                .addIngredient('t', item(GridWorksChannels.INVENTORY_TOTAL_SLOTS))
                .addIngredient('f', item(GridWorksChannels.INVENTORY_OCCUPIED_RATIO))
                .addIngredient('v', item(GridWorksChannels.FLUID_AVAILABLE))
                .addIngredient('p', item(GridWorksChannels.FLUID_PRESENT))
                .addIngredient('y', item(GridWorksChannels.FLUID_TYPE))
                .addIngredient('m', item(GridWorksChannels.FLUID_AMOUNT))
                .addIngredient('k', item(GridWorksChannels.FLUID_CAPACITY))
                .addIngredient('z', item(GridWorksChannels.FLUID_FILL_RATIO))
                .addIngredient('w', item(GridWorksChannels.ALARM_NAME))
                .addIngredient('u', item(GridWorksChannels.ALARM_SEVERITY))
                .addIngredient('g', item(GridWorksChannels.ALARM_CONDITION_ACTIVE))
                .addIngredient('h', item(GridWorksChannels.ALARM_LATCHED))
                .addIngredient('j', item(GridWorksChannels.ALARM_ACKNOWLEDGED))
                .addIngredient('l', item(GridWorksChannels.ALARM_OCCURRENCES))
                .addIngredient('c', item(GridWorksChannels.ALARM_LAST_TRIGGERED_EPOCH_MS))
                .addIngredient('d', addressedSignalItem)
                .build();
    }

    public int observedSignalCount() {
        return latestSignals.size();
    }

    private SignalValueItem item(ControlChannel channel) {
        SignalValueItem item = signalItems.get(channel);
        if (item == null) {
            throw new IllegalArgumentException("Factory Monitor has no display item for " + channel);
        }
        return item;
    }

    private Map<ControlChannel, SignalValueItem> createSignalItems() {
        Map<ControlChannel, SignalValueItem> items = new LinkedHashMap<>();
        for (SignalDefinition definition : DEFINITIONS) {
            items.put(definition.channel(), new SignalValueItem(definition));
        }
        return Map.copyOf(items);
    }

    private static String displayValue(ControlValue value) {
        if (value instanceof ControlValue.BooleanValue booleanValue) {
            return booleanValue.value() ? "true" : "false";
        }
        if (value instanceof ControlValue.NumberValue numberValue) {
            return Double.toString(numberValue.value());
        }
        if (value instanceof ControlValue.TextValue textValue) {
            return textValue.value().isEmpty() ? "(empty)" : textValue.value();
        }
        return value.toString();
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8).toUpperCase();
    }

    private final class SignalValueItem extends AbstractItem {
        private final SignalDefinition definition;

        private SignalValueItem(SignalDefinition definition) {
            this.definition = definition;
        }

        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            ItemStackBuilder builder = ItemStackBuilder.of(definition.material())
                    .name(Component.text(definition.label(), NamedTextColor.GOLD))
                    .lore(Component.text(definition.channel().toString(), NamedTextColor.DARK_GRAY));

            ControlSignal signal = latestSignals.get(definition.channel());
            if (signal == null) {
                return builder.lore(Component.text("Waiting for signal", NamedTextColor.YELLOW));
            }

            return builder.lore(
                    Component.text(
                            "Value: " + displayValue(signal.value()),
                            NamedTextColor.WHITE
                    ),
                    Component.text(
                            "Source: " + shortId(signal.source()),
                            NamedTextColor.GRAY
                    ),
                    Component.text(
                            "Sequence: " + signal.sequence(),
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
        }
    }

    private final class AddressedSignalItem extends AbstractItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            ControlSignal signal = lastAddressedSignal;
            if (signal == null) {
                return ItemStackBuilder.of(Material.ENDER_EYE)
                        .name(Component.text("Addressed Command", NamedTextColor.GOLD))
                        .lore(Component.text(
                                "Waiting for addressed command",
                                NamedTextColor.YELLOW
                        ));
            }

            return ItemStackBuilder.of(Material.ENDER_EYE)
                    .name(Component.text("Addressed Command", NamedTextColor.GOLD))
                    .lore(
                            Component.text(
                                    "Address: "
                                            + ControlAddress.fromChannel(signal.channel()).value(),
                                    NamedTextColor.WHITE
                            ),
                            Component.text(
                                    "Value: " + displayValue(signal.value()),
                                    NamedTextColor.WHITE
                            ),
                            Component.text(
                                    "Source: " + shortId(signal.source()),
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

    private record SignalDefinition(
            ControlChannel channel,
            String label,
            Material material
    ) {
    }
}
