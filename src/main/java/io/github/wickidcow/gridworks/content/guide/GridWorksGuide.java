package io.github.wickidcow.gridworks.content.guide;

import io.github.pylonmc.rebar.content.guide.RebarGuide;
import io.github.pylonmc.rebar.guide.button.PageButton;
import io.github.pylonmc.rebar.guide.pages.base.SimpleStaticGuidePage;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.content.GridWorksContent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

public final class GridWorksGuide {
    private static PageButton rootButton;

    private GridWorksGuide() {
        throw new AssertionError("Utility class");
    }

    public static void register(GridWorks plugin) {
        unregister();
        GridWorksGuideCatalog.validateCatalog();

        SimpleStaticGuidePage landing = new SimpleStaticGuidePage(
                new NamespacedKey(plugin, GridWorksGuideCatalog.ROOT_PAGE_KEY)
        );

        for (GridWorksGuideCatalog.Category category
                : GridWorksGuideCatalog.CATEGORIES) {
            SimpleStaticGuidePage page = new SimpleStaticGuidePage(
                    new NamespacedKey(plugin, category.key())
            );

            for (String itemId : category.itemIds()) {
                page.addItem(itemForId(itemId).clone());
            }

            landing.addPage(iconFor(category.key()), page);
        }

        rootButton = new PageButton(plugin.getMaterial(), landing);
        RebarGuide.getRootPage().addButton(rootButton);
    }

    public static void unregister() {
        if (rootButton == null) {
            return;
        }

        RebarGuide.getRootPage().getButtons().remove(rootButton);
        rootButton = null;
    }

    public static boolean isRegistered() {
        return rootButton != null
                && RebarGuide.getRootPage().getButtons().contains(rootButton);
    }

    public static int categoryCount() {
        return GridWorksGuideCatalog.CATEGORIES.size();
    }

    public static int itemCount() {
        return GridWorksGuideCatalog.CATEGORIES.stream()
                .mapToInt(category -> category.itemIds().size())
                .sum();
    }

    private static Material iconFor(String categoryKey) {
        return switch (categoryKey) {
            case "core_linking" -> Material.RECOVERY_COMPASS;
            case "sensors" -> Material.OBSERVER;
            case "logic_production" -> Material.CRAFTER;
            case "monitoring_alarms" -> Material.TINTED_GLASS;
            case "actuators_power" -> Material.LIGHTNING_ROD;
            default -> throw new IllegalArgumentException(
                    "Unknown GridWorks guide category: " + categoryKey
            );
        };
    }

    private static ItemStack itemForId(String itemId) {
        return switch (itemId) {
            case "control_interface" -> GridWorksContent.CONTROL_INTERFACE_ITEM;
            case "alarm_indicator" -> GridWorksContent.ALARM_INDICATOR_ITEM;
            case "alarm_console" -> GridWorksContent.ALARM_CONSOLE_ITEM;
            case "redstone_sensor" -> GridWorksContent.REDSTONE_SENSOR_ITEM;
            case "status_light" -> GridWorksContent.STATUS_LIGHT_ITEM;
            case "control_relay" -> GridWorksContent.CONTROL_RELAY_ITEM;
            case "cargo_isolator" -> GridWorksContent.CARGO_ISOLATOR_ITEM;
            case "addressed_relay" -> GridWorksContent.ADDRESSED_RELAY_ITEM;
            case "delay_relay" -> GridWorksContent.DELAY_RELAY_ITEM;
            case "inventory_sensor" -> GridWorksContent.INVENTORY_SENSOR_ITEM;
            case "machine_sensor" -> GridWorksContent.MACHINE_SENSOR_ITEM;
            case "pulse_relay" -> GridWorksContent.PULSE_RELAY_ITEM;
            case "fluid_sensor" -> GridWorksContent.FLUID_SENSOR_ITEM;
            case "fluid_valve" -> GridWorksContent.FLUID_VALVE_ITEM;
            case "power_grid_sensor" -> GridWorksContent.POWER_GRID_SENSOR_ITEM;
            case "power_limiter" -> GridWorksContent.POWER_LIMITER_ITEM;
            case "factory_controller" -> GridWorksContent.FACTORY_CONTROLLER_ITEM;
            case "batch_controller" -> GridWorksContent.BATCH_CONTROLLER_ITEM;
            case "sequence_controller" -> GridWorksContent.SEQUENCE_CONTROLLER_ITEM;
            case "load_shedding_controller" ->
                    GridWorksContent.LOAD_SHEDDING_CONTROLLER_ITEM;
            case "smart_breaker" -> GridWorksContent.SMART_BREAKER_ITEM;
            case "factory_monitor" -> GridWorksContent.FACTORY_MONITOR_ITEM;
            case "gridworks_linker" -> GridWorksContent.GRIDWORKS_LINKER_ITEM;
            default -> throw new IllegalArgumentException(
                    "Unknown GridWorks content id: " + itemId
            );
        };
    }
}
