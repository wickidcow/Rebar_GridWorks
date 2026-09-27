package io.github.wickidcow.gridworks.content.block;

import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.block.interfaces.GuiRebarBlock;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.pylonmc.rebar.util.gui.GuiItems;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.alarm.AlarmAcknowledgeRequest;
import io.github.wickidcow.gridworks.alarm.AlarmHistoryState;
import io.github.wickidcow.gridworks.alarm.AlarmSeverity;
import io.github.wickidcow.gridworks.api.control.BooleanInputConfigurable;
import io.github.wickidcow.gridworks.api.control.BooleanInputMode;
import io.github.wickidcow.gridworks.api.control.ControlChannel;
import io.github.wickidcow.gridworks.api.control.ControlSignal;
import io.github.wickidcow.gridworks.api.control.ControlStateSource;
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
import xyz.xenondevs.invui.window.AnvilWindow;

public final class AlarmIndicatorBlock extends PhysicalControlNodeBlock
        implements GuiRebarBlock, BooleanInputConfigurable, ControlStateSource {
    private static final NamespacedKey NAME_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:alarm_name")
    );
    private static final NamespacedKey SEVERITY_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:alarm_severity")
    );
    private static final NamespacedKey SOUND_ENABLED_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:alarm_sound_enabled")
    );
    private static final NamespacedKey LATCHED_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:alarm_latched")
    );
    private static final NamespacedKey ACKNOWLEDGED_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:alarm_acknowledged")
    );
    private static final NamespacedKey OCCURRENCE_COUNT_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:alarm_occurrence_count")
    );
    private static final NamespacedKey LAST_TRIGGERED_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:alarm_last_triggered_epoch_ms")
    );
    private static final NamespacedKey INPUT_MODE_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("gridworks:alarm_input_mode")
    );

    private static final String DEFAULT_NAME = "Alarm Indicator";
    private static final int MAX_NAME_LENGTH = 32;

    private final AlarmLatch alarmLatch;
    private final AlarmHistoryState alarmHistory;
    private volatile String alarmName;
    private volatile AlarmSeverity severity;
    private volatile boolean soundEnabled;
    private volatile BooleanInputMode inputMode;

    private final NameItem nameItem = new NameItem();
    private final SeverityItem severityItem = new SeverityItem();
    private final SoundItem soundItem = new SoundItem();
    private final AcknowledgeItem acknowledgeItem = new AcknowledgeItem();
    private final TestItem testItem = new TestItem();
    private final StatusItem statusItem = new StatusItem();

    public AlarmIndicatorBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
        this.alarmName = DEFAULT_NAME;
        this.severity = AlarmSeverity.WARNING;
        this.soundEnabled = true;
        this.inputMode = BooleanInputMode.LEGACY;
        this.alarmLatch = new AlarmLatch(false, false);
        this.alarmHistory = new AlarmHistoryState(0L, 0L);
    }

    public AlarmIndicatorBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);

        String storedName = pdc.get(NAME_KEY, PersistentDataType.STRING);
        String storedSeverity = pdc.get(SEVERITY_KEY, PersistentDataType.STRING);
        Byte storedSound = pdc.get(SOUND_ENABLED_KEY, PersistentDataType.BYTE);
        Byte storedLatched = pdc.get(LATCHED_KEY, PersistentDataType.BYTE);
        Byte storedAcknowledged = pdc.get(ACKNOWLEDGED_KEY, PersistentDataType.BYTE);
        Long storedOccurrences = pdc.get(OCCURRENCE_COUNT_KEY, PersistentDataType.LONG);
        Long storedLastTriggered = pdc.get(LAST_TRIGGERED_KEY, PersistentDataType.LONG);

        this.alarmName = normalizeName(storedName);
        this.severity = AlarmSeverity.fromStored(storedSeverity);
        this.soundEnabled = storedSound == null || storedSound != 0;
        this.inputMode = BooleanInputMode.fromStored(
                pdc.get(INPUT_MODE_KEY, PersistentDataType.STRING)
        );
        this.alarmLatch = new AlarmLatch(
                storedLatched != null && storedLatched != 0,
                storedAcknowledged != null && storedAcknowledged != 0
        );
        this.alarmHistory = new AlarmHistoryState(
                storedOccurrences == null ? 0L : storedOccurrences,
                storedLastTriggered == null ? 0L : storedLastTriggered
        );
    }

    @Override
    public boolean accepts(@NotNull ControlChannel channel) {
        return inputMode.accepts(channel)
                || GridWorksChannels.ALARM_ACKNOWLEDGE.equals(channel);
    }

    @Override
    protected void afterActivated() {
        alarmLatch.resetObservation();
        applyVisualState();
    }

    @Override
    protected void handleSignal(@NotNull ControlSignal signal) {
        if (GridWorksChannels.ALARM_ACKNOWLEDGE.equals(signal.channel())
                && signal.value() instanceof ControlValue.TextValue textValue) {
            if (AlarmAcknowledgeRequest.matches(textValue.value(), getNodeId())) {
                runOnServerThreadIfActive(this::acknowledge);
            }
            return;
        }

        if (signal.value() instanceof ControlValue.BooleanValue booleanValue) {
            boolean input = booleanValue.value();
            runOnServerThreadIfActive(() -> acceptInput(input));
        }
    }

    @Override
    public void publishCurrentState() {
        publishAlarmState();
    }

    @Override
    protected void writeNodeData(@NotNull PersistentDataContainer pdc) {
        pdc.set(NAME_KEY, PersistentDataType.STRING, alarmName);
        pdc.set(SEVERITY_KEY, PersistentDataType.STRING, severity.name());
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
        pdc.set(
                OCCURRENCE_COUNT_KEY,
                PersistentDataType.LONG,
                alarmHistory.occurrenceCount()
        );
        pdc.set(
                LAST_TRIGGERED_KEY,
                PersistentDataType.LONG,
                alarmHistory.lastTriggeredEpochMillis()
        );
        pdc.set(INPUT_MODE_KEY, PersistentDataType.STRING, inputMode.name());
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
                .setStructure("n # v # s a t # x")
                .addIngredient('#', GuiItems.background())
                .addIngredient('n', nameItem)
                .addIngredient('v', severityItem)
                .addIngredient('s', soundItem)
                .addIngredient('a', acknowledgeItem)
                .addIngredient('t', testItem)
                .addIngredient('x', statusItem)
                .build();
    }

    @Override
    public @NotNull Component getGuiTitle() {
        return Component.text(alarmName, NamedTextColor.GOLD);
    }

    @Override
    public @NotNull BooleanInputMode getBooleanInputMode() {
        return inputMode;
    }

    @Override
    public void setBooleanInputMode(@NotNull BooleanInputMode mode) {
        inputMode = Objects.requireNonNull(mode, "mode");

        runOnServerThreadIfActive(() -> {
            alarmLatch.resetObservation();
            applyVisualState();
            acknowledgeItem.notifyWindows();
            statusItem.notifyWindows();
            GridWorks.getInstance().getPhysicalControlNetwork().replayStateSources(getNodeId());
        });
    }

    public @NotNull String getAlarmName() {
        return alarmName;
    }

    public @NotNull AlarmSeverity getSeverity() {
        return severity;
    }

    public long getOccurrenceCount() {
        return alarmHistory.occurrenceCount();
    }

    public long getLastTriggeredEpochMillis() {
        return alarmHistory.lastTriggeredEpochMillis();
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
        if (ring) {
            alarmHistory.recordTrigger(System.currentTimeMillis());
        }

        applyVisualState();
        acknowledgeItem.notifyWindows();
        statusItem.notifyWindows();
        publishAlarmState();

        if (ring && soundEnabled) {
            playAlarmSound();
        }
    }

    private void acknowledge() {
        alarmLatch.acknowledge();
        applyVisualState();
        acknowledgeItem.notifyWindows();
        statusItem.notifyWindows();
        publishAlarmState();
    }

    private void publishAlarmState() {
        var bus = GridWorks.getInstance().getControlBus();

        bus.publish(
                getNodeId(),
                GridWorksChannels.ALARM_NAME,
                ControlValue.of(alarmName)
        );
        bus.publish(
                getNodeId(),
                GridWorksChannels.ALARM_SEVERITY,
                ControlValue.of(severity.name())
        );
        bus.publish(
                getNodeId(),
                GridWorksChannels.ALARM_CONDITION_ACTIVE,
                ControlValue.of(alarmLatch.isConditionActive())
        );
        bus.publish(
                getNodeId(),
                GridWorksChannels.ALARM_LATCHED,
                ControlValue.of(alarmLatch.isLatched())
        );
        bus.publish(
                getNodeId(),
                GridWorksChannels.ALARM_ACKNOWLEDGED,
                ControlValue.of(alarmLatch.isAcknowledged())
        );
        bus.publish(
                getNodeId(),
                GridWorksChannels.ALARM_OCCURRENCES,
                ControlValue.of((double) alarmHistory.occurrenceCount())
        );
        bus.publish(
                getNodeId(),
                GridWorksChannels.ALARM_LAST_TRIGGERED_EPOCH_MS,
                ControlValue.of((double) alarmHistory.lastTriggeredEpochMillis())
        );
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
            return normalized.substring(0, MAX_NAME_LENGTH);
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
                                .name(Component.text(alarmName, NamedTextColor.GOLD))
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
                                .name(Component.text("Rename Alarm", NamedTextColor.GOLD))
                                .lore(Component.text(
                                        "Type above; changes save immediately.",
                                        NamedTextColor.GRAY
                                ))
                )
                .build();

        try {
            AnvilWindow window = AnvilWindow.builder()
                    .setViewer(player)
                    .setUpperGui(upperGui)
                    .setLowerGui(lowerGui)
                    .setTitle(Component.text("Name Alarm Indicator"))
                    .addRenameHandler(rawName -> {
                        if (firstRename[0]) {
                            firstRename[0] = false;
                            return;
                        }

                        alarmName = normalizeName(rawName);
                        nameItem.notifyWindows();
                        publishAlarmState();
                    })
                    .build(player);
            window.open();
        } catch (RuntimeException exception) {
            GridWorks.getInstance().getLogger().log(
                    java.util.logging.Level.SEVERE,
                    "Could not open Alarm Indicator rename window",
                    exception
            );
            player.sendMessage(Component.text(
                    "GridWorks could not open the rename window.",
                    NamedTextColor.RED
            ));
        }
    }

    private void changeSeverity(int direction) {
        severity = severity.cycle(direction);
        severityItem.notifyWindows();
        statusItem.notifyWindows();
        publishAlarmState();
    }

    private void toggleSound() {
        soundEnabled = !soundEnabled;
        soundItem.notifyWindows();
    }

    private static String formatHistoryTime(long epochMillis) {
        if (epochMillis <= 0L) {
            return "never";
        }
        return java.time.Instant.ofEpochMilli(epochMillis).toString();
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

    private final class NameItem extends AlarmItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(Material.NAME_TAG, "Name: " + alarmName)
                    .lore(Component.text(
                            "Click to rename this alarm",
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

    private final class SeverityItem extends AlarmItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            Material material = switch (severity) {
                case CRITICAL -> Material.RED_DYE;
                case WARNING -> Material.YELLOW_DYE;
                case INFO -> Material.LIGHT_BLUE_DYE;
            };

            return item(material, "Severity: " + severity.displayName())
                    .lore(
                            Component.text(
                                    "Left/right click to cycle severity",
                                    NamedTextColor.YELLOW
                            ),
                            Component.text(
                                    "Existing alarms default to Warning",
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
            if (clickType.isLeftClick()) {
                changeSeverity(1);
            } else if (clickType.isRightClick()) {
                changeSeverity(-1);
            }
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
                    Component.text("Severity: " + severity.displayName(), NamedTextColor.WHITE),
                    Component.text("Latch: " + latch, NamedTextColor.WHITE),
                    Component.text(
                            "Occurrences: " + alarmHistory.occurrenceCount(),
                            NamedTextColor.GRAY
                    ),
                    Component.text(
                            "Last trigger: " + formatHistoryTime(
                                    alarmHistory.lastTriggeredEpochMillis()
                            ),
                            NamedTextColor.GRAY
                    ),
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
