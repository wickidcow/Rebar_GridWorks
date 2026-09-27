package io.github.wickidcow.gridworks.power;

import io.github.wickidcow.gridworks.api.power.PowerGridProvider;
import io.github.wickidcow.gridworks.api.power.PowerGridSnapshot;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.block.Block;
import org.bukkit.plugin.ServicesManager;
import org.jetbrains.annotations.NotNull;

/**
 * Resolves the highest-priority currently registered PowerGridProvider.
 */
public final class ServicePowerGridBridge implements PowerGridBridge {
    private final ServicesManager servicesManager;
    private final String unavailableStatus;

    public ServicePowerGridBridge(
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
        return servicesManager.load(PowerGridProvider.class) != null;
    }

    @Override
    public @NotNull String status() {
        PowerGridProvider provider = servicesManager.load(PowerGridProvider.class);
        return provider == null
                ? unavailableStatus
                : "Power provider: " + provider.providerId();
    }

    @Override
    public @NotNull Optional<PowerGridSnapshot> snapshotFor(@NotNull Block block) {
        Objects.requireNonNull(block, "block");

        PowerGridProvider provider = servicesManager.load(PowerGridProvider.class);
        if (provider == null) {
            return Optional.empty();
        }

        return Objects.requireNonNull(
                provider.snapshotFor(block),
                "PowerGridProvider.snapshotFor returned null"
        );
    }
}
