package io.github.wickidcow.gridworks.power;

import io.github.wickidcow.gridworks.api.power.PowerBranchControlResult;
import io.github.wickidcow.gridworks.api.power.PowerBranchProvider;
import io.github.wickidcow.gridworks.api.power.PowerBranchSnapshot;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.plugin.ServicesManager;
import org.jetbrains.annotations.NotNull;

/**
 * Dynamically resolves the highest-priority PowerBranchProvider.
 */
public final class ServicePowerBranchBridge implements PowerBranchBridge {
    private final ServicesManager servicesManager;
    private final String unavailableStatus;

    public ServicePowerBranchBridge(
            ServicesManager servicesManager,
            String unavailableStatus
    ) {
        this.servicesManager = Objects.requireNonNull(
                servicesManager,
                "servicesManager"
        );
        this.unavailableStatus = Objects.requireNonNull(
                unavailableStatus,
                "unavailableStatus"
        );
    }

    @Override
    public boolean isAvailable() {
        return servicesManager.load(PowerBranchProvider.class) != null;
    }

    @Override
    public @NotNull String status() {
        PowerBranchProvider provider = servicesManager.load(PowerBranchProvider.class);
        return provider == null
                ? unavailableStatus
                : "Branch provider: " + provider.providerId();
    }

    @Override
    public @NotNull Optional<PowerBranchSnapshot> snapshotFor(
            @NotNull Block target,
            @NotNull BlockFace side
    ) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(side, "side");

        PowerBranchProvider provider = servicesManager.load(PowerBranchProvider.class);
        if (provider == null) {
            return Optional.empty();
        }

        return Objects.requireNonNull(
                provider.snapshotFor(target, side),
                "PowerBranchProvider.snapshotFor returned null"
        );
    }

    @Override
    public @NotNull PowerBranchControlResult setEnabled(
            @NotNull Block target,
            @NotNull BlockFace side,
            boolean enabled
    ) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(side, "side");

        PowerBranchProvider provider = servicesManager.load(PowerBranchProvider.class);
        if (provider == null) {
            return PowerBranchControlResult.targetNotFound(unavailableStatus);
        }

        return Objects.requireNonNull(
                provider.setEnabled(target, side, enabled),
                "PowerBranchProvider.setEnabled returned null"
        );
    }

    @Override
    public @NotNull PowerBranchControlResult setPowerLimitWatts(
            @NotNull Block target,
            @NotNull BlockFace side,
            double watts
    ) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(side, "side");
        if (!Double.isFinite(watts) || watts < 0.0) {
            throw new IllegalArgumentException(
                    "watts must be finite and non-negative"
            );
        }

        PowerBranchProvider provider = servicesManager.load(PowerBranchProvider.class);
        if (provider == null) {
            return PowerBranchControlResult.targetNotFound(unavailableStatus);
        }

        return Objects.requireNonNull(
                provider.setPowerLimitWatts(target, side, watts),
                "PowerBranchProvider.setPowerLimitWatts returned null"
        );
    }
}
