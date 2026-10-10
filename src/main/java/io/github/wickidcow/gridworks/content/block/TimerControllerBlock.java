package io.github.wickidcow.gridworks.content.block;

import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.block.interfaces.GuiRebarBlock;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.pylonmc.rebar.util.gui.GuiItems;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.api.control.BooleanInputConfigurable;
import io.github.wickidcow.gridworks.api.control.BooleanInputMode;
import io.github.wickidcow.gridworks.api.control.ControlAddress;
import io.github.wickidcow.gridworks.api.control.ControlChannel;
import io.github.wickidcow.gridworks.api.control.ControlCommandChannel;
import io.github.wickidcow.gridworks.api.control.ControlOutputMode;
import io.github.wickidcow.gridworks.api.control.ControlSignal;
import io.github.wickidcow.gridworks.api.control.ControlStateSource;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.control.TimerCycleEngine;
import io.github.wickidcow.gridworks.control.TimerInputSelector;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
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
 * A physical, loaded-only Rebar timer. Settings persist; running tasks do not.
 *
 * <p>Input comes from one directly linked loaded source. On restart or relink
 * the first observed boolean is merely a baseline, never an ON edge. Each
 * active timer owns at most one delayed task and advances at most one phase
 * when the server wakes up late.</p>
 */
public final class TimerControllerBlock extends PhysicalControlNodeBlock
        implements GuiRebarBlock, BooleanInputConfigurable, ControlStateSource {

    private static final NamespacedKey MODE_KEY = key("timer_mode");
    private static final NamespacedKey INITIAL_KEY = key("timer_initial_ticks");
    private static final NamespacedKey ON_KEY = key("timer_on_ticks");
    private static final NamespacedKey OFF_KEY = key("timer_off_ticks");
    private static final NamespacedKey INPUT_KEY = key("timer_input_mode");
    private static final NamespacedKey SOURCE_KEY = key("timer_input_source");
    private static final NamespacedKey OUTPUT_KEY = key("timer_output_circuit");
    private static final NamespacedKey OUTPUT_MODE_KEY = key("timer_output_mode");
    private static final NamespacedKey OUTPUT_ADDRESS_KEY = key("timer_output_address");

    private static final long DEFAULT_INITIAL_TICKS = 0L;
    private static final long DEFAULT_ON_TICKS = 20L;
    private static final long DEFAULT_OFF_TICKS = 20L;

    private final TimerCycleEngine engine;
    private long initialTicks;
    private long onTicks;
    private long offTicks;
    private BooleanInputMode inputMode;
    private ControlCommandChannel outputCircuit;
    private ControlOutputMode outputMode;
    private ControlAddress outputAddress;
    private final TimerInputSelector sourceSelector;
    private BukkitTask scheduled;

    private final ControllerItem modeItem = new ControllerItem("mode");
    private final ControllerItem inputItem = new ControllerItem("input");
    private final ControllerItem sourceItem = new ControllerItem("source");
    private final ControllerItem outputModeItem = new ControllerItem("output_mode");
    private final ControllerItem circuitItem = new ControllerItem("circuit");
    private final ControllerItem addressItem = new ControllerItem("address");
    private final ControllerItem initialItem = new ControllerItem("initial");
    private final ControllerItem onItem = new ControllerItem("on");
    private final ControllerItem offItem = new ControllerItem("off");
    private final ControllerItem statusItem = new ControllerItem("status");
    private final ControllerItem startItem = new ControllerItem("start");
    private final ControllerItem stopItem = new ControllerItem("stop");

    public TimerControllerBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
        initialTicks = DEFAULT_INITIAL_TICKS;
        onTicks = DEFAULT_ON_TICKS;
        offTicks = DEFAULT_OFF_TICKS;
        inputMode = BooleanInputMode.REDSTONE;
        outputCircuit = ControlCommandChannel.DEFAULT;
        outputMode = ControlOutputMode.CIRCUIT;
        outputAddress = ControlAddress.defaultFor(getNodeId(), "timer");
        engine = new TimerCycleEngine(TimerCycleEngine.Mode.ONE_SHOT, initialTicks, onTicks, offTicks);
        sourceSelector = new TimerInputSelector(null);
    }

    public TimerControllerBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);
        TimerCycleEngine.Mode mode;
        try {
            mode = TimerCycleEngine.Mode.valueOf(pdc.getOrDefault(
                    MODE_KEY, PersistentDataType.STRING, TimerCycleEngine.Mode.ONE_SHOT.name()));
        } catch (IllegalArgumentException exception) {
            mode = TimerCycleEngine.Mode.ONE_SHOT;
        }
        initialTicks = duration(pdc.get(INITIAL_KEY, PersistentDataType.LONG),
                DEFAULT_INITIAL_TICKS, 0L);
        onTicks = duration(pdc.get(ON_KEY, PersistentDataType.LONG), DEFAULT_ON_TICKS, 1L);
        offTicks = duration(pdc.get(OFF_KEY, PersistentDataType.LONG), DEFAULT_OFF_TICKS, 1L);
        if (mode == TimerCycleEngine.Mode.REPEATING_PULSE) {
            onTicks = 1L;
        }
        // Old data, if any, is never allowed to revive a pending task.
        engine = new TimerCycleEngine(mode, initialTicks, onTicks, offTicks);
        sourceSelector = new TimerInputSelector(uuidFromStored(pdc.get(SOURCE_KEY, PersistentDataType.STRING)));
        inputMode = BooleanInputMode.fromStored(pdc.get(INPUT_KEY, PersistentDataType.STRING));
        // This is a new item: default to Redstone-only when the setting is missing.
        if (!pdc.has(INPUT_KEY, PersistentDataType.STRING)) {
            inputMode = BooleanInputMode.REDSTONE;
        }
        outputCircuit = ControlCommandChannel.fromStored(pdc.get(OUTPUT_KEY, PersistentDataType.STRING));
        outputMode = ControlOutputMode.fromStored(pdc.get(OUTPUT_MODE_KEY, PersistentDataType.STRING));
        outputAddress = ControlAddress.fromStoredOrDefault(
                pdc.get(OUTPUT_ADDRESS_KEY, PersistentDataType.STRING),
                ControlAddress.defaultFor(getNodeId(), "timer"));
    }

    @Override
    public boolean accepts(@NotNull ControlChannel channel) {
        return inputMode.accepts(channel);
    }

    @Override
    protected void beforeActivated() {
        cancelScheduled();
        engine.resetAfterLoad();
        sourceSelector.resetActive();
    }

    @Override
    protected void afterActivated() {
        // Activation may already have replayed a current input. Reflect the
        // engine's actual state instead of blindly darkening the visual.
        setVisual(engine.state().output());
        publishCurrentState();
    }

    @Override
    protected void handleSignal(@NotNull ControlSignal signal) {
        runOnServerThreadIfActive(() -> {
            if (!inputMode.accepts(signal.channel())
                    || !(signal.value() instanceof ControlValue.BooleanValue input)) {
                return;
            }
            // Source selection is tied to direct physical links so losing that
            // peer can fail safely OFF. No world scan or remote chunk loading.
            if (!sourceSelector.accept(signal.source(), GridWorks.getInstance()
                    .getPhysicalControlNetwork().isLinked(getNodeId(), signal.source()))) {
                return;
            }
            boolean before = engine.state().output();
            engine.observeInput(input.value(), currentTick());
            reconcile(before);
        });
    }

    @Override
    public void onControlPeerUnavailable(@NotNull UUID peerId) {
        runOnServerThreadIfActive(() -> {
            if (sourceSelector.peerUnavailable(peerId)) {
                boolean before = engine.state().output();
                engine.resetAfterLoad();
                reconcile(before);
            }
        });
    }

    @Override
    public void publishCurrentState() {
        publishTo(currentOutputChannel(), engine.state().output());
    }

    @Override
    protected void beforeDeactivated() {
        sendOffBeforeDisconnect();
    }

    @Override
    protected void beforeRemoved() {
        sendOffBeforeDisconnect();
    }

    private void sendOffBeforeDisconnect() {
        cancelScheduled();
        if (GridWorks.getInstance().getPhysicalControlNetwork().isActive(getNodeId())) {
            publishTo(currentOutputChannel(), false);
        }
        engine.resetAfterLoad();
        sourceSelector.resetActive();
    }

    @Override
    protected void afterDeactivated() {
        cancelScheduled();
        engine.resetAfterLoad();
        sourceSelector.resetActive();
    }

    @Override
    protected void afterRemoved() {
        cancelScheduled();
        engine.resetAfterLoad();
        sourceSelector.resetActive();
    }

    @Override
    protected void writeNodeData(@NotNull PersistentDataContainer pdc) {
        pdc.set(MODE_KEY, PersistentDataType.STRING, engine.mode().name());
        pdc.set(INITIAL_KEY, PersistentDataType.LONG, initialTicks);
        pdc.set(ON_KEY, PersistentDataType.LONG, onTicks);
        pdc.set(OFF_KEY, PersistentDataType.LONG, offTicks);
        pdc.set(INPUT_KEY, PersistentDataType.STRING, inputMode.name());
        if (sourceSelector.preferredSource() == null) {
            pdc.remove(SOURCE_KEY);
        } else {
            pdc.set(SOURCE_KEY, PersistentDataType.STRING, sourceSelector.preferredSource().toString());
        }
        pdc.set(OUTPUT_KEY, PersistentDataType.STRING, outputCircuit.name());
        pdc.set(OUTPUT_MODE_KEY, PersistentDataType.STRING, outputMode.name());
        pdc.set(OUTPUT_ADDRESS_KEY, PersistentDataType.STRING, outputAddress.value());
    }

    @Override
    public @NotNull BooleanInputMode getBooleanInputMode() {
        return inputMode;
    }

    @Override
    public void setBooleanInputMode(@NotNull BooleanInputMode mode) {
        Objects.requireNonNull(mode, "mode");
        runOnServerThreadIfActive(() -> {
            inputMode = mode;
            sourceSelector.resetActive();
            boolean before = engine.state().output();
            engine.resetAfterLoad();
            reconcile(before);
            GridWorks.getInstance().getPhysicalControlNetwork().replayStateSources(getNodeId());
        });
    }

    @Override
    public @NotNull Component getGuiTitle() {
        return Component.text("Timer Controller", NamedTextColor.GOLD);
    }

    @Override
    public @NotNull Gui createGui() {
        return Gui.builder()
                .setStructure("m i t c a d n f x", "# # # u # z # s #")
                .addIngredient('#', GuiItems.background())
                .addIngredient('m', modeItem)
                .addIngredient('i', inputItem)
                .addIngredient('s', sourceItem)
                .addIngredient('t', outputModeItem)
                .addIngredient('c', circuitItem)
                .addIngredient('a', addressItem)
                .addIngredient('d', initialItem)
                .addIngredient('n', onItem)
                .addIngredient('f', offItem)
                .addIngredient('x', statusItem)
                .addIngredient('u', startItem)
                .addIngredient('z', stopItem)
                .build();
    }

    public boolean isOutputOn() {
        return engine.state().output();
    }

    public @NotNull TimerCycleEngine.State getTimerState() {
        return engine.state();
    }

    private void setMode(int direction) {
        TimerCycleEngine.Mode[] modes = TimerCycleEngine.Mode.values();
        TimerCycleEngine.Mode next = modes[Math.floorMod(engine.mode().ordinal()
                + (direction >= 0 ? 1 : -1), modes.length)];
        if (next == TimerCycleEngine.Mode.REPEATING_PULSE) {
            onTicks = 1L;
        }
        reconfigure(next);
    }

    private void changeDuration(String kind, long delta) {
        switch (kind) {
            case "initial" -> initialTicks = duration(initialTicks + delta, initialTicks, 0L);
            case "on" -> {
                if (engine.mode() != TimerCycleEngine.Mode.REPEATING_PULSE) {
                    onTicks = duration(onTicks + delta, onTicks, 1L);
                }
            }
            case "off" -> offTicks = duration(offTicks + delta, offTicks, 1L);
            default -> throw new IllegalArgumentException("Unknown timer duration");
        }
        reconfigure(engine.mode());
    }

    private void reconfigure(TimerCycleEngine.Mode next) {
        boolean before = engine.state().output();
        sourceSelector.resetActive();
        engine.configure(next, initialTicks, onTicks, offTicks);
        reconcile(before);
        GridWorks.getInstance().getPhysicalControlNetwork().replayStateSources(getNodeId());
    }

    private void cycleSource(int direction) {
        List<UUID> loaded = GridWorks.getInstance()
                .getPhysicalControlNetwork().activeLinkedNodes(getNodeId());
        List<UUID> choices = new ArrayList<>();
        choices.add(null); // AUTO
        choices.addAll(loaded);
        int current = choices.indexOf(sourceSelector.preferredSource());
        int next = Math.floorMod((current < 0 ? 0 : current)
                + (direction >= 0 ? 1 : -1), choices.size());
        UUID selected = choices.get(next);
        if (Objects.equals(selected, sourceSelector.preferredSource())) {
            return;
        }
        boolean before = engine.state().output();
        sourceSelector.select(selected);
        engine.resetAfterLoad();
        reconcile(before);
        GridWorks.getInstance().getPhysicalControlNetwork().replayStateSources(getNodeId());
    }

    private void manualStart() {
        boolean before = engine.state().output();
        engine.manualStart(currentTick());
        reconcile(before);
    }

    private void manualStop() {
        boolean before = engine.state().output();
        engine.stop();
        reconcile(before);
    }

    private void reconcile(boolean previousOutput) {
        boolean current = engine.state().output();
        if (current != previousOutput) {
            setVisual(current);
            publishCurrentState();
        }
        notifyItems();
        scheduleNext();
    }

    private void scheduleNext() {
        cancelScheduled();
        var due = engine.state().nextDueTick();
        if (due.isEmpty()) {
            return;
        }

        long now = currentTick();
        if (due.getAsLong() <= now) {
            boolean before = engine.state().output();
            engine.advance(now);
            reconcile(before);
            return;
        }

        long delay = Math.max(1L, due.getAsLong() - now);
        GridWorks plugin = GridWorks.getInstance();
        scheduled = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            scheduled = null;
            runOnServerThreadIfActive(() -> {
                boolean before = engine.state().output();
                engine.advance(currentTick());
                reconcile(before);
            });
        }, delay);
    }

    private void cancelScheduled() {
        BukkitTask task = scheduled;
        scheduled = null;
        if (task != null) {
            task.cancel();
        }
    }

    private void publishTo(ControlChannel channel, boolean enabled) {
        GridWorks.getInstance().getControlBus().publish(
                getNodeId(), channel, ControlValue.of(enabled));
    }

    private ControlChannel currentOutputChannel() {
        return outputMode == ControlOutputMode.ADDRESS
                ? outputAddress.channel() : outputCircuit.channel();
    }

    private void setOutputCircuit(int direction) {
        ControlChannel old = currentOutputChannel();
        outputCircuit = outputCircuit.cycle(direction);
        if (outputMode == ControlOutputMode.CIRCUIT) {
            publishTo(old, false);
            publishCurrentState();
        }
        notifyItems();
    }

    private void toggleOutputMode() {
        ControlChannel old = currentOutputChannel();
        publishTo(old, false);
        outputMode = outputMode.toggle();
        publishCurrentState();
        notifyItems();
    }

    private void setOutputAddress(ControlAddress address) {
        if (address.equals(outputAddress)) {
            return;
        }
        ControlChannel old = currentOutputChannel();
        outputAddress = address;
        if (outputMode == ControlOutputMode.ADDRESS) {
            publishTo(old, false);
            publishCurrentState();
        }
        notifyItems();
    }

    private void setVisual(boolean powered) {
        if (getBlock().getBlockData() instanceof Lightable light) {
            if (light.isLit() != powered) {
                light.setLit(powered);
                getBlock().setBlockData(light);
            }
        }
    }

    private void notifyItems() {
        modeItem.notifyWindows();
        inputItem.notifyWindows();
        outputModeItem.notifyWindows();
        circuitItem.notifyWindows();
        addressItem.notifyWindows();
        initialItem.notifyWindows();
        onItem.notifyWindows();
        offItem.notifyWindows();
        statusItem.notifyWindows();
        sourceItem.notifyWindows();
    }

    private void openAddressWindow(Player player) {
        final boolean[] initialRename = {true};
        Gui upper = Gui.builder().setStructure("# a #")
                .addIngredient('#', GuiItems.background())
                .addIngredient('a', ItemStackBuilder.of(Material.NAME_TAG)
                        .name(Component.text(outputAddress.value(), NamedTextColor.GOLD)))
                .build();
        Gui lower = Gui.builder()
                .setStructure("# # # # # # # # #", "# # # # i # # # #", "# # # # # # # # #")
                .addIngredient('#', GuiItems.background())
                .addIngredient('i', ItemStackBuilder.of(Material.PAPER)
                        .name(Component.text("Set Timer Output Address", NamedTextColor.GOLD))
                        .lore(Component.text("Example: furnace_clock", NamedTextColor.GRAY)))
                .build();
        try {
            AnvilWindow window = AnvilWindow.builder().setViewer(player)
                    .setUpperGui(upper).setLowerGui(lower)
                    .setTitle(Component.text("Timer Output Address"))
                    .addRenameHandler(raw -> {
                        if (initialRename[0]) {
                            initialRename[0] = false;
                            return;
                        }
                        try {
                            ControlAddress next = ControlAddress.fromUserInput(raw);
                            runOnServerThreadIfActive(() -> setOutputAddress(next));
                        } catch (IllegalArgumentException exception) {
                            player.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
                        }
                    }).build(player);
            window.open();
        } catch (RuntimeException exception) {
            GridWorks.getInstance().getLogger().log(java.util.logging.Level.SEVERE,
                    "Could not open Timer Controller address window", exception);
            player.sendMessage(Component.text("Could not open the Timer address editor.", NamedTextColor.RED));
        }
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    private static UUID uuidFromStored(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static long currentTick() {
        return Integer.toUnsignedLong(Bukkit.getCurrentTick());
    }

    private static long duration(Long value, long fallback, long minimum) {
        return value == null ? fallback
                : Math.clamp(value, minimum, TimerCycleEngine.MAX_DURATION_TICKS);
    }

    private static String displayDuration(long ticks) {
        return String.format(java.util.Locale.ROOT, "%.2fs", ticks / 20.0);
    }

    private static NamespacedKey key(String name) {
        return Objects.requireNonNull(NamespacedKey.fromString("gridworks:" + name));
    }

    private final class ControllerItem extends AbstractItem {
        private final String kind;
        private ControllerItem(String kind) {
            this.kind = kind;
        }

        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            ItemStackBuilder builder;
            switch (kind) {
                case "mode" -> builder = item(Material.CLOCK, "Mode: " + engine.mode().name())
                        .lore(Component.text("Left/right: cycle One-shot / Pulse / Duty", NamedTextColor.YELLOW));
                case "input" -> builder = item(Material.OBSERVER, "Input: " + inputMode.displayName())
                        .lore(Component.text("Left/right: cycle Control Bus input", NamedTextColor.YELLOW),
                                Component.text("First state establishes a baseline", NamedTextColor.GRAY));
                case "source" -> {
                    UUID preferred = sourceSelector.preferredSource();
                    UUID active = sourceSelector.activeSource();
                    builder = item(Material.COMPASS, "Input source: "
                            + (preferred == null ? "AUTO" : shortId(preferred)))
                            .lore(Component.text("Left/right: choose a loaded direct link",
                                    NamedTextColor.YELLOW),
                                    Component.text("Active: " + (active == null ? "WAITING" : shortId(active)),
                                            NamedTextColor.GRAY),
                                    Component.text("An explicit source stays selected during unload",
                                            NamedTextColor.DARK_GRAY));
                }
                case "output_mode" -> builder = item(Material.ENDER_EYE,
                        "Output mode: " + outputMode.displayName())
                        .lore(Component.text("Click to switch Circuit / Address", NamedTextColor.YELLOW));
                case "circuit" -> builder = item(Material.REDSTONE,
                        "Output circuit: " + outputCircuit.displayName())
                        .lore(Component.text(outputMode == ControlOutputMode.CIRCUIT
                                ? "Left/right: cycle Default, A-D"
                                : "Switch output mode to Circuit to edit", NamedTextColor.YELLOW));
                case "address" -> builder = item(Material.NAME_TAG, "Address: " + outputAddress.value())
                        .lore(Component.text(outputMode == ControlOutputMode.ADDRESS
                                ? "Click to edit addressed output"
                                : "Switch output mode to Address to edit", NamedTextColor.YELLOW));
                case "initial" -> builder = item(Material.CLOCK,
                        "Start delay: " + displayDuration(initialTicks))
                        .lore(Component.text("Left +5 / Right -5 ticks; Shift = 20", NamedTextColor.YELLOW));
                case "on" -> builder = item(Material.LIME_DYE,
                        "ON time: " + displayDuration(onTicks))
                        .lore(Component.text(engine.mode() == TimerCycleEngine.Mode.REPEATING_PULSE
                                ? "Repeating pulse is always 1 tick"
                                : "Left +5 / Right -5 ticks; Shift = 20", NamedTextColor.YELLOW));
                case "off" -> builder = item(Material.RED_DYE,
                        "OFF time: " + displayDuration(offTicks))
                        .lore(Component.text("Left +5 / Right -5 ticks; Shift = 20", NamedTextColor.YELLOW));
                case "status" -> {
                    var state = engine.state();
                    builder = item(state.output() ? Material.REDSTONE_TORCH : Material.LEVER,
                            "Timer: " + state.phase().name())
                            .lore(Component.text("Output: " + (state.output() ? "ON" : "OFF"),
                                            NamedTextColor.GRAY),
                                    Component.text("Cycles: " + state.completedCycles(), NamedTextColor.GRAY),
                                    Component.text("Source: " + (sourceSelector.activeSource() == null ? "WAITING"
                                            : shortId(sourceSelector.activeSource())), NamedTextColor.GRAY));
                }
                case "start" -> builder = item(Material.LIME_CONCRETE, "Manual Start")
                        .lore(Component.text("Restart the schedule now", NamedTextColor.YELLOW));
                case "stop" -> builder = item(Material.RED_CONCRETE, "Stop / Reset")
                        .lore(Component.text("Cancel tasks and immediately output OFF", NamedTextColor.YELLOW));
                default -> throw new IllegalArgumentException("Unknown GUI item: " + kind);
            }
            return builder;
        }

        private ItemStackBuilder item(Material material, String name) {
            return ItemStackBuilder.of(material)
                    .name(Component.text(name, NamedTextColor.GOLD));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType,
                                @NotNull Player player, @NotNull Click click) {
            if ("address".equals(kind)) {
                if (outputMode != ControlOutputMode.ADDRESS) {
                    return;
                }
                player.closeInventory();
                GridWorks plugin = GridWorks.getInstance();
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        runOnServerThreadIfActive(() -> openAddressWindow(player)));
                return;
            }
            runOnServerThreadIfActive(() -> {
                int direction = clickType.isRightClick() ? -1 : 1;
                long step = clickType.isShiftClick() ? 20L : 5L;
                switch (kind) {
                    case "mode" -> setMode(direction);
                    case "input" -> setBooleanInputMode(inputMode.cycle(direction));
                    case "source" -> cycleSource(direction);
                    case "output_mode" -> toggleOutputMode();
                    case "circuit" -> {
                        if (outputMode == ControlOutputMode.CIRCUIT) {
                            setOutputCircuit(direction);
                        }
                    }
                    case "initial", "on", "off" -> changeDuration(kind, direction * step);
                    case "start" -> manualStart();
                    case "stop" -> manualStop();
                    default -> {
                    }
                }
            });
        }
    }
}
