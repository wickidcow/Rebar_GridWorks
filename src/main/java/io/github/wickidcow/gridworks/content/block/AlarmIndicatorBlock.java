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
import io.github.wickidcow.gridworks.control.RisingEdgeTrigger;
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

    private final RisingEdgeTrigger edgeTrigger = new RisingEdgeTrigger();
    private volatile boolean active;
    private volatile boolean soundEnabled;

    private final SoundItem soundItem = new SoundItem();
    private final TestItem testItem = new TestItem();
    private final StatusItem statusItem = new StatusItem();

    public AlarmIndicatorBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
        this.soundEnabled = true;
    }

    public AlarmIndicatorBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);
        Byte stored = pdc.get(SOUND_ENABLED_KEY, PersistentDataType.BYTE);
        this.soundEnabled = stored == null || stored != 0;
    }

    @Override
    public boolean accepts(@NotNull ControlChannel channel) {
        return GridWorksChannels.REDSTONE_POWERED.equals(channel)
                || GridWorksChannels.CONTROL_ENABLED.equals(channel);
    }

    @Override
    protected void afterActivated() {
        active = false;
        edgeTrigger.reset();
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
    }

    @Override
    protected void afterDeactivated() {
        active = false;
        edgeTrigger.reset();
    }

    @Override
    protected void afterRemoved() {
        active = false;
        edgeTrigger.reset();
    }

    @Override
    public @NotNull Gui createGui() {
        return Gui.builder()
                .setStructure("s # t # x")
                .addIngredient('#', GuiItems.background())
                .addIngredient('s', soundItem)
                .addIngredient('t', testItem)
                .addIngredient('x', statusItem)
                .build();
    }

    public boolean isActive() {
        return active;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    private void acceptInput(boolean input) {
        boolean ring = edgeTrigger.observe(input);
        active = input;
        applyVisualState();
        statusItem.notifyWindows();

        if (ring && soundEnabled) {
            playAlarmSound();
        }
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

        if (lightable.isLit() == active) {
            return;
        }

        lightable.setLit(active);
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
            return item(
                    active ? Material.REDSTONE_TORCH : Material.GRAY_DYE,
                    "Alarm: " + (active ? "ACTIVE" : "CLEAR")
            ).lore(
                    Component.text(
                            "Visible state follows the Control Bus level",
                            NamedTextColor.GRAY
                    ),
                    Component.text(
                            "Sound only fires on a real false -> true edge",
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
