package io.github.wickidcow.gridworks.power;

import io.github.wickidcow.gridworks.api.power.PowerBranchControlResult;
import io.github.wickidcow.gridworks.api.power.PowerBranchSnapshot;
import java.util.Optional;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;

public interface PowerBranchBridge {
    boolean isAvailable();

    @NotNull String status();

    @NotNull Optional<PowerBranchSnapshot> snapshotFor(
            @NotNull Block target,
            @NotNull BlockFace side
    );

    @NotNull PowerBranchControlResult setEnabled(
            @NotNull Block target,
            @NotNull BlockFace side,
            boolean enabled
    );

    @NotNull PowerBranchControlResult setPowerLimitWatts(
            @NotNull Block target,
            @NotNull BlockFace side,
            double watts
    );
}
