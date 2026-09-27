package io.github.wickidcow.gridworks.fluid;

import io.github.pylonmc.rebar.block.BlockStorage;
import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.block.interfaces.FluidTankRebarBlock;
import io.github.pylonmc.rebar.fluid.RebarFluid;
import org.bukkit.block.Block;

public final class FluidProbe {
    private FluidProbe() {
        throw new AssertionError("Utility class");
    }

    public static FluidSnapshot snapshot(Block block) {
        RebarBlock rebarBlock = BlockStorage.get(block);
        if (!(rebarBlock instanceof FluidTankRebarBlock tank)) {
            return FluidSnapshot.unavailable();
        }

        RebarFluid fluid = tank.getFluidType();
        String fluidKey = fluid == null ? "" : fluid.getKey().toString();

        return new FluidSnapshot(
                true,
                fluidKey,
                Math.max(0.0, tank.getFluidAmount()),
                Math.max(0.0, tank.getFluidCapacity())
        );
    }
}
