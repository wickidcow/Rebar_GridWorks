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
import io.github.wickidcow.gridworks.control.DelayedBooleanTransition;
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

public final class DelayRelayBlock extends PhysicalControlNodeBlock
        implements GuiRebarBlock, BooleanInputConfigurable {
    private static final NamespacedKey ON_DELAY_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:delay_relay_on_ticks")
    );
    private static final NamespacedKey OFF_DELAY_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:delay_relay_off_ticks")
    );
    private static final NamespacedKey INPUT_MODE_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:delay_relay_input_mode")
    );

    private static final long DEFAULT_DELAY_TICKS = 20L;
    private static final long MIN_DELAY_TICKS = 0L;
    private static final long MAX_DELAY_TICKS = 1200L;

    private final DelayedBooleanTransition transition = new DelayedBooleanTransition(false);
    private volatile long onDelayTicks;
    private volatile long offDelayTicks;
    private volatile boolean powered;
    private volatile BooleanInputMode inputMode;
    private BukkitTask pendingTask;

    private final OnDelayItem onDelayItem = new OnDelayItem();
    private final OffDelayItem offDelayItem = new OffDelayItem();
    private final StatusItem statusItem = new StatusItem();

    public DelayRelayBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
        this.onDelayTicks = DEFAULT_DELAY_TICKS;
        this.offDelayTicks = DEFAULT_DELAY_TICKS;
        this.inputMode = BooleanInputMode.LEGACY;
    }

    public DelayRelayBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);
        Long storedOn = pdc.get(ON_DELAY_KEY, PersistentDataType.LONG);
        Long storedOff = pdc.get(OFF_DELAY_KEY, PersistentDataType.LONG);
        this.onDelayTicks = clampDelay(storedOn == null ? DEFAULT_DELAY_TICKS : storedOn);
        this.offDelayTicks = clampDelay(storedOff == null ? DEFAULT_DELAY_TICKS : storedOff);
        this.inputMode = BooleanInputMode.fromStored(
                pdc.get(INPUT_MODE_KEY, PersistentDataType.STRING)
        );
    }

    @Override
    public boolean accepts(@NotNull ControlChannel channel) {
        return inputMode.accepts(channel);
    }

    @Override
    protected void afterActivated() {
        powered = false;
        transition.reset(false);
        applyOutputState();
    }

    @Override
    protected void handleSignal(@NotNull ControlSignal signal) {
        if (!(signal.value() instanceof ControlValue.BooleanValue booleanValue)) {
            return;
        }

        boolean input = booleanValue.value();
        runOnServerThreadIfActive(() -> observeInput(input));
    }

    @Override
    protected void writeNodeData(@NotNull PersistentDataContainer pdc) {
        pdc.set(ON_DELAY_KEY, PersistentDataType.LONG, onDelayTicks);
        pdc.set(OFF_DELAY_KEY, PersistentDataType.LONG, offDelayTicks);
        pdc.set(INPUT_MODE_KEY, PersistentDataType.STRING, inputMode.name());
    }

    @Override
    protected void afterDeactivated() {
        cancelPendingTask();
        powered = false;
        transition.reset(false);
    }

    @Override
    protected void afterRemoved() {
        cancelPendingTask();
        powered = false;
        transition.reset(false);
    }

    @Override
    public @NotNull Gui createGui() {
        return Gui.builder()
                .setStructure("a # b # s")
                .addIngredient('#', GuiItems.background())
                .addIngredient('a', onDelayItem)
                .addIngredient('b', offDelayItem)
                .addIngredient('s', statusItem)
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
            cancelPendingTask();
            powered = false;
            transition.reset(false);
            applyOutputState();
            statusItem.notifyWindows();
            GridWorks.getInstance().getPhysicalControlNetwork().replayStateSources(getNodeId());
        });
    }

    public long getOnDelayTicks() {
        return onDelayTicks;
    }

    public long getOffDelayTicks() {
        return offDelayTicks;
    }

    public boolean isPowered() {
        return powered;
    }

    private void observeInput(boolean input) {
        DelayedBooleanTransition.Action action = transition.observe(input);

        switch (action) {
            case NONE -> {
            }
            case CANCEL_PENDING -> {
                cancelPendingTask();
                statusItem.notifyWindows();
            }
            case SCHEDULE_ON -> scheduleTransition(true, onDelayTicks);
            case SCHEDULE_OFF -> scheduleTransition(false, offDelayTicks);
        }
    }

    private void scheduleTransition(boolean target, long delayTicks) {
        cancelPendingTask();

        if (delayTicks <= 0L) {
            commitTransition(target);
            return;
        }

        statusItem.notifyWindows();
        pulseTask(delayTicks, target);
    }

    private void pulseTask(long delayTicks, boolean target) {
        GridWorks plugin = GridWorks.getInstance();
        pendingTask = plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> {
                    pendingTask = null;
                    runOnServerThreadIfActive(() -> commitTransition(target));
                },
                delayTicks
        );
    }

    private void commitTransition(boolean target) {
        if (!transition.commit(target)) {
            statusItem.notifyWindows();
            return;
        }

        powered = transition.output();
        applyOutputState();
        statusItem.notifyWindows();
    }

    private void changeOnDelay(long delta) {
        onDelayTicks = clampDelay(onDelayTicks + delta);
        onDelayItem.notifyWindows();
    }

    private void changeOffDelay(long delta) {
        offDelayTicks = clampDelay(offDelayTicks + delta);
        offDelayItem.notifyWindows();
    }

    private void cancelPendingTask() {
        BukkitTask task = pendingTask;
        pendingTask = null;
        if (task != null) {
            task.cancel();
        }
    }

    private void applyOutputState() {
        BlockData blockData = getBlock().getBlockData();
        if (!(blockData instanceof Switch relaySwitch)) {
            throw new IllegalStateException(
                    "Delay Relay block material no longer provides Switch block data: "
                            + blockData.getMaterial()
            );
        }

        if (relaySwitch.isPowered() == powered) {
            return;
        }

        relaySwitch.setPowered(powered);
        getBlock().setBlockData(relaySwitch);
    }

    private static long clampDelay(long ticks) {
        return Math.clamp(ticks, MIN_DELAY_TICKS, MAX_DELAY_TICKS);
    }

    private static String formatDuration(long ticks) {
        if (ticks == 0L) {
            return "instant";
        }
        if (ticks % 20L == 0L) {
            return (ticks / 20L) + "s";
        }
        return String.format(java.util.Locale.ROOT, "%.2fs", ticks / 20.0);
    }

    private abstract class DelayItem extends AbstractItem {
        protected ItemStackBuilder item(Material material, String name) {
            return ItemStackBuilder.of(material)
                    .name(Component.text(name, NamedTextColor.GOLD));
        }

        protected long clickStep(ClickType clickType) {
            return clickType.isShiftClick() ? 20L : 5L;
        }
    }

    private final class OnDelayItem extends DelayItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(Material.LIME_DYE, "ON delay: " + formatDuration(onDelayTicks))
                    .lore(
                            Component.text("Left +5 ticks / Right -5 ticks", NamedTextColor.YELLOW),
                            Component.text("Shift uses 20 ticks (1 second)", NamedTextColor.YELLOW)
                    );
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            long step = clickStep(clickType);
            if (clickType.isLeftClick()) {
                changeOnDelay(step);
            } else if (clickType.isRightClick()) {
                changeOnDelay(-step);
            }
        }
    }

    private final class OffDelayItem extends DelayItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(Material.RED_DYE, "OFF delay: " + formatDuration(offDelayTicks))
                    .lore(
                            Component.text("Left +5 ticks / Right -5 ticks", NamedTextColor.YELLOW),
                            Component.text("Shift uses 20 ticks (1 second)", NamedTextColor.YELLOW)
                    );
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            long step = clickStep(clickType);
            if (clickType.isLeftClick()) {
                changeOffDelay(step);
            } else if (clickType.isRightClick()) {
                changeOffDelay(-step);
            }
        }
    }

    private final class StatusItem extends DelayItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            Boolean input = transition.input();
            Boolean pending = transition.pendingTarget();

            ItemStackBuilder builder = item(
                    powered ? Material.REDSTONE_TORCH : Material.LEVER,
                    "Output: " + (powered ? "ON" : "OFF")
            ).lore(
                    Component.text(
                            "Input: " + (input == null ? "WAITING" : (input ? "ON" : "OFF")),
                            NamedTextColor.GRAY
                    )
            );

            if (pending != null) {
                builder.lore(Component.text(
                        "Pending: " + (pending ? "ON" : "OFF"),
                        NamedTextColor.YELLOW
                ));
            } else {
                builder.lore(Component.text("Pending: none", NamedTextColor.DARK_GRAY));
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
}
