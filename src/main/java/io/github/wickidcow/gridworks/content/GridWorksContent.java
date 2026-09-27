package io.github.wickidcow.gridworks.content;

import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.item.RebarItem;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.content.block.ControlInterfaceBlock;
import io.github.wickidcow.gridworks.content.block.ControlRelayBlock;
import io.github.wickidcow.gridworks.content.block.FactoryControllerBlock;
import io.github.wickidcow.gridworks.content.block.FluidSensorBlock;
import io.github.wickidcow.gridworks.content.block.InventorySensorBlock;
import io.github.wickidcow.gridworks.content.block.RedstoneSensorBlock;
import io.github.wickidcow.gridworks.content.block.StatusLightBlock;
import io.github.wickidcow.gridworks.content.item.ControlLinker;
import io.github.wickidcow.gridworks.content.listener.RedstoneSensorListener;
import io.github.wickidcow.gridworks.content.listener.StatusLightListener;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

public final class GridWorksContent {
    public static NamespacedKey CONTROL_INTERFACE;
    public static NamespacedKey REDSTONE_SENSOR;
    public static NamespacedKey STATUS_LIGHT;
    public static NamespacedKey CONTROL_RELAY;
    public static NamespacedKey INVENTORY_SENSOR;
    public static NamespacedKey FLUID_SENSOR;
    public static NamespacedKey FACTORY_CONTROLLER;
    public static NamespacedKey GRIDWORKS_LINKER;

    public static ItemStack CONTROL_INTERFACE_ITEM;
    public static ItemStack REDSTONE_SENSOR_ITEM;
    public static ItemStack STATUS_LIGHT_ITEM;
    public static ItemStack CONTROL_RELAY_ITEM;
    public static ItemStack INVENTORY_SENSOR_ITEM;
    public static ItemStack FLUID_SENSOR_ITEM;
    public static ItemStack FACTORY_CONTROLLER_ITEM;
    public static ItemStack GRIDWORKS_LINKER_ITEM;

    private GridWorksContent() {
        throw new AssertionError("Utility class");
    }

    public static void register(GridWorks plugin) {
        CONTROL_INTERFACE = new NamespacedKey(plugin, "control_interface");
        REDSTONE_SENSOR = new NamespacedKey(plugin, "redstone_sensor");
        STATUS_LIGHT = new NamespacedKey(plugin, "status_light");
        CONTROL_RELAY = new NamespacedKey(plugin, "control_relay");
        INVENTORY_SENSOR = new NamespacedKey(plugin, "inventory_sensor");
        FLUID_SENSOR = new NamespacedKey(plugin, "fluid_sensor");
        FACTORY_CONTROLLER = new NamespacedKey(plugin, "factory_controller");
        GRIDWORKS_LINKER = new NamespacedKey(plugin, "gridworks_linker");

        RebarBlock.register(CONTROL_INTERFACE, Material.LODESTONE, ControlInterfaceBlock.class);
        RebarBlock.register(REDSTONE_SENSOR, Material.REDSTONE_LAMP, RedstoneSensorBlock.class);
        RebarBlock.register(STATUS_LIGHT, Material.REDSTONE_LAMP, StatusLightBlock.class);
        RebarBlock.register(CONTROL_RELAY, Material.LEVER, ControlRelayBlock.class);
        RebarBlock.register(INVENTORY_SENSOR, Material.CYAN_GLAZED_TERRACOTTA, InventorySensorBlock.class);
        RebarBlock.register(FLUID_SENSOR, Material.LIGHT_BLUE_GLAZED_TERRACOTTA, FluidSensorBlock.class);
        RebarBlock.register(FACTORY_CONTROLLER, Material.CHISELED_COPPER, FactoryControllerBlock.class);

        CONTROL_INTERFACE_ITEM = ItemStackBuilder.rebar(Material.LODESTONE, CONTROL_INTERFACE).build();
        REDSTONE_SENSOR_ITEM = ItemStackBuilder.rebar(Material.REDSTONE_LAMP, REDSTONE_SENSOR).build();
        STATUS_LIGHT_ITEM = ItemStackBuilder.rebar(Material.REDSTONE_LAMP, STATUS_LIGHT).build();
        CONTROL_RELAY_ITEM = ItemStackBuilder.rebar(Material.LEVER, CONTROL_RELAY).build();
        INVENTORY_SENSOR_ITEM = ItemStackBuilder.rebar(Material.CYAN_GLAZED_TERRACOTTA, INVENTORY_SENSOR).build();
        FLUID_SENSOR_ITEM = ItemStackBuilder.rebar(Material.LIGHT_BLUE_GLAZED_TERRACOTTA, FLUID_SENSOR).build();
        FACTORY_CONTROLLER_ITEM = ItemStackBuilder.rebar(Material.CHISELED_COPPER, FACTORY_CONTROLLER).build();
        GRIDWORKS_LINKER_ITEM = ItemStackBuilder.rebar(Material.RECOVERY_COMPASS, GRIDWORKS_LINKER).build();

        RebarItem.register(RebarItem.class, CONTROL_INTERFACE_ITEM, CONTROL_INTERFACE);
        RebarItem.register(RebarItem.class, REDSTONE_SENSOR_ITEM, REDSTONE_SENSOR);
        RebarItem.register(RebarItem.class, STATUS_LIGHT_ITEM, STATUS_LIGHT);
        RebarItem.register(RebarItem.class, CONTROL_RELAY_ITEM, CONTROL_RELAY);
        RebarItem.register(RebarItem.class, INVENTORY_SENSOR_ITEM, INVENTORY_SENSOR);
        RebarItem.register(RebarItem.class, FLUID_SENSOR_ITEM, FLUID_SENSOR);
        RebarItem.register(RebarItem.class, FACTORY_CONTROLLER_ITEM, FACTORY_CONTROLLER);
        RebarItem.register(ControlLinker.class, GRIDWORKS_LINKER_ITEM);

        plugin.getServer().getPluginManager().registerEvents(new RedstoneSensorListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new StatusLightListener(), plugin);
    }
}
