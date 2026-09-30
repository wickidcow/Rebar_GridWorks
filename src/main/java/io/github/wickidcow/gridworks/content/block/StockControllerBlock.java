package io.github.wickidcow.gridworks.content.block;

import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.block.interfaces.GuiRebarBlock;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.pylonmc.rebar.util.gui.GuiItems;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.api.control.ControlAddress;
import io.github.wickidcow.gridworks.api.control.ControlChannel;
import io.github.wickidcow.gridworks.api.control.ControlCommandChannel;
import io.github.wickidcow.gridworks.api.control.ControlOutputMode;
import io.github.wickidcow.gridworks.api.control.ControlSignal;
import io.github.wickidcow.gridworks.api.control.ControlStateSource;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.api.control.GridWorksChannels;
import io.github.wickidcow.gridworks.inventory.StockHysteresis;
import io.github.wickidcow.gridworks.inventory.StockMetric;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
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
import xyz.xenondevs.invui.window.AnvilWindow;

public final class StockControllerBlock extends PhysicalControlNodeBlock
        implements GuiRebarBlock, ControlStateSource {

    private static final NamespacedKey METRIC_KEY = key("stock_controller_metric");
    private static final NamespacedKey LOW_THRESHOLD_KEY =
            key("stock_controller_low_threshold");
    private static final NamespacedKey HIGH_THRESHOLD_KEY =
            key("stock_controller_high_threshold");
    private static final NamespacedKey SOURCE_KEY = key("stock_controller_source");
    private static final NamespacedKey OUTPUT_CIRCUIT_KEY =
            key("stock_controller_output_circuit");
    private static final NamespacedKey OUTPUT_MODE_KEY =
            key("stock_controller_output_mode");
    private static final NamespacedKey OUTPUT_ADDRESS_KEY =
            key("stock_controller_output_address");
    private static final NamespacedKey DEMAND_KEY = key("stock_controller_demand");

    private StockMetric metric;
    private double lowThreshold;
    private double highThreshold;
    private UUID sourceId;
    private ControlCommandChannel outputCircuit;
    private ControlOutputMode outputMode;
    private ControlAddress outputAddress;

    /**
     * Persistent hysteresis latch. This is intentionally retained across
     * unload/reload so a value in the deadband does not lose its prior state.
     * It is never published until fresh/current source telemetry is known.
     */
    private boolean demand;
    private boolean outputKnown;
    private Double currentValue;

    private final MetricItem metricItem = new MetricItem();
    private final ThresholdItem lowThresholdItem = new ThresholdItem(true);
    private final ThresholdItem highThresholdItem = new ThresholdItem(false);
    private final SourceItem sourceItem = new SourceItem();
    private final OutputModeItem outputModeItem = new OutputModeItem();
    private final OutputCircuitItem outputCircuitItem = new OutputCircuitItem();
    private final OutputAddressItem outputAddressItem = new OutputAddressItem();
    private final StatusItem statusItem = new StatusItem();

    public StockControllerBlock(
            @NotNull Block block,
            @NotNull BlockCreateContext context
    ) {
        super(block, context);
        metric = StockMetric.ITEM_COUNT;
        lowThreshold = metric.defaultLow();
        highThreshold = metric.defaultHigh();
        outputCircuit = ControlCommandChannel.DEFAULT;
        outputMode = ControlOutputMode.CIRCUIT;
        outputAddress = ControlAddress.defaultFor(getNodeId(), "stock");
    }

    public StockControllerBlock(
            @NotNull Block block,
            @NotNull PersistentDataContainer pdc
    ) {
        super(block, pdc);

        metric = StockMetric.fromStored(
                pdc.get(METRIC_KEY, PersistentDataType.STRING)
        );

        Double storedLow = pdc.get(LOW_THRESHOLD_KEY, PersistentDataType.DOUBLE);
        Double storedHigh = pdc.get(HIGH_THRESHOLD_KEY, PersistentDataType.DOUBLE);
        lowThreshold = storedLow == null ? metric.defaultLow() : storedLow;
        highThreshold = storedHigh == null ? metric.defaultHigh() : storedHigh;
        if (!thresholdsValid(lowThreshold, highThreshold, metric.max())) {
            lowThreshold = metric.defaultLow();
            highThreshold = metric.defaultHigh();
        }

        sourceId = uuidFromStored(pdc.get(SOURCE_KEY, PersistentDataType.STRING));
        outputCircuit = ControlCommandChannel.fromStored(
                pdc.get(OUTPUT_CIRCUIT_KEY, PersistentDataType.STRING)
        );
        outputMode = ControlOutputMode.fromStored(
                pdc.get(OUTPUT_MODE_KEY, PersistentDataType.STRING)
        );
        outputAddress = ControlAddress.fromStoredOrDefault(
                pdc.get(OUTPUT_ADDRESS_KEY, PersistentDataType.STRING),
                ControlAddress.defaultFor(getNodeId(), "stock")
        );

        Byte storedDemand = pdc.get(DEMAND_KEY, PersistentDataType.BYTE);
        demand = storedDemand != null && storedDemand != 0;
    }

    @Override
    protected void beforeActivated() {
        currentValue = null;
        outputKnown = false;
    }

    @Override
    public boolean accepts(@NotNull ControlChannel channel) {
        return metric.channel().equals(channel)
                || GridWorksChannels.INVENTORY_AVAILABLE.equals(channel);
    }

    @Override
    protected void handleSignal(@NotNull ControlSignal signal) {
        runOnServerThreadIfActive(() -> handleSignalOnServerThread(signal));
    }

    private void handleSignalOnServerThread(ControlSignal signal) {
        if (GridWorksChannels.INVENTORY_AVAILABLE.equals(signal.channel())) {
            handleAvailability(signal);
            return;
        }

        if (!metric.channel().equals(signal.channel())
                || !(signal.value() instanceof ControlValue.NumberValue numberValue)) {
            return;
        }

        if (!selectOrMatchSource(signal.source())) {
            return;
        }

        double value = numberValue.value();
        if (value < 0.0 || value > metric.max()) {
            setUnknown(true);
            return;
        }

        boolean nextDemand = StockHysteresis.nextDemand(
                demand,
                value,
                lowThreshold,
                highThreshold
        );
        boolean changed = !outputKnown || demand != nextDemand;
        currentValue = value;
        demand = nextDemand;
        outputKnown = true;

        if (changed) {
            publishOutput(demand);
        }
        notifyItems();
    }

    private void handleAvailability(ControlSignal signal) {
        if (!(signal.value() instanceof ControlValue.BooleanValue booleanValue)
                || booleanValue.value()) {
            return;
        }

        if (sourceId != null && sourceId.equals(signal.source())) {
            setUnknown(true);
        }
    }

    private boolean selectOrMatchSource(UUID signalSource) {
        if (!GridWorks.getInstance()
                .getPhysicalControlNetwork()
                .isLinked(getNodeId(), signalSource)) {
            return false;
        }

        if (sourceId == null) {
            sourceId = signalSource;
            sourceItem.notifyWindows();
            return true;
        }

        return sourceId.equals(signalSource);
    }

    @Override
    public void onControlPeerUnavailable(@NotNull UUID peerId) {
        if (sourceId == null || !sourceId.equals(peerId)) {
            return;
        }

        runOnServerThreadIfActive(() -> setUnknown(true));
    }

    @Override
    public void publishCurrentState() {
        publishOutput(outputKnown && demand);
    }

    @Override
    protected void writeNodeData(@NotNull PersistentDataContainer pdc) {
        pdc.set(METRIC_KEY, PersistentDataType.STRING, metric.name());
        pdc.set(LOW_THRESHOLD_KEY, PersistentDataType.DOUBLE, lowThreshold);
        pdc.set(HIGH_THRESHOLD_KEY, PersistentDataType.DOUBLE, highThreshold);

        if (sourceId == null) {
            pdc.remove(SOURCE_KEY);
        } else {
            pdc.set(SOURCE_KEY, PersistentDataType.STRING, sourceId.toString());
        }

        pdc.set(
                OUTPUT_CIRCUIT_KEY,
                PersistentDataType.STRING,
                outputCircuit.name()
        );
        pdc.set(OUTPUT_MODE_KEY, PersistentDataType.STRING, outputMode.name());
        pdc.set(
                OUTPUT_ADDRESS_KEY,
                PersistentDataType.STRING,
                outputAddress.value()
        );
        pdc.set(
                DEMAND_KEY,
                PersistentDataType.BYTE,
                demand ? (byte) 1 : (byte) 0
        );
    }

    @Override
    public @NotNull Gui createGui() {
        return Gui.builder()
                .setStructure("m l h s # o c a x")
                .addIngredient('#', GuiItems.background())
                .addIngredient('m', metricItem)
                .addIngredient('l', lowThresholdItem)
                .addIngredient('h', highThresholdItem)
                .addIngredient('s', sourceItem)
                .addIngredient('o', outputModeItem)
                .addIngredient('c', outputCircuitItem)
                .addIngredient('a', outputAddressItem)
                .addIngredient('x', statusItem)
                .build();
    }

    @Override
    public @NotNull Component getGuiTitle() {
        return Component.text("Stock Controller", NamedTextColor.GOLD);
    }

    public boolean isDemandingStock() {
        return outputKnown && demand;
    }

    public boolean isOutputKnown() {
        return outputKnown;
    }

    public double getLowThreshold() {
        return lowThreshold;
    }

    public double getHighThreshold() {
        return highThreshold;
    }

    public @NotNull StockMetric getMetric() {
        return metric;
    }

    public @NotNull ControlChannel getOutputChannel() {
        return currentOutputChannel();
    }

    private void setUnknown(boolean forcePublish) {
        boolean wasKnown = outputKnown;
        outputKnown = false;
        currentValue = null;

        if (wasKnown || forcePublish) {
            publishOutput(false);
        }
        notifyItems();
    }

    private void changeMetric(int direction) {
        metric = metric.cycle(direction);
        lowThreshold = metric.defaultLow();
        highThreshold = metric.defaultHigh();
        sourceId = null;
        demand = false;
        setUnknown(true);
    }

    private void changeThreshold(boolean low, double delta) {
        if (low) {
            lowThreshold = StockHysteresis.clampLow(
                    lowThreshold + delta,
                    highThreshold,
                    metric.step()
            );
        } else {
            highThreshold = StockHysteresis.clampHigh(
                    highThreshold + delta,
                    lowThreshold,
                    metric.step(),
                    metric.max()
            );
        }

        if (currentValue != null) {
            boolean nextDemand = StockHysteresis.nextDemand(
                    demand,
                    currentValue,
                    lowThreshold,
                    highThreshold
            );
            boolean changed = !outputKnown || demand != nextDemand;
            demand = nextDemand;
            outputKnown = true;
            if (changed) {
                publishOutput(demand);
            }
        }

        notifyItems();
    }

    private void cycleSource(int direction) {
        List<UUID> choices = GridWorks.getInstance()
                .getPhysicalControlNetwork()
                .activeLinkedNodes(getNodeId());

        UUID next;
        if (choices.isEmpty()) {
            next = null;
        } else if (sourceId == null) {
            next = direction >= 0 ? choices.getFirst() : choices.getLast();
        } else {
            int index = choices.indexOf(sourceId);
            if (index < 0) {
                next = null;
            } else {
                int candidate = index + (direction >= 0 ? 1 : -1);
                next = candidate < 0 || candidate >= choices.size()
                        ? null
                        : choices.get(candidate);
            }
        }

        sourceId = next;
        demand = false;
        setUnknown(true);

        if (GridWorks.getInstance().getPhysicalControlNetwork().isActive(getNodeId())) {
            GridWorks.getInstance()
                    .getPhysicalControlNetwork()
                    .replayStateSources(getNodeId());
        }
    }

    private ControlChannel currentOutputChannel() {
        return outputMode == ControlOutputMode.ADDRESS
                ? outputAddress.channel()
                : outputCircuit.channel();
    }

    private void publishOutput(boolean enabled) {
        GridWorks.getInstance().getControlBus().publish(
                getNodeId(),
                currentOutputChannel(),
                ControlValue.of(enabled)
        );
    }

    private void clearOutputChannel(ControlChannel channel) {
        GridWorks.getInstance().getControlBus().publish(
                getNodeId(),
                channel,
                ControlValue.of(false)
        );
    }

    private void toggleOutputMode() {
        ControlChannel previous = currentOutputChannel();
        clearOutputChannel(previous);
        outputMode = outputMode.toggle();
        publishOutput(outputKnown && demand);
        notifyItems();
    }

    private void changeOutputCircuit(int direction) {
        if (outputMode != ControlOutputMode.CIRCUIT) {
            return;
        }

        ControlCommandChannel previous = outputCircuit;
        outputCircuit = outputCircuit.cycle(direction);
        if (previous == outputCircuit) {
            return;
        }

        clearOutputChannel(previous.channel());
        publishOutput(outputKnown && demand);
        notifyItems();
    }

    private void setOutputAddress(ControlAddress next) {
        if (outputAddress.equals(next)) {
            return;
        }

        if (outputMode == ControlOutputMode.ADDRESS) {
            clearOutputChannel(outputAddress.channel());
        }
        outputAddress = next;
        if (outputMode == ControlOutputMode.ADDRESS) {
            publishOutput(outputKnown && demand);
        }
        notifyItems();
    }

    private void openOutputAddressWindow(Player player) {
        final boolean[] firstRename = {true};

        Gui upperGui = Gui.builder()
                .setStructure("# a #")
                .addIngredient('#', GuiItems.background())
                .addIngredient(
                        'a',
                        ItemStackBuilder.of(Material.NAME_TAG)
                                .name(Component.text(
                                        outputAddress.value(),
                                        NamedTextColor.GOLD
                                ))
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
                                .name(Component.text(
                                        "Set Stock Output Address",
                                        NamedTextColor.GOLD
                                ))
                                .lore(Component.text(
                                        "Example: iron_smelter_enable",
                                        NamedTextColor.GRAY
                                ))
                )
                .build();

        try {
            AnvilWindow window = AnvilWindow.builder()
                    .setViewer(player)
                    .setUpperGui(upperGui)
                    .setLowerGui(lowerGui)
                    .setTitle(Component.text("Stock Output Address"))
                    .addRenameHandler(raw -> {
                        if (firstRename[0]) {
                            firstRename[0] = false;
                            return;
                        }

                        try {
                            setOutputAddress(ControlAddress.fromUserInput(raw));
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
                    "Could not open Stock Controller address window",
                    exception
            );
            player.sendMessage(Component.text(
                    "GridWorks could not open the output address window.",
                    NamedTextColor.RED
            ));
        }
    }

    private void notifyItems() {
        metricItem.notifyWindows();
        lowThresholdItem.notifyWindows();
        highThresholdItem.notifyWindows();
        sourceItem.notifyWindows();
        outputModeItem.notifyWindows();
        outputCircuitItem.notifyWindows();
        outputAddressItem.notifyWindows();
        statusItem.notifyWindows();
    }

    private abstract class ControllerItem extends AbstractItem {
        protected ItemStackBuilder item(Material material, String name) {
            return ItemStackBuilder.of(material)
                    .name(Component.text(name, NamedTextColor.GOLD));
        }
    }

    private final class MetricItem extends ControllerItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(
                    metric == StockMetric.ITEM_COUNT ? Material.CHEST : Material.HOPPER,
                    "Metric: " + metric.displayName()
            ).lore(
                    Component.text(
                            "Left/right click to change metric",
                            NamedTextColor.YELLOW
                    ),
                    Component.text(
                            "Output means: production needed",
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
                changeMetric(1);
            } else if (clickType.isRightClick()) {
                changeMetric(-1);
            }
        }
    }

    private final class ThresholdItem extends ControllerItem {
        private final boolean low;

        private ThresholdItem(boolean low) {
            this.low = low;
        }

        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            double value = low ? lowThreshold : highThreshold;
            return item(
                    low ? Material.REDSTONE_TORCH : Material.LIME_DYE,
                    (low ? "Start at/below: " : "Stop at/above: ")
                            + formatMetric(value)
            ).lore(
                    Component.text(
                            "Left +" + formatMetric(metric.step())
                                    + " / Right -" + formatMetric(metric.step()),
                            NamedTextColor.YELLOW
                    ),
                    Component.text(
                            "Shift uses " + formatMetric(metric.shiftStep()),
                            NamedTextColor.YELLOW
                    ),
                    Component.text(
                            "Low/high remain separated for hysteresis",
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
            double step = clickType.isShiftClick()
                    ? metric.shiftStep()
                    : metric.step();
            if (clickType.isLeftClick()) {
                changeThreshold(low, step);
            } else if (clickType.isRightClick()) {
                changeThreshold(low, -step);
            }
        }
    }

    private final class SourceItem extends ControllerItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(
                    Material.TARGET,
                    "Inventory source: "
                            + (sourceId == null ? "AUTO" : shortId(sourceId))
            ).lore(
                    Component.text(
                            "AUTO binds to the first matching direct Inventory Sensor",
                            NamedTextColor.GRAY
                    ),
                    Component.text(
                            "Left/right: cycle loaded direct links",
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
            if (clickType.isLeftClick()) {
                cycleSource(1);
            } else if (clickType.isRightClick()) {
                cycleSource(-1);
            }
        }
    }

    private final class OutputModeItem extends ControllerItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(
                    outputMode == ControlOutputMode.ADDRESS
                            ? Material.ENDER_EYE
                            : Material.REDSTONE,
                    "Output mode: " + outputMode.displayName()
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
            toggleOutputMode();
        }
    }

    private final class OutputCircuitItem extends ControllerItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            ItemStackBuilder builder = item(
                    outputMode == ControlOutputMode.CIRCUIT
                            ? Material.REDSTONE
                            : Material.GRAY_DYE,
                    "Output circuit: " + outputCircuit.displayName()
            );

            if (outputMode == ControlOutputMode.CIRCUIT) {
                builder.lore(Component.text(
                        "Left/right: cycle Default / A / B / C / D",
                        NamedTextColor.YELLOW
                ));
            } else {
                builder.lore(Component.text(
                        "Switch output mode to Circuit to edit",
                        NamedTextColor.DARK_GRAY
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
            if (outputMode != ControlOutputMode.CIRCUIT) {
                return;
            }
            if (clickType.isLeftClick()) {
                changeOutputCircuit(1);
            } else if (clickType.isRightClick()) {
                changeOutputCircuit(-1);
            }
        }
    }

    private final class OutputAddressItem extends ControllerItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            ItemStackBuilder builder = item(
                    outputMode == ControlOutputMode.ADDRESS
                            ? Material.NAME_TAG
                            : Material.GRAY_DYE,
                    "Output address: " + outputAddress.value()
            ).lore(Component.text(
                    outputAddress.channel().toString(),
                    NamedTextColor.GRAY
            ));

            if (outputMode == ControlOutputMode.ADDRESS) {
                builder.lore(Component.text(
                        "Click to edit addressed output",
                        NamedTextColor.YELLOW
                ));
            } else {
                builder.lore(Component.text(
                        "Switch output mode to Address to edit",
                        NamedTextColor.DARK_GRAY
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
            if (outputMode != ControlOutputMode.ADDRESS) {
                return;
            }

            player.closeInventory();
            GridWorks.getInstance().getServer().getScheduler().runTask(
                    GridWorks.getInstance(),
                    () -> openOutputAddressWindow(player)
            );
        }
    }

    private final class StatusItem extends ControllerItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            String state = !outputKnown
                    ? "WAITING"
                    : (demand ? "NEEDS STOCK" : "STOCKED");
            NamedTextColor color = !outputKnown
                    ? NamedTextColor.YELLOW
                    : (demand ? NamedTextColor.GREEN : NamedTextColor.AQUA);

            ItemStackBuilder builder = ItemStackBuilder.of(
                            !outputKnown
                                    ? Material.GRAY_DYE
                                    : (demand ? Material.LIME_DYE : Material.CYAN_DYE)
                    )
                    .name(Component.text("State: " + state, color))
                    .lore(
                            Component.text(
                                    "ON starts/keeps production below the low band",
                                    NamedTextColor.GRAY
                            ),
                            Component.text(
                                    "OFF stops production at the high band",
                                    NamedTextColor.GRAY
                            )
                    );

            if (currentValue != null) {
                builder.lore(Component.text(
                        "Current: " + formatMetric(currentValue),
                        NamedTextColor.WHITE
                ));
            } else {
                builder.lore(Component.text(
                        "Missing telemetry is fail-safe OFF",
                        NamedTextColor.YELLOW
                ));
            }

            builder.lore(Component.text(
                    "Output: " + currentOutputChannel(),
                    NamedTextColor.DARK_GRAY
            ));
            return builder;
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
        }
    }

    private String formatMetric(double value) {
        if (metric == StockMetric.OCCUPIED_RATIO) {
            return String.format(Locale.ROOT, "%.0f%%", value * 100.0);
        }

        if (value <= Long.MAX_VALUE && value == Math.rint(value)) {
            return String.format(Locale.ROOT, "%.0f", value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static boolean thresholdsValid(double low, double high, double max) {
        try {
            StockHysteresis.validateThresholds(low, high);
            return high <= max;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static UUID uuidFromStored(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private static NamespacedKey key(String value) {
        return Objects.requireNonNull(
                NamespacedKey.fromString("gridworks:" + value)
        );
    }
}
