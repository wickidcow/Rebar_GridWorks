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
import java.util.HashMap;
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
            new SignalDefinition(GridWorksChannels.BATCH_FAULT, "Batch Fault", Material.RED_CONCRETE),
            new SignalDefinition(GridWorksChannels.BATCH_WATCHDOG_TICKS, "Batch Watchdog", Material.CLOCK),
            new SignalDefinition(GridWorksChannels.BATCH_RATE_AVAILABLE, "Batch Rate Available", Material.LIME_DYE),
            new SignalDefinition(GridWorksChannels.BATCH_RATE_PER_MINUTE, "Batch Rate / Minute", Material.MINECART),
            new SignalDefinition(GridWorksChannels.BATCH_ETA_SECONDS, "Batch ETA Seconds", Material.COMPASS),
            new SignalDefinition(GridWorksChannels.SEQUENCE_RUNNING, "Sequence Running", Material.ORANGE_DYE),
            new SignalDefinition(GridWorksChannels.SEQUENCE_STAGE, "Sequence Stage", Material.COPPER_BULB),
            new SignalDefinition(GridWorksChannels.SEQUENCE_COMPLETE, "Sequence Complete", Material.LIME_CONCRETE),
            new SignalDefinition(GridWorksChannels.SEQUENCE_FAULT, "Sequence Fault", Material.RED_CONCRETE),
            new SignalDefinition(GridWorksChannels.SEQUENCE_FAULT_REASON, "Sequence Fault Reason", Material.NAME_TAG),
            new SignalDefinition(GridWorksChannels.SEQUENCE_FAULT_INTERLOCK_ACTIVE, "Fault Interlock Active", Material.SCULK_SENSOR),
            new SignalDefinition(GridWorksChannels.SEQUENCE_TIMEOUT_TICKS, "Active Stage Timeout", Material.CLOCK),
            new SignalDefinition(GridWorksChannels.SEQUENCE_COMPLETED_RUNS, "Sequence Completed Runs", Material.NETHER_STAR),
            new SignalDefinition(GridWorksChannels.SEQUENCE_LAST_COMPLETION_EPOCH_MS, "Last Sequence Completion", Material.CLOCK),

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

    private static final char[] PAGE_SLOT_KEYS = {
            'a', 'b', 'c', 'e', 'f', 'g', 'h', 'i', 'j',
            'k', 'l', 'm', 'o', 'p', 'q', 'r', 's', 't',
            'u', 'v', 'w', 'y', 'z', '0', '1', '2', '3',
            '4', '5', '6', '7', '8', '9', 'A', 'B', 'C'
    };
    private static final Map<ControlChannel, Integer> CHANNEL_SLOT_INDEX =
            createChannelSlotIndex();

    private final FactoryMonitorTelemetry telemetry = new FactoryMonitorTelemetry();
    private final Map<UUID, UUID> selectedSources = new ConcurrentHashMap<>();
    private final Map<UUID, MonitorPage> selectedPages = new ConcurrentHashMap<>();
    private final Map<ControlChannel, SignalValueItem> signalItems = createSignalItems();
    private final PageSignalItem[] pageSignalItems = createPageSignalItems();
    private final AddressedSignalItem addressedSignalItem = new AddressedSignalItem();
    private final SourceSelectorItem sourceSelectorItem = new SourceSelectorItem();
    private final RefreshItem refreshItem = new RefreshItem();
    private final PageTitleItem pageTitleItem = new PageTitleItem();
    private final PageNavItem previousPageItem = new PageNavItem(-1);
    private final PageNavItem nextPageItem = new PageNavItem(1);

    public FactoryMonitorBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
    }

    public FactoryMonitorBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);
    }

    @Override
    protected void beforeActivated() {
        telemetry.clear();
        selectedSources.clear();
        selectedPages.clear();
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

        Integer slotIndex = CHANNEL_SLOT_INDEX.get(signal.channel());
        if (slotIndex == null) {
            return;
        }

        runOnServerThreadIfActive(() -> {
            pageSignalItems[slotIndex].notifyWindows();
            sourceSelectorItem.notifyWindows();
            refreshItem.notifyWindows();
        });
    }

    @Override
    public @NotNull Gui createGui() {
        pruneTelemetryToLiveComponent();

        var builder = Gui.builder()
                .setStructure(
                        "L d # P # # n x R",
                        "a b c e f g h i j",
                        "k l m o p q r s t",
                        "u v w y z 0 1 2 3",
                        "4 5 6 7 8 9 A B C",
                        "# # # # # # # # #"
                )
                .addIngredient('#', GuiItems.background())
                .addIngredient('L', previousPageItem)
                .addIngredient('d', addressedSignalItem)
                .addIngredient('P', pageTitleItem)
                .addIngredient('n', sourceSelectorItem)
                .addIngredient('x', refreshItem)
                .addIngredient('R', nextPageItem);

        for (int index = 0; index < PAGE_SLOT_KEYS.length; index++) {
            builder.addIngredient(PAGE_SLOT_KEYS[index], pageSignalItems[index]);
        }

        return builder.build();
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

    private MonitorPage pageFor(Player player) {
        return selectedPages.getOrDefault(
                player.getUniqueId(),
                MonitorPage.AUTOMATION
        );
    }

    private void cyclePage(Player player, int delta) {
        MonitorPage[] pages = MonitorPage.values();
        MonitorPage current = pageFor(player);
        int next = Math.floorMod(current.ordinal() + delta, pages.length);
        selectedPages.put(player.getUniqueId(), pages[next]);
        notifyPageItems();
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
        notifyPageItems();
        addressedSignalItem.notifyWindows();
        sourceSelectorItem.notifyWindows();
        refreshItem.notifyWindows();
    }

    private void notifyPageItems() {
        for (PageSignalItem item : pageSignalItems) {
            item.notifyWindows();
        }
        pageTitleItem.notifyWindows();
        previousPageItem.notifyWindows();
        nextPageItem.notifyWindows();
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

    private PageSignalItem[] createPageSignalItems() {
        PageSignalItem[] items = new PageSignalItem[PAGE_SLOT_KEYS.length];
        for (int index = 0; index < items.length; index++) {
            items[index] = new PageSignalItem(index);
        }
        return items;
    }

    private static Map<ControlChannel, Integer> createChannelSlotIndex() {
        Map<ControlChannel, Integer> slots = new HashMap<>();
        for (MonitorPage page : MonitorPage.values()) {
            if (page.channels().size() > PAGE_SLOT_KEYS.length) {
                throw new IllegalStateException(
                        "Factory Monitor page " + page.displayName()
                                + " has too many signals"
                );
            }

            for (int index = 0; index < page.channels().size(); index++) {
                ControlChannel channel = page.channels().get(index);
                if (slots.put(channel, index) != null) {
                    throw new IllegalStateException(
                            "Factory Monitor channel appears on multiple pages: " + channel
                    );
                }
            }
        }

        Set<ControlChannel> defined = DEFINITIONS.stream()
                .map(SignalDefinition::channel)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (!slots.keySet().equals(defined)) {
            throw new IllegalStateException(
                    "Factory Monitor page definitions do not match signal definitions"
            );
        }

        return Map.copyOf(slots);
    }

    private static String displayValue(ControlChannel channel, ControlValue value) {
        if ((GridWorksChannels.MACHINE_LAST_CYCLE_EPOCH_MS.equals(channel)
                || GridWorksChannels.ALARM_LAST_TRIGGERED_EPOCH_MS.equals(channel)
                || GridWorksChannels.SEQUENCE_LAST_COMPLETION_EPOCH_MS.equals(channel))
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

    private final class PageSignalItem extends AbstractItem {
        private final int slotIndex;

        private PageSignalItem(int slotIndex) {
            this.slotIndex = slotIndex;
        }

        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            MonitorPage page = pageFor(player);
            if (slotIndex >= page.channels().size()) {
                return ItemStackBuilder.of(Material.BLACK_STAINED_GLASS_PANE)
                        .name(Component.text(" "));
            }

            ControlChannel channel = page.channels().get(slotIndex);
            SignalValueItem item = signalItems.get(channel);
            if (item == null) {
                throw new IllegalStateException(
                        "Factory Monitor has no item for " + channel
                );
            }
            return item.getItemProvider(player);
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
        }
    }

    private final class PageTitleItem extends AbstractItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            MonitorPage page = pageFor(player);
            return ItemStackBuilder.of(Material.WRITABLE_BOOK)
                    .name(Component.text(page.displayName(), NamedTextColor.GOLD))
                    .lore(
                            Component.text(
                                    "Page " + (page.ordinal() + 1)
                                            + "/" + MonitorPage.values().length,
                                    NamedTextColor.AQUA
                            ),
                            Component.text(
                                    page.channels().size() + " telemetry channel(s)",
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

    private final class PageNavItem extends AbstractItem {
        private final int direction;

        private PageNavItem(int direction) {
            this.direction = direction;
        }

        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            MonitorPage current = pageFor(player);
            MonitorPage[] pages = MonitorPage.values();
            MonitorPage destination = pages[Math.floorMod(
                    current.ordinal() + direction,
                    pages.length
            )];

            return ItemStackBuilder.of(Material.ARROW)
                    .name(Component.text(
                            direction < 0 ? "Previous Page" : "Next Page",
                            NamedTextColor.GOLD
                    ))
                    .lore(Component.text(
                            destination.displayName(),
                            NamedTextColor.AQUA
                    ));
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            if (clickType.isLeftClick() || clickType.isRightClick()) {
                cyclePage(player, direction);
            }
        }
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

    private enum MonitorPage {
        AUTOMATION(
                "Automation",
                List.of(
                        GridWorksChannels.REDSTONE_POWERED,
                        GridWorksChannels.REDSTONE_STRENGTH,
                        GridWorksChannels.CONTROL_ENABLED,
                        GridWorksChannels.CONTROL_A,
                        GridWorksChannels.CONTROL_B,
                        GridWorksChannels.CONTROL_C,
                        GridWorksChannels.CONTROL_D
                )
        ),
        RESOURCES(
                "Resources",
                List.of(
                        GridWorksChannels.INVENTORY_AVAILABLE,
                        GridWorksChannels.INVENTORY_ITEMS,
                        GridWorksChannels.INVENTORY_OCCUPIED_SLOTS,
                        GridWorksChannels.INVENTORY_TOTAL_SLOTS,
                        GridWorksChannels.INVENTORY_OCCUPIED_RATIO,
                        GridWorksChannels.FLUID_AVAILABLE,
                        GridWorksChannels.FLUID_PRESENT,
                        GridWorksChannels.FLUID_TYPE,
                        GridWorksChannels.FLUID_AMOUNT,
                        GridWorksChannels.FLUID_CAPACITY,
                        GridWorksChannels.FLUID_FILL_RATIO
                )
        ),
        PRODUCTION(
                "Production",
                List.of(
                        GridWorksChannels.MACHINE_AVAILABLE,
                        GridWorksChannels.MACHINE_KIND,
                        GridWorksChannels.MACHINE_PROCESSING,
                        GridWorksChannels.MACHINE_PROGRESS,
                        GridWorksChannels.MACHINE_PROCESS_TIME_TICKS,
                        GridWorksChannels.MACHINE_TICKS_REMAINING,
                        GridWorksChannels.MACHINE_OBSERVED_CYCLES,
                        GridWorksChannels.MACHINE_LAST_CYCLE_EPOCH_MS,
                        GridWorksChannels.BATCH_PROGRESS,
                        GridWorksChannels.BATCH_TARGET,
                        GridWorksChannels.BATCH_COMPLETE,
                        GridWorksChannels.BATCH_FAULT,
                        GridWorksChannels.BATCH_WATCHDOG_TICKS,
                        GridWorksChannels.BATCH_RATE_AVAILABLE,
                        GridWorksChannels.BATCH_RATE_PER_MINUTE,
                        GridWorksChannels.BATCH_ETA_SECONDS,
                        GridWorksChannels.SEQUENCE_RUNNING,
                        GridWorksChannels.SEQUENCE_STAGE,
                        GridWorksChannels.SEQUENCE_COMPLETE,
                        GridWorksChannels.SEQUENCE_FAULT,
                        GridWorksChannels.SEQUENCE_FAULT_REASON,
                        GridWorksChannels.SEQUENCE_FAULT_INTERLOCK_ACTIVE,
                        GridWorksChannels.SEQUENCE_TIMEOUT_TICKS,
                        GridWorksChannels.SEQUENCE_COMPLETED_RUNS,
                        GridWorksChannels.SEQUENCE_LAST_COMPLETION_EPOCH_MS
                )
        ),
        POWER_AND_ALARMS(
                "Power & Alarms",
                List.of(
                        GridWorksChannels.POWER_AVAILABLE,
                        GridWorksChannels.POWER_PRODUCTION_CAPACITY_WATTS,
                        GridWorksChannels.POWER_DEMAND_WATTS,
                        GridWorksChannels.POWER_RESERVE_WATTS,
                        GridWorksChannels.POWER_LOAD_RATIO,
                        GridWorksChannels.POWER_POWERED_CONSUMER_RATIO,
                        GridWorksChannels.POWER_UNPOWERED_CONSUMERS,
                        GridWorksChannels.POWER_CONSUMER_COUNT,
                        GridWorksChannels.POWER_PRODUCER_COUNT,
                        GridWorksChannels.ALARM_NAME,
                        GridWorksChannels.ALARM_SEVERITY,
                        GridWorksChannels.ALARM_CONDITION_ACTIVE,
                        GridWorksChannels.ALARM_LATCHED,
                        GridWorksChannels.ALARM_ACKNOWLEDGED,
                        GridWorksChannels.ALARM_OCCURRENCES,
                        GridWorksChannels.ALARM_LAST_TRIGGERED_EPOCH_MS
                )
        );

        private final String displayName;
        private final List<ControlChannel> channels;

        MonitorPage(String displayName, List<ControlChannel> channels) {
            this.displayName = displayName;
            this.channels = List.copyOf(channels);
        }

        private String displayName() {
            return displayName;
        }

        private List<ControlChannel> channels() {
            return channels;
        }
    }

    private record SignalDefinition(
            ControlChannel channel,
            String label,
            Material material
    ) {
    }
}
