package io.github.wickidcow.gridworks.power;

import io.github.wickidcow.gridworks.api.power.PowerGridSnapshot;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

public final class UnavailablePowerGridBridge implements PowerGridBridge {
    private final String status;

    public UnavailablePowerGridBridge(String status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public @NotNull String status() {
        return status;
    }

    @Override
    public @NotNull Optional<PowerGridSnapshot> snapshotFor(@NotNull Block block) {
        Objects.requireNonNull(block, "block");
        return Optional.empty();
    }
}
