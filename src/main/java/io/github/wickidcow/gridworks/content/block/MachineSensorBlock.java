package io.github.wickidcow.gridworks.content.block;

import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.block.interfaces.GuiRebarBlock;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.pylonmc.rebar.util.gui.GuiItems;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.api.control.ControlPublication;
import io.github.wickidcow.gridworks.api.control.ControlStateSource;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.api.control.GridWorksChannels;
import io.github.wickidcow.gridworks.machine.MachineProbe;
import io.github.wickidcow.gridworks.machine.MachineSnapshot;
import io.github.wickidcow.gridworks.machine.ObservedMachineCycleCounter;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
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

public final class MachineSensorBlock extends PhysicalControlNodeBlock
        implements ControlStateSource, GuiRebarBlock {
    private static final NamespacedKey OBSERVED_CYCLES_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:machine_sensor_observed_cycles")
    );
    private static final NamespacedKey LAST_CYCLE_EPOCH_MS_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:machine_sensor_last_cycle_epoch_ms")
    );

    private final ObservedMachineCycleCounter cycleCounter;
    private final CycleStatusItem cycleStatusItem = new CycleStatusItem();
    private final ResetCyclesItem resetCyclesItem = new ResetCyclesItem();
    private MachineSnapshot lastSnapshot;

    public MachineSensorBlock(
            @NotNull Block block,
            @NotNull BlockCreateContext context
    ) {
        super(block, context);
        this.cycleCounter = new ObservedMachineCycleCounter();
        setFacing(context.getFacing());
    }

    public MachineSensorBlock(
            @NotNull Block block,
            @NotNull PersistentDataContainer pdc
    ) {
        super(block, pdc);
        this.cycleCounter = ObservedMachineCycleCounter.fromStored(
                pdc.get(OBSERVED_CYCLES_KEY, PersistentDataType.LONG),
                pdc.get(LAST_CYCLE_EPOCH_MS_KEY, PersistentDataType.LONG)
        );
    }

    @Override
    protected void beforeActivated() {
        lastSnapshot = null;
        cycleCounter.resetObservation();
    }

    @Override
    protected void afterActivated() {
        GridWorks.getInstance().getMachineSensorManager().register(this);
    }

    @Override
    protected void afterDeactivated() {
        GridWorks.getInstance().getMachineSensorManager().unregister(this);
    }

    @Override
    protected void afterRemoved() {
        GridWorks.getInstance().getMachineSensorManager().unregister(this);
    }

    @Override
    protected void writeNodeData(@NotNull PersistentDataContainer pdc) {
        pdc.set(OBSERVED_CYCLES_KEY, PersistentDataType.LONG, cycleCounter.observedCycles());
        pdc.set(LAST_CYCLE_EPOCH_MS_KEY, PersistentDataType.LONG, cycleCounter.lastCycleEpochMillis());
    }

    @Override
    public void publishCurrentState() {
        if (lastSnapshot != null) {
            publish(lastSnapshot);
        }
    }

    @Override
    public @NotNull Gui createGui() {
        return Gui.builder()
                .setStructure("c # r")
                .addIngredient('#', GuiItems.background())
                .addIngredient('c', cycleStatusItem)
                .addIngredient('r', resetCyclesItem)
                .build();
    }

    public void sampleNow() {
        MachineSnapshot snapshot = readTarget();
        boolean completedObservedCycle = cycleCounter.observe(
                snapshot,
                System.currentTimeMillis()
        );

        if (snapshot.equals(lastSnapshot) && !completedObservedCycle) {
            return;
        }

        lastSnapshot = snapshot;
        publish(snapshot);
        cycleStatusItem.notifyWindows();
        resetCyclesItem.notifyWindows();
    }

    public @NotNull String describeSnapshot() {
        MachineSnapshot snapshot = lastSnapshot;
        long observedCycles = cycleCounter.observedCycles();
        if (snapshot == null || !snapshot.available()) {
            return "no supported Rebar processor target, "
                    + observedCycles
                    + " observed cycle(s)";
        }
        if (!snapshot.processing()) {
            return snapshot.kind()
                    + ", idle, "
                    + observedCycles
                    + " observed cycle(s)";
        }

        return snapshot.kind()
                + ", processing "
                + String.format(Locale.ROOT, "%.0f%%", snapshot.progress() * 100.0)
                + ", "
                + snapshot.ticksRemaining()
                + " ticks remaining, "
                + observedCycles
                + " observed cycle(s)";
    }

    private void resetObservedCycles() {
        cycleCounter.resetCount();
        publishCurrentState();
        cycleStatusItem.notifyWindows();
        resetCyclesItem.notifyWindows();
    }

    private static String formatLastCycle(long epochMillis) {
        return epochMillis <= 0L ? "never" : Instant.ofEpochMilli(epochMillis).toString();
    }

    private MachineSnapshot readTarget() {
        BlockFace facing = getFacing();
        Block source = getBlock();
        int targetX = source.getX() + facing.getModX();
        int targetY = source.getY() + facing.getModY();
        int targetZ = source.getZ() + facing.getModZ();

        World world = source.getWorld();
        if (!world.isChunkLoaded(targetX >> 4, targetZ >> 4)) {
            return MachineSnapshot.unavailable();
        }

        return MachineProbe.snapshot(world.getBlockAt(targetX, targetY, targetZ));
    }

    private BlockFace getFacing() {
        BlockData data = getBlock().getBlockData();
        if (!(data instanceof Directional directional)) {
            throw new IllegalStateException(
                    "Machine Sensor material no longer provides Directional block data: "
                            + data.getMaterial()
            );
        }
        return directional.getFacing();
    }

    private void setFacing(BlockFace facing) {
        BlockData data = getBlock().getBlockData();
        if (!(data instanceof Directional directional)) {
            throw new IllegalStateException(
                    "Machine Sensor material no longer provides Directional block data: "
                            + data.getMaterial()
            );
        }

        if (!directional.getFaces().contains(facing)) {
            throw new IllegalArgumentException(
                    "Machine Sensor material cannot face " + facing
            );
        }

        directional.setFacing(facing);
        getBlock().setBlockData(directional);
    }

    private void publish(MachineSnapshot snapshot) {
        var publications = new java.util.ArrayList<ControlPublication>(8);
        publications.add(ControlPublication.of(
                GridWorksChannels.MACHINE_AVAILABLE,
                ControlValue.of(snapshot.available())
        ));

        if (snapshot.available()) {
            publications.add(ControlPublication.of(
                    GridWorksChannels.MACHINE_KIND,
                    ControlValue.of(snapshot.kind())
            ));
            publications.add(ControlPublication.of(
                    GridWorksChannels.MACHINE_PROCESSING,
                    ControlValue.of(snapshot.processing())
            ));
            publications.add(ControlPublication.of(
                    GridWorksChannels.MACHINE_PROGRESS,
                    ControlValue.of(snapshot.progress())
            ));
            publications.add(ControlPublication.of(
                    GridWorksChannels.MACHINE_PROCESS_TIME_TICKS,
                    ControlValue.of((double) snapshot.processTimeTicks())
            ));
            publications.add(ControlPublication.of(
                    GridWorksChannels.MACHINE_TICKS_REMAINING,
                    ControlValue.of((double) snapshot.ticksRemaining())
            ));
        }

        publications.add(ControlPublication.of(
                GridWorksChannels.MACHINE_OBSERVED_CYCLES,
                ControlValue.of((double) cycleCounter.observedCycles())
        ));
        publications.add(ControlPublication.of(
                GridWorksChannels.MACHINE_LAST_CYCLE_EPOCH_MS,
                ControlValue.of((double) cycleCounter.lastCycleEpochMillis())
        ));

        GridWorks.getInstance().getControlBus().publishBatch(
                getNodeId(),
                publications
        );
    }


    private final class CycleStatusItem extends AbstractItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            MachineSnapshot snapshot = lastSnapshot;
            String machineState;
            if (snapshot == null || !snapshot.available()) {
                machineState = "Target unavailable";
            } else if (snapshot.processing()) {
                machineState = "Processing";
            } else {
                machineState = "Idle";
            }

            return ItemStackBuilder.of(Material.CRAFTING_TABLE)
                    .name(Component.text("Observed Machine Cycles", NamedTextColor.GOLD))
                    .lore(
                            Component.text("Count: " + cycleCounter.observedCycles(), NamedTextColor.WHITE),
                            Component.text(
                                    "Last: " + formatLastCycle(cycleCounter.lastCycleEpochMillis()),
                                    NamedTextColor.GRAY
                            ),
                            Component.text("Machine: " + machineState, NamedTextColor.AQUA),
                            Component.text(
                                    "Counts observed processing -> idle transitions",
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

    private final class ResetCyclesItem extends AbstractItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return ItemStackBuilder.of(Material.BARRIER)
                    .name(Component.text("Reset Observed Cycles", NamedTextColor.RED))
                    .lore(
                            Component.text(
                                    "Shift + right click to reset count and timestamp",
                                    NamedTextColor.YELLOW
                            ),
                            Component.text(
                                    "Current processing baseline is preserved",
                                    NamedTextColor.GRAY
                            ),
                            Component.text(
                                    "A running machine can become cycle 1 when it next goes idle",
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
                resetObservedCycles();
            }
        }
    }
}

