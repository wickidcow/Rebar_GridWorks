package io.github.wickidcow.gridworks.content.block;

import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.block.interfaces.GuiRebarBlock;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.pylonmc.rebar.util.gui.GuiItems;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.alarm.AlarmAcknowledgeRequest;
import io.github.wickidcow.gridworks.alarm.AlarmConsoleFilter;
import io.github.wickidcow.gridworks.alarm.AlarmTelemetryState;
import io.github.wickidcow.gridworks.api.control.ControlChannel;
import io.github.wickidcow.gridworks.api.control.ControlSignal;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.api.control.GridWorksChannels;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemProvider;

public final class AlarmConsoleBlock extends PhysicalControlNodeBlock implements GuiRebarBlock {
    private static final int SLOT_COUNT = 18;
    private static final NamespacedKey FILTER_KEY = java.util.Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:alarm_console_filter")
    );

    private final Map<UUID, AlarmTelemetryState> alarms = new ConcurrentHashMap<>();
    private volatile List<AlarmTelemetryState.Snapshot> visibleAlarms = List.of();
    private volatile AlarmConsoleFilter filter;
    private final AtomicBoolean rebuildScheduled = new AtomicBoolean();

    private final List<AlarmSlotItem> alarmSlots = createSlots();
    private final RefreshItem refreshItem = new RefreshItem();
    private final FilterItem filterItem = new FilterItem();
    private final AcknowledgeAllItem acknowledgeAllItem = new AcknowledgeAllItem();
    private final SummaryItem summaryItem = new SummaryItem();

    public AlarmConsoleBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
        this.filter = AlarmConsoleFilter.ALL;
    }

    public AlarmConsoleBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);
        this.filter = AlarmConsoleFilter.fromStored(
                pdc.get(FILTER_KEY, PersistentDataType.STRING)
        );
    }

    @Override
    public boolean accepts(@NotNull ControlChannel channel) {
        return GridWorksChannels.ALARM_NAME.equals(channel)
                || GridWorksChannels.ALARM_SEVERITY.equals(channel)
                || GridWorksChannels.ALARM_CONDITION_ACTIVE.equals(channel)
                || GridWorksChannels.ALARM_LATCHED.equals(channel)
                || GridWorksChannels.ALARM_ACKNOWLEDGED.equals(channel)
                || GridWorksChannels.ALARM_OCCURRENCES.equals(channel)
                || GridWorksChannels.ALARM_LAST_TRIGGERED_EPOCH_MS.equals(channel);
    }

    @Override
    protected void handleSignal(@NotNull ControlSignal signal) {
        AlarmTelemetryState state = alarms.computeIfAbsent(
                signal.source(),
                AlarmTelemetryState::new
        );

        if (!state.apply(signal)) {
            return;
        }

        scheduleRebuild();
    }

    private void scheduleRebuild() {
        if (!rebuildScheduled.compareAndSet(false, true)) {
            return;
        }

        GridWorks plugin = GridWorks.getInstance();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            rebuildScheduled.set(false);
            runOnServerThreadIfActive(() -> {
                rebuildVisibleAlarms();
                notifyItems();
            });
        });
    }

    @Override
    protected void writeNodeData(@NotNull PersistentDataContainer pdc) {
        pdc.set(FILTER_KEY, PersistentDataType.STRING, filter.name());
    }

    @Override
    protected void afterDeactivated() {
        rebuildScheduled.set(false);
        alarms.clear();
        visibleAlarms = List.of();
    }

    @Override
    protected void afterRemoved() {
        rebuildScheduled.set(false);
        alarms.clear();
        visibleAlarms = List.of();
    }

    @Override
    public @NotNull Gui createGui() {
        rebuildVisibleAlarms();

        return Gui.builder()
                .setStructure(
                        "0 1 2 3 4 5 6 7 8",
                        "9 a b c d e f g h",
                        "# z q # s # x # #"
                )
                .addIngredient('#', GuiItems.background())
                .addIngredient('0', alarmSlots.get(0))
                .addIngredient('1', alarmSlots.get(1))
                .addIngredient('2', alarmSlots.get(2))
                .addIngredient('3', alarmSlots.get(3))
                .addIngredient('4', alarmSlots.get(4))
                .addIngredient('5', alarmSlots.get(5))
                .addIngredient('6', alarmSlots.get(6))
                .addIngredient('7', alarmSlots.get(7))
                .addIngredient('8', alarmSlots.get(8))
                .addIngredient('9', alarmSlots.get(9))
                .addIngredient('a', alarmSlots.get(10))
                .addIngredient('b', alarmSlots.get(11))
                .addIngredient('c', alarmSlots.get(12))
                .addIngredient('d', alarmSlots.get(13))
                .addIngredient('e', alarmSlots.get(14))
                .addIngredient('f', alarmSlots.get(15))
                .addIngredient('g', alarmSlots.get(16))
                .addIngredient('h', alarmSlots.get(17))
                .addIngredient('z', filterItem)
                .addIngredient('q', refreshItem)
                .addIngredient('s', summaryItem)
                .addIngredient('x', acknowledgeAllItem)
                .build();
    }

    public int trackedAlarmCount() {
        return alarms.size();
    }

    public int visibleAlarmCount() {
        return visibleAlarms.size();
    }

    public @NotNull AlarmConsoleFilter getFilter() {
        return filter;
    }

    private void rebuildVisibleAlarms() {
        Set<UUID> activeComponent;

        try {
            activeComponent = GridWorks.getInstance()
                    .getPhysicalControlNetwork()
                    .activeComponentNodes(getNodeId());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            visibleAlarms = List.of();
            return;
        }

        alarms.keySet().removeIf(source -> !activeComponent.contains(source));

        List<AlarmTelemetryState.Snapshot> snapshots = new ArrayList<>();
        for (AlarmTelemetryState state : alarms.values()) {
            AlarmTelemetryState.Snapshot snapshot = state.snapshot();
            if (filter.accepts(snapshot)) {
                snapshots.add(snapshot);
            }
        }

        snapshots.sort(
                Comparator
                        .comparing((AlarmTelemetryState.Snapshot snapshot) -> !snapshot.isLatched())
                        .thenComparing(snapshot -> snapshot.isAcknowledged())
                        .thenComparingInt(snapshot -> snapshot.severity().priority())
                        .thenComparing(snapshot -> !snapshot.isConditionActive())
                        .thenComparing(
                                AlarmTelemetryState.Snapshot::name,
                                String.CASE_INSENSITIVE_ORDER
                        )
                        .thenComparing(AlarmTelemetryState.Snapshot::source)
        );

        visibleAlarms = List.copyOf(snapshots);
    }

    private void changeFilter(int direction) {
        filter = filter.cycle(direction);
        rebuildVisibleAlarms();
        notifyItems();
    }

    private void refreshFromBus() {
        rebuildVisibleAlarms();
        notifyItems();

        GridWorks.getInstance()
                .getPhysicalControlNetwork()
                .replayStateSources(getNodeId());
    }

    private void acknowledge(UUID source) {
        GridWorks.getInstance().getControlBus().publish(
                getNodeId(),
                GridWorksChannels.ALARM_ACKNOWLEDGE,
                ControlValue.of(AlarmAcknowledgeRequest.target(source))
        );
    }

    private void acknowledgeAll() {
        GridWorks.getInstance().getControlBus().publish(
                getNodeId(),
                GridWorksChannels.ALARM_ACKNOWLEDGE,
                ControlValue.of(AlarmAcknowledgeRequest.ALL)
        );
    }

    private void notifyItems() {
        for (AlarmSlotItem item : alarmSlots) {
            item.notifyWindows();
        }
        filterItem.notifyWindows();
        summaryItem.notifyWindows();
    }

    private List<AlarmSlotItem> createSlots() {
        List<AlarmSlotItem> slots = new ArrayList<>(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots.add(new AlarmSlotItem(i));
        }
        return List.copyOf(slots);
    }

    private static String formatHistoryTime(long epochMillis) {
        if (epochMillis <= 0L) {
            return "never";
        }
        return java.time.Instant.ofEpochMilli(epochMillis).toString();
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8).toUpperCase();
    }

    private final class AlarmSlotItem extends AbstractItem {
        private final int index;

        private AlarmSlotItem(int index) {
            this.index = index;
        }

        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            List<AlarmTelemetryState.Snapshot> current = visibleAlarms;
            if (index >= current.size()) {
                return ItemStackBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                        .name(Component.text("No alarm", NamedTextColor.DARK_GRAY));
            }

            AlarmTelemetryState.Snapshot alarm = current.get(index);
            Material material;
            NamedTextColor nameColor;

            if (alarm.isLatched() && !alarm.isAcknowledged()) {
                material = Material.RED_DYE;
                nameColor = NamedTextColor.RED;
            } else if (alarm.isLatched()) {
                material = Material.YELLOW_DYE;
                nameColor = NamedTextColor.YELLOW;
            } else {
                material = Material.LIME_DYE;
                nameColor = NamedTextColor.GREEN;
            }

            ItemStackBuilder builder = ItemStackBuilder.of(material)
                    .name(Component.text(alarm.name(), nameColor))
                    .lore(
                            Component.text(
                                    "Severity: " + alarm.severity().displayName(),
                                    severityColor(alarm.severity())
                            ),
                            Component.text(
                                    "Condition: " + state(alarm.conditionActive()),
                                    NamedTextColor.GRAY
                            ),
                            Component.text(
                                    "Latch: " + state(alarm.latched()),
                                    NamedTextColor.GRAY
                            ),
                            Component.text(
                                    "Acknowledged: " + state(alarm.acknowledged()),
                                    NamedTextColor.GRAY
                            ),
                            Component.text(
                                    "Occurrences: " + alarm.occurrenceCount(),
                                    NamedTextColor.GRAY
                            ),
                            Component.text(
                                    "Last: " + formatHistoryTime(
                                            alarm.lastTriggeredEpochMillis()
                                    ),
                                    NamedTextColor.GRAY
                            ),
                            Component.text(
                                    "Node: " + shortId(alarm.source()),
                                    NamedTextColor.DARK_GRAY
                            )
                    );

            if (alarm.isLatched()) {
                builder.lore(Component.text(
                        "Click to acknowledge",
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
            List<AlarmTelemetryState.Snapshot> current = visibleAlarms;
            if (index >= current.size()) {
                return;
            }

            AlarmTelemetryState.Snapshot alarm = current.get(index);
            if (alarm.isLatched()) {
                acknowledge(alarm.source());
            }
        }

        private NamedTextColor severityColor(io.github.wickidcow.gridworks.alarm.AlarmSeverity severity) {
            return switch (severity) {
                case CRITICAL -> NamedTextColor.RED;
                case WARNING -> NamedTextColor.YELLOW;
                case INFO -> NamedTextColor.AQUA;
            };
        }

        private String state(Boolean value) {
            if (value == null) {
                return "WAITING";
            }
            return value ? "YES" : "NO";
        }
    }

    private final class FilterItem extends AbstractItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return ItemStackBuilder.of(Material.HOPPER)
                    .name(Component.text(
                            "Filter: " + filter.displayName(),
                            NamedTextColor.GOLD
                    ))
                    .lore(
                            Component.text(
                                    "Left/right click to cycle filters",
                                    NamedTextColor.YELLOW
                            ),
                            Component.text(
                                    "All / Warning+ / Critical / Latched / Unacknowledged",
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
            if (clickType.isLeftClick()) {
                changeFilter(1);
            } else if (clickType.isRightClick()) {
                changeFilter(-1);
            }
        }
    }

    private final class RefreshItem extends AbstractItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return ItemStackBuilder.of(Material.COMPASS)
                    .name(Component.text("Refresh alarms", NamedTextColor.GOLD))
                    .lore(Component.text(
                            "Prune disconnected sources and replay current alarm state",
                            NamedTextColor.YELLOW
                    ));
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            refreshFromBus();
        }
    }

    private final class SummaryItem extends AbstractItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            int tracked = alarms.size();
            int total = visibleAlarms.size();
            int latched = 0;
            int unacknowledged = 0;

            for (AlarmTelemetryState.Snapshot alarm : visibleAlarms) {
                if (alarm.isLatched()) {
                    latched++;
                    if (!alarm.isAcknowledged()) {
                        unacknowledged++;
                    }
                }
            }

            return ItemStackBuilder.of(Material.PAPER)
                    .name(Component.text("Alarm Summary", NamedTextColor.GOLD))
                    .lore(
                            Component.text("Tracked: " + tracked, NamedTextColor.WHITE),
                            Component.text(
                                    "Visible: " + total + " (" + filter.displayName() + ")",
                                    NamedTextColor.WHITE
                            ),
                            Component.text("Latched: " + latched, NamedTextColor.YELLOW),
                            Component.text(
                                    "Unacknowledged: " + unacknowledged,
                                    unacknowledged > 0
                                            ? NamedTextColor.RED
                                            : NamedTextColor.GREEN
                            ),
                            Component.text(
                                    total > SLOT_COUNT
                                            ? "Showing highest-priority " + SLOT_COUNT
                                            : "Showing all matching alarm sources",
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

    private final class AcknowledgeAllItem extends AbstractItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return ItemStackBuilder.of(Material.BELL)
                    .name(Component.text("Acknowledge All", NamedTextColor.GOLD))
                    .lore(Component.text(
                            "Acknowledge every alarm in this loaded Control Bus component",
                            NamedTextColor.YELLOW
                    ));
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            acknowledgeAll();
        }
    }
}
