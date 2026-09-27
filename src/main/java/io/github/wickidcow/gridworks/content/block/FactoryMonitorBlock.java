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
import io.github.wickidcow.gridworks.api.control.GridWorksChannels;
import io.github.wickidcow.gridworks.monitor.FactoryMonitorTelemetry;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
            new SignalDefinition(GridWorksChannels.ALARM_LAST_TRIGGERED_EPOCH_MS, "Alarm Last Triggered", Material.CLOCK),

            new SignalDefinition(GridWorksChannels.BATCH_PROGRESS, "Batch Progress", Material.CRAFTER),
            new SignalDefinition(GridWorksChannels.BATCH_TARGET, "Batch Target", Material.TARGET),
            new SignalDefinition(GridWorksChannels.BATCH_COMPLETE, "Batch Complete", Material.LIME_DYE),
            new SignalDefinition(GridWorksChannels.SEQUENCE_RUNNING, "Sequence Running", Material.ORANGE_DYE),
            new SignalDefinition(GridWorksChannels.SEQUENCE_STAGE, "Sequence Stage", Material.COPPER_BULB),
            new SignalDefinition(GridWorksChannels.SEQUENCE_COMPLETE, "Sequence Complete", Material.LIME_CONCRETE),

            new SignalDefinition(GridWorksChannels.MACHINE_AVAILABLE, "Machine Available", Material.YELLOW_GLAZED_TERRACOTTA),
            new SignalDefinition(GridWorksChannels.MACHINE_KIND, "Machine Kind", Material.NAME_TAG),
            new SignalDefinition(GridWorksChannels.MACHINE_PROCESSING, "Machine Processing", Material.FURNACE),
            new SignalDefinition(GridWorksChannels.MACHINE_PROGRESS, "Machine Progress", Material.CLOCK),
            new SignalDefinition(GridWorksChannels.MACHINE_PROCESS_TIME_TICKS, "Machine Process Time", Material.REPEATER),
            new SignalDefinition(GridWorksChannels.MACHINE_TICKS_REMAINING, "Machine Time Remaining", Material.COMPARATOR),
            new SignalDefinition(GridWorksChannels.MACHINE_OBSERVED_CYCLES, "Observed Machine Cycles", Material.CRAFTING_TABLE),
            new SignalDefinition(GridWorksChannels.MACHINE_LAST_CYCLE_EPOCH_MS, "Last Observed Cycle", Material.CLOCK),

            new SignalDefinition(GridWorksChannels.POWER_AVAILABLE, "Power Grid Available", Material.LIGHTNING_ROD),
            new SignalDefinition(GridWorksChannels.POWER_PRODUCTION_CAPACITY_WATTS, "Power Capacity", Material.REDSTONE_BLOCK),
            new SignalDefinition(GridWorksChannels.POWER_DEMAND_WATTS, "Power Demand", Material.COMPARATOR),
            new SignalDefinition(GridWorksChannels.POWER_RESERVE_WATTS, "Power Reserve", Material.COPPER_BLOCK),
            new SignalDefinition(GridWorksChannels.POWER_LOAD_RATIO, "Power Load", Material.REPEATER),
            new SignalDefinition(GridWorksChannels.POWER_POWERED_CONSUMER_RATIO, "Consumers Powered", Material.LIME_DYE),
            new SignalDefinition(GridWorksChannels.POWER_UNPOWERED_CONSUMERS, "Unpowered Consumers", Material.RED_DYE),
            new SignalDefinition(GridWorksChannels.POWER_CONSUMER_COUNT, "Power Consumers", Material.PAPER),
            new SignalDefinition(GridWorksChannels.POWER_PRODUCER_COUNT, "Power Producers", Material.BLAZE_POWDER)
    );

    private final FactoryMonitorTelemetry telemetry = new FactoryMonitorTelemetry();
    private final Map<UUID, UUID> selectedSources = new ConcurrentHashMap<>();
    private final Map<ControlChannel, SignalValueItem> signalItems = createSignalItems();
    private final AddressedSignalItem addressedSignalItem = new AddressedSignalItem();
    private final SourceSelectorItem sourceSelectorItem = new SourceSelectorItem();
    private final RefreshItem refreshItem = new RefreshItem();

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
        if (!telemetry.observe(signal)) {
            return;
        }

        if (ControlAddress.isAddressedChannel(signal.channel())) {
            runOnServerThreadIfActive(() -> {
                addressedSignalItem.notifyWindows();
                sourceSelectorItem.notifyWindows();
                refreshItem.notifyWindows();
            });
            return;
        }

        SignalValueItem item = signalItems.get(signal.channel());
        if (item == null) {
            return;
        }

        runOnServerThreadIfActive(() -> {
            item.notifyWindows();
            sourceSelectorItem.notifyWindows();
            refreshItem.notifyWindows();
        });
    }

    @Override
    public @NotNull Gui createGui() {
        pruneTelemetryToLiveComponent();

        return Gui.builder()
                .setStructure(
                        "r s 0 1 2 3 4 n x",
                        "a i o t f C L # #",
                        "v p y m k z Y Z D",
                        "w u g h j b N e #",
                        "l c d E F G H I J",
                        "P Q R S T U V W X"
                )
                .addIngredient('#', GuiItems.background())
                .addIngredient('r', item(GridWorksChannels.REDSTONE_POWERED))
                .addIngredient('s', item(GridWorksChannels.REDSTONE_STRENGTH))
                .addIngredient('0', item(GridWorksChannels.CONTROL_ENABLED))
                .addIngredient('1', item(GridWorksChannels.CONTROL_A))
                .addIngredient('2', item(GridWorksChannels.CONTROL_B))
                .addIngredient('3', item(GridWorksChannels.CONTROL_C))
                .addIngredient('4', item(GridWorksChannels.CONTROL_D))
                .addIngredient('n', sourceSelectorItem)
                .addIngredient('x', refreshItem)
                .addIngredient('a', item(GridWorksChannels.INVENTORY_AVAILABLE))
                .addIngredient('i', item(GridWorksChannels.INVENTORY_ITEMS))
                .addIngredient('o', item(GridWorksChannels.INVENTORY_OCCUPIED_SLOTS))
                .addIngredient('t', item(GridWorksChannels.INVENTORY_TOTAL_SLOTS))
                .addIngredient('f', item(GridWorksChannels.INVENTORY_OCCUPIED_RATIO))
                .addIngredient('C', item(GridWorksChannels.MACHINE_OBSERVED_CYCLES))
                .addIngredient('L', item(GridWorksChannels.MACHINE_LAST_CYCLE_EPOCH_MS))
                .addIngredient('v', item(GridWorksChannels.FLUID_AVAILABLE))
                .addIngredient('p', item(GridWorksChannels.FLUID_PRESENT))
                .addIngredient('y', item(GridWorksChannels.FLUID_TYPE))
                .addIngredient('m', item(GridWorksChannels.FLUID_AMOUNT))
                .addIngredient('k', item(GridWorksChannels.FLUID_CAPACITY))
                .addIngredient('z', item(GridWorksChannels.FLUID_FILL_RATIO))
                .addIngredient('Y', item(GridWorksChannels.SEQUENCE_RUNNING))
                .addIngredient('Z', item(GridWorksChannels.SEQUENCE_STAGE))
                .addIngredient('D', item(GridWorksChannels.SEQUENCE_COMPLETE))
                .addIngredient('w', item(GridWorksChannels.ALARM_NAME))
                .addIngredient('u', item(GridWorksChannels.ALARM_SEVERITY))
                .addIngredient('g', item(GridWorksChannels.ALARM_CONDITION_ACTIVE))
                .addIngredient('h', item(GridWorksChannels.ALARM_LATCHED))
                .addIngredient('j', item(GridWorksChannels.ALARM_ACKNOWLEDGED))
                .addIngredient('b', item(GridWorksChannels.BATCH_PROGRESS))
                .addIngredient('N', item(GridWorksChannels.BATCH_TARGET))
                .addIngredient('e', item(GridWorksChannels.BATCH_COMPLETE))
                .addIngredient('l', item(GridWorksChannels.ALARM_OCCURRENCES))
                .addIngredient('c', item(GridWorksChannels.ALARM_LAST_TRIGGERED_EPOCH_MS))
                .addIngredient('d', addressedSignalItem)
                .addIngredient('E', item(GridWorksChannels.MACHINE_AVAILABLE))
                .addIngredient('F', item(GridWorksChannels.MACHINE_KIND))
                .addIngredient('G', item(GridWorksChannels.MACHINE_PROCESSING))
                .addIngredient('H', item(GridWorksChannels.MACHINE_PROGRESS))
                .addIngredient('I', item(GridWorksChannels.MACHINE_PROCESS_TIME_TICKS))
                .addIngredient('J', item(GridWorksChannels.MACHINE_TICKS_REMAINING))
                .addIngredient('P', item(GridWorksChannels.POWER_AVAILABLE))
                .addIngredient('Q', item(GridWorksChannels.POWER_PRODUCTION_CAPACITY_WATTS))
                .addIngredient('R', item(GridWorksChannels.POWER_DEMAND_WATTS))
                .addIngredient('S', item(GridWorksChannels.POWER_RESERVE_WATTS))
                .addIngredient('T', item(GridWorksChannels.POWER_LOAD_RATIO))
                .addIngredient('U', item(GridWorksChannels.POWER_POWERED_CONSUMER_RATIO))
                .addIngredient('V', item(GridWorksChannels.POWER_UNPOWERED_CONSUMERS))
                .addIngredient('W', item(GridWorksChannels.POWER_CONSUMER_COUNT))
                .addIngredient('X', item(GridWorksChannels.POWER_PRODUCER_COUNT))
                .build();
    }

    public int observedSignalCount() {
        return telemetry.signalCount();
    }

    public int observedSourceCount() {
        return telemetry.sourceIds().size();
    }

    private ControlSignal signalFor(Player player, ControlChannel channel) {
        UUID selected = selectedSources.get(player.getUniqueId());
        return selected == null
                ? telemetry.latest(channel).orElse(null)
                : telemetry.latest(selected, channel).orElse(null);
    }

    private ControlSignal addressedSignalFor(Player player) {
        UUID selected = selectedSources.get(player.getUniqueId());
        return selected == null
                ? telemetry.latestAddressed().orElse(null)
                : telemetry.latestAddressed(selected).orElse(null);
    }

    private List<UUID> sourceIds() {
        return telemetry.sourceIds().stream().sorted().toList();
    }

    private void cycleSource(Player player, int delta) {
        List<UUID> sources = sourceIds();
        UUID viewerId = player.getUniqueId();
        UUID selected = selectedSources.get(viewerId);

        int currentIndex = 0;
        if (selected != null) {
            int sourceIndex = sources.indexOf(selected);
            if (sourceIndex >= 0) {
                currentIndex = sourceIndex + 1;
            } else {
                selectedSources.remove(viewerId);
            }
        }

        int nextIndex = Math.floorMod(currentIndex + delta, sources.size() + 1);
        if (nextIndex == 0) {
            selectedSources.remove(viewerId);
        } else {
            selectedSources.put(viewerId, sources.get(nextIndex - 1));
        }

        notifyMonitorItems();
    }

    private void pruneTelemetryToLiveComponent() {
        Set<UUID> activeSources;
        try {
            activeSources = GridWorks.getInstance()
                    .getPhysicalControlNetwork()
                    .activeComponentNodes(getNodeId());
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            return;
        }

        telemetry.retainSources(activeSources);
        selectedSources.forEach((viewerId, sourceId) -> {
            if (!activeSources.contains(sourceId)) {
                selectedSources.remove(viewerId, sourceId);
            }
        });
    }

    private void refreshTelemetry() {
        pruneTelemetryToLiveComponent();

        try {
            GridWorks.getInstance()
                    .getPhysicalControlNetwork()
                    .replayStateSources(getNodeId());
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            return;
        }

        notifyMonitorItems();
    }

    private void notifyMonitorItems() {
        for (SignalValueItem item : signalItems.values()) {
            item.notifyWindows();
        }
        addressedSignalItem.notifyWindows();
        sourceSelectorItem.notifyWindows();
        refreshItem.notifyWindows();
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

    private static String displayValue(ControlChannel channel, ControlValue value) {
        if ((GridWorksChannels.MACHINE_LAST_CYCLE_EPOCH_MS.equals(channel)
                || GridWorksChannels.ALARM_LAST_TRIGGERED_EPOCH_MS.equals(channel))
                && value instanceof ControlValue.NumberValue numberValue) {
            long epochMillis = (long) numberValue.value();
            return epochMillis <= 0L
                    ? "never"
                    : Instant.ofEpochMilli(epochMillis).toString();
        }
        return displayValue(value);
    }

    private static String displayValue(ControlValue value) {
        if (value instanceof ControlValue.BooleanValue booleanValue) {
            return booleanValue.value() ? "true" : "false";
        }
        if (value instanceof ControlValue.NumberValue numberValue) {
            double numeric = numberValue.value();
            if (numeric == Math.rint(numeric)
                    && Math.abs(numeric) <= 9_007_199_254_740_991.0) {
                return Long.toString((long) numeric);
            }
            return Double.toString(numeric);
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

            ControlSignal signal = signalFor(player, definition.channel());
            if (signal == null) {
                UUID selected = selectedSources.get(player.getUniqueId());
                return builder.lore(Component.text(
                        selected == null
                                ? "Waiting for signal"
                                : "No signal from " + shortId(selected),
                        NamedTextColor.YELLOW
                ));
            }

            return builder.lore(
                    Component.text(
                            "Value: " + displayValue(definition.channel(), signal.value()),
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
            ControlSignal signal = addressedSignalFor(player);
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

    private final class SourceSelectorItem extends AbstractItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            List<UUID> sources = sourceIds();
            UUID viewerId = player.getUniqueId();
            UUID selected = selectedSources.get(viewerId);

            if (selected != null && !sources.contains(selected)) {
                selectedSources.remove(viewerId, selected);
                selected = null;
            }

            ItemStackBuilder builder = ItemStackBuilder.of(Material.SPYGLASS)
                    .name(Component.text("Signal Source", NamedTextColor.GOLD));

            if (selected == null) {
                return builder.lore(
                        Component.text("View: Overview", NamedTextColor.AQUA),
                        Component.text(
                                "Newest value per channel across "
                                        + sources.size()
                                        + " source(s)",
                                NamedTextColor.GRAY
                        ),
                        Component.text("Left/right click to cycle", NamedTextColor.YELLOW)
                );
            }

            return builder.lore(
                    Component.text("View: " + shortId(selected), NamedTextColor.AQUA),
                    Component.text(
                            "Tracked signals: " + telemetry.sourceSignalCount(selected),
                            NamedTextColor.GRAY
                    ),
                    Component.text("Left/right click to cycle", NamedTextColor.YELLOW)
            );
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            if (clickType.isLeftClick()) {
                cycleSource(player, 1);
            } else if (clickType.isRightClick()) {
                cycleSource(player, -1);
            }
        }
    }

    private final class RefreshItem extends AbstractItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return ItemStackBuilder.of(Material.COMPASS)
                    .name(Component.text("Refresh Monitor", NamedTextColor.GOLD))
                    .lore(
                            Component.text(
                                    "Tracked: " + telemetry.sourceIds().size()
                                            + " source(s), " + telemetry.signalCount()
                                            + " signal(s)",
                                    NamedTextColor.GRAY
                            ),
                            Component.text(
                                    "Prunes sources outside the loaded component",
                                    NamedTextColor.GRAY
                            ),
                            Component.text(
                                    "Replays loaded state sources; never loads chunks",
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
            if (clickType.isLeftClick() || clickType.isRightClick()) {
                refreshTelemetry();
            }
        }
    }

    private record SignalDefinition(
            ControlChannel channel,
            String label,
            Material material
    ) {
    }
}
