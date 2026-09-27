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
import io.github.wickidcow.gridworks.api.power.LoadSheddingFailSafeMode;
import io.github.wickidcow.gridworks.api.power.LoadSheddingOutputState;
import io.github.wickidcow.gridworks.api.power.LoadSheddingPolicy;
import io.github.wickidcow.gridworks.api.power.LoadSheddingRoutes;
import io.github.wickidcow.gridworks.api.power.LoadSheddingStage;
import io.github.wickidcow.gridworks.api.power.LoadSheddingThresholds;
import java.util.List;
import java.util.Objects;
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

public final class LoadSheddingControllerBlock extends PhysicalControlNodeBlock
        implements GuiRebarBlock, ControlStateSource {

    private static final NamespacedKey SOURCE_KEY = key("load_shed_source");
    private static final NamespacedKey ESSENTIAL_ADDRESS_KEY = key("load_shed_essential_address");
    private static final NamespacedKey NORMAL_ADDRESS_KEY = key("load_shed_normal_address");
    private static final NamespacedKey OPTIONAL_ADDRESS_KEY = key("load_shed_optional_address");
    private static final NamespacedKey FAIL_SAFE_KEY = key("load_shed_fail_safe");
    private static final NamespacedKey LAST_STAGE_KEY = key("load_shed_last_stage");
    private static final NamespacedKey OPTIONAL_SHED_AT_KEY =
            key("load_shed_optional_shed_at");
    private static final NamespacedKey OPTIONAL_RESTORE_AT_KEY =
            key("load_shed_optional_restore_at");
    private static final NamespacedKey NORMAL_SHED_AT_KEY =
            key("load_shed_normal_shed_at");
    private static final NamespacedKey NORMAL_RESTORE_AT_KEY =
            key("load_shed_normal_restore_at");

    private UUID sourceId;
    private ControlAddress essentialAddress;
    private ControlAddress normalAddress;
    private ControlAddress optionalAddress;
    private LoadSheddingFailSafeMode failSafeMode;
    private LoadSheddingThresholds thresholds;
    private LoadSheddingPolicy policy;

    private boolean powerAvailable;
    private Double loadRatio;
    private Integer unpoweredConsumers;
    private boolean telemetryKnown;
    private LoadSheddingOutputState lastPublishedState;

    private final SourceItem sourceItem = new SourceItem();
    private final StatusItem statusItem = new StatusItem();
    private final FailSafeItem failSafeItem = new FailSafeItem();
    private final AddressItem essentialAddressItem = new AddressItem(Tier.ESSENTIAL);
    private final AddressItem normalAddressItem = new AddressItem(Tier.NORMAL);
    private final AddressItem optionalAddressItem = new AddressItem(Tier.OPTIONAL);
    private final ThresholdItem optionalShedItem =
            new ThresholdItem(ThresholdKind.OPTIONAL_SHED);
    private final ThresholdItem optionalRestoreItem =
            new ThresholdItem(ThresholdKind.OPTIONAL_RESTORE);
    private final ThresholdItem normalShedItem =
            new ThresholdItem(ThresholdKind.NORMAL_SHED);
    private final ThresholdItem normalRestoreItem =
            new ThresholdItem(ThresholdKind.NORMAL_RESTORE);

    public LoadSheddingControllerBlock(
            @NotNull Block block,
            @NotNull BlockCreateContext context
    ) {
        super(block, context);

        this.sourceId = null;
        this.essentialAddress = ControlAddress.defaultFor(getNodeId(), "essential");
        this.normalAddress = ControlAddress.defaultFor(getNodeId(), "normal");
        this.optionalAddress = ControlAddress.defaultFor(getNodeId(), "optional");
        this.failSafeMode = LoadSheddingFailSafeMode.ESSENTIAL_ONLY;
        this.thresholds = LoadSheddingThresholds.defaults();
        this.policy = thresholds.createPolicy(LoadSheddingStage.NORMAL);
    }

    public LoadSheddingControllerBlock(
            @NotNull Block block,
            @NotNull PersistentDataContainer pdc
    ) {
        super(block, pdc);

        this.sourceId = uuidFromStored(
                pdc.get(SOURCE_KEY, PersistentDataType.STRING)
        );
        this.essentialAddress = ControlAddress.fromStoredOrDefault(
                pdc.get(ESSENTIAL_ADDRESS_KEY, PersistentDataType.STRING),
                ControlAddress.defaultFor(getNodeId(), "essential")
        );
        this.normalAddress = ControlAddress.fromStoredOrDefault(
                pdc.get(NORMAL_ADDRESS_KEY, PersistentDataType.STRING),
                ControlAddress.defaultFor(getNodeId(), "normal")
        );
        this.optionalAddress = ControlAddress.fromStoredOrDefault(
                pdc.get(OPTIONAL_ADDRESS_KEY, PersistentDataType.STRING),
                ControlAddress.defaultFor(getNodeId(), "optional")
        );
        this.failSafeMode = LoadSheddingFailSafeMode.fromStored(
                pdc.get(FAIL_SAFE_KEY, PersistentDataType.STRING)
        );
        this.thresholds = LoadSheddingThresholds.fromStoredOrDefault(
                pdc.get(OPTIONAL_SHED_AT_KEY, PersistentDataType.DOUBLE),
                pdc.get(OPTIONAL_RESTORE_AT_KEY, PersistentDataType.DOUBLE),
                pdc.get(NORMAL_SHED_AT_KEY, PersistentDataType.DOUBLE),
                pdc.get(NORMAL_RESTORE_AT_KEY, PersistentDataType.DOUBLE)
        );
        this.policy = thresholds.createPolicy(
                LoadSheddingStage.fromStored(
                        pdc.get(LAST_STAGE_KEY, PersistentDataType.STRING)
                )
        );

        LoadSheddingRoutes routes = LoadSheddingRoutes.repaired(
                getNodeId(),
                essentialAddress,
                normalAddress,
                optionalAddress
        );
        this.essentialAddress = routes.essential();
        this.normalAddress = routes.normal();
        this.optionalAddress = routes.optional();
    }

    @Override
    public boolean accepts(@NotNull ControlChannel channel) {
        return GridWorksChannels.POWER_AVAILABLE.equals(channel)
                || GridWorksChannels.POWER_LOAD_RATIO.equals(channel)
                || GridWorksChannels.POWER_UNPOWERED_CONSUMERS.equals(channel)
                || GridWorksChannels.POWER_SAMPLE_REVISION.equals(channel);
    }

    @Override
    protected void handleSignal(@NotNull ControlSignal signal) {
        runOnServerThreadIfActive(() -> handleSignalOnServerThread(signal));
    }

    @Override
    public void onControlPeerUnavailable(@NotNull UUID peerId) {
        if (!peerId.equals(sourceId)) {
            return;
        }

        runOnServerThreadIfActive(() -> {
            powerAvailable = false;
            loadRatio = null;
            unpoweredConsumers = null;
            telemetryKnown = false;
            publishOutputs(false);
            notifyItems();
        });
    }

    @Override
    public void publishCurrentState() {
        publishOutputs(true);
    }

    @Override
    protected void writeNodeData(@NotNull PersistentDataContainer pdc) {
        if (sourceId == null) {
            pdc.remove(SOURCE_KEY);
        } else {
            pdc.set(SOURCE_KEY, PersistentDataType.STRING, sourceId.toString());
        }

        pdc.set(
                ESSENTIAL_ADDRESS_KEY,
                PersistentDataType.STRING,
                essentialAddress.value()
        );
        pdc.set(
                NORMAL_ADDRESS_KEY,
                PersistentDataType.STRING,
                normalAddress.value()
        );
        pdc.set(
                OPTIONAL_ADDRESS_KEY,
                PersistentDataType.STRING,
                optionalAddress.value()
        );
        pdc.set(FAIL_SAFE_KEY, PersistentDataType.STRING, failSafeMode.name());
        pdc.set(LAST_STAGE_KEY, PersistentDataType.STRING, policy.stage().name());
        pdc.set(
                OPTIONAL_SHED_AT_KEY,
                PersistentDataType.DOUBLE,
                thresholds.optionalShedAt()
        );
        pdc.set(
                OPTIONAL_RESTORE_AT_KEY,
                PersistentDataType.DOUBLE,
                thresholds.optionalRestoreAt()
        );
        pdc.set(
                NORMAL_SHED_AT_KEY,
                PersistentDataType.DOUBLE,
                thresholds.normalShedAt()
        );
        pdc.set(
                NORMAL_RESTORE_AT_KEY,
                PersistentDataType.DOUBLE,
                thresholds.normalRestoreAt()
        );
    }

    @Override
    public @NotNull Gui createGui() {
        return Gui.builder()
                .setStructure(
                        "s # x # f # # # #",
                        "e # n # o # # # #",
                        "a b # c d # # # #"
                )
                .addIngredient('#', GuiItems.background())
                .addIngredient('s', sourceItem)
                .addIngredient('x', statusItem)
                .addIngredient('f', failSafeItem)
                .addIngredient('e', essentialAddressItem)
                .addIngredient('n', normalAddressItem)
                .addIngredient('o', optionalAddressItem)
                .addIngredient('a', optionalShedItem)
                .addIngredient('b', optionalRestoreItem)
                .addIngredient('c', normalShedItem)
                .addIngredient('d', normalRestoreItem)
                .build();
    }

    @Override
    public @NotNull Component getGuiTitle() {
        return Component.text("Load Shedding Controller", NamedTextColor.GOLD);
    }

    public @NotNull LoadSheddingStage getStage() {
        return policy.stage();
    }

    public boolean isTelemetryKnown() {
        return telemetryKnown;
    }

    public @NotNull LoadSheddingFailSafeMode getFailSafeMode() {
        return failSafeMode;
    }

    public @NotNull LoadSheddingThresholds getThresholds() {
        return thresholds;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public @NotNull LoadSheddingOutputState getOutputState() {
        return currentOutputState();
    }

    public @NotNull ControlAddress getEssentialAddress() {
        return essentialAddress;
    }

    public @NotNull ControlAddress getNormalAddress() {
        return normalAddress;
    }

    public @NotNull ControlAddress getOptionalAddress() {
        return optionalAddress;
    }

    private void handleSignalOnServerThread(ControlSignal signal) {
        if (!acceptSource(signal.source())) {
            return;
        }

        if (GridWorksChannels.POWER_AVAILABLE.equals(signal.channel())) {
            if (signal.value() instanceof ControlValue.BooleanValue booleanValue) {
                powerAvailable = booleanValue.value();
                if (!powerAvailable) {
                    loadRatio = null;
                    unpoweredConsumers = null;
                    telemetryKnown = false;
                    publishOutputs(false);
                    notifyItems();
                }
            }
            return;
        }

        if (GridWorksChannels.POWER_LOAD_RATIO.equals(signal.channel())) {
            if (signal.value() instanceof ControlValue.NumberValue numberValue) {
                loadRatio = Math.max(0.0, numberValue.value());
            }
            return;
        }

        if (GridWorksChannels.POWER_UNPOWERED_CONSUMERS.equals(signal.channel())) {
            if (signal.value() instanceof ControlValue.NumberValue numberValue) {
                double value = Math.max(0.0, numberValue.value());
                unpoweredConsumers = (int) Math.min(Integer.MAX_VALUE, Math.floor(value));
            }
            return;
        }

        if (GridWorksChannels.POWER_SAMPLE_REVISION.equals(signal.channel())) {
            evaluateCompletedSample();
        }
    }

    private boolean acceptSource(UUID source) {
        if (sourceId != null) {
            return sourceId.equals(source);
        }

        if (!GridWorks.getInstance()
                .getPhysicalControlNetwork()
                .isLinked(getNodeId(), source)) {
            return false;
        }

        sourceId = source;
        sourceItem.notifyWindows();
        return true;
    }

    private void evaluateCompletedSample() {
        if (!powerAvailable || loadRatio == null || unpoweredConsumers == null) {
            telemetryKnown = false;
            publishOutputs(false);
            notifyItems();
            return;
        }

        policy.update(loadRatio, unpoweredConsumers);
        telemetryKnown = true;
        publishOutputs(false);
        notifyItems();
    }

    private LoadSheddingOutputState currentOutputState() {
        if (telemetryKnown) {
            return LoadSheddingOutputState.fromStage(policy.stage());
        }
        return failSafeMode.resolve(policy.stage());
    }

    private void publishOutputs(boolean force) {
        LoadSheddingOutputState state = currentOutputState();
        if (!force && state.equals(lastPublishedState)) {
            return;
        }

        var bus = GridWorks.getInstance().getControlBus();
        bus.publish(
                getNodeId(),
                essentialAddress.channel(),
                ControlValue.of(state.essentialEnabled())
        );
        bus.publish(
                getNodeId(),
                normalAddress.channel(),
                ControlValue.of(state.normalEnabled())
        );
        bus.publish(
                getNodeId(),
                optionalAddress.channel(),
                ControlValue.of(state.optionalEnabled())
        );

        lastPublishedState = state;
    }

    private void cycleSource(int direction) {
        List<UUID> choices = GridWorks.getInstance()
                .getPhysicalControlNetwork()
                .activeLinkedNodes(getNodeId());

        UUID next;
        if (choices.isEmpty()) {
            next = null;
        } else if (sourceId == null) {
            next = direction >= 0 ? choices.getFirst() : choices.getLast();
        } else {
            int index = choices.indexOf(sourceId);
            if (index < 0) {
                next = null;
            } else {
                int candidate = index + (direction >= 0 ? 1 : -1);
                next = candidate < 0 || candidate >= choices.size()
                        ? null
                        : choices.get(candidate);
            }
        }

        if (Objects.equals(sourceId, next)) {
            return;
        }

        sourceId = next;
        powerAvailable = false;
        loadRatio = null;
        unpoweredConsumers = null;
        telemetryKnown = false;
        policy.reset();
        publishOutputs(false);
        notifyItems();

        GridWorks.getInstance()
                .getPhysicalControlNetwork()
                .replayStateSources(getNodeId());
    }

    private void cycleFailSafe(int direction) {
        failSafeMode = failSafeMode.cycle(direction);
        publishOutputs(false);
        failSafeItem.notifyWindows();
        statusItem.notifyWindows();
    }

    private void changeThreshold(ThresholdKind kind, double delta) {
        LoadSheddingStage previousStage = policy.stage();
        LoadSheddingThresholds next = kind.adjust(thresholds, delta);
        if (next.equals(thresholds)) {
            return;
        }

        thresholds = next;
        policy = thresholds.createPolicy(previousStage);

        if (telemetryKnown && loadRatio != null && unpoweredConsumers != null) {
            policy.update(loadRatio, unpoweredConsumers);
        }

        publishOutputs(false);
        notifyItems();
    }

    private void setAddress(Tier tier, ControlAddress next) {
        ControlAddress current = addressFor(tier);
        if (current.equals(next)) {
            return;
        }

        if (next.equals(addressFor(tier.otherOne()))
                || next.equals(addressFor(tier.otherTwo()))) {
            throw new IllegalArgumentException(
                    "Load-shedding tier addresses must be distinct"
            );
        }

        LoadSheddingOutputState state = currentOutputState();
        boolean currentValue = valueForTier(state, tier);

        GridWorks.getInstance().getControlBus().publish(
                getNodeId(),
                current.channel(),
                ControlValue.of(false)
        );

        setAddressField(tier, next);

        GridWorks.getInstance().getControlBus().publish(
                getNodeId(),
                next.channel(),
                ControlValue.of(currentValue)
        );

        addressItem(tier).notifyWindows();
    }

    private void openAddressWindow(Player player, Tier tier) {
        final boolean[] firstRename = {true};

        Gui upperGui = Gui.builder()
                .setStructure("# a #")
                .addIngredient('#', GuiItems.background())
                .addIngredient(
                        'a',
                        ItemStackBuilder.of(Material.NAME_TAG)
                                .name(Component.text(
                                        addressFor(tier).value(),
                                        NamedTextColor.GOLD
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
                                .name(Component.text(
                                        "Set " + tier.displayName + " Address",
                                        NamedTextColor.GOLD
                                ))
                                .lore(Component.text(
                                        "Tier addresses must be distinct.",
                                        NamedTextColor.GRAY
                                ))
                )
                .build();

        try {
            AnvilWindow window = AnvilWindow.builder()
                    .setViewer(player)
                    .setUpperGui(upperGui)
                    .setLowerGui(lowerGui)
                    .setTitle(Component.text("Load Shedding Address"))
                    .addRenameHandler(raw -> {
                        if (firstRename[0]) {
                            firstRename[0] = false;
                            return;
                        }

                        try {
                            setAddress(tier, ControlAddress.fromUserInput(raw));
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
                    java.util.logging.Level.SEVERE,
                    "Could not open Load Shedding address window",
                    exception
            );
            player.sendMessage(Component.text(
                    "GridWorks could not open the address window.",
                    NamedTextColor.RED
            ));
        }
    }

    private ControlAddress addressFor(Tier tier) {
        return switch (tier) {
            case ESSENTIAL -> essentialAddress;
            case NORMAL -> normalAddress;
            case OPTIONAL -> optionalAddress;
        };
    }

    private void setAddressField(Tier tier, ControlAddress address) {
        switch (tier) {
            case ESSENTIAL -> essentialAddress = address;
            case NORMAL -> normalAddress = address;
            case OPTIONAL -> optionalAddress = address;
        }
    }

    private AddressItem addressItem(Tier tier) {
        return switch (tier) {
            case ESSENTIAL -> essentialAddressItem;
            case NORMAL -> normalAddressItem;
            case OPTIONAL -> optionalAddressItem;
        };
    }

    private static boolean valueForTier(
            LoadSheddingOutputState state,
            Tier tier
    ) {
        return switch (tier) {
            case ESSENTIAL -> state.essentialEnabled();
            case NORMAL -> state.normalEnabled();
            case OPTIONAL -> state.optionalEnabled();
        };
    }

    private void notifyItems() {
        sourceItem.notifyWindows();
        statusItem.notifyWindows();
        failSafeItem.notifyWindows();
        essentialAddressItem.notifyWindows();
        normalAddressItem.notifyWindows();
        optionalAddressItem.notifyWindows();
        optionalShedItem.notifyWindows();
        optionalRestoreItem.notifyWindows();
        normalShedItem.notifyWindows();
        normalRestoreItem.notifyWindows();
    }

    private static UUID uuidFromStored(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static NamespacedKey key(String value) {
        return Objects.requireNonNull(
                NamespacedKey.fromString("gridworks:" + value)
        );
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8).toUpperCase();
    }

    private abstract class ControllerItem extends AbstractItem {
        protected ItemStackBuilder item(Material material, String name) {
            return ItemStackBuilder.of(material)
                    .name(Component.text(name, NamedTextColor.GOLD));
        }
    }

    private final class SourceItem extends ControllerItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(
                    Material.TARGET,
                    "Power source: " + (sourceId == null ? "AUTO" : shortId(sourceId))
            ).lore(
                    Component.text(
                            "Left/right: cycle directly linked loaded nodes",
                            NamedTextColor.YELLOW
                    ),
                    Component.text(
                            "AUTO binds to the first linked power telemetry source",
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
                cycleSource(1);
            } else if (clickType.isRightClick()) {
                cycleSource(-1);
            }
        }
    }

    private final class StatusItem extends ControllerItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            LoadSheddingOutputState state = currentOutputState();
            String status = telemetryKnown
                    ? policy.stage().name()
                    : "WAITING / " + failSafeMode.displayName();

            ItemStackBuilder builder = item(
                    telemetryKnown ? Material.COMPARATOR : Material.GRAY_DYE,
                    "Stage: " + status
            ).lore(
                    Component.text(
                            "Essential: " + onOff(state.essentialEnabled()),
                            NamedTextColor.WHITE
                    ),
                    Component.text(
                            "Normal: " + onOff(state.normalEnabled()),
                            NamedTextColor.WHITE
                    ),
                    Component.text(
                            "Optional: " + onOff(state.optionalEnabled()),
                            NamedTextColor.WHITE
                    )
            );

            if (loadRatio != null) {
                builder.lore(Component.text(
                        String.format(
                                java.util.Locale.ROOT,
                                "Load: %.1f%%",
                                loadRatio * 100.0
                        ),
                        NamedTextColor.GRAY
                ));
            }
            if (unpoweredConsumers != null) {
                builder.lore(Component.text(
                        "Unpowered consumers: " + unpoweredConsumers,
                        NamedTextColor.GRAY
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

    private final class FailSafeItem extends ControllerItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(
                    Material.SHIELD,
                    "Missing telemetry: " + failSafeMode.displayName()
            ).lore(
                    Component.text(
                            "Left/right click to cycle fail-safe behavior",
                            NamedTextColor.YELLOW
                    ),
                    Component.text(
                            "Essential Only / Allow All / Hold Last",
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
                cycleFailSafe(1);
            } else if (clickType.isRightClick()) {
                cycleFailSafe(-1);
            }
        }
    }

    private final class ThresholdItem extends ControllerItem {
        private final ThresholdKind kind;

        private ThresholdItem(ThresholdKind kind) {
            this.kind = kind;
        }

        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(
                    kind.material,
                    kind.displayName + ": " + formatPercent(kind.value(thresholds))
            ).lore(
                    Component.text(
                            "Left +5% / Right -5%",
                            NamedTextColor.YELLOW
                    ),
                    Component.text(
                            "Shift uses 25%",
                            NamedTextColor.YELLOW
                    ),
                    Component.text(
                            "Edits clamp to preserve hysteresis ordering",
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
            double step = clickType.isShiftClick() ? 0.25 : 0.05;
            if (clickType.isLeftClick()) {
                changeThreshold(kind, step);
            } else if (clickType.isRightClick()) {
                changeThreshold(kind, -step);
            }
        }
    }

    private final class AddressItem extends ControllerItem {
        private final Tier tier;

        private AddressItem(Tier tier) {
            this.tier = tier;
        }

        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
            return item(
                    tier.material,
                    tier.displayName + ": " + addressFor(tier).value()
            ).lore(
                    Component.text(
                            addressFor(tier).channel().toString(),
                            NamedTextColor.GRAY
                    ),
                    Component.text("Click to edit address", NamedTextColor.YELLOW)
            );
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
                    () -> openAddressWindow(player, tier)
            );
        }
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    private static String formatPercent(double ratio) {
        return String.format(java.util.Locale.ROOT, "%.0f%%", ratio * 100.0);
    }

    private enum ThresholdKind {
        OPTIONAL_SHED("Optional shed", Material.REDSTONE_TORCH) {
            @Override
            double value(LoadSheddingThresholds thresholds) {
                return thresholds.optionalShedAt();
            }

            @Override
            LoadSheddingThresholds adjust(
                    LoadSheddingThresholds thresholds,
                    double delta
            ) {
                return thresholds.withOptionalShedAt(
                        thresholds.optionalShedAt() + delta
                );
            }
        },
        OPTIONAL_RESTORE("Optional restore", Material.LEVER) {
            @Override
            double value(LoadSheddingThresholds thresholds) {
                return thresholds.optionalRestoreAt();
            }

            @Override
            LoadSheddingThresholds adjust(
                    LoadSheddingThresholds thresholds,
                    double delta
            ) {
                return thresholds.withOptionalRestoreAt(
                        thresholds.optionalRestoreAt() + delta
                );
            }
        },
        NORMAL_SHED("Normal shed", Material.REDSTONE_BLOCK) {
            @Override
            double value(LoadSheddingThresholds thresholds) {
                return thresholds.normalShedAt();
            }

            @Override
            LoadSheddingThresholds adjust(
                    LoadSheddingThresholds thresholds,
                    double delta
            ) {
                return thresholds.withNormalShedAt(
                        thresholds.normalShedAt() + delta
                );
            }
        },
        NORMAL_RESTORE("Normal restore", Material.COMPARATOR) {
            @Override
            double value(LoadSheddingThresholds thresholds) {
                return thresholds.normalRestoreAt();
            }

            @Override
            LoadSheddingThresholds adjust(
                    LoadSheddingThresholds thresholds,
                    double delta
            ) {
                return thresholds.withNormalRestoreAt(
                        thresholds.normalRestoreAt() + delta
                );
            }
        };

        private final String displayName;
        private final Material material;

        ThresholdKind(String displayName, Material material) {
            this.displayName = displayName;
            this.material = material;
        }

        abstract double value(LoadSheddingThresholds thresholds);

        abstract LoadSheddingThresholds adjust(
                LoadSheddingThresholds thresholds,
                double delta
        );
    }

    private enum Tier {
        ESSENTIAL("Essential", Material.NETHER_STAR),
        NORMAL("Normal", Material.IRON_INGOT),
        OPTIONAL("Optional", Material.FEATHER);

        private final String displayName;
        private final Material material;

        Tier(String displayName, Material material) {
            this.displayName = displayName;
            this.material = material;
        }

        private Tier otherOne() {
            return switch (this) {
                case ESSENTIAL -> NORMAL;
                case NORMAL -> ESSENTIAL;
                case OPTIONAL -> ESSENTIAL;
            };
        }

        private Tier otherTwo() {
            return switch (this) {
                case ESSENTIAL -> OPTIONAL;
                case NORMAL -> OPTIONAL;
                case OPTIONAL -> NORMAL;
            };
        }
    }
}
