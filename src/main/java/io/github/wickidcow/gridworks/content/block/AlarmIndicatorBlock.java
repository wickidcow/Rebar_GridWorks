package io.github.wickidcow.gridworks.content.block;

import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.block.interfaces.GuiRebarBlock;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.pylonmc.rebar.util.gui.GuiItems;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.api.control.ControlChannel;
import io.github.wickidcow.gridworks.api.control.ControlSignal;
import io.github.wickidcow.gridworks.api.control.ControlValue;
import io.github.wickidcow.gridworks.api.control.GridWorksChannels;
import io.github.wickidcow.gridworks.control.AlarmLatch;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemProvider;

public final class AlarmIndicatorBlock extends PhysicalControlNodeBlock implements GuiRebarBlock {
    private static final NamespacedKey SOUND_ENABLED_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:alarm_sound_enabled")
    );
    private static final NamespacedKey LATCHED_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:alarm_latched")
    );
    private static final NamespacedKey ACKNOWLEDGED_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:alarm_acknowledged")
    );

    private final AlarmLatch alarmLatch;
    private volatile boolean soundEnabled;

    private final SoundItem soundItem = new SoundItem();
    private final AcknowledgeItem acknowledgeItem = new AcknowledgeItem();
    private final TestItem testItem = new TestItem();
    private final StatusItem statusItem = new StatusItem();

    public AlarmIndicatorBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
        this.soundEnabled = true;
        this.alarmLatch = new AlarmLatch(false, false);
    }

    public AlarmIndicatorBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);

        Byte storedSound = pdc.get(SOUND_ENABLED_KEY, PersistentDataType.BYTE);
        Byte storedLatched = pdc.get(LATCHED_KEY, PersistentDataType.BYTE);
        Byte storedAcknowledged = pdc.get(ACKNOWLEDGED_KEY, PersistentDataType.BYTE);

        this.soundEnabled = storedSound == null || storedSound != 0;
        this.alarmLatch = new AlarmLatch(
                storedLatched != null && storedLatched != 0,
                storedAcknowledged != null && storedAcknowledged != 0
        );
    }

    @Override
    public boolean accepts(@NotNull ControlChannel channel) {
        return GridWorksChannels.REDSTONE_POWERED.equals(channel)
                || GridWorksChannels.CONTROL_ENABLED.equals(channel);
    }

    @Override
    protected void afterActivated() {
        alarmLatch.resetObservation();
        applyVisualState();
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
        pdc.set(
                SOUND_ENABLED_KEY,
                PersistentDataType.BYTE,
                soundEnabled ? (byte) 1 : (byte) 0
        );
        pdc.set(
                LATCHED_KEY,
                PersistentDataType.BYTE,
                alarmLatch.isLatched() ? (byte) 1 : (byte) 0
        );
        pdc.set(
                ACKNOWLEDGED_KEY,
                PersistentDataType.BYTE,
                alarmLatch.isAcknowledged() ? (byte) 1 : (byte) 0
        );
    }

    @Override
    protected void afterDeactivated() {
        alarmLatch.resetObservation();
    }

    @Override
    protected void afterRemoved() {
        alarmLatch.resetObservation();
    }

    @Override
    public @NotNull Gui createGui() {
        return Gui.builder()
                .setStructure("s # a # t # x")
                .addIngredient('#', GuiItems.background())
                .addIngredient('s', soundItem)
                .addIngredient('a', acknowledgeItem)
                .addIngredient('t', testItem)
                .addIngredient('x', statusItem)
                .build();
    }

    public boolean isConditionActive() {
        return alarmLatch.isConditionActive();
    }

    public boolean isLatched() {
        return alarmLatch.isLatched();
    }

    public boolean isAcknowledged() {
        return alarmLatch.isAcknowledged();
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    private void acceptInput(boolean input) {
        boolean ring = alarmLatch.observe(input);
        applyVisualState();
        acknowledgeItem.notifyWindows();
        statusItem.notifyWindows();

        if (ring && soundEnabled) {
            playAlarmSound();
        }
    }

    private void acknowledge() {
        alarmLatch.acknowledge();
        applyVisualState();
        acknowledgeItem.notifyWindows();
        statusItem.notifyWindows();
    }

    private void toggleSound() {
        soundEnabled = !soundEnabled;
        soundItem.notifyWindows();
    }

    private void playAlarmSound() {
        getBlock().getWorld().playSound(
                getBlock().getLocation().add(0.5, 0.5, 0.5),
                Sound.BLOCK_NOTE_BLOCK_BELL,
                SoundCategory.BLOCKS,
                1.0f,
                1.0f
        );
    }

    private void applyVisualState() {
        BlockData blockData = getBlock().getBlockData();
        if (!(blockData instanceof Lightable lightable)) {
            throw new IllegalStateException(
                    "Alarm Indicator material no longer provides Lightable block data: "
                            + blockData.getMaterial()
            );
        }

        boolean shouldBeLit = alarmLatch.isLatched();
        if (lightable.isLit() == shouldBeLit) {
            return;
        }

        lightable.setLit(shouldBeLit);
        getBlock().setBlockData(lightable);
    }

    private abstract class AlarmItem extends AbstractItem {
        protected ItemStackBuilder item(Material material, String name) {
            return ItemStackBuilder.of(material)
                    .name(Component.text(name, NamedTextColor.GOLD));
        }
    }

    private final class SoundItem extends AlarmItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(
                    soundEnabled ? Material.NOTE_BLOCK : Material.BARRIER,
                    "Sound: " + (soundEnabled ? "ENABLED" : "MUTED")
            ).lore(Component.text(
                    "Click to toggle the rising-edge bell",
                    NamedTextColor.YELLOW
            ));
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            toggleSound();
        }
    }

    private final class AcknowledgeItem extends AlarmItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            Material material = alarmLatch.isLatched()
                    ? (alarmLatch.isAcknowledged() ? Material.LIME_DYE : Material.YELLOW_DYE)
                    : Material.GRAY_DYE;

            String state;
            if (!alarmLatch.isLatched()) {
                state = "CLEAR";
            } else if (alarmLatch.isAcknowledged()) {
                state = "ACKNOWLEDGED";
            } else {
                state = "UNACKNOWLEDGED";
            }

            return item(material, "Alarm latch: " + state)
                    .lore(Component.text(
                            alarmLatch.isLatched()
                                    ? "Click to acknowledge this alarm"
                                    : "No alarm is currently latched",
                            alarmLatch.isLatched()
                                    ? NamedTextColor.YELLOW
                                    : NamedTextColor.DARK_GRAY
                    ));
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            acknowledge();
        }
    }

    private final class TestItem extends AlarmItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(Material.BELL, "Test Alarm")
                    .lore(Component.text(
                            "Click to play the local alarm sound",
                            NamedTextColor.YELLOW
                    ));
        }

        @Override
        public void handleClick(
                @NotNull ClickType clickType,
                @NotNull Player player,
                @NotNull Click click
        ) {
            playAlarmSound();
        }
    }

    private final class StatusItem extends AlarmItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            String condition = alarmLatch.isConditionActive() ? "ACTIVE" : "CLEAR";
            String latch = !alarmLatch.isLatched()
                    ? "CLEAR"
                    : (alarmLatch.isAcknowledged() ? "ACK" : "UNACK");

            return item(
                    alarmLatch.isLatched() ? Material.REDSTONE_TORCH : Material.GRAY_DYE,
                    "Condition: " + condition
            ).lore(
                    Component.text("Latch: " + latch, NamedTextColor.WHITE),
                    Component.text(
                            "Unacknowledged faults stay latched after clearing",
                            NamedTextColor.GRAY
                    ),
                    Component.text(
                            "Acknowledged faults clear when the condition clears",
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
