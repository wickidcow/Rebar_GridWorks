package io.github.wickidcow.gridworks.content.block;

import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

public final class ControlInterfaceBlock extends PhysicalControlNodeBlock {
    public ControlInterfaceBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
    }

    public ControlInterfaceBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);
    }
}
