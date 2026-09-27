package io.github.wickidcow.gridworks.content;

import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.item.RebarItem;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.content.block.AlarmConsoleBlock;
import io.github.wickidcow.gridworks.content.block.AlarmIndicatorBlock;
import io.github.wickidcow.gridworks.content.block.AddressedRelayBlock;
import io.github.wickidcow.gridworks.content.block.ControlInterfaceBlock;
import io.github.wickidcow.gridworks.content.block.ControlRelayBlock;
import io.github.wickidcow.gridworks.content.block.CargoIsolatorBlock;
import io.github.wickidcow.gridworks.content.block.DelayRelayBlock;
import io.github.wickidcow.gridworks.content.block.FactoryControllerBlock;
import io.github.wickidcow.gridworks.content.block.FactoryMonitorBlock;
import io.github.wickidcow.gridworks.content.block.FluidSensorBlock;
import io.github.wickidcow.gridworks.content.block.FluidValveBlock;
import io.github.wickidcow.gridworks.content.block.InventorySensorBlock;
import io.github.wickidcow.gridworks.content.block.LoadSheddingControllerBlock;
import io.github.wickidcow.gridworks.content.block.MachineSensorBlock;
import io.github.wickidcow.gridworks.content.block.PulseRelayBlock;
import io.github.wickidcow.gridworks.content.block.PowerGridSensorBlock;
import io.github.wickidcow.gridworks.content.block.PowerLimiterBlock;
import io.github.wickidcow.gridworks.content.block.RedstoneSensorBlock;
import io.github.wickidcow.gridworks.content.block.SmartBreakerBlock;
import io.github.wickidcow.gridworks.content.block.StatusLightBlock;
import io.github.wickidcow.gridworks.content.item.ControlLinker;
import io.github.wickidcow.gridworks.content.listener.RedstoneSensorListener;
import io.github.wickidcow.gridworks.content.listener.StatusLightListener;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

public final class GridWorksContent {
    public static NamespacedKey CONTROL_INTERFACE;
    public static NamespacedKey ALARM_INDICATOR;
    public static NamespacedKey ALARM_CONSOLE;
    public static NamespacedKey REDSTONE_SENSOR;
    public static NamespacedKey STATUS_LIGHT;
    public static NamespacedKey CONTROL_RELAY;
    public static NamespacedKey CARGO_ISOLATOR;
    public static NamespacedKey ADDRESSED_RELAY;
    public static NamespacedKey DELAY_RELAY;
    public static NamespacedKey INVENTORY_SENSOR;
    public static NamespacedKey MACHINE_SENSOR;
    public static NamespacedKey PULSE_RELAY;
    public static NamespacedKey FLUID_SENSOR;
    public static NamespacedKey FLUID_VALVE;
    public static NamespacedKey POWER_GRID_SENSOR;
    public static NamespacedKey POWER_LIMITER;
    public static NamespacedKey FACTORY_CONTROLLER;
    public static NamespacedKey LOAD_SHEDDING_CONTROLLER;
    public static NamespacedKey SMART_BREAKER;
    public static NamespacedKey FACTORY_MONITOR;
    public static NamespacedKey GRIDWORKS_LINKER;

    public static ItemStack CONTROL_INTERFACE_ITEM;
    public static ItemStack ALARM_INDICATOR_ITEM;
    public static ItemStack ALARM_CONSOLE_ITEM;
    public static ItemStack REDSTONE_SENSOR_ITEM;
    public static ItemStack STATUS_LIGHT_ITEM;
    public static ItemStack CONTROL_RELAY_ITEM;
    public static ItemStack CARGO_ISOLATOR_ITEM;
    public static ItemStack ADDRESSED_RELAY_ITEM;
    public static ItemStack DELAY_RELAY_ITEM;
    public static ItemStack INVENTORY_SENSOR_ITEM;
    public static ItemStack MACHINE_SENSOR_ITEM;
    public static ItemStack PULSE_RELAY_ITEM;
    public static ItemStack FLUID_SENSOR_ITEM;
    public static ItemStack FLUID_VALVE_ITEM;
    public static ItemStack POWER_GRID_SENSOR_ITEM;
    public static ItemStack POWER_LIMITER_ITEM;
    public static ItemStack FACTORY_CONTROLLER_ITEM;
    public static ItemStack LOAD_SHEDDING_CONTROLLER_ITEM;
    public static ItemStack SMART_BREAKER_ITEM;
    public static ItemStack FACTORY_MONITOR_ITEM;
    public static ItemStack GRIDWORKS_LINKER_ITEM;

    private GridWorksContent() {
        throw new AssertionError("Utility class");
    }

    public static void register(GridWorks plugin) {
        CONTROL_INTERFACE = new NamespacedKey(plugin, "control_interface");
        ALARM_INDICATOR = new NamespacedKey(plugin, "alarm_indicator");
        ALARM_CONSOLE = new NamespacedKey(plugin, "alarm_console");
        REDSTONE_SENSOR = new NamespacedKey(plugin, "redstone_sensor");
        STATUS_LIGHT = new NamespacedKey(plugin, "status_light");
        CONTROL_RELAY = new NamespacedKey(plugin, "control_relay");
        CARGO_ISOLATOR = new NamespacedKey(plugin, "cargo_isolator");
        ADDRESSED_RELAY = new NamespacedKey(plugin, "addressed_relay");
        DELAY_RELAY = new NamespacedKey(plugin, "delay_relay");
        INVENTORY_SENSOR = new NamespacedKey(plugin, "inventory_sensor");
        MACHINE_SENSOR = new NamespacedKey(plugin, "machine_sensor");
        PULSE_RELAY = new NamespacedKey(plugin, "pulse_relay");
        FLUID_SENSOR = new NamespacedKey(plugin, "fluid_sensor");
        FLUID_VALVE = new NamespacedKey(plugin, "fluid_valve");
        POWER_GRID_SENSOR = new NamespacedKey(plugin, "power_grid_sensor");
        POWER_LIMITER = new NamespacedKey(plugin, "power_limiter");
        FACTORY_CONTROLLER = new NamespacedKey(plugin, "factory_controller");
        LOAD_SHEDDING_CONTROLLER = new NamespacedKey(plugin, "load_shedding_controller");
        SMART_BREAKER = new NamespacedKey(plugin, "smart_breaker");
        FACTORY_MONITOR = new NamespacedKey(plugin, "factory_monitor");
        GRIDWORKS_LINKER = new NamespacedKey(plugin, "gridworks_linker");

        RebarBlock.register(CONTROL_INTERFACE, Material.LODESTONE, ControlInterfaceBlock.class);
        RebarBlock.register(ALARM_INDICATOR, Material.REDSTONE_LAMP, AlarmIndicatorBlock.class);
        RebarBlock.register(ALARM_CONSOLE, Material.POLISHED_BLACKSTONE_BRICKS, AlarmConsoleBlock.class);
        RebarBlock.register(REDSTONE_SENSOR, Material.REDSTONE_LAMP, RedstoneSensorBlock.class);
        RebarBlock.register(STATUS_LIGHT, Material.REDSTONE_LAMP, StatusLightBlock.class);
        RebarBlock.register(CONTROL_RELAY, Material.LEVER, ControlRelayBlock.class);
        RebarBlock.register(CARGO_ISOLATOR, Material.STONECUTTER, CargoIsolatorBlock.class);
        RebarBlock.register(ADDRESSED_RELAY, Material.LEVER, AddressedRelayBlock.class);
        RebarBlock.register(DELAY_RELAY, Material.LEVER, DelayRelayBlock.class);
        RebarBlock.register(INVENTORY_SENSOR, Material.CYAN_GLAZED_TERRACOTTA, InventorySensorBlock.class);
        RebarBlock.register(
                MACHINE_SENSOR,
                Material.YELLOW_GLAZED_TERRACOTTA,
                MachineSensorBlock.class
        );
        RebarBlock.register(PULSE_RELAY, Material.LEVER, PulseRelayBlock.class);
        RebarBlock.register(FLUID_SENSOR, Material.LIGHT_BLUE_GLAZED_TERRACOTTA, FluidSensorBlock.class);
        RebarBlock.register(FLUID_VALVE, Material.END_ROD, FluidValveBlock.class);
        RebarBlock.register(POWER_GRID_SENSOR, Material.LIGHTNING_ROD, PowerGridSensorBlock.class);
        RebarBlock.register(POWER_LIMITER, Material.COMPARATOR, PowerLimiterBlock.class);
        RebarBlock.register(FACTORY_CONTROLLER, Material.CHISELED_COPPER, FactoryControllerBlock.class);
        RebarBlock.register(
                LOAD_SHEDDING_CONTROLLER,
                Material.POLISHED_DEEPSLATE,
                LoadSheddingControllerBlock.class
        );
        RebarBlock.register(SMART_BREAKER, Material.END_ROD, SmartBreakerBlock.class);
        RebarBlock.register(FACTORY_MONITOR, Material.TINTED_GLASS, FactoryMonitorBlock.class);

        CONTROL_INTERFACE_ITEM = ItemStackBuilder.rebar(Material.LODESTONE, CONTROL_INTERFACE).build();
        ALARM_INDICATOR_ITEM = ItemStackBuilder.rebar(Material.REDSTONE_LAMP, ALARM_INDICATOR).build();
        ALARM_CONSOLE_ITEM = ItemStackBuilder.rebar(Material.POLISHED_BLACKSTONE_BRICKS, ALARM_CONSOLE).build();
        REDSTONE_SENSOR_ITEM = ItemStackBuilder.rebar(Material.REDSTONE_LAMP, REDSTONE_SENSOR).build();
        STATUS_LIGHT_ITEM = ItemStackBuilder.rebar(Material.REDSTONE_LAMP, STATUS_LIGHT).build();
        CONTROL_RELAY_ITEM = ItemStackBuilder.rebar(Material.LEVER, CONTROL_RELAY).build();
        CARGO_ISOLATOR_ITEM = ItemStackBuilder.rebar(Material.STONECUTTER, CARGO_ISOLATOR).build();
        ADDRESSED_RELAY_ITEM = ItemStackBuilder.rebar(Material.LEVER, ADDRESSED_RELAY).build();
        DELAY_RELAY_ITEM = ItemStackBuilder.rebar(Material.LEVER, DELAY_RELAY).build();
        INVENTORY_SENSOR_ITEM = ItemStackBuilder.rebar(Material.CYAN_GLAZED_TERRACOTTA, INVENTORY_SENSOR).build();
        MACHINE_SENSOR_ITEM = ItemStackBuilder.rebar(
                Material.YELLOW_GLAZED_TERRACOTTA,
                MACHINE_SENSOR
        ).build();
        PULSE_RELAY_ITEM = ItemStackBuilder.rebar(Material.LEVER, PULSE_RELAY).build();
        FLUID_SENSOR_ITEM = ItemStackBuilder.rebar(Material.LIGHT_BLUE_GLAZED_TERRACOTTA, FLUID_SENSOR).build();
        FLUID_VALVE_ITEM = ItemStackBuilder.rebar(Material.END_ROD, FLUID_VALVE).build();
        POWER_GRID_SENSOR_ITEM = ItemStackBuilder.rebar(Material.LIGHTNING_ROD, POWER_GRID_SENSOR).build();
        POWER_LIMITER_ITEM = ItemStackBuilder.rebar(Material.COMPARATOR, POWER_LIMITER).build();
        FACTORY_CONTROLLER_ITEM = ItemStackBuilder.rebar(Material.CHISELED_COPPER, FACTORY_CONTROLLER).build();
        LOAD_SHEDDING_CONTROLLER_ITEM = ItemStackBuilder.rebar(
                Material.POLISHED_DEEPSLATE,
                LOAD_SHEDDING_CONTROLLER
        ).build();
        SMART_BREAKER_ITEM = ItemStackBuilder.rebar(
                Material.END_ROD,
                SMART_BREAKER
        ).build();
        FACTORY_MONITOR_ITEM = ItemStackBuilder.rebar(Material.TINTED_GLASS, FACTORY_MONITOR).build();
        GRIDWORKS_LINKER_ITEM = ItemStackBuilder.rebar(Material.RECOVERY_COMPASS, GRIDWORKS_LINKER).build();

        RebarItem.register(RebarItem.class, CONTROL_INTERFACE_ITEM, CONTROL_INTERFACE);
        RebarItem.register(RebarItem.class, ALARM_INDICATOR_ITEM, ALARM_INDICATOR);
        RebarItem.register(RebarItem.class, ALARM_CONSOLE_ITEM, ALARM_CONSOLE);
        RebarItem.register(RebarItem.class, REDSTONE_SENSOR_ITEM, REDSTONE_SENSOR);
        RebarItem.register(RebarItem.class, STATUS_LIGHT_ITEM, STATUS_LIGHT);
        RebarItem.register(RebarItem.class, CONTROL_RELAY_ITEM, CONTROL_RELAY);
        RebarItem.register(RebarItem.class, CARGO_ISOLATOR_ITEM, CARGO_ISOLATOR);
        RebarItem.register(RebarItem.class, ADDRESSED_RELAY_ITEM, ADDRESSED_RELAY);
        RebarItem.register(RebarItem.class, DELAY_RELAY_ITEM, DELAY_RELAY);
        RebarItem.register(RebarItem.class, INVENTORY_SENSOR_ITEM, INVENTORY_SENSOR);
        RebarItem.register(RebarItem.class, MACHINE_SENSOR_ITEM, MACHINE_SENSOR);
        RebarItem.register(RebarItem.class, PULSE_RELAY_ITEM, PULSE_RELAY);
        RebarItem.register(RebarItem.class, FLUID_SENSOR_ITEM, FLUID_SENSOR);
        RebarItem.register(RebarItem.class, FLUID_VALVE_ITEM, FLUID_VALVE);
        RebarItem.register(RebarItem.class, POWER_GRID_SENSOR_ITEM, POWER_GRID_SENSOR);
        RebarItem.register(RebarItem.class, POWER_LIMITER_ITEM, POWER_LIMITER);
        RebarItem.register(RebarItem.class, FACTORY_CONTROLLER_ITEM, FACTORY_CONTROLLER);
        RebarItem.register(
                RebarItem.class,
                LOAD_SHEDDING_CONTROLLER_ITEM,
                LOAD_SHEDDING_CONTROLLER
        );
        RebarItem.register(RebarItem.class, SMART_BREAKER_ITEM, SMART_BREAKER);
        RebarItem.register(RebarItem.class, FACTORY_MONITOR_ITEM, FACTORY_MONITOR);
        RebarItem.register(ControlLinker.class, GRIDWORKS_LINKER_ITEM);

        plugin.getServer().getPluginManager().registerEvents(new RedstoneSensorListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new StatusLightListener(), plugin);
    }
}
