package io.github.wickidcow.gridworks.content.block;

import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.block.interfaces.GuiRebarBlock;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.pylonmc.rebar.util.gui.GuiItems;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.api.control.ControlAddress;
import io.github.wickidcow.gridworks.api.control.ControlChannel;
import io.github.wickidcow.gridworks.api.control.ControlSignal;
import io.github.wickidcow.gridworks.api.control.ControlStateSource;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.api.control.GridWorksChannels;
import io.github.wickidcow.gridworks.control.RisingEdgeTrigger;
import io.github.wickidcow.gridworks.production.SequenceRoutes;
import io.github.wickidcow.gridworks.production.SequenceStateMachine;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
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

/**
 * Four-stage event-driven production sequencer.
 *
 * <p>Each stage owns one trigger address and one active-output address. The
 * first trigger state observed after entering a stage is only a baseline;
 * a later false-to-true transition advances the sequence. This makes topology
 * replay and server reload safe even when a trigger source is already true.</p>
 */
public final class SequenceControllerBlock extends PhysicalControlNodeBlock
        implements GuiRebarBlock, ControlStateSource {

    private static final NamespacedKey PHASE_KEY = key("sequence_phase");
    private static final NamespacedKey STAGE_KEY = key("sequence_stage");
    private static final NamespacedKey START_ADDRESS_KEY = key("sequence_start_address");
    private static final NamespacedKey COMPLETE_ADDRESS_KEY = key("sequence_complete_address");

    private static final NamespacedKey[] TRIGGER_ADDRESS_KEYS = {
            key("sequence_trigger_1"),
            key("sequence_trigger_2"),
            key("sequence_trigger_3"),
            key("sequence_trigger_4")
    };
    private static final NamespacedKey[] OUTPUT_ADDRESS_KEYS = {
            key("sequence_output_1"),
            key("sequence_output_2"),
            key("sequence_output_3"),
            key("sequence_output_4")
    };

    private final SequenceStateMachine sequence;
    private final RisingEdgeTrigger startEdge = new RisingEdgeTrigger();
    private final RisingEdgeTrigger stageEdge = new RisingEdgeTrigger();
    private volatile SequenceRoutes routes;

    private final StatusItem statusItem = new StatusItem();
    private final StartItem startItem = new StartItem();
    private final AdvanceItem advanceItem = new AdvanceItem();
    private final AbortItem abortItem = new AbortItem();
    private final SpecialAddressItem startAddressItem =
            new SpecialAddressItem(true);
    private final SpecialAddressItem completeAddressItem =
            new SpecialAddressItem(false);
    private final StageStatusItem[] stageStatusItems = createStageStatusItems();
    private final StageAddressItem[] stageOutputItems =
            createStageAddressItems(false);
    private final StageAddressItem[] stageTriggerItems =
            createStageAddressItems(true);

    public SequenceControllerBlock(
            @NotNull Block block,
            @NotNull BlockCreateContext context
    ) {
        super(block, context);
        this.sequence = new SequenceStateMachine();
        this.routes = SequenceRoutes.defaults(getNodeId());
    }

    public SequenceControllerBlock(
            @NotNull Block block,
            @NotNull PersistentDataContainer pdc
    ) {
        super(block, pdc);
        this.sequence = SequenceStateMachine.fromStored(
                pdc.get(PHASE_KEY, PersistentDataType.STRING),
                pdc.get(STAGE_KEY, PersistentDataType.INTEGER)
        );
        this.routes = loadRoutes(pdc, getNodeId());
    }

    @Override
    protected void afterActivated() {
        startEdge.reset();
        stageEdge.reset();
    }

    @Override
    protected void afterDeactivated() {
        startEdge.reset();
        stageEdge.reset();
    }

    @Override
    protected void afterRemoved() {
        startEdge.reset();
        stageEdge.reset();
    }

    @Override
    public boolean accepts(@NotNull ControlChannel channel) {
        SequenceRoutes currentRoutes = routes;
        if (currentRoutes.start().channel().equals(channel)) {
            return true;
        }

        int stage = sequence.currentStage();
        return sequence.isRunning()
                && stage > 0
                && currentRoutes.trigger(stage).channel().equals(channel);
    }

    @Override
    protected void handleSignal(@NotNull ControlSignal signal) {
        if (!(signal.value() instanceof ControlValue.BooleanValue booleanValue)) {
            return;
        }

        ControlChannel channel = signal.channel();
        boolean value = booleanValue.value();
        runOnServerThreadIfActive(() -> handleBooleanSignal(channel, value));
    }

    @Override
    protected void writeNodeData(@NotNull PersistentDataContainer pdc) {
        pdc.set(PHASE_KEY, PersistentDataType.STRING, sequence.phase().name());
        pdc.set(STAGE_KEY, PersistentDataType.INTEGER, sequence.currentStage());

        SequenceRoutes currentRoutes = routes;
        pdc.set(
                START_ADDRESS_KEY,
                PersistentDataType.STRING,
                currentRoutes.start().value()
        );
        pdc.set(
                COMPLETE_ADDRESS_KEY,
                PersistentDataType.STRING,
                currentRoutes.complete().value()
        );

        for (int stage = 1; stage <= SequenceStateMachine.STAGE_COUNT; stage++) {
            pdc.set(
                    TRIGGER_ADDRESS_KEYS[stage - 1],
                    PersistentDataType.STRING,
                    currentRoutes.trigger(stage).value()
            );
            pdc.set(
                    OUTPUT_ADDRESS_KEYS[stage - 1],
                    PersistentDataType.STRING,
                    currentRoutes.output(stage).value()
            );
        }
    }

    @Override
    public void publishCurrentState() {
        var bus = GridWorks.getInstance().getControlBus();
        boolean running = sequence.isRunning();
        boolean complete = sequence.isComplete();
        int currentStage = sequence.currentStage();

        bus.publish(
                getNodeId(),
                GridWorksChannels.SEQUENCE_RUNNING,
                ControlValue.of(running)
        );
        bus.publish(
                getNodeId(),
                GridWorksChannels.SEQUENCE_STAGE,
                ControlValue.of((double) currentStage)
        );
        bus.publish(
                getNodeId(),
                GridWorksChannels.SEQUENCE_COMPLETE,
                ControlValue.of(complete)
        );

        SequenceRoutes currentRoutes = routes;
        for (int stage = 1; stage <= SequenceStateMachine.STAGE_COUNT; stage++) {
            bus.publish(
                    getNodeId(),
                    currentRoutes.output(stage).channel(),
                    ControlValue.of(running && currentStage == stage)
            );
        }
        bus.publish(
                getNodeId(),
                currentRoutes.complete().channel(),
                ControlValue.of(complete)
        );
    }

    @Override
    public @NotNull Gui createGui() {
        return Gui.builder()
                .setStructure(
                        "s # b # n # x # c",
                        "1 o t # # # # # #",
                        "2 p u # # # # # #",
                        "3 q v # # # # # #",
                        "4 r w # # # # # #",
                        "i # # # # # # # #"
                )
                .addIngredient('#', GuiItems.background())
                .addIngredient('s', statusItem)
                .addIngredient('b', startItem)
                .addIngredient('n', advanceItem)
                .addIngredient('x', abortItem)
                .addIngredient('c', completeAddressItem)
                .addIngredient('i', startAddressItem)
                .addIngredient('1', stageStatusItems[0])
                .addIngredient('2', stageStatusItems[1])
                .addIngredient('3', stageStatusItems[2])
                .addIngredient('4', stageStatusItems[3])
                .addIngredient('o', stageOutputItems[0])
                .addIngredient('p', stageOutputItems[1])
                .addIngredient('q', stageOutputItems[2])
                .addIngredient('r', stageOutputItems[3])
                .addIngredient('t', stageTriggerItems[0])
                .addIngredient('u', stageTriggerItems[1])
                .addIngredient('v', stageTriggerItems[2])
                .addIngredient('w', stageTriggerItems[3])
                .build();
    }

    @Override
    public @NotNull Component getGuiTitle() {
        return Component.text("Sequence Controller", NamedTextColor.GOLD);
    }

    public @NotNull SequenceStateMachine.Phase getSequencePhase() {
        return sequence.phase();
    }

    public int getCurrentStage() {
        return sequence.currentStage();
    }

    public @NotNull SequenceRoutes getRoutes() {
        return routes;
    }

    private void handleBooleanSignal(ControlChannel channel, boolean value) {
        SequenceRoutes currentRoutes = routes;

        if (currentRoutes.start().channel().equals(channel)) {
            if (startEdge.observe(value)) {
                startSequence();
            }
            return;
        }

        int stage = sequence.currentStage();
        if (!sequence.isRunning()
                || stage <= 0
                || !currentRoutes.trigger(stage).channel().equals(channel)) {
            return;
        }

        if (stageEdge.observe(value)) {
            advanceSequence();
        }
    }

    private void startSequence() {
        sequence.start();
        stageEdge.reset();
        publishCurrentState();
        notifyItems();
        requestStateReplay();
    }

    private void advanceSequence() {
        if (!sequence.isRunning()) {
            return;
        }

        sequence.advance();
        stageEdge.reset();
        publishCurrentState();
        notifyItems();

        if (sequence.isRunning()) {
            requestStateReplay();
        }
    }

    private void abortSequence() {
        sequence.abort();
        stageEdge.reset();
        publishCurrentState();
        notifyItems();
    }

    private void setStartAddress(ControlAddress next) {
        if (!canEditRoutes()) {
            return;
        }

        try {
            routes = routes.withStart(next);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "That address is already used by this sequence.",
                    exception
            );
        }

        startEdge.reset();
        notifyItems();
        requestStateReplay();
    }

    private void setCompleteAddress(ControlAddress next) {
        if (!canEditRoutes()) {
            return;
        }

        SequenceRoutes previous = routes;
        SequenceRoutes changed;
        try {
            changed = previous.withComplete(next);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "That address is already used by this sequence.",
                    exception
            );
        }

        clearAddress(previous.complete());
        routes = changed;
        publishCurrentState();
        notifyItems();
    }

    private void setStageTrigger(int stage, ControlAddress next) {
        if (!canEditRoutes()) {
            return;
        }

        try {
            routes = routes.withTrigger(stage, next);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "That address is already used by this sequence.",
                    exception
            );
        }
        notifyItems();
    }

    private void setStageOutput(int stage, ControlAddress next) {
        if (!canEditRoutes()) {
            return;
        }

        SequenceRoutes previous = routes;
        SequenceRoutes changed;
        try {
            changed = previous.withOutput(stage, next);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "That address is already used by this sequence.",
                    exception
            );
        }

        clearAddress(previous.output(stage));
        routes = changed;
        publishCurrentState();
        notifyItems();
    }

    private boolean canEditRoutes() {
        return !sequence.isRunning();
    }

    private void clearAddress(ControlAddress address) {
        GridWorks.getInstance().getControlBus().publish(
                getNodeId(),
                address.channel(),
                ControlValue.of(false)
        );
    }

    private void requestStateReplay() {
        GridWorks.getInstance()
                .getPhysicalControlNetwork()
                .replayStateSources(getNodeId());
    }

    private void notifyItems() {
        statusItem.notifyWindows();
        startItem.notifyWindows();
        advanceItem.notifyWindows();
        abortItem.notifyWindows();
        startAddressItem.notifyWindows();
        completeAddressItem.notifyWindows();

        for (int index = 0; index < SequenceStateMachine.STAGE_COUNT; index++) {
            stageStatusItems[index].notifyWindows();
            stageOutputItems[index].notifyWindows();
            stageTriggerItems[index].notifyWindows();
        }
    }

    private void openAddressWindow(
            Player player,
            String title,
            ControlAddress current,
            Consumer<ControlAddress> setter
    ) {
        if (!canEditRoutes()) {
            player.sendMessage(Component.text(
                    "Stop the running sequence before editing routes.",
                    NamedTextColor.RED
            ));
            return;
        }

        final boolean[] firstRename = {true};

        Gui upperGui = Gui.builder()
                .setStructure("# a #")
                .addIngredient('#', GuiItems.background())
                .addIngredient(
                        'a',
                        ItemStackBuilder.of(Material.NAME_TAG)
                                .name(Component.text(current.value(), NamedTextColor.GOLD))
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
                                .name(Component.text("Set Sequence Address", NamedTextColor.GOLD))
                                .lore(
                                        Component.text(
                                                "Every input and output address must be unique.",
                                                NamedTextColor.GRAY
                                        ),
                                        Component.text(
                                                "Spaces are normalized to underscores.",
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
                    .setTitle(Component.text(title))
                    .addRenameHandler(raw -> {
                        if (firstRename[0]) {
                            firstRename[0] = false;
                            return;
                        }

                        try {
                            setter.accept(ControlAddress.fromUserInput(raw));
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
                    Level.SEVERE,
                    "Could not open Sequence Controller address window",
                    exception
            );
            player.sendMessage(Component.text(
                    "GridWorks could not open the address window.",
                    NamedTextColor.RED
            ));
        }
    }

    private static SequenceRoutes loadRoutes(
            PersistentDataContainer pdc,
            UUID nodeId
    ) {
        SequenceRoutes defaults = SequenceRoutes.defaults(nodeId);
        List<ControlAddress> triggers = new ArrayList<>();
        List<ControlAddress> outputs = new ArrayList<>();

        for (int stage = 1; stage <= SequenceStateMachine.STAGE_COUNT; stage++) {
            triggers.add(ControlAddress.fromStoredOrDefault(
                    pdc.get(
                            TRIGGER_ADDRESS_KEYS[stage - 1],
                            PersistentDataType.STRING
                    ),
                    defaults.trigger(stage)
            ));
            outputs.add(ControlAddress.fromStoredOrDefault(
                    pdc.get(
                            OUTPUT_ADDRESS_KEYS[stage - 1],
                            PersistentDataType.STRING
                    ),
                    defaults.output(stage)
            ));
        }

        try {
            return new SequenceRoutes(
                    ControlAddress.fromStoredOrDefault(
                            pdc.get(START_ADDRESS_KEY, PersistentDataType.STRING),
                            defaults.start()
                    ),
                    ControlAddress.fromStoredOrDefault(
                            pdc.get(COMPLETE_ADDRESS_KEY, PersistentDataType.STRING),
                            defaults.complete()
                    ),
                    triggers,
                    outputs
            );
        } catch (IllegalArgumentException ignored) {
            // Corrupt/legacy route collisions are repaired atomically rather
            // than allowing the controller to publish into one of its inputs.
            return defaults;
        }
    }

    private static NamespacedKey key(String value) {
        return Objects.requireNonNull(
                NamespacedKey.fromString("gridworks:" + value)
        );
    }

    private StageStatusItem[] createStageStatusItems() {
        StageStatusItem[] items =
                new StageStatusItem[SequenceStateMachine.STAGE_COUNT];
        for (int stage = 1; stage <= SequenceStateMachine.STAGE_COUNT; stage++) {
            items[stage - 1] = new StageStatusItem(stage);
        }
        return items;
    }

    private StageAddressItem[] createStageAddressItems(boolean trigger) {
        StageAddressItem[] items =
                new StageAddressItem[SequenceStateMachine.STAGE_COUNT];
        for (int stage = 1; stage <= SequenceStateMachine.STAGE_COUNT; stage++) {
            items[stage - 1] = new StageAddressItem(stage, trigger);
        }
        return items;
    }

    private abstract class SequenceItem extends AbstractItem {
        protected ItemStackBuilder item(Material material, String name) {
            return ItemStackBuilder.of(material)
                    .name(Component.text(name, NamedTextColor.GOLD));
        }
    }

    private final class StatusItem extends SequenceItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            SequenceStateMachine.Phase phase = sequence.phase();
            int stage = sequence.currentStage();

            return item(
                    phase == SequenceStateMachine.Phase.COMPLETE
                            ? Material.LIME_CONCRETE
                            : phase == SequenceStateMachine.Phase.RUNNING
                            ? Material.ORANGE_CONCRETE
                            : Material.GRAY_CONCRETE,
                    "Sequence: " + phase.name()
            ).lore(
                    Component.text(
                            phase == SequenceStateMachine.Phase.RUNNING
                                    ? "Active stage: " + stage
                                    : phase == SequenceStateMachine.Phase.COMPLETE
                                    ? "All four stages completed"
                                    : "No active stage",
                            phase == SequenceStateMachine.Phase.COMPLETE
                                    ? NamedTextColor.GREEN
                                    : NamedTextColor.WHITE
                    ),
                    Component.text(
                            "Stage changes are event-driven; no timer loop",
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

    private final class StartItem extends SequenceItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(Material.LIME_DYE, "Start / Restart Sequence")
                    .lore(
                            Component.text(
                                    "Shift + left click to begin at stage 1",
                                    NamedTextColor.YELLOW
                            ),
                            Component.text(
                                    "Also starts on a rising edge at "
                                            + routes.start().value(),
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
            if (clickType.isLeftClick() && clickType.isShiftClick()) {
                startSequence();
            }
        }
    }

    private final class AdvanceItem extends SequenceItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(Material.SPECTRAL_ARROW, "Manual Advance")
                    .lore(
                            Component.text(
                                    sequence.isRunning()
                                            ? "Shift + left click to advance now"
                                            : "Start the sequence first",
                                    sequence.isRunning()
                                            ? NamedTextColor.YELLOW
                                            : NamedTextColor.DARK_GRAY
                            ),
                            Component.text(
                                    "Automatic advance uses the active stage trigger",
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
            if (clickType.isLeftClick() && clickType.isShiftClick()) {
                advanceSequence();
            }
        }
    }

    private final class AbortItem extends SequenceItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(Material.BARRIER, "Abort / Reset")
                    .lore(
                            Component.text(
                                    "Shift + right click to return to IDLE",
                                    NamedTextColor.YELLOW
                            ),
                            Component.text(
                                    "All stage and completion outputs are cleared",
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
            if (clickType.isRightClick() && clickType.isShiftClick()) {
                abortSequence();
            }
        }
    }

    private final class StageStatusItem extends SequenceItem {
        private final int stage;

        private StageStatusItem(int stage) {
            this.stage = stage;
        }

        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            SequenceStateMachine.Phase phase = sequence.phase();
            int current = sequence.currentStage();
            boolean active = phase == SequenceStateMachine.Phase.RUNNING
                    && current == stage;
            boolean passed = phase == SequenceStateMachine.Phase.COMPLETE
                    || (phase == SequenceStateMachine.Phase.RUNNING
                    && current > stage);

            String state = active ? "ACTIVE" : passed ? "PASSED" : "WAITING";
            Material material = active
                    ? Material.LIME_CONCRETE
                    : passed ? Material.LIGHT_GRAY_CONCRETE : Material.GRAY_CONCRETE;

            return item(material, "Stage " + stage + ": " + state)
                    .lore(
                            Component.text(
                                    "Output: " + routes.output(stage).value(),
                                    NamedTextColor.WHITE
                            ),
                            Component.text(
                                    "Advance: " + routes.trigger(stage).value(),
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

    private final class StageAddressItem extends SequenceItem {
        private final int stage;
        private final boolean trigger;

        private StageAddressItem(int stage, boolean trigger) {
            this.stage = stage;
            this.trigger = trigger;
        }

        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            ControlAddress address = trigger
                    ? routes.trigger(stage)
                    : routes.output(stage);

            return item(
                    trigger ? Material.TRIPWIRE_HOOK : Material.REDSTONE_TORCH,
                    "Stage " + stage + (trigger ? " Trigger" : " Output")
            ).lore(
                    Component.text(address.value(), NamedTextColor.AQUA),
                    Component.text(address.channel().toString(), NamedTextColor.DARK_GRAY),
                    Component.text(
                            canEditRoutes()
                                    ? "Click to edit"
                                    : "Abort the sequence before editing routes",
                            canEditRoutes()
                                    ? NamedTextColor.YELLOW
                                    : NamedTextColor.RED
                    )
            );
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            if (!canEditRoutes()) {
                return;
            }

            ControlAddress current = trigger
                    ? routes.trigger(stage)
                    : routes.output(stage);
            player.closeInventory();
            GridWorks.getInstance().getServer().getScheduler().runTask(
                    GridWorks.getInstance(),
                    () -> openAddressWindow(
                            player,
                            "Stage " + stage + (trigger ? " Trigger" : " Output"),
                            current,
                            next -> {
                                if (trigger) {
                                    setStageTrigger(stage, next);
                                } else {
                                    setStageOutput(stage, next);
                                }
                            }
                    )
            );
        }
    }

    private final class SpecialAddressItem extends SequenceItem {
        private final boolean start;

        private SpecialAddressItem(boolean start) {
            this.start = start;
        }

        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            ControlAddress address = start ? routes.start() : routes.complete();

            return item(
                    start ? Material.ENDER_EYE : Material.FIREWORK_STAR,
                    start ? "Start Input" : "Completion Output"
            ).lore(
                    Component.text(address.value(), NamedTextColor.AQUA),
                    Component.text(address.channel().toString(), NamedTextColor.DARK_GRAY),
                    Component.text(
                            canEditRoutes()
                                    ? "Click to edit"
                                    : "Abort the sequence before editing routes",
                            canEditRoutes()
                                    ? NamedTextColor.YELLOW
                                    : NamedTextColor.RED
                    )
            );
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            if (!canEditRoutes()) {
                return;
            }

            ControlAddress current = start ? routes.start() : routes.complete();
            player.closeInventory();
            GridWorks.getInstance().getServer().getScheduler().runTask(
                    GridWorks.getInstance(),
                    () -> openAddressWindow(
                            player,
                            start ? "Sequence Start Input" : "Sequence Completion Output",
                            current,
                            start
                                    ? SequenceControllerBlock.this::setStartAddress
                                    : SequenceControllerBlock.this::setCompleteAddress
                    )
            );
        }
    }
}
