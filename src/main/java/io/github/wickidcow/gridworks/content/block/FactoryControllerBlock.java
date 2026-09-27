package io.github.wickidcow.gridworks.content.block;

import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.block.interfaces.GuiRebarBlock;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.pylonmc.rebar.util.gui.GuiItems;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.api.control.ComparisonOperator;
import io.github.wickidcow.gridworks.api.control.ControlAddress;
import io.github.wickidcow.gridworks.api.control.ControlChannel;
import io.github.wickidcow.gridworks.api.control.ControlCommandChannel;
import io.github.wickidcow.gridworks.api.control.ControlOutputMode;
import io.github.wickidcow.gridworks.api.control.ControlSignal;
import io.github.wickidcow.gridworks.api.control.ControlStateSource;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.api.control.GridWorksChannels;
import io.github.wickidcow.gridworks.api.control.LogicOperator;
import io.github.wickidcow.gridworks.api.control.NumericControlRule;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

public final class FactoryControllerBlock extends PhysicalControlNodeBlock
        implements GuiRebarBlock, ControlStateSource {
    // Condition A intentionally keeps the original persistent keys for compatibility.
    private static final NamespacedKey CHANNEL_A_KEY = key("factory_controller_channel");
    private static final NamespacedKey OPERATOR_A_KEY = key("factory_controller_operator");
    private static final NamespacedKey THRESHOLD_A_KEY = key("factory_controller_threshold");
    private static final NamespacedKey SOURCE_A_KEY = key("factory_controller_source");

    private static final NamespacedKey CONDITION_B_ENABLED_KEY =
            key("factory_controller_condition_b_enabled");
    private static final NamespacedKey CHANNEL_B_KEY =
            key("factory_controller_channel_b");
    private static final NamespacedKey OPERATOR_B_KEY =
            key("factory_controller_operator_b");
    private static final NamespacedKey THRESHOLD_B_KEY =
            key("factory_controller_threshold_b");
    private static final NamespacedKey SOURCE_B_KEY =
            key("factory_controller_source_b");
    private static final NamespacedKey LOGIC_OPERATOR_KEY =
            key("factory_controller_logic_operator");

    private static final NamespacedKey NAME_KEY = key("factory_controller_name");
    private static final NamespacedKey PRESET_KEY = key("factory_controller_preset");
    private static final NamespacedKey OUTPUT_CIRCUIT_KEY =
            key("factory_controller_output_circuit");
    private static final NamespacedKey OUTPUT_MODE_KEY =
            key("factory_controller_output_mode");
    private static final NamespacedKey OUTPUT_ADDRESS_KEY =
            key("factory_controller_output_address");
    private static final NamespacedKey OUTPUT_KEY = key("factory_controller_output");
    private static final NamespacedKey OUTPUT_KNOWN_KEY = key("factory_controller_output_known");

    private static final List<Metric> METRICS = List.of(
            new Metric(
                    "Inventory Items",
                    GridWorksChannels.INVENTORY_ITEMS,
                    Material.CHEST,
                    64.0,
                    1.0,
                    64.0,
                    Double.MAX_VALUE
            ),
            new Metric(
                    "Occupied Slots",
                    GridWorksChannels.INVENTORY_OCCUPIED_SLOTS,
                    Material.HOPPER,
                    1.0,
                    1.0,
                    9.0,
                    Double.MAX_VALUE
            ),
            new Metric(
                    "Redstone Strength",
                    GridWorksChannels.REDSTONE_STRENGTH,
                    Material.REDSTONE,
                    8.0,
                    1.0,
                    5.0,
                    15.0
            ),
            new Metric(
                    "Inventory Fill",
                    GridWorksChannels.INVENTORY_OCCUPIED_RATIO,
                    Material.COMPARATOR,
                    0.75,
                    0.05,
                    0.25,
                    1.0
            ),
            new Metric(
                    "Fluid Amount",
                    GridWorksChannels.FLUID_AMOUNT,
                    Material.WATER_BUCKET,
                    1000.0,
                    100.0,
                    1000.0,
                    Double.MAX_VALUE
            ),
            new Metric(
                    "Fluid Fill",
                    GridWorksChannels.FLUID_FILL_RATIO,
                    Material.LIGHT_BLUE_STAINED_GLASS,
                    0.75,
                    0.05,
                    0.25,
                    1.0
            ),
            new Metric(
                    "Power Capacity",
                    GridWorksChannels.POWER_PRODUCTION_CAPACITY_WATTS,
                    Material.REDSTONE_BLOCK,
                    1000.0,
                    100.0,
                    1000.0,
                    Double.MAX_VALUE
            ),
            new Metric(
                    "Power Demand",
                    GridWorksChannels.POWER_DEMAND_WATTS,
                    Material.COMPARATOR,
                    1000.0,
                    100.0,
                    1000.0,
                    Double.MAX_VALUE
            ),
            new Metric(
                    "Power Reserve",
                    GridWorksChannels.POWER_RESERVE_WATTS,
                    Material.COPPER_BLOCK,
                    250.0,
                    50.0,
                    250.0,
                    Double.MAX_VALUE
            ),
            new Metric(
                    "Power Load",
                    GridWorksChannels.POWER_LOAD_RATIO,
                    Material.REPEATER,
                    0.90,
                    0.05,
                    0.25,
                    10.0
            ),
            new Metric(
                    "Consumers Powered",
                    GridWorksChannels.POWER_POWERED_CONSUMER_RATIO,
                    Material.LIME_DYE,
                    1.0,
                    0.05,
                    0.25,
                    1.0
            ),
            new Metric(
                    "Unpowered Consumers",
                    GridWorksChannels.POWER_UNPOWERED_CONSUMERS,
                    Material.RED_DYE,
                    1.0,
                    1.0,
                    5.0,
                    Double.MAX_VALUE
            )
    );

    private static final String DEFAULT_NAME = "Factory Controller";
    private static final int MAX_NAME_LENGTH = 32;

    private final Condition conditionA;
    private final Condition conditionB;
    private boolean conditionBEnabled;
    private LogicOperator logicOperator;
    private String controllerName;
    private ControllerPreset preset;
    private ControlCommandChannel outputCircuit;
    private ControlOutputMode outputMode;
    private ControlAddress outputAddress;
    private boolean outputEnabled;
    private boolean outputKnown;

    private final MetricItem metricAItem = new MetricItem(0);
    private final OperatorItem operatorAItem = new OperatorItem(0);
    private final ThresholdItem thresholdAItem = new ThresholdItem(0);
    private final SourceItem sourceAItem = new SourceItem(0);

    private final NameItem nameItem = new NameItem();
    private final PresetItem presetItem = new PresetItem();
    private final ConditionToggleItem conditionBToggleItem = new ConditionToggleItem();
    private final LogicItem logicItem = new LogicItem();
    private final MetricItem metricBItem = new MetricItem(1);
    private final OperatorItem operatorBItem = new OperatorItem(1);
    private final ThresholdItem thresholdBItem = new ThresholdItem(1);
    private final SourceItem sourceBItem = new SourceItem(1);
    private final OutputModeItem outputModeItem = new OutputModeItem();
    private final OutputCircuitItem outputCircuitItem = new OutputCircuitItem();
    private final OutputAddressItem outputAddressItem = new OutputAddressItem();
    private final OutputItem outputItem = new OutputItem();

    public FactoryControllerBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);

        Metric defaultMetric = METRICS.get(2);
        this.conditionA = new Condition(
                new NumericControlRule(
                        defaultMetric.channel(),
                        ComparisonOperator.GREATER_OR_EQUAL,
                        defaultMetric.defaultThreshold()
                ),
                null
        );
        this.conditionB = new Condition(
                new NumericControlRule(
                        defaultMetric.channel(),
                        ComparisonOperator.GREATER_OR_EQUAL,
                        defaultMetric.defaultThreshold()
                ),
                null
        );
        this.conditionBEnabled = false;
        this.logicOperator = LogicOperator.AND;
        this.controllerName = DEFAULT_NAME;
        this.preset = ControllerPreset.CUSTOM;
        this.outputCircuit = ControlCommandChannel.DEFAULT;
        this.outputMode = ControlOutputMode.CIRCUIT;
        this.outputAddress = ControlAddress.defaultFor(getNodeId(), "controller");
    }

    public FactoryControllerBlock(
            @NotNull Block block,
            @NotNull PersistentDataContainer pdc
    ) {
        super(block, pdc);

        this.conditionA = loadCondition(
                pdc,
                CHANNEL_A_KEY,
                OPERATOR_A_KEY,
                THRESHOLD_A_KEY,
                SOURCE_A_KEY
        );
        this.conditionB = loadCondition(
                pdc,
                CHANNEL_B_KEY,
                OPERATOR_B_KEY,
                THRESHOLD_B_KEY,
                SOURCE_B_KEY
        );

        Byte storedBEnabled = pdc.get(CONDITION_B_ENABLED_KEY, PersistentDataType.BYTE);
        this.conditionBEnabled = storedBEnabled != null && storedBEnabled != 0;

        String storedLogic = pdc.get(LOGIC_OPERATOR_KEY, PersistentDataType.STRING);
        this.logicOperator = parseLogicOperator(storedLogic);
        this.controllerName = normalizeName(pdc.get(NAME_KEY, PersistentDataType.STRING));
        this.preset = parsePreset(pdc.get(PRESET_KEY, PersistentDataType.STRING));
        this.outputCircuit = ControlCommandChannel.fromStored(
                pdc.get(OUTPUT_CIRCUIT_KEY, PersistentDataType.STRING)
        );
        this.outputMode = ControlOutputMode.fromStored(
                pdc.get(OUTPUT_MODE_KEY, PersistentDataType.STRING)
        );
        this.outputAddress = ControlAddress.fromStoredOrDefault(
                pdc.get(OUTPUT_ADDRESS_KEY, PersistentDataType.STRING),
                ControlAddress.defaultFor(getNodeId(), "controller")
        );

        Byte storedOutput = pdc.get(OUTPUT_KEY, PersistentDataType.BYTE);
        Byte storedKnown = pdc.get(OUTPUT_KNOWN_KEY, PersistentDataType.BYTE);
        this.outputEnabled = storedOutput != null && storedOutput != 0;
        this.outputKnown = storedKnown != null && storedKnown != 0;
    }

    @Override
    public boolean accepts(@NotNull ControlChannel channel) {
        return conditionA.rule.channel().equals(channel)
                || (conditionBEnabled && conditionB.rule.channel().equals(channel));
    }

    @Override
    protected void handleSignal(@NotNull ControlSignal signal) {
        runOnServerThreadIfActive(() -> handleSignalOnServerThread(signal));
    }

    private void handleSignalOnServerThread(@NotNull ControlSignal signal) {
        boolean changed = acceptForCondition(conditionA, signal, sourceAItem);

        if (conditionBEnabled) {
            changed |= acceptForCondition(conditionB, signal, sourceBItem);
        }

        if (changed) {
            updateOutputFromConditions();
            notifyConditionItems();
        }
    }

    @Override
    public void publishCurrentState() {
        // WAITING is represented as fail-safe OFF on the wire.
        publishOutput(outputEnabled);
    }

    @Override
    protected void writeNodeData(@NotNull PersistentDataContainer pdc) {
        writeCondition(
                pdc,
                conditionA,
                CHANNEL_A_KEY,
                OPERATOR_A_KEY,
                THRESHOLD_A_KEY,
                SOURCE_A_KEY
        );
        writeCondition(
                pdc,
                conditionB,
                CHANNEL_B_KEY,
                OPERATOR_B_KEY,
                THRESHOLD_B_KEY,
                SOURCE_B_KEY
        );

        pdc.set(
                CONDITION_B_ENABLED_KEY,
                PersistentDataType.BYTE,
                conditionBEnabled ? (byte) 1 : (byte) 0
        );
        pdc.set(
                LOGIC_OPERATOR_KEY,
                PersistentDataType.STRING,
                logicOperator.name()
        );
        pdc.set(NAME_KEY, PersistentDataType.STRING, controllerName);
        pdc.set(PRESET_KEY, PersistentDataType.STRING, preset.name());
        pdc.set(OUTPUT_CIRCUIT_KEY, PersistentDataType.STRING, outputCircuit.name());
        pdc.set(OUTPUT_MODE_KEY, PersistentDataType.STRING, outputMode.name());
        pdc.set(OUTPUT_ADDRESS_KEY, PersistentDataType.STRING, outputAddress.value());
        pdc.set(
                OUTPUT_KEY,
                PersistentDataType.BYTE,
                outputEnabled ? (byte) 1 : (byte) 0
        );
        pdc.set(
                OUTPUT_KNOWN_KEY,
                PersistentDataType.BYTE,
                outputKnown ? (byte) 1 : (byte) 0
        );
    }

    @Override
    public @NotNull Gui createGui() {
        return Gui.builder()
                .setStructure(
                        "a o t s # m c d x",
                        "n z # # l # # # #",
                        "e b p q r # # # #"
                )
                .addIngredient('#', GuiItems.background())
                .addIngredient('a', metricAItem)
                .addIngredient('o', operatorAItem)
                .addIngredient('t', thresholdAItem)
                .addIngredient('s', sourceAItem)
                .addIngredient('m', outputModeItem)
                .addIngredient('c', outputCircuitItem)
                .addIngredient('d', outputAddressItem)
                .addIngredient('n', nameItem)
                .addIngredient('z', presetItem)
                .addIngredient('l', logicItem)
                .addIngredient('e', conditionBToggleItem)
                .addIngredient('b', metricBItem)
                .addIngredient('p', operatorBItem)
                .addIngredient('q', thresholdBItem)
                .addIngredient('r', sourceBItem)
                .addIngredient('x', outputItem)
                .build();
    }

    @Override
    public @NotNull Component getGuiTitle() {
        return Component.text(controllerName, NamedTextColor.GOLD);
    }

    public @NotNull String describeRule() {
        String first = describeCondition("A", conditionA);
        if (!conditionBEnabled) {
            return controllerName + ": " + first;
        }
        return controllerName + ": " + first + " " + logicOperator.name() + " " + describeCondition("B", conditionB);
    }

    public boolean isOutputEnabled() {
        return outputKnown && outputEnabled;
    }

    public @NotNull ControlCommandChannel getOutputCircuit() {
        return outputCircuit;
    }

    public @NotNull ControlOutputMode getOutputMode() {
        return outputMode;
    }

    public @NotNull ControlAddress getOutputAddress() {
        return outputAddress;
    }

    public @NotNull ControlChannel getOutputChannel() {
        return currentOutputChannel();
    }

    private boolean acceptForCondition(
            Condition condition,
            ControlSignal signal,
            SourceItem sourceItem
    ) {
        Optional<Boolean> evaluation = condition.rule.evaluate(signal);
        if (evaluation.isEmpty()) {
            return false;
        }

        if (condition.sourceId == null) {
            if (!GridWorks.getInstance().getPhysicalControlNetwork().isLinked(
                    getNodeId(),
                    signal.source()
            )) {
                return false;
            }

            condition.sourceId = signal.source();
            sourceItem.notifyWindows();
        } else if (!condition.sourceId.equals(signal.source())) {
            return false;
        }

        ControlValue.NumberValue numberValue = (ControlValue.NumberValue) signal.value();
        condition.lastObserved = numberValue.value();
        condition.lastResult = evaluation.orElseThrow();
        return true;
    }

    private void updateOutputFromConditions() {
        Optional<Boolean> combined;

        if (!conditionBEnabled) {
            combined = conditionA.result();
        } else {
            combined = logicOperator.combine(conditionA.result(), conditionB.result());
        }

        if (combined.isPresent()) {
            setOutput(combined.orElseThrow(), true, false);
        } else {
            // Unknown is always fail-safe OFF for receivers, but remains WAITING in the GUI.
            setOutput(false, false, true);
        }
    }

    private void setOutput(boolean enabled, boolean known, boolean forcePublish) {
        boolean stateChanged = outputEnabled != enabled || outputKnown != known;
        outputEnabled = enabled;
        outputKnown = known;

        if (stateChanged || forcePublish) {
            publishOutput(enabled);
        }
        outputModeItem.notifyWindows();
        outputCircuitItem.notifyWindows();
        outputAddressItem.notifyWindows();
        outputItem.notifyWindows();
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
        publishOutput(outputEnabled);
        outputModeItem.notifyWindows();
        outputCircuitItem.notifyWindows();
        outputAddressItem.notifyWindows();
        outputItem.notifyWindows();
    }

    private void changeOutputCircuit(int direction) {
        if (outputMode != ControlOutputMode.CIRCUIT) {
            return;
        }
        ControlCommandChannel previous = outputCircuit;
        ControlCommandChannel next = outputCircuit.cycle(direction);
        if (previous == next) {
            return;
        }

        // Explicitly clear the old circuit so receivers do not remain latched ON.
        clearOutputChannel(previous.channel());

        outputCircuit = next;
        publishOutput(outputEnabled);
        outputCircuitItem.notifyWindows();
        outputItem.notifyWindows();
    }

    private void setOutputAddress(ControlAddress next) {
        if (outputAddress.equals(next)) {
            return;
        }

        ControlChannel previous = currentOutputChannel();
        if (outputMode == ControlOutputMode.ADDRESS) {
            clearOutputChannel(previous);
        }

        outputAddress = next;

        if (outputMode == ControlOutputMode.ADDRESS) {
            publishOutput(outputEnabled);
        }

        outputAddressItem.notifyWindows();
        outputItem.notifyWindows();
    }

    private void openOutputAddressWindow(Player player) {
        final boolean[] firstRename = {true};

        Gui upperGui = Gui.builder()
                .setStructure("# a #")
                .addIngredient('#', GuiItems.background())
                .addIngredient(
                        'a',
                        ItemStackBuilder.of(Material.NAME_TAG)
                                .name(Component.text(outputAddress.value(), NamedTextColor.GOLD))
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
                                .name(Component.text("Set Output Address", NamedTextColor.GOLD))
                                .lore(
                                        Component.text(
                                                "Example: ore_line_1",
                                                NamedTextColor.GRAY
                                        ),
                                        Component.text(
                                                "Same address can intentionally control a group.",
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
                    .setTitle(Component.text("Controller Output Address"))
                    .addRenameHandler(raw -> {
                        if (firstRename[0]) {
                            firstRename[0] = false;
                            return;
                        }

                        try {
                            setOutputAddress(ControlAddress.fromUserInput(raw));
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
                    "Could not open Factory Controller output address window",
                    exception
            );
            player.sendMessage(Component.text(
                    "GridWorks could not open the output address window.",
                    NamedTextColor.RED
            ));
        }
    }

    private void changeMetric(int conditionIndex, int direction) {
        markCustom();
        Condition condition = condition(conditionIndex);
        int current = metricIndex(condition.rule.channel());
        Metric metric = METRICS.get(Math.floorMod(current + direction, METRICS.size()));

        condition.rule = new NumericControlRule(
                metric.channel(),
                condition.rule.operator(),
                metric.defaultThreshold()
        );
        resetConditionInput(condition);
        updateOutputFromConditions();
        notifyConditionItems();
    }

    private void changeOperator(int conditionIndex, int direction) {
        markCustom();
        Condition condition = condition(conditionIndex);
        ComparisonOperator[] operators = ComparisonOperator.values();
        int next = Math.floorMod(condition.rule.operator().ordinal() + direction, operators.length);

        condition.rule = new NumericControlRule(
                condition.rule.channel(),
                operators[next],
                condition.rule.threshold()
        );
        reevaluateCondition(condition);
        updateOutputFromConditions();
        notifyConditionItems();
    }

    private void changeThreshold(int conditionIndex, double delta) {
        markCustom();
        Condition condition = condition(conditionIndex);
        Metric metric = metricFor(condition.rule.channel());
        double next = Math.clamp(
                condition.rule.threshold() + delta,
                0.0,
                metric.maxThreshold()
        );

        condition.rule = new NumericControlRule(
                condition.rule.channel(),
                condition.rule.operator(),
                next
        );
        reevaluateCondition(condition);
        updateOutputFromConditions();
        notifyConditionItems();
    }

    private void cycleSource(int conditionIndex, int direction) {
        Condition condition = condition(conditionIndex);
        List<UUID> choices = GridWorks.getInstance()
                .getPhysicalControlNetwork()
                .activeLinkedNodes(getNodeId());

        UUID current = condition.sourceId;
        UUID next;

        if (choices.isEmpty()) {
            next = null;
        } else if (current == null) {
            next = direction >= 0 ? choices.getFirst() : choices.getLast();
        } else {
            int index = choices.indexOf(current);
            if (index < 0) {
                next = null;
            } else {
                int candidate = index + (direction >= 0 ? 1 : -1);
                if (candidate < 0 || candidate >= choices.size()) {
                    next = null;
                } else {
                    next = choices.get(candidate);
                }
            }
        }

        condition.sourceId = next;
        condition.lastObserved = null;
        condition.lastResult = null;
        updateOutputFromConditions();
        notifyConditionItems();
    }

    private void toggleConditionB() {
        markCustom();
        conditionBEnabled = !conditionBEnabled;
        if (!conditionBEnabled) {
            conditionB.lastObserved = null;
            conditionB.lastResult = null;
        }
        updateOutputFromConditions();
        notifyConditionItems();
    }

    private void toggleLogicOperator() {
        markCustom();
        logicOperator = logicOperator == LogicOperator.AND
                ? LogicOperator.OR
                : LogicOperator.AND;
        updateOutputFromConditions();
        notifyConditionItems();
    }

    private void markCustom() {
        if (preset != ControllerPreset.CUSTOM) {
            preset = ControllerPreset.CUSTOM;
            presetItem.notifyWindows();
        }
    }

    private void cyclePreset(int direction) {
        ControllerPreset[] presets = ControllerPreset.values();
        int next = Math.floorMod(
                preset.ordinal() + (direction >= 0 ? 1 : -1),
                presets.length
        );
        ControllerPreset selected = presets[next];

        if (selected == ControllerPreset.CUSTOM) {
            preset = ControllerPreset.CUSTOM;
            presetItem.notifyWindows();
            return;
        }

        applyPreset(selected);
    }

    private void applyPreset(ControllerPreset selected) {
        preset = Objects.requireNonNull(selected, "selected");

        conditionA.rule = selected.ruleA();
        resetConditionInput(conditionA);

        conditionBEnabled = selected.conditionBEnabled();
        logicOperator = selected.logicOperator();

        if (selected.ruleB() != null) {
            conditionB.rule = selected.ruleB();
        }
        resetConditionInput(conditionB);

        updateOutputFromConditions();
        notifyConditionItems();
    }

    private void reevaluateCondition(Condition condition) {
        if (condition.lastObserved == null) {
            condition.lastResult = null;
            return;
        }
        condition.lastResult = condition.rule.test(condition.lastObserved);
    }

    private static void resetConditionInput(Condition condition) {
        condition.sourceId = null;
        condition.lastObserved = null;
        condition.lastResult = null;
    }

    private void notifyConditionItems() {
        metricAItem.notifyWindows();
        operatorAItem.notifyWindows();
        thresholdAItem.notifyWindows();
        sourceAItem.notifyWindows();
        conditionBToggleItem.notifyWindows();
        logicItem.notifyWindows();
        metricBItem.notifyWindows();
        operatorBItem.notifyWindows();
        thresholdBItem.notifyWindows();
        sourceBItem.notifyWindows();
        nameItem.notifyWindows();
        presetItem.notifyWindows();
        outputModeItem.notifyWindows();
        outputCircuitItem.notifyWindows();
        outputAddressItem.notifyWindows();
        outputItem.notifyWindows();
    }

    private Condition condition(int index) {
        return index == 0 ? conditionA : conditionB;
    }

    private boolean conditionEnabled(int index) {
        return index == 0 || conditionBEnabled;
    }

    private static String describeCondition(String label, Condition condition) {
        String source = condition.sourceId == null ? "AUTO" : shortId(condition.sourceId);
        return label + "[" + condition.rule.describe() + ", source " + source + "]";
    }

    private static Condition loadCondition(
            PersistentDataContainer pdc,
            NamespacedKey channelKey,
            NamespacedKey operatorKey,
            NamespacedKey thresholdKey,
            NamespacedKey sourceKey
    ) {
        Metric metric = metricFromStored(pdc.get(channelKey, PersistentDataType.STRING));
        ComparisonOperator operator = operatorFromStored(
                pdc.get(operatorKey, PersistentDataType.STRING)
        );
        Double storedThreshold = pdc.get(thresholdKey, PersistentDataType.DOUBLE);
        double threshold = storedThreshold != null && Double.isFinite(storedThreshold)
                ? Math.clamp(storedThreshold, 0.0, metric.maxThreshold())
                : metric.defaultThreshold();

        return new Condition(
                new NumericControlRule(metric.channel(), operator, threshold),
                uuidFromStored(pdc.get(sourceKey, PersistentDataType.STRING))
        );
    }

    private static void writeCondition(
            PersistentDataContainer pdc,
            Condition condition,
            NamespacedKey channelKey,
            NamespacedKey operatorKey,
            NamespacedKey thresholdKey,
            NamespacedKey sourceKey
    ) {
        pdc.set(channelKey, PersistentDataType.STRING, condition.rule.channel().toString());
        pdc.set(operatorKey, PersistentDataType.STRING, condition.rule.operator().name());
        pdc.set(thresholdKey, PersistentDataType.DOUBLE, condition.rule.threshold());

        if (condition.sourceId == null) {
            pdc.remove(sourceKey);
        } else {
            pdc.set(sourceKey, PersistentDataType.STRING, condition.sourceId.toString());
        }
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

    private static LogicOperator parseLogicOperator(String stored) {
        if (stored != null) {
            try {
                return LogicOperator.valueOf(stored);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return LogicOperator.AND;
    }

    private static ControllerPreset parsePreset(String stored) {
        if (stored != null) {
            try {
                return ControllerPreset.valueOf(stored);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return ControllerPreset.CUSTOM;
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

    private static String normalizeName(String raw) {
        if (raw == null) {
            return DEFAULT_NAME;
        }

        String normalized = raw
                .replaceAll("\\p{Cntrl}", "")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.isEmpty()) {
            return DEFAULT_NAME;
        }

        if (normalized.length() > MAX_NAME_LENGTH) {
            normalized = normalized.substring(0, MAX_NAME_LENGTH);
        }
        return normalized;
    }

    private void openRenameWindow(Player player) {
        final boolean[] firstRename = {true};

        Gui upperGui = Gui.builder()
                .setStructure("# n #")
                .addIngredient('#', GuiItems.background())
                .addIngredient(
                        'n',
                        ItemStackBuilder.of(Material.NAME_TAG)
                                .name(Component.text(controllerName, NamedTextColor.GOLD))
                                .lore(Component.text(
                                        "Type a new controller name",
                                        NamedTextColor.GRAY
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
                                .name(Component.text("Rename Controller", NamedTextColor.GOLD))
                                .lore(
                                        Component.text(
                                                "Type above; changes save immediately.",
                                                NamedTextColor.GRAY
                                        ),
                                        Component.text(
                                                "Close this window when finished.",
                                                NamedTextColor.YELLOW
                                        )
                                )
                )
                .build();

        try {
            AnvilWindow window = AnvilWindow.builder()
                    .setViewer(player)
                    .setUpperGui(upperGui)
                    .setLowerGui(lowerGui)
                    .setTitle(Component.text("Name Factory Controller"))
                    .addRenameHandler(rawName -> {
                        if (firstRename[0]) {
                            firstRename[0] = false;
                            return;
                        }

                        controllerName = normalizeName(rawName);
                        nameItem.notifyWindows();
                    })
                    .build(player);
            window.open();
        } catch (RuntimeException exception) {
            GridWorks.getInstance().getLogger().log(
                    java.util.logging.Level.SEVERE,
                    "Could not open Factory Controller rename window",
                    exception
            );
            player.sendMessage(Component.text(
                    "GridWorks could not open the rename window.",
                    NamedTextColor.RED
            ));
        }
    }

    private abstract class ControllerItem extends AbstractItem {
        protected ItemStackBuilder item(Material material, String name) {
            return ItemStackBuilder.of(material)
                    .name(Component.text(name, NamedTextColor.GOLD));
        }

        protected Component conditionState(Condition condition) {
            if (condition.lastResult == null) {
                return Component.text("Result: WAITING", NamedTextColor.YELLOW);
            }
            return Component.text(
                    "Result: " + (condition.lastResult ? "TRUE" : "FALSE"),
                    condition.lastResult ? NamedTextColor.GREEN : NamedTextColor.RED
            );
        }
    }

    private final class MetricItem extends ControllerItem {
        private final int conditionIndex;

        private MetricItem(int conditionIndex) {
            this.conditionIndex = conditionIndex;
        }

        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            Condition condition = condition(conditionIndex);
            Metric metric = metricFor(condition.rule.channel());
            String label = conditionIndex == 0 ? "A" : "B";

            ItemStackBuilder builder = item(
                    conditionEnabled(conditionIndex) ? metric.material() : Material.GRAY_DYE,
                    "Condition " + label + " Input: " + metric.name()
            ).lore(
                    Component.text(condition.rule.channel().toString(), NamedTextColor.GRAY),
                    conditionState(condition)
            );

            if (conditionEnabled(conditionIndex)) {
                builder.lore(
                        Component.text("Left click: next metric", NamedTextColor.YELLOW),
                        Component.text("Right click: previous metric", NamedTextColor.YELLOW)
                );
            } else {
                builder.lore(Component.text("Enable Condition B first", NamedTextColor.DARK_GRAY));
            }
            return builder;
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            if (!conditionEnabled(conditionIndex)) {
                return;
            }
            if (clickType.isLeftClick()) {
                changeMetric(conditionIndex, 1);
            } else if (clickType.isRightClick()) {
                changeMetric(conditionIndex, -1);
            }
        }
    }

    private final class OperatorItem extends ControllerItem {
        private final int conditionIndex;

        private OperatorItem(int conditionIndex) {
            this.conditionIndex = conditionIndex;
        }

        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            Condition condition = condition(conditionIndex);
            String label = conditionIndex == 0 ? "A" : "B";

            return item(
                    conditionEnabled(conditionIndex) ? Material.COMPARATOR : Material.GRAY_DYE,
                    "Condition " + label + " Comparison: " + condition.rule.operator().symbol()
            ).lore(
                    Component.text(condition.rule.operator().name(), NamedTextColor.GRAY),
                    conditionEnabled(conditionIndex)
                            ? Component.text("Left/right click to cycle", NamedTextColor.YELLOW)
                            : Component.text("Enable Condition B first", NamedTextColor.DARK_GRAY)
            );
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            if (!conditionEnabled(conditionIndex)) {
                return;
            }
            if (clickType.isLeftClick()) {
                changeOperator(conditionIndex, 1);
            } else if (clickType.isRightClick()) {
                changeOperator(conditionIndex, -1);
            }
        }
    }

    private final class ThresholdItem extends ControllerItem {
        private final int conditionIndex;

        private ThresholdItem(int conditionIndex) {
            this.conditionIndex = conditionIndex;
        }

        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            Condition condition = condition(conditionIndex);
            Metric metric = metricFor(condition.rule.channel());
            String label = conditionIndex == 0 ? "A" : "B";

            ItemStackBuilder builder = item(
                    conditionEnabled(conditionIndex) ? Material.REPEATER : Material.GRAY_DYE,
                    "Condition " + label + " Threshold: " + condition.rule.threshold()
            );

            if (conditionEnabled(conditionIndex)) {
                builder.lore(
                        Component.text(
                                "Left +" + metric.step() + " / Right -" + metric.step(),
                                NamedTextColor.YELLOW
                        ),
                        Component.text(
                                "Shift uses " + metric.shiftStep(),
                                NamedTextColor.YELLOW
                        )
                );
            } else {
                builder.lore(Component.text("Enable Condition B first", NamedTextColor.DARK_GRAY));
            }
            return builder;
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            if (!conditionEnabled(conditionIndex)) {
                return;
            }

            Metric metric = metricFor(condition(conditionIndex).rule.channel());
            double step = clickType.isShiftClick() ? metric.shiftStep() : metric.step();

            if (clickType.isLeftClick()) {
                changeThreshold(conditionIndex, step);
            } else if (clickType.isRightClick()) {
                changeThreshold(conditionIndex, -step);
            }
        }
    }

    private final class SourceItem extends ControllerItem {
        private final int conditionIndex;

        private SourceItem(int conditionIndex) {
            this.conditionIndex = conditionIndex;
        }

        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            Condition condition = condition(conditionIndex);
            String label = conditionIndex == 0 ? "A" : "B";
            String source = condition.sourceId == null ? "AUTO" : shortId(condition.sourceId);

            ItemStackBuilder builder = item(
                    conditionEnabled(conditionIndex) ? Material.TARGET : Material.GRAY_DYE,
                    "Condition " + label + " Source: " + source
            );

            if (conditionEnabled(conditionIndex)) {
                builder.lore(
                        Component.text(
                                "AUTO binds to the first matching direct sensor",
                                NamedTextColor.GRAY
                        ),
                        Component.text(
                                "Left/right: cycle loaded direct links",
                                NamedTextColor.YELLOW
                        )
                );
            } else {
                builder.lore(Component.text("Enable Condition B first", NamedTextColor.DARK_GRAY));
            }
            return builder;
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            if (!conditionEnabled(conditionIndex)) {
                return;
            }
            if (clickType.isLeftClick()) {
                cycleSource(conditionIndex, 1);
            } else if (clickType.isRightClick()) {
                cycleSource(conditionIndex, -1);
            }
        }
    }

    private final class NameItem extends ControllerItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(Material.NAME_TAG, "Name: " + controllerName)
                    .lore(Component.text(
                            "Click to rename this controller",
                            NamedTextColor.YELLOW
                    ));
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
                    () -> openRenameWindow(player)
            );
        }
    }

    private final class PresetItem extends ControllerItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(Material.KNOWLEDGE_BOOK, "Preset: " + preset.displayName())
                    .lore(
                            Component.text(
                                    "Left click: next preset",
                                    NamedTextColor.YELLOW
                            ),
                            Component.text(
                                    "Right click: previous preset",
                                    NamedTextColor.YELLOW
                            ),
                            Component.text(
                                    "Manual rule edits return to Custom",
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
                cyclePreset(1);
            } else if (clickType.isRightClick()) {
                cyclePreset(-1);
            }
        }
    }

    private final class ConditionToggleItem extends ControllerItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(
                    conditionBEnabled ? Material.LIME_DYE : Material.GRAY_DYE,
                    "Condition B: " + (conditionBEnabled ? "ENABLED" : "DISABLED")
            ).lore(
                    Component.text(
                            "Click to " + (conditionBEnabled ? "disable" : "enable") + " second condition",
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
            toggleConditionB();
        }
    }

    private final class LogicItem extends ControllerItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(
                    conditionBEnabled ? Material.REDSTONE_TORCH : Material.GRAY_DYE,
                    "Logic: " + logicOperator.name()
            ).lore(
                    conditionBEnabled
                            ? Component.text("Click to switch AND / OR", NamedTextColor.YELLOW)
                            : Component.text("Used when Condition B is enabled", NamedTextColor.DARK_GRAY)
            );
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            if (conditionBEnabled) {
                toggleLogicOperator();
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
            ).lore(
                    Component.text(
                            "Click to switch Circuit / Address",
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
            ).lore(Component.text(
                    outputCircuit.channel().toString(),
                    NamedTextColor.GRAY
            ));

            if (outputMode == ControlOutputMode.CIRCUIT) {
                builder.lore(Component.text(
                        "Left/right click to cycle Default / A / B / C / D",
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

    private final class OutputItem extends ControllerItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            String output = !outputKnown ? "WAITING" : (outputEnabled ? "ON" : "OFF");
            NamedTextColor color = !outputKnown
                    ? NamedTextColor.YELLOW
                    : (outputEnabled ? NamedTextColor.GREEN : NamedTextColor.RED);

            ItemStackBuilder builder = ItemStackBuilder.of(
                            outputKnown && outputEnabled ? Material.LIME_DYE : Material.GRAY_DYE
                    )
                    .name(Component.text("Output: " + output, color))
                    .lore(Component.text(
                            "Publishes " + currentOutputChannel(),
                            NamedTextColor.GRAY
                    ));

            if (conditionA.lastObserved != null) {
                builder.lore(Component.text(
                        "A input: " + conditionA.lastObserved,
                        NamedTextColor.GRAY
                ));
            }
            if (conditionBEnabled && conditionB.lastObserved != null) {
                builder.lore(Component.text(
                        "B input: " + conditionB.lastObserved,
                        NamedTextColor.GRAY
                ));
            }
            if (!outputKnown) {
                builder.lore(Component.text(
                        "Receivers are fail-safe OFF while waiting",
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
        }
    }

    private static final class Condition {
        private NumericControlRule rule;
        private UUID sourceId;
        private Double lastObserved;
        private Boolean lastResult;

        private Condition(NumericControlRule rule, UUID sourceId) {
            this.rule = Objects.requireNonNull(rule, "rule");
            this.sourceId = sourceId;
        }

        private Optional<Boolean> result() {
            return Optional.ofNullable(lastResult);
        }
    }

    private enum ControllerPreset {
        CUSTOM("Custom", null, false, LogicOperator.AND, null),
        LOW_TANK(
                "Low Tank",
                new NumericControlRule(
                        GridWorksChannels.FLUID_FILL_RATIO,
                        ComparisonOperator.LESS_OR_EQUAL,
                        0.25
                ),
                false,
                LogicOperator.AND,
                null
        ),
        HIGH_INVENTORY(
                "Inventory High",
                new NumericControlRule(
                        GridWorksChannels.INVENTORY_OCCUPIED_RATIO,
                        ComparisonOperator.GREATER_OR_EQUAL,
                        0.75
                ),
                false,
                LogicOperator.AND,
                null
        ),
        REDSTONE_THRESHOLD(
                "Redstone >= 8",
                new NumericControlRule(
                        GridWorksChannels.REDSTONE_STRENGTH,
                        ComparisonOperator.GREATER_OR_EQUAL,
                        8.0
                ),
                false,
                LogicOperator.AND,
                null
        ),
        STOCK_AND_FLUID_READY(
                "Stock + Fluid Ready",
                new NumericControlRule(
                        GridWorksChannels.INVENTORY_ITEMS,
                        ComparisonOperator.GREATER_OR_EQUAL,
                        64.0
                ),
                true,
                LogicOperator.AND,
                new NumericControlRule(
                        GridWorksChannels.FLUID_FILL_RATIO,
                        ComparisonOperator.GREATER_OR_EQUAL,
                        0.25
                )
        ),
        SUPPLY_ALERT(
                "Supply Alert",
                new NumericControlRule(
                        GridWorksChannels.INVENTORY_ITEMS,
                        ComparisonOperator.LESS_OR_EQUAL,
                        64.0
                ),
                true,
                LogicOperator.OR,
                new NumericControlRule(
                        GridWorksChannels.FLUID_FILL_RATIO,
                        ComparisonOperator.LESS_OR_EQUAL,
                        0.25
                )
        ),
        POWER_LOAD_HIGH(
                "Power Load >= 90%",
                new NumericControlRule(
                        GridWorksChannels.POWER_LOAD_RATIO,
                        ComparisonOperator.GREATER_OR_EQUAL,
                        0.90
                ),
                false,
                LogicOperator.AND,
                null
        ),
        POWER_SHORTAGE(
                "Power Shortage",
                new NumericControlRule(
                        GridWorksChannels.POWER_UNPOWERED_CONSUMERS,
                        ComparisonOperator.GREATER_OR_EQUAL,
                        1.0
                ),
                false,
                LogicOperator.AND,
                null
        ),
        LOAD_SHED_TRIGGER(
                "Load Shed Trigger",
                new NumericControlRule(
                        GridWorksChannels.POWER_LOAD_RATIO,
                        ComparisonOperator.GREATER_OR_EQUAL,
                        0.95
                ),
                true,
                LogicOperator.OR,
                new NumericControlRule(
                        GridWorksChannels.POWER_UNPOWERED_CONSUMERS,
                        ComparisonOperator.GREATER_OR_EQUAL,
                        1.0
                )
        );

        private final String displayName;
        private final NumericControlRule ruleA;
        private final boolean conditionBEnabled;
        private final LogicOperator logicOperator;
        private final NumericControlRule ruleB;

        ControllerPreset(
                String displayName,
                NumericControlRule ruleA,
                boolean conditionBEnabled,
                LogicOperator logicOperator,
                NumericControlRule ruleB
        ) {
            this.displayName = displayName;
            this.ruleA = ruleA;
            this.conditionBEnabled = conditionBEnabled;
            this.logicOperator = logicOperator;
            this.ruleB = ruleB;
        }

        private String displayName() {
            return displayName;
        }

        private NumericControlRule ruleA() {
            if (ruleA == null) {
                throw new IllegalStateException("Custom preset has no fixed rule");
            }
            return ruleA;
        }

        private boolean conditionBEnabled() {
            return conditionBEnabled;
        }

        private LogicOperator logicOperator() {
            return logicOperator;
        }

        private NumericControlRule ruleB() {
            return ruleB;
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
