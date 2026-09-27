package io.github.wickidcow.gridworks.content.block;

import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.block.interfaces.GuiRebarBlock;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.pylonmc.rebar.util.gui.GuiItems;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.api.control.BooleanInputConfigurable;
import io.github.wickidcow.gridworks.api.control.BooleanInputMode;
import io.github.wickidcow.gridworks.api.control.ControlChannel;
import io.github.wickidcow.gridworks.api.control.ControlSignal;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.api.control.GridWorksChannels;
import io.github.wickidcow.gridworks.control.RisingEdgeTrigger;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Switch;
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

public final class PulseRelayBlock extends PhysicalControlNodeBlock
        implements GuiRebarBlock, BooleanInputConfigurable {
    private static final NamespacedKey PULSE_TICKS_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:pulse_relay_ticks")
    );
    private static final NamespacedKey INPUT_MODE_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:pulse_relay_input_mode")
    );

    private static final long DEFAULT_PULSE_TICKS = 20L;
    private static final long MIN_PULSE_TICKS = 1L;
    private static final long MAX_PULSE_TICKS = 1200L;

    private final RisingEdgeTrigger edgeTrigger = new RisingEdgeTrigger();
    private volatile long pulseTicks;
    private volatile boolean powered;
    private volatile BooleanInputMode inputMode;
    private BukkitTask pulseTask;

    private final DurationItem durationItem = new DurationItem();
    private final TestPulseItem testPulseItem = new TestPulseItem();
    private final OutputItem outputItem = new OutputItem();

    public PulseRelayBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
        this.pulseTicks = DEFAULT_PULSE_TICKS;
        this.powered = false;
        this.inputMode = BooleanInputMode.LEGACY;
    }

    public PulseRelayBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);
        Long stored = pdc.get(PULSE_TICKS_KEY, PersistentDataType.LONG);
        this.pulseTicks = clampPulseTicks(stored == null ? DEFAULT_PULSE_TICKS : stored);
        this.powered = false;
        this.inputMode = BooleanInputMode.fromStored(
                pdc.get(INPUT_MODE_KEY, PersistentDataType.STRING)
        );
    }

    @Override
    public boolean accepts(@NotNull ControlChannel channel) {
        return inputMode.accepts(channel);
    }

    @Override
    protected void beforeActivated() {
        powered = false;
        edgeTrigger.reset();
    }

    @Override
    protected void afterActivated() {
        applyOutputState();
    }

    @Override
    protected void handleSignal(@NotNull ControlSignal signal) {
        if (!(signal.value() instanceof ControlValue.BooleanValue booleanValue)) {
            return;
        }

        boolean input = booleanValue.value();
        runOnServerThreadIfActive(() -> acceptInput(input));
    }

    @Override
    protected void writeNodeData(@NotNull PersistentDataContainer pdc) {
        pdc.set(PULSE_TICKS_KEY, PersistentDataType.LONG, pulseTicks);
        pdc.set(INPUT_MODE_KEY, PersistentDataType.STRING, inputMode.name());
    }

    @Override
    protected void afterDeactivated() {
        cancelPulse();
        powered = false;
        edgeTrigger.reset();
    }

    @Override
    protected void afterRemoved() {
        cancelPulse();
        powered = false;
        edgeTrigger.reset();
    }

    @Override
    public @NotNull Gui createGui() {
        return Gui.builder()
                .setStructure("d # t # x")
                .addIngredient('#', GuiItems.background())
                .addIngredient('d', durationItem)
                .addIngredient('t', testPulseItem)
                .addIngredient('x', outputItem)
                .build();
    }

    @Override
    public @NotNull BooleanInputMode getBooleanInputMode() {
        return inputMode;
    }

    @Override
    public void setBooleanInputMode(@NotNull BooleanInputMode mode) {
        inputMode = Objects.requireNonNull(mode, "mode");

        runOnServerThreadIfActive(() -> {
            cancelPulse();
            powered = false;
            edgeTrigger.reset();
            applyOutputState();
            outputItem.notifyWindows();
            GridWorks.getInstance().getPhysicalControlNetwork().replayStateSources(getNodeId());
        });
    }

    public long getPulseTicks() {
        return pulseTicks;
    }

    public boolean isPowered() {
        return powered;
    }

    private void acceptInput(boolean input) {
        if (edgeTrigger.observe(input)) {
            triggerPulse();
        }
    }

    private void triggerPulse() {
        cancelPulse();

        powered = true;
        applyOutputState();
        outputItem.notifyWindows();

        pulseTask = GridWorks.getInstance().getServer().getScheduler().runTaskLater(
                GridWorks.getInstance(),
                () -> {
                    pulseTask = null;
                    powered = false;
                    runOnServerThreadIfActive(() -> {
                        applyOutputState();
                        outputItem.notifyWindows();
                    });
                },
                pulseTicks
        );
    }

    private void changeDuration(long delta) {
        pulseTicks = clampPulseTicks(pulseTicks + delta);
        durationItem.notifyWindows();
    }

    private void cancelPulse() {
        BukkitTask task = pulseTask;
        pulseTask = null;
        if (task != null) {
            task.cancel();
        }
    }

    private void applyOutputState() {
        BlockData blockData = getBlock().getBlockData();
        if (!(blockData instanceof Switch relaySwitch)) {
            throw new IllegalStateException(
                    "Pulse Relay block material no longer provides Switch block data: "
                            + blockData.getMaterial()
            );
        }

        if (relaySwitch.isPowered() == powered) {
            return;
        }

        relaySwitch.setPowered(powered);
        getBlock().setBlockData(relaySwitch);
    }

    private static long clampPulseTicks(long ticks) {
        return Math.clamp(ticks, MIN_PULSE_TICKS, MAX_PULSE_TICKS);
    }

    private static String formatDuration(long ticks) {
        if (ticks % 20L == 0L) {
            return (ticks / 20L) + "s";
        }
        return String.format(java.util.Locale.ROOT, "%.2fs", ticks / 20.0);
    }

    private abstract class RelayItem extends AbstractItem {
        protected ItemStackBuilder item(Material material, String name) {
            return ItemStackBuilder.of(material)
                    .name(Component.text(name, NamedTextColor.GOLD));
        }
    }

    private final class DurationItem extends RelayItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(Material.CLOCK, "Pulse: " + formatDuration(pulseTicks))
                    .lore(
                            Component.text("Left +5 ticks / Right -5 ticks", NamedTextColor.YELLOW),
                            Component.text("Shift uses 20 ticks (1 second)", NamedTextColor.YELLOW),
                            Component.text("Range: 1 tick to 60 seconds", NamedTextColor.GRAY)
                    );
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            long step = clickType.isShiftClick() ? 20L : 5L;
            if (clickType.isLeftClick()) {
                changeDuration(step);
            } else if (clickType.isRightClick()) {
                changeDuration(-step);
            }
        }
    }

    private final class TestPulseItem extends RelayItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(Material.BLAZE_POWDER, "Test Pulse")
                    .lore(Component.text(
                            "Click to fire one pulse now",
                            NamedTextColor.YELLOW
                    ));
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            triggerPulse();
        }
    }

    private final class OutputItem extends RelayItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(
                    powered ? Material.LIME_DYE : Material.GRAY_DYE,
                    "Output: " + (powered ? "PULSING" : "IDLE")
            ).lore(
                    Component.text(
                            "Triggers on observed false -> true edges",
                            NamedTextColor.GRAY
                    ),
                    Component.text(
                            "First replayed state establishes a baseline only",
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
}
