package io.github.wickidcow.gridworks.api.power;

import java.util.Optional;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Public service contract for addons that can expose an electricity network to
 * GridWorks without coupling GridWorks core to that provider's implementation.
 *
 * <p>Providers register this service through Bukkit's ServicesManager. When
 * multiple providers are registered, Bukkit's normal service-priority rules
 * select the active provider.</p>
 */
public interface PowerGridProvider {
    /**
     * Stable human-readable provider identifier for diagnostics.
     */
    @NotNull String providerId();

    /**
     * Returns a snapshot for the electricity network associated with the given
     * loaded block, or empty when that block is not part of a known grid.
     *
     * <p>Implementations must not force chunks to load.</p>
     */
    @NotNull Optional<PowerGridSnapshot> snapshotFor(@NotNull Block block);
}
