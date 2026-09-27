package io.github.wickidcow.gridworks.power;

import io.github.wickidcow.gridworks.api.power.PowerGridSnapshot;
import java.util.Optional;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Upstream-sensitive electricity adapter boundary.
 *
 * <p>Implementations may depend on a particular Rebar/Pylon electricity API,
 * while the rest of GridWorks depends only on this contract and the neutral
 * {@link PowerGridSnapshot} model.</p>
 */
public interface PowerGridBridge {
    boolean isAvailable();

    @NotNull String status();

    @NotNull Optional<PowerGridSnapshot> snapshotFor(@NotNull Block block);
}
