package io.github.wickidcow.gridworks.machine;

import io.github.pylonmc.rebar.block.BlockStorage;
import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.block.interfaces.ProcessorRebarBlock;
import io.github.pylonmc.rebar.block.interfaces.RecipeProcessorRebarBlock;
import org.bukkit.block.Block;

public final class MachineProbe {
    private MachineProbe() {
        throw new AssertionError("Utility class");
    }

    public static MachineSnapshot snapshot(Block block) {
        RebarBlock rebarBlock = BlockStorage.get(block);

        if (rebarBlock instanceof RecipeProcessorRebarBlock<?> processor) {
            if (!processor.isProcessingRecipe()) {
                return MachineSnapshot.idle("recipe_processor");
            }

            return MachineSnapshot.processing(
                    "recipe_processor",
                    processor.getRecipeTimeTicks(),
                    processor.getRecipeTicksRemaining()
            );
        }

        if (rebarBlock instanceof ProcessorRebarBlock processor) {
            if (!processor.isProcessing()) {
                return MachineSnapshot.idle("processor");
            }

            return MachineSnapshot.processing(
                    "processor",
                    processor.getProcessTimeTicks(),
                    processor.getProcessTicksRemaining()
            );
        }

        return MachineSnapshot.unavailable();
    }
}
