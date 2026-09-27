package io.github.wickidcow.gridworks.content.block;

import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.block.interfaces.GuiRebarBlock;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.pylonmc.rebar.util.gui.GuiItems;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.api.control.ComparisonOperator;
import io.github.wickidcow.gridworks.api.control.ControlChannel;
import io.github.wickidcow.gridworks.api.control.ControlSignal;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.api.control.GridWorksChannels;
import io.github.wickidcow.gridworks.api.control.NumericControlRule;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemProvider;

public final class FactoryControllerBlock extends PhysicalControlNodeBlock implements GuiRebarBlock {
    private static final NamespacedKey CHANNEL_KEY = key("factory_controller_channel");
    private static final NamespacedKey OPERATOR_KEY = key("factory_controller_operator");
    private static final NamespacedKey THRESHOLD_KEY = key("factory_controller_threshold");
    private static final NamespacedKey SOURCE_KEY = key("factory_controller_source");
    private static final NamespacedKey OUTPUT_KEY = key("factory_controller_output");
    private static final NamespacedKey OUTPUT_KNOWN_KEY = key("factory_controller_output_known");

    private static final List<Metric> METRICS = List.of(
            new Metric("Inventory Items", GridWorksChannels.INVENTORY_ITEMS, Material.CHEST, 64.0, 1.0, 64.0, Double.MAX_VALUE),
            new Metric("Occupied Slots", GridWorksChannels.INVENTORY_OCCUPIED_SLOTS, Material.HOPPER, 1.0, 1.0, 9.0, Double.MAX_VALUE),
            new Metric("Inventory Fill", GridWorksChannels.INVENTORY_OCCUPIED_RATIO, Material.COMPARATOR, 0.75, 0.05, 0.25, 1.0),
            new Metric("Fluid Amount", GridWorksChannels.FLUID_AMOUNT, Material.WATER_BUCKET, 1000.0, 100.0, 1000.0, Double.MAX_VALUE),
            new Metric("Fluid Fill", GridWorksChannels.FLUID_FILL_RATIO, Material.LIGHT_BLUE_STAINED_GLASS, 0.75, 0.05, 0.25, 1.0)
    );

    private NumericControlRule rule;
    private UUID sourceId;
    private Double lastObserved;
    private boolean outputEnabled;
    private boolean outputKnown;

    private final ChannelItem channelItem = new ChannelItem();
    private final OperatorItem operatorItem = new OperatorItem();
    private final ThresholdItem thresholdItem = new ThresholdItem();
    private final SourceItem sourceItem = new SourceItem();
    private final OutputItem outputItem = new OutputItem();

    public FactoryControllerBlock(@NotNull org.bukkit.block.Block block, @NotNull BlockCreateContext context) {
        super(block, context);
        Metric metric = METRICS.get(2);
        this.rule = new NumericControlRule(
                metric.channel(),
                ComparisonOperator.GREATER_OR_EQUAL,
                metric.defaultThreshold()
        );
    }

    public FactoryControllerBlock(
            @NotNull org.bukkit.block.Block block,
            @NotNull PersistentDataContainer pdc
    ) {
        super(block, pdc);

        Metric metric = metricFromStored(pdc.get(CHANNEL_KEY, PersistentDataType.STRING));
        ComparisonOperator operator = operatorFromStored(
                pdc.get(OPERATOR_KEY, PersistentDataType.STRING)
        );
        Double storedThreshold = pdc.get(THRESHOLD_KEY, PersistentDataType.DOUBLE);
        double threshold = storedThreshold != null && Double.isFinite(storedThreshold)
                ? Math.clamp(storedThreshold, 0.0, metric.maxThreshold())
                : metric.defaultThreshold();

        this.rule = new NumericControlRule(metric.channel(), operator, threshold);
        this.sourceId = uuidFromStored(pdc.get(SOURCE_KEY, PersistentDataType.STRING));

        Byte storedOutput = pdc.get(OUTPUT_KEY, PersistentDataType.BYTE);
        Byte storedKnown = pdc.get(OUTPUT_KNOWN_KEY, PersistentDataType.BYTE);
        this.outputEnabled = storedOutput != null && storedOutput != 0;
        this.outputKnown = storedKnown != null && storedKnown != 0;
    }

    @Override
    public boolean accepts(@NotNull ControlChannel channel) {
        return rule.channel().equals(channel);
    }

    @Override
    protected void handleSignal(@NotNull ControlSignal signal) {
        var evaluation = rule.evaluate(signal);
        if (evaluation.isEmpty()) {
            return;
        }

        if (sourceId == null) {
            if (!GridWorks.getInstance().getPhysicalControlNetwork().isLinked(
                    getNodeId(),
                    signal.source()
            )) {
                return;
            }
            sourceId = signal.source();
            sourceItem.notifyWindows();
        } else if (!sourceId.equals(signal.source())) {
            return;
        }

        ControlValue.NumberValue numberValue = (ControlValue.NumberValue) signal.value();
        lastObserved = numberValue.value();
        setOutput(evaluation.orElseThrow(), false);
    }

    @Override
    public void onControlPeerAvailable(@NotNull UUID peerId) {
        if (!outputKnown || Objects.equals(sourceId, peerId)) {
            return;
        }
        publishOutput();
    }

    @Override
    protected void writeNodeData(@NotNull PersistentDataContainer pdc) {
        pdc.set(CHANNEL_KEY, PersistentDataType.STRING, rule.channel().toString());
        pdc.set(OPERATOR_KEY, PersistentDataType.STRING, rule.operator().name());
        pdc.set(THRESHOLD_KEY, PersistentDataType.DOUBLE, rule.threshold());

        if (sourceId == null) {
            pdc.remove(SOURCE_KEY);
        } else {
            pdc.set(SOURCE_KEY, PersistentDataType.STRING, sourceId.toString());
        }

        pdc.set(OUTPUT_KEY, PersistentDataType.BYTE, outputEnabled ? (byte) 1 : (byte) 0);
        pdc.set(OUTPUT_KNOWN_KEY, PersistentDataType.BYTE, outputKnown ? (byte) 1 : (byte) 0);
    }

    @Override
    public @NotNull Gui createGui() {
        return Gui.builder()
                .setStructure("c # o # t # s # x")
                .addIngredient('#', GuiItems.background())
                .addIngredient('c', channelItem)
                .addIngredient('o', operatorItem)
                .addIngredient('t', thresholdItem)
                .addIngredient('s', sourceItem)
                .addIngredient('x', outputItem)
                .build();
    }

    public @NotNull String describeRule() {
        String source = sourceId == null ? "AUTO" : shortId(sourceId);
        return rule.describe() + " [source " + source + "]";
    }

    public boolean isOutputEnabled() {
        return outputKnown && outputEnabled;
    }

    private void changeMetric(int direction) {
        int current = metricIndex(rule.channel());
        int next = Math.floorMod(current + direction, METRICS.size());
        Metric metric = METRICS.get(next);

        rule = new NumericControlRule(
                metric.channel(),
                rule.operator(),
                metric.defaultThreshold()
        );
        sourceId = null;
        lastObserved = null;
        setOutput(false, true);
        notifyConfigItems();
    }

    private void changeOperator(int direction) {
        ComparisonOperator[] operators = ComparisonOperator.values();
        int next = Math.floorMod(rule.operator().ordinal() + direction, operators.length);
        rule = new NumericControlRule(rule.channel(), operators[next], rule.threshold());
        reevaluate();
        notifyConfigItems();
    }

    private void changeThreshold(double delta) {
        Metric metric = metricFor(rule.channel());
        double next = Math.clamp(
                rule.threshold() + delta,
                0.0,
                metric.maxThreshold()
        );
        rule = new NumericControlRule(rule.channel(), rule.operator(), next);
        reevaluate();
        notifyConfigItems();
    }

    private void clearSource() {
        sourceId = null;
        lastObserved = null;
        setOutput(false, true);
        notifyConfigItems();
    }

    private void reevaluate() {
        if (lastObserved != null) {
            setOutput(rule.test(lastObserved), false);
        }
    }

    private void setOutput(boolean enabled, boolean forcePublish) {
        boolean changed = !outputKnown || outputEnabled != enabled;
        outputEnabled = enabled;
        outputKnown = true;

        if (changed || forcePublish) {
            publishOutput();
        }
        outputItem.notifyWindows();
    }

    private void publishOutput() {
        GridWorks.getInstance().getControlBus().publish(
                getNodeId(),
                GridWorksChannels.CONTROL_ENABLED,
                ControlValue.of(outputEnabled)
        );
    }

    private void notifyConfigItems() {
        channelItem.notifyWindows();
        operatorItem.notifyWindows();
        thresholdItem.notifyWindows();
        sourceItem.notifyWindows();
        outputItem.notifyWindows();
    }

    private static Metric metricFromStored(String stored) {
        if (stored != null) {
            try {
                return metricFor(ControlChannel.parse(stored));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return METRICS.get(2);
    }

    private static ComparisonOperator operatorFromStored(String stored) {
        if (stored != null) {
            try {
                return ComparisonOperator.valueOf(stored);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return ComparisonOperator.GREATER_OR_EQUAL;
    }

    private static UUID uuidFromStored(String stored) {
        if (stored == null) {
            return null;
        }
        try {
            return UUID.fromString(stored);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static Metric metricFor(ControlChannel channel) {
        return METRICS.stream()
                .filter(metric -> metric.channel().equals(channel))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported Factory Controller channel " + channel
                ));
    }

    private static int metricIndex(ControlChannel channel) {
        for (int i = 0; i < METRICS.size(); i++) {
            if (METRICS.get(i).channel().equals(channel)) {
                return i;
            }
        }
        return 0;
    }

    private static NamespacedKey key(String value) {
        return Objects.requireNonNull(NamespacedKey.fromString("gridworks:" + value));
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8).toUpperCase();
    }

    private abstract class ControllerItem extends AbstractItem {
        protected ItemStackBuilder item(Material material, String name) {
            return ItemStackBuilder.of(material)
                    .name(Component.text(name, NamedTextColor.GOLD));
        }
    }

    private final class ChannelItem extends ControllerItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            Metric metric = metricFor(rule.channel());
            return item(metric.material(), "Input: " + metric.name())
                    .lore(
                            Component.text(rule.channel().toString(), NamedTextColor.GRAY),
                            Component.text("Left click: next metric", NamedTextColor.YELLOW),
                            Component.text("Right click: previous metric", NamedTextColor.YELLOW)
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

    private final class OperatorItem extends ControllerItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(Material.COMPARATOR, "Comparison: " + rule.operator().symbol())
                    .lore(
                            Component.text(rule.operator().name(), NamedTextColor.GRAY),
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
                changeOperator(1);
            } else if (clickType.isRightClick()) {
                changeOperator(-1);
            }
        }
    }

    private final class ThresholdItem extends ControllerItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            Metric metric = metricFor(rule.channel());
            return item(Material.REPEATER, "Threshold: " + rule.threshold())
                    .lore(
                            Component.text(
                                    "Left +" + metric.step() + " / Right -" + metric.step(),
                                    NamedTextColor.YELLOW
                            ),
                            Component.text(
                                    "Shift uses " + metric.shiftStep(),
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
            Metric metric = metricFor(rule.channel());
            double step = clickType.isShiftClick() ? metric.shiftStep() : metric.step();

            if (clickType.isLeftClick()) {
                changeThreshold(step);
            } else if (clickType.isRightClick()) {
                changeThreshold(-step);
            }
        }
    }

    private final class SourceItem extends ControllerItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            String source = sourceId == null ? "AUTO" : shortId(sourceId);
            return item(Material.TARGET, "Source: " + source)
                    .lore(
                            Component.text(
                                    "AUTO binds to the first matching directly linked sensor",
                                    NamedTextColor.GRAY
                            ),
                            Component.text("Click to clear binding", NamedTextColor.YELLOW)
                    );
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            clearSource();
        }
    }

    private final class OutputItem extends ControllerItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            String output = !outputKnown ? "WAITING" : (outputEnabled ? "ON" : "OFF");
            NamedTextColor color = !outputKnown
                    ? NamedTextColor.YELLOW
                    : (outputEnabled ? NamedTextColor.GREEN : NamedTextColor.RED);

            ItemStackBuilder builder = ItemStackBuilder.of(
                            outputEnabled ? Material.LIME_DYE : Material.GRAY_DYE
                    )
                    .name(Component.text("Output: " + output, color))
                    .lore(Component.text(
                            "Publishes gridworks:control/enabled",
                            NamedTextColor.GRAY
                    ));

            if (lastObserved != null) {
                builder.lore(Component.text(
                        "Last input: " + lastObserved,
                        NamedTextColor.GRAY
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
        }
    }

    private record Metric(
            String name,
            ControlChannel channel,
            Material material,
            double defaultThreshold,
            double step,
            double shiftStep,
            double maxThreshold
    ) {
    }
}
