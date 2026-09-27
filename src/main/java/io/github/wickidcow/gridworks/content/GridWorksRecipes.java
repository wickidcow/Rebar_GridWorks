package io.github.wickidcow.gridworks.content;

import io.github.pylonmc.rebar.recipe.RecipeType;
import io.github.pylonmc.rebar.recipe.vanilla.ShapedRebarRecipe;
import io.github.wickidcow.gridworks.GridWorks;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;

public final class GridWorksRecipes {
    private static final String GROUP = "gridworks";
    private static final List<NamespacedKey> REGISTERED_KEYS = new ArrayList<>();

    private GridWorksRecipes() {
        throw new AssertionError("Utility class");
    }

    public static void register(GridWorks plugin) {
        unregister();

        register(recipe(plugin, "control_interface", amount(GridWorksContent.CONTROL_INTERFACE_ITEM, 2),
                "CRC",
                "RQR",
                "CRC")
                .setIngredient('C', Material.COPPER_INGOT)
                .setIngredient('R', Material.REDSTONE)
                .setIngredient('Q', Material.QUARTZ));

        register(recipe(plugin, "gridworks_linker", GridWorksContent.GRIDWORKS_LINKER_ITEM,
                " C ",
                "RKR",
                " C ")
                .setIngredient('C', Material.COPPER_INGOT)
                .setIngredient('R', Material.REDSTONE)
                .setIngredient('K', Material.COMPASS));

        register(recipe(plugin, "redstone_sensor", GridWorksContent.REDSTONE_SENSOR_ITEM,
                "CRC",
                "ROR",
                "CRC")
                .setIngredient('C', Material.COPPER_INGOT)
                .setIngredient('R', Material.REDSTONE)
                .setIngredient('O', Material.OBSERVER));

        register(recipe(plugin, "inventory_sensor", GridWorksContent.INVENTORY_SENSOR_ITEM,
                "CRC",
                "RHR",
                "CRC")
                .setIngredient('C', Material.COPPER_INGOT)
                .setIngredient('R', Material.REDSTONE)
                .setIngredient('H', Material.HOPPER));

        register(recipe(plugin, "fluid_sensor", GridWorksContent.FLUID_SENSOR_ITEM,
                "CGC",
                "RBR",
                "CGC")
                .setIngredient('C', Material.COPPER_INGOT)
                .setIngredient('G', Material.GLASS)
                .setIngredient('R', Material.REDSTONE)
                .setIngredient('B', Material.BUCKET));

        register(recipe(plugin, "machine_sensor", GridWorksContent.MACHINE_SENSOR_ITEM,
                "CRC",
                "OQO",
                "CRC")
                .setIngredient('C', Material.COPPER_INGOT)
                .setIngredient('R', Material.REDSTONE)
                .setIngredient('O', Material.OBSERVER)
                .setIngredient('Q', Material.COMPARATOR));

        register(recipe(plugin, "status_light", GridWorksContent.STATUS_LIGHT_ITEM,
                " C ",
                "RLR",
                " C ")
                .setIngredient('C', Material.COPPER_INGOT)
                .setIngredient('R', Material.REDSTONE)
                .setIngredient('L', Material.REDSTONE_LAMP));

        register(recipe(plugin, "control_relay", GridWorksContent.CONTROL_RELAY_ITEM,
                "CRC",
                "RLR",
                "CRC")
                .setIngredient('C', Material.COPPER_INGOT)
                .setIngredient('R', Material.REDSTONE)
                .setIngredient('L', Material.LEVER));

        register(recipe(plugin, "addressed_relay", GridWorksContent.ADDRESSED_RELAY_ITEM,
                " R ",
                "ECE",
                " R ")
                .setIngredient('R', Material.REDSTONE)
                .setIngredient('E', Material.ENDER_PEARL)
                .setIngredient('C', GridWorksContent.CONTROL_RELAY_ITEM));

        register(recipe(plugin, "pulse_relay", GridWorksContent.PULSE_RELAY_ITEM,
                " R ",
                "CTC",
                " R ")
                .setIngredient('R', Material.REPEATER)
                .setIngredient('C', GridWorksContent.CONTROL_RELAY_ITEM)
                .setIngredient('T', Material.CLOCK));

        register(recipe(plugin, "delay_relay", GridWorksContent.DELAY_RELAY_ITEM,
                "RTR",
                "TCT",
                "RTR")
                .setIngredient('R', Material.REPEATER)
                .setIngredient('T', Material.CLOCK)
                .setIngredient('C', GridWorksContent.CONTROL_RELAY_ITEM));

        register(recipe(plugin, "alarm_indicator", GridWorksContent.ALARM_INDICATOR_ITEM,
                " R ",
                "SBS",
                " R ")
                .setIngredient('R', Material.REDSTONE)
                .setIngredient('S', GridWorksContent.STATUS_LIGHT_ITEM)
                .setIngredient('B', Material.BELL));

        register(recipe(plugin, "factory_controller", GridWorksContent.FACTORY_CONTROLLER_ITEM,
                "CQC",
                "RIR",
                "CPC")
                .setIngredient('C', Material.COPPER_INGOT)
                .setIngredient('Q', Material.COMPARATOR)
                .setIngredient('R', Material.REPEATER)
                .setIngredient('I', GridWorksContent.CONTROL_INTERFACE_ITEM)
                .setIngredient('P', Material.COPPER_BLOCK));

        register(recipe(plugin, "factory_monitor", GridWorksContent.FACTORY_MONITOR_ITEM,
                "TGT",
                "CIC",
                "TQT")
                .setIngredient('T', Material.TINTED_GLASS)
                .setIngredient('G', Material.GLOWSTONE_DUST)
                .setIngredient('C', Material.COPPER_INGOT)
                .setIngredient('I', GridWorksContent.CONTROL_INTERFACE_ITEM)
                .setIngredient('Q', Material.COMPARATOR));

        register(recipe(plugin, "alarm_console", GridWorksContent.ALARM_CONSOLE_ITEM,
                "TAT",
                "MIM",
                "TAT")
                .setIngredient('T', Material.TINTED_GLASS)
                .setIngredient('A', GridWorksContent.ALARM_INDICATOR_ITEM)
                .setIngredient('M', Material.COPPER_INGOT)
                .setIngredient('I', GridWorksContent.CONTROL_INTERFACE_ITEM));

        register(recipe(plugin, "power_grid_sensor", GridWorksContent.POWER_GRID_SENSOR_ITEM,
                " C ",
                "LIL",
                " Q ")
                .setIngredient('C', Material.COMPARATOR)
                .setIngredient('L', Material.LIGHTNING_ROD)
                .setIngredient('I', GridWorksContent.CONTROL_INTERFACE_ITEM)
                .setIngredient('Q', Material.QUARTZ));

        register(recipe(plugin, "load_shedding_controller", GridWorksContent.LOAD_SHEDDING_CONTROLLER_ITEM,
                "CBC",
                "RFR",
                "CBC")
                .setIngredient('C', Material.COPPER_BLOCK)
                .setIngredient('B', Material.COMPARATOR)
                .setIngredient('R', Material.REDSTONE_BLOCK)
                .setIngredient('F', GridWorksContent.FACTORY_CONTROLLER_ITEM));

        register(recipe(plugin, "smart_breaker", GridWorksContent.SMART_BREAKER_ITEM,
                "ILI",
                "RCR",
                "ILI")
                .setIngredient('I', Material.IRON_INGOT)
                .setIngredient('L', Material.LIGHTNING_ROD)
                .setIngredient('R', Material.REDSTONE)
                .setIngredient('C', GridWorksContent.CONTROL_RELAY_ITEM));

        register(recipe(plugin, "power_limiter", GridWorksContent.POWER_LIMITER_ITEM,
                " C ",
                "QBR",
                " C ")
                .setIngredient('C', Material.COPPER_INGOT)
                .setIngredient('Q', Material.COMPARATOR)
                .setIngredient('B', GridWorksContent.SMART_BREAKER_ITEM)
                .setIngredient('R', Material.REDSTONE));

        register(recipe(plugin, "fluid_valve", GridWorksContent.FLUID_VALVE_ITEM,
                "CBC",
                "RVR",
                "CGC")
                .setIngredient('C', Material.COPPER_INGOT)
                .setIngredient('B', Material.BUCKET)
                .setIngredient('R', Material.REDSTONE)
                .setIngredient('V', GridWorksContent.CONTROL_RELAY_ITEM)
                .setIngredient('G', Material.GLASS));

        register(recipe(plugin, "cargo_isolator", GridWorksContent.CARGO_ISOLATOR_ITEM,
                "CIC",
                "RVR",
                "CHC")
                .setIngredient('C', Material.COPPER_INGOT)
                .setIngredient('I', Material.IRON_BARS)
                .setIngredient('R', Material.REDSTONE)
                .setIngredient('V', GridWorksContent.CONTROL_RELAY_ITEM)
                .setIngredient('H', Material.HOPPER));
    }

    public static void unregister() {
        for (NamespacedKey key : List.copyOf(REGISTERED_KEYS)) {
            RecipeType.VANILLA_SHAPED.removeRecipe(key);
        }
        REGISTERED_KEYS.clear();
    }

    public static List<NamespacedKey> registeredKeys() {
        return List.copyOf(REGISTERED_KEYS);
    }

    public static List<String> registeredIds() {
        return REGISTERED_KEYS.stream()
                .map(NamespacedKey::getKey)
                .toList();
    }

    private static ShapedRecipe recipe(
            GridWorks plugin,
            String key,
            ItemStack result,
            String... shape
    ) {
        ShapedRecipe recipe = new ShapedRecipe(
                new NamespacedKey(plugin, key),
                result.clone()
        ).shape(shape);
        recipe.setGroup(GROUP);
        recipe.setCategory(CraftingBookCategory.REDSTONE);
        return recipe;
    }

    private static ItemStack amount(ItemStack template, int amount) {
        ItemStack result = template.clone();
        result.setAmount(amount);
        return result;
    }

    private static void register(ShapedRecipe recipe) {
        RecipeType.VANILLA_SHAPED.addRecipe(
                ShapedRebarRecipe.fromVanilla(recipe)
        );
        REGISTERED_KEYS.add(recipe.getKey());
    }
}
