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
import io.github.wickidcow.gridworks.control.RisingEdgeTrigger;
import io.github.wickidcow.gridworks.production.BatchPaceTracker;
import io.github.wickidcow.gridworks.production.BatchProgressTracker;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.window.AnvilWindow;

/**
 * Event-driven production target controller.
 *
 * <p>Only cumulative cycle telemetry from directly linked peers contributes.
 * The first value from each peer is a baseline, so historical/offline work is
 * never backfilled into the active batch.</p>
 */
public final class BatchControllerBlock extends PhysicalControlNodeBlock
        implements GuiRebarBlock, ControlStateSource {
    private static final NamespacedKey TARGET_KEY = key("batch_controller_target");
    private static final NamespacedKey PROGRESS_KEY = key("batch_controller_progress");
    private static final NamespacedKey OUTPUT_MODE_KEY = key("batch_controller_output_mode");
    private static final NamespacedKey OUTPUT_CIRCUIT_KEY = key("batch_controller_output_circuit");
    private static final NamespacedKey OUTPUT_ADDRESS_KEY = key("batch_controller_output_address");
    private static final NamespacedKey FAULT_KEY = key("batch_controller_fault");
    private static final NamespacedKey FAULT_ADDRESS_KEY = key("batch_controller_fault_address");
    private static final NamespacedKey RESET_ADDRESS_KEY = key("batch_controller_reset_address");
    private static final NamespacedKey WATCHDOG_TICKS_KEY = key("batch_controller_watchdog_ticks");

    private static final long DEFAULT_WATCHDOG_TICKS = 0L;
    private static final long MAX_WATCHDOG_TICKS = 72_000L;

    private final BatchProgressTracker tracker;
    private final BatchPaceTracker paceTracker;
    private final Set<UUID> activePeers = ConcurrentHashMap.newKeySet();
    private final RisingEdgeTrigger resetEdge = new RisingEdgeTrigger();

    private ControlOutputMode outputMode;
    private ControlCommandChannel outputCircuit;
    private ControlAddress outputAddress;
    private ControlAddress faultAddress;
    private ControlAddress resetAddress;
    private boolean faulted;
    private long watchdogTicks;
    private BukkitTask watchdogTask;

    private final ProgressItem progressItem = new ProgressItem();
    private final TargetItem targetItem = new TargetItem();
    private final SourcesItem sourcesItem = new SourcesItem();
    private final ResetItem resetItem = new ResetItem();
    private final OutputModeItem outputModeItem = new OutputModeItem();
    private final OutputCircuitItem outputCircuitItem = new OutputCircuitItem();
    private final OutputAddressItem outputAddressItem = new OutputAddressItem();
    private final OutputStateItem outputStateItem = new OutputStateItem();
    private final WatchdogItem watchdogItem = new WatchdogItem();
    private final FaultAddressItem faultAddressItem = new FaultAddressItem();
    private final ResetAddressItem resetAddressItem = new ResetAddressItem();
    private final PaceItem paceItem = new PaceItem();

    public BatchControllerBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
        tracker = new BatchProgressTracker();
        paceTracker = new BatchPaceTracker();
        outputMode = ControlOutputMode.CIRCUIT;
        outputCircuit = ControlCommandChannel.DEFAULT;
        outputAddress = ControlAddress.defaultFor(getNodeId(), "batch");
        faultAddress = defaultFaultAddress(getNodeId(), outputAddress);
        resetAddress = defaultResetAddress(getNodeId(), outputAddress, faultAddress);
        faulted = false;
        watchdogTicks = DEFAULT_WATCHDOG_TICKS;
    }

    public BatchControllerBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);

        Long storedTarget = pdc.get(TARGET_KEY, PersistentDataType.LONG);
        Long storedProgress = pdc.get(PROGRESS_KEY, PersistentDataType.LONG);
        tracker = new BatchProgressTracker(
                validTargetOrDefault(storedTarget),
                validProgressOrZero(storedProgress)
        );
        paceTracker = new BatchPaceTracker();

        outputMode = ControlOutputMode.fromStored(
                pdc.get(OUTPUT_MODE_KEY, PersistentDataType.STRING)
        );
        outputCircuit = ControlCommandChannel.fromStored(
                pdc.get(OUTPUT_CIRCUIT_KEY, PersistentDataType.STRING)
        );
        outputAddress = ControlAddress.fromStoredOrDefault(
                pdc.get(OUTPUT_ADDRESS_KEY, PersistentDataType.STRING),
                ControlAddress.defaultFor(getNodeId(), "batch")
        );

        Byte storedFault = pdc.get(FAULT_KEY, PersistentDataType.BYTE);
        faulted = storedFault != null && storedFault != 0;
        faultAddress = loadFaultAddress(pdc, getNodeId(), outputAddress);
        resetAddress = loadResetAddress(
                pdc,
                getNodeId(),
                outputAddress,
                faultAddress
        );
        Long storedWatchdog = pdc.get(WATCHDOG_TICKS_KEY, PersistentDataType.LONG);
        watchdogTicks = clampWatchdogTicks(
                storedWatchdog == null ? DEFAULT_WATCHDOG_TICKS : storedWatchdog
        );
    }

    @Override
    protected void beforeActivated() {
        resetEdge.reset();
        paceTracker.reset();
        cancelWatchdog();
    }

    @Override
    protected void afterActivated() {
        scheduleWatchdog();
    }

    @Override
    protected void afterDeactivated() {
        cancelWatchdog();
        paceTracker.reset();
        resetEdge.reset();
    }

    @Override
    protected void afterRemoved() {
        cancelWatchdog();
        paceTracker.reset();
        resetEdge.reset();
    }

    @Override
    public boolean accepts(@NotNull ControlChannel channel) {
        return GridWorksChannels.MACHINE_OBSERVED_CYCLES.equals(channel)
                || resetAddress.channel().equals(channel);
    }

    @Override
    protected void handleSignal(@NotNull ControlSignal signal) {
        if (resetAddress.channel().equals(signal.channel())
                && signal.value() instanceof ControlValue.BooleanValue booleanValue) {
            boolean reset = booleanValue.value();
            runOnServerThreadIfActive(() -> {
                if (resetEdge.observe(reset)) {
                    resetBatch();
                }
            });
            return;
        }

        if (!(signal.value() instanceof ControlValue.NumberValue numberValue)) {
            return;
        }

        long sourceCount = exactCycleCount(numberValue.value());
        if (sourceCount < 0L) {
            return;
        }

        UUID source = signal.source();
        runOnServerThreadIfActive(() -> observeSource(source, sourceCount));
    }

    @Override
    public void onControlPeerAvailable(UUID peerId) {
        activePeers.add(Objects.requireNonNull(peerId, "peerId"));
        tracker.forgetSource(peerId);
        runOnServerThreadIfActive(() -> {
            paceTracker.reset();
            publishCurrentState();
            sourcesItem.notifyWindows();
            paceItem.notifyWindows();
        });
    }

    @Override
    public void onControlPeerUnavailable(UUID peerId) {
        activePeers.remove(Objects.requireNonNull(peerId, "peerId"));
        tracker.forgetSource(peerId);
        runOnServerThreadIfActive(() -> {
            paceTracker.reset();
            publishCurrentState();
            sourcesItem.notifyWindows();
            paceItem.notifyWindows();
        });
    }

    @Override
    protected void writeNodeData(@NotNull PersistentDataContainer pdc) {
        pdc.set(TARGET_KEY, PersistentDataType.LONG, tracker.target());
        pdc.set(PROGRESS_KEY, PersistentDataType.LONG, tracker.progress());
        pdc.set(OUTPUT_MODE_KEY, PersistentDataType.STRING, outputMode.name());
        pdc.set(OUTPUT_CIRCUIT_KEY, PersistentDataType.STRING, outputCircuit.name());
        pdc.set(OUTPUT_ADDRESS_KEY, PersistentDataType.STRING, outputAddress.value());
        pdc.set(FAULT_KEY, PersistentDataType.BYTE, faulted ? (byte) 1 : (byte) 0);
        pdc.set(FAULT_ADDRESS_KEY, PersistentDataType.STRING, faultAddress.value());
        pdc.set(RESET_ADDRESS_KEY, PersistentDataType.STRING, resetAddress.value());
        pdc.set(WATCHDOG_TICKS_KEY, PersistentDataType.LONG, watchdogTicks);
    }

    @Override
    public void publishCurrentState() {
        var bus = GridWorks.getInstance().getControlBus();
        boolean complete = tracker.isComplete() && !faulted;
        bus.publish(
                getNodeId(),
                GridWorksChannels.BATCH_PROGRESS,
                ControlValue.of((double) tracker.progress())
        );
        bus.publish(
                getNodeId(),
                GridWorksChannels.BATCH_TARGET,
                ControlValue.of((double) tracker.target())
        );
        bus.publish(
                getNodeId(),
                GridWorksChannels.BATCH_COMPLETE,
                ControlValue.of(complete)
        );
        bus.publish(
                getNodeId(),
                GridWorksChannels.BATCH_FAULT,
                ControlValue.of(faulted)
        );
        bus.publish(
                getNodeId(),
                GridWorksChannels.BATCH_WATCHDOG_TICKS,
                ControlValue.of((double) watchdogTicks)
        );

        boolean paceAvailable = !faulted && paceTracker.isAvailable();
        bus.publish(
                getNodeId(),
                GridWorksChannels.BATCH_RATE_AVAILABLE,
                ControlValue.of(paceAvailable)
        );
        if (paceAvailable) {
            bus.publish(
                    getNodeId(),
                    GridWorksChannels.BATCH_RATE_PER_MINUTE,
                    ControlValue.of(paceTracker.ratePerMinute())
            );
            paceTracker.etaSeconds(tracker.remaining()).ifPresent(eta ->
                    bus.publish(
                            getNodeId(),
                            GridWorksChannels.BATCH_ETA_SECONDS,
                            ControlValue.of(eta)
                    )
            );
        }

        bus.publish(
                getNodeId(),
                currentOutputChannel(),
                ControlValue.of(complete)
        );
        bus.publish(
                getNodeId(),
                faultAddress.channel(),
                ControlValue.of(faulted)
        );
    }

    @Override
    public @NotNull Gui createGui() {
        return Gui.builder()
                .setStructure(
                        "p # t # s # r # o",
                        "# # m # c # a # #",
                        "w # f # u # v # #"
                )
                .addIngredient('#', GuiItems.background())
                .addIngredient('p', progressItem)
                .addIngredient('t', targetItem)
                .addIngredient('s', sourcesItem)
                .addIngredient('r', resetItem)
                .addIngredient('o', outputStateItem)
                .addIngredient('m', outputModeItem)
                .addIngredient('c', outputCircuitItem)
                .addIngredient('a', outputAddressItem)
                .addIngredient('w', watchdogItem)
                .addIngredient('f', faultAddressItem)
                .addIngredient('u', resetAddressItem)
                .addIngredient('v', paceItem)
                .build();
    }

    public long getBatchProgress() {
        return tracker.progress();
    }

    public long getBatchTarget() {
        return tracker.target();
    }

    public boolean isBatchComplete() {
        return tracker.isComplete() && !faulted;
    }

    public boolean isBatchFaulted() {
        return faulted;
    }

    public long getWatchdogTicks() {
        return watchdogTicks;
    }

    public boolean isBatchRateAvailable() {
        return !faulted && paceTracker.isAvailable();
    }

    public double getBatchRatePerMinute() {
        return paceTracker.ratePerMinute();
    }

    public @NotNull OptionalDouble getBatchEtaSeconds() {
        if (faulted) {
            return OptionalDouble.empty();
        }
        return paceTracker.etaSeconds(tracker.remaining());
    }

    public @NotNull ControlAddress getFaultAddress() {
        return faultAddress;
    }

    public @NotNull ControlAddress getResetAddress() {
        return resetAddress;
    }

    public int getTrackedSourceCount() {
        return tracker.trackedSourceCount();
    }

    public @NotNull ControlOutputMode getOutputMode() {
        return outputMode;
    }

    public @NotNull ControlCommandChannel getOutputCircuit() {
        return outputCircuit;
    }

    public @NotNull ControlAddress getOutputAddress() {
        return outputAddress;
    }

    public @NotNull ControlChannel getOutputChannel() {
        return currentOutputChannel();
    }

    private void observeSource(UUID source, long sourceCount) {
        if (!activePeers.contains(source)) {
            return;
        }

        if (faulted) {
            int previousSources = tracker.trackedSourceCount();
            tracker.rebaseline(source, sourceCount);
            if (tracker.trackedSourceCount() != previousSources) {
                sourcesItem.notifyWindows();
            }
            return;
        }

        int previousSources = tracker.trackedSourceCount();
        BatchProgressTracker.Observation observation = tracker.observe(source, sourceCount);
        if (tracker.trackedSourceCount() != previousSources) {
            sourcesItem.notifyWindows();
        }

        if (observation.appliedDelta() <= 0L) {
            return;
        }

        paceTracker.observeProgress(
                observation.appliedDelta(),
                System.nanoTime()
        );

        if (tracker.isComplete()) {
            cancelWatchdog();
        } else {
            scheduleWatchdog();
        }
        publishCurrentState();
        notifyStateItems();
    }

    private void resetBatch() {
        tracker.resetProgress();
        paceTracker.reset();
        faulted = false;
        scheduleWatchdog();
        publishCurrentState();
        notifyStateItems();
        notifyFaultItems();
    }

    private void changeTarget(int direction, boolean largeStep) {
        long current = tracker.target();
        long step = largeStep ? 64L : 1L;
        long next;

        if (direction >= 0) {
            next = current > BatchProgressTracker.MAX_EXACT_COUNT - step
                    ? BatchProgressTracker.MAX_EXACT_COUNT
                    : current + step;
        } else {
            next = Math.max(1L, current - step);
        }

        if (next == current) {
            return;
        }

        tracker.setTarget(next);
        if (!faulted) {
            if (tracker.isComplete()) {
                cancelWatchdog();
            } else {
                scheduleWatchdog();
            }
        }
        publishCurrentState();
        notifyStateItems();
    }

    private void toggleOutputMode() {
        ControlChannel previous = currentOutputChannel();
        clearOutputChannel(previous);
        outputMode = outputMode.toggle();
        publishCurrentState();
        notifyOutputItems();
    }

    private void changeOutputCircuit(int direction) {
        if (outputMode != ControlOutputMode.CIRCUIT) {
            return;
        }

        ControlCommandChannel next = outputCircuit.cycle(direction);
        if (next == outputCircuit) {
            return;
        }

        clearOutputChannel(outputCircuit.channel());
        outputCircuit = next;
        publishCurrentState();
        notifyOutputItems();
    }

    private void setOutputAddress(ControlAddress next) {
        Objects.requireNonNull(next, "next");
        if (faultAddress.equals(next)) {
            throw new IllegalArgumentException(
                    "That address is already used by the batch fault output."
            );
        }
        if (resetAddress.equals(next)) {
            throw new IllegalArgumentException(
                    "That address is already used by the batch reset input."
            );
        }
        if (outputAddress.equals(next)) {
            return;
        }

        if (outputMode == ControlOutputMode.ADDRESS) {
            clearOutputChannel(outputAddress.channel());
        }

        outputAddress = next;
        if (outputMode == ControlOutputMode.ADDRESS) {
            publishCurrentState();
        }
        notifyOutputItems();
    }

    private void setFaultAddress(ControlAddress next) {
        Objects.requireNonNull(next, "next");
        if (outputAddress.equals(next)) {
            throw new IllegalArgumentException(
                    "That address is already used by the batch completion output."
            );
        }
        if (resetAddress.equals(next)) {
            throw new IllegalArgumentException(
                    "That address is already used by the batch reset input."
            );
        }
        if (faultAddress.equals(next)) {
            return;
        }

        clearOutputChannel(faultAddress.channel());
        faultAddress = next;
        publishCurrentState();
        notifyFaultItems();
    }

    private void setResetAddress(ControlAddress next) {
        Objects.requireNonNull(next, "next");
        if (outputAddress.equals(next) || faultAddress.equals(next)) {
            throw new IllegalArgumentException(
                    "That address is already used by a batch output."
            );
        }
        if (resetAddress.equals(next)) {
            return;
        }

        resetAddress = next;
        resetEdge.reset();
        notifyFaultItems();
        GridWorks.getInstance()
                .getPhysicalControlNetwork()
                .replayStateSources(getNodeId());
    }

    private void changeWatchdog(long delta) {
        long current = watchdogTicks;
        long next = Math.max(
                0L,
                Math.min(MAX_WATCHDOG_TICKS, current + delta)
        );
        if (next == current) {
            return;
        }

        watchdogTicks = next;
        if (!faulted && !tracker.isComplete()) {
            scheduleWatchdog();
        } else {
            cancelWatchdog();
        }
        publishCurrentState();
        notifyFaultItems();
    }

    private void scheduleWatchdog() {
        cancelWatchdog();
        if (watchdogTicks <= 0L || faulted || tracker.isComplete()) {
            return;
        }

        long expectedProgress = tracker.progress();
        watchdogTask = GridWorks.getInstance().getServer().getScheduler().runTaskLater(
                GridWorks.getInstance(),
                () -> {
                    watchdogTask = null;
                    runOnServerThreadIfActive(
                            () -> faultIfStillStalled(expectedProgress)
                    );
                },
                watchdogTicks
        );
    }

    private void faultIfStillStalled(long expectedProgress) {
        if (faulted
                || tracker.isComplete()
                || tracker.progress() != expectedProgress) {
            return;
        }

        faulted = true;
        paceTracker.reset();
        cancelWatchdog();
        publishCurrentState();
        notifyStateItems();
        notifyFaultItems();
    }

    private void cancelWatchdog() {
        BukkitTask task = watchdogTask;
        watchdogTask = null;
        if (task != null) {
            task.cancel();
        }
    }

    private ControlChannel currentOutputChannel() {
        return outputMode == ControlOutputMode.ADDRESS
                ? outputAddress.channel()
                : outputCircuit.channel();
    }

    private void clearOutputChannel(ControlChannel channel) {
        GridWorks.getInstance().getControlBus().publish(
                getNodeId(),
                channel,
                ControlValue.of(false)
        );
    }

    private void notifyStateItems() {
        progressItem.notifyWindows();
        targetItem.notifyWindows();
        resetItem.notifyWindows();
        outputStateItem.notifyWindows();
        paceItem.notifyWindows();
    }

    private void notifyOutputItems() {
        outputModeItem.notifyWindows();
        outputCircuitItem.notifyWindows();
        outputAddressItem.notifyWindows();
        outputStateItem.notifyWindows();
    }

    private void notifyFaultItems() {
        watchdogItem.notifyWindows();
        faultAddressItem.notifyWindows();
        resetAddressItem.notifyWindows();
        progressItem.notifyWindows();
        outputStateItem.notifyWindows();
        paceItem.notifyWindows();
    }

    private void openAddressWindow(
            Player player,
            String title,
            ControlAddress current,
            Consumer<ControlAddress> setter
    ) {
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
                                .name(Component.text(title, NamedTextColor.GOLD))
                                .lore(
                                        Component.text("Spaces normalize to underscores.", NamedTextColor.GRAY),
                                        Component.text(
                                                "Completion and fault addresses must be distinct.",
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
                    "Could not open Batch Controller address window",
                    exception
            );
            player.sendMessage(Component.text(
                    "GridWorks could not open the address window.",
                    NamedTextColor.RED
            ));
        }
    }

    private static ControlAddress loadResetAddress(
            PersistentDataContainer pdc,
            UUID nodeId,
            ControlAddress outputAddress,
            ControlAddress faultAddress
    ) {
        ControlAddress fallback = defaultResetAddress(
                nodeId,
                outputAddress,
                faultAddress
        );
        ControlAddress stored = ControlAddress.fromStoredOrDefault(
                pdc.get(RESET_ADDRESS_KEY, PersistentDataType.STRING),
                fallback
        );
        return stored.equals(outputAddress) || stored.equals(faultAddress)
                ? fallback
                : stored;
    }

    private static ControlAddress defaultResetAddress(
            UUID nodeId,
            ControlAddress outputAddress,
            ControlAddress faultAddress
    ) {
        for (int attempt = 1; attempt <= 100; attempt++) {
            String prefix = attempt == 1 ? "batch_reset" : "batch_reset_" + attempt;
            ControlAddress candidate = ControlAddress.defaultFor(nodeId, prefix);
            if (!candidate.equals(outputAddress) && !candidate.equals(faultAddress)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate a unique batch reset address");
    }

    private static ControlAddress loadFaultAddress(
            PersistentDataContainer pdc,
            UUID nodeId,
            ControlAddress outputAddress
    ) {
        ControlAddress fallback = defaultFaultAddress(nodeId, outputAddress);
        ControlAddress stored = ControlAddress.fromStoredOrDefault(
                pdc.get(FAULT_ADDRESS_KEY, PersistentDataType.STRING),
                fallback
        );
        return stored.equals(outputAddress) ? fallback : stored;
    }

    private static ControlAddress defaultFaultAddress(
            UUID nodeId,
            ControlAddress outputAddress
    ) {
        for (int attempt = 1; attempt <= 100; attempt++) {
            String prefix = attempt == 1 ? "batch_fault" : "batch_fault_" + attempt;
            ControlAddress candidate = ControlAddress.defaultFor(nodeId, prefix);
            if (!candidate.equals(outputAddress)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate a unique batch fault address");
    }

    private static long clampWatchdogTicks(long ticks) {
        return Math.max(0L, Math.min(MAX_WATCHDOG_TICKS, ticks));
    }

    private static String formatWatchdog(long ticks) {
        if (ticks <= 0L) {
            return "OFF";
        }
        if (ticks % 20L == 0L) {
            return (ticks / 20L) + "s";
        }
        return String.format(java.util.Locale.ROOT, "%.2fs", ticks / 20.0);
    }

    private static long exactCycleCount(double value) {
        if (!Double.isFinite(value)
                || value < 0.0
                || value > BatchProgressTracker.MAX_EXACT_COUNT
                || value != Math.rint(value)) {
            return -1L;
        }
        return (long) value;
    }

    private static long validTargetOrDefault(Long stored) {
        if (stored == null
                || stored < 1L
                || stored > BatchProgressTracker.MAX_EXACT_COUNT) {
            return BatchProgressTracker.DEFAULT_TARGET;
        }
        return stored;
    }

    private static long validProgressOrZero(Long stored) {
        if (stored == null || stored < 0L) {
            return 0L;
        }
        return Math.min(stored, BatchProgressTracker.MAX_EXACT_COUNT);
    }

    private static NamespacedKey key(String value) {
        return Objects.requireNonNull(NamespacedKey.fromString("gridworks:" + value));
    }

    private abstract class BatchItem extends AbstractItem {
        protected ItemStackBuilder item(Material material, String name) {
            return ItemStackBuilder.of(material)
                    .name(Component.text(name, NamedTextColor.GOLD));
        }
    }

    private final class ProgressItem extends BatchItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            boolean complete = tracker.isComplete() && !faulted;
            return item(
                    faulted
                            ? Material.RED_CONCRETE
                            : complete ? Material.LIME_CONCRETE : Material.CRAFTER,
                    "Batch Progress"
            ).lore(
                    Component.text(
                            tracker.progress() + " / " + tracker.target(),
                            complete ? NamedTextColor.GREEN : NamedTextColor.WHITE
                    ),
                    Component.text(
                            faulted
                                    ? "FAULT: no progress watchdog expired"
                                    : complete
                                    ? "Target reached"
                                    : tracker.remaining() + " cycle(s) remaining",
                            faulted
                                    ? NamedTextColor.RED
                                    : complete ? NamedTextColor.GREEN : NamedTextColor.AQUA
                    ),
                    Component.text(
                            "Only new cycle deltas while directly linked count",
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

    private final class TargetItem extends BatchItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(Material.TARGET, "Target: " + tracker.target())
                    .lore(
                            Component.text("Left +1 / Right -1", NamedTextColor.YELLOW),
                            Component.text("Shift uses 64 cycles", NamedTextColor.YELLOW),
                            Component.text(
                                    "Changing target reevaluates completion immediately",
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
            int direction;
            if (clickType.isLeftClick()) {
                direction = 1;
            } else if (clickType.isRightClick()) {
                direction = -1;
            } else {
                return;
            }
            changeTarget(direction, clickType.isShiftClick());
        }
    }

    private final class SourcesItem extends BatchItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(Material.SPYGLASS, "Cycle Sources")
                    .lore(
                            Component.text(
                                    "Direct peers: " + activePeers.size(),
                                    NamedTextColor.WHITE
                            ),
                            Component.text(
                                    "Cycle baselines: " + tracker.trackedSourceCount(),
                                    NamedTextColor.AQUA
                            ),
                            Component.text(
                                    "First count after link/relink is baseline only",
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

    private final class ResetItem extends BatchItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(Material.BARRIER, "Start New Batch")
                    .lore(
                            Component.text(
                                    "Shift + right click to reset progress to 0",
                                    NamedTextColor.YELLOW
                            ),
                            Component.text(
                                    "Live source baselines are preserved",
                                    NamedTextColor.GRAY
                            ),
                            Component.text(
                                    "Also clears a latched batch fault",
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
            if (clickType.isRightClick() && clickType.isShiftClick()) {
                resetBatch();
            }
        }
    }

    private final class OutputModeItem extends BatchItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(Material.COMPARATOR, "Output Mode: " + outputMode.displayName())
                    .lore(
                            Component.text(
                                    "Click to switch Circuit / Address",
                                    NamedTextColor.YELLOW
                            ),
                            Component.text(
                                    "Old active route is cleared before switching",
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
            if (clickType.isLeftClick() || clickType.isRightClick()) {
                toggleOutputMode();
            }
        }
    }

    private final class OutputCircuitItem extends BatchItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            ItemStackBuilder builder = item(
                    Material.REDSTONE_TORCH,
                    "Output Circuit: " + outputCircuit.displayName()
            );
            if (outputMode != ControlOutputMode.CIRCUIT) {
                return builder.lore(Component.text(
                        "Switch output mode to Circuit to edit",
                        NamedTextColor.DARK_GRAY
                ));
            }
            return builder.lore(
                    Component.text("Left next / Right previous", NamedTextColor.YELLOW)
            );
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            if (clickType.isLeftClick()) {
                changeOutputCircuit(1);
            } else if (clickType.isRightClick()) {
                changeOutputCircuit(-1);
            }
        }
    }

    private final class OutputAddressItem extends BatchItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            ItemStackBuilder builder = item(
                    Material.NAME_TAG,
                    "Output Address: " + outputAddress.value()
            );
            if (outputMode != ControlOutputMode.ADDRESS) {
                return builder.lore(Component.text(
                        "Switch output mode to Address to edit",
                        NamedTextColor.DARK_GRAY
                ));
            }
            return builder.lore(Component.text(
                    "Click to edit addressed output",
                    NamedTextColor.YELLOW
            ));
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            if (outputMode != ControlOutputMode.ADDRESS
                    || (!clickType.isLeftClick() && !clickType.isRightClick())) {
                return;
            }

            player.closeInventory();
            GridWorks.getInstance().getServer().getScheduler().runTask(
                    GridWorks.getInstance(),
                    () -> openAddressWindow(
                            player,
                            "Batch Completion Output",
                            outputAddress,
                            BatchControllerBlock.this::setOutputAddress
                    )
            );
        }
    }

    private final class WatchdogItem extends BatchItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(
                    watchdogTicks <= 0L ? Material.GRAY_DYE : Material.CLOCK,
                    "No-Progress Watchdog: " + formatWatchdog(watchdogTicks)
            ).lore(
                    Component.text("Left +5s / Right -5s", NamedTextColor.YELLOW),
                    Component.text("Shift uses 60 seconds", NamedTextColor.YELLOW),
                    Component.text(
                            "OFF preserves wait-forever behavior",
                            NamedTextColor.GRAY
                    ),
                    Component.text(
                            faulted
                                    ? "FAULT latched until Start New Batch"
                                    : "Deadline resets whenever batch progress increases",
                            faulted ? NamedTextColor.RED : NamedTextColor.DARK_GRAY
                    )
            );
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            long step = clickType.isShiftClick() ? 1_200L : 100L;
            if (clickType.isLeftClick()) {
                changeWatchdog(step);
            } else if (clickType.isRightClick()) {
                changeWatchdog(-step);
            }
        }
    }

    private final class FaultAddressItem extends BatchItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(
                    faulted ? Material.REDSTONE_BLOCK : Material.NAME_TAG,
                    "Fault Output: " + faultAddress.value()
            ).lore(
                    Component.text(
                            faultAddress.channel().toString(),
                            NamedTextColor.AQUA
                    ),
                    Component.text(
                            faulted ? "Output: ON (latched)" : "Output: OFF",
                            faulted ? NamedTextColor.RED : NamedTextColor.GRAY
                    ),
                    Component.text(
                            "Click to edit addressed fault output",
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
            if (!clickType.isLeftClick() && !clickType.isRightClick()) {
                return;
            }

            player.closeInventory();
            GridWorks.getInstance().getServer().getScheduler().runTask(
                    GridWorks.getInstance(),
                    () -> openAddressWindow(
                            player,
                            "Batch Fault Output",
                            faultAddress,
                            BatchControllerBlock.this::setFaultAddress
                    )
            );
        }
    }

    private final class ResetAddressItem extends BatchItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(Material.ENDER_EYE, "Reset / Start Input")
                    .lore(
                            Component.text(resetAddress.value(), NamedTextColor.AQUA),
                            Component.text(
                                    resetAddress.channel().toString(),
                                    NamedTextColor.DARK_GRAY
                            ),
                            Component.text(
                                    "Rising edge starts a fresh batch and clears fault",
                                    NamedTextColor.GRAY
                            ),
                            Component.text(
                                    "First replayed state is baseline only",
                                    NamedTextColor.DARK_GRAY
                            ),
                            Component.text("Click to edit", NamedTextColor.YELLOW)
                    );
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            if (!clickType.isLeftClick() && !clickType.isRightClick()) {
                return;
            }

            player.closeInventory();
            GridWorks.getInstance().getServer().getScheduler().runTask(
                    GridWorks.getInstance(),
                    () -> openAddressWindow(
                            player,
                            "Batch Reset Input",
                            resetAddress,
                            BatchControllerBlock.this::setResetAddress
                    )
            );
        }
    }

    private final class PaceItem extends BatchItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            if (faulted) {
                return item(Material.RED_DYE, "Production Pace: UNAVAILABLE")
                        .lore(Component.text(
                                "Batch fault is latched",
                                NamedTextColor.RED
                        ));
            }

            if (!paceTracker.isAvailable()) {
                return item(Material.CLOCK, "Production Pace: LEARNING")
                        .lore(
                                Component.text(
                                        "Two positive progress events establish pace",
                                        NamedTextColor.GRAY
                                ),
                                Component.text(
                                        "Resets after reload or new batch",
                                        NamedTextColor.DARK_GRAY
                                )
                        );
            }

            double rate = paceTracker.ratePerMinute();
            OptionalDouble eta = paceTracker.etaSeconds(tracker.remaining());
            return item(Material.CLOCK, "Production Pace")
                    .lore(
                            Component.text(
                                    String.format(
                                            Locale.ROOT,
                                            "%.2f cycles/min",
                                            rate
                                    ),
                                    NamedTextColor.AQUA
                            ),
                            Component.text(
                                    eta.isPresent()
                                            ? String.format(
                                                    Locale.ROOT,
                                                    "ETA: %.1f seconds",
                                                    eta.orElseThrow()
                                            )
                                            : "ETA unavailable",
                                    NamedTextColor.WHITE
                            ),
                            Component.text(
                                    "Based on the latest positive progress interval",
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

    private final class OutputStateItem extends BatchItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            boolean complete = tracker.isComplete() && !faulted;
            return item(
                    complete ? Material.LIME_DYE : Material.RED_DYE,
                    complete ? "Output: ON" : "Output: OFF"
            ).lore(
                    Component.text(
                            outputMode.displayName() + " / " + currentOutputChannel(),
                            NamedTextColor.AQUA
                    ),
                    Component.text(
                            faulted
                                    ? "Batch fault latched; reset required"
                                    : complete ? "Batch target reached" : "Batch still running",
                            faulted
                                    ? NamedTextColor.RED
                                    : complete ? NamedTextColor.GREEN : NamedTextColor.GRAY
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
}
