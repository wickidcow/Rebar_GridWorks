package io.github.wickidcow.gridworks.api.power;

import java.util.Optional;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;

/**
 * Public service contract for providers that can control one electrical branch
 * exposed on a block face.
 *
 * <p>The supplied block is already loaded. Providers must not force chunks to
 * load. The face is the side of the target block that points toward the
 * GridWorks breaker/limiter.</p>
 */
public interface PowerBranchProvider {
    @NotNull String providerId();

    @NotNull Optional<PowerBranchSnapshot> snapshotFor(
            @NotNull Block target,
            @NotNull BlockFace side
    );

    /**
     * Opens or closes the branch. Implementations must be idempotent.
     *
     * <p>An APPLIED result means a subsequent snapshot should immediately
     * reflect the requested state.</p>
     */
    @NotNull PowerBranchControlResult setEnabled(
            @NotNull Block target,
            @NotNull BlockFace side,
            boolean enabled
    );

    /**
     * Optional branch limiter hook.
     *
     * <p>GridWorks uses {@link Double#MAX_VALUE} as the provider-neutral
     * representation of bypass/unlimited.</p>
     */
    default @NotNull PowerBranchControlResult setPowerLimitWatts(
            @NotNull Block target,
            @NotNull BlockFace side,
            double watts
    ) {
        return PowerBranchControlResult.unsupported(
                "This provider does not support branch power limits"
        );
    }
}
