package io.github.wickidcow.gridworks.content;

import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.item.RebarItem;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.content.block.ControlInterfaceBlock;
import io.github.wickidcow.gridworks.content.item.ControlLinker;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

public final class GridWorksContent {
    public static NamespacedKey CONTROL_INTERFACE;
    public static NamespacedKey GRIDWORKS_LINKER;

    public static ItemStack CONTROL_INTERFACE_ITEM;
    public static ItemStack GRIDWORKS_LINKER_ITEM;

    private GridWorksContent() {
        throw new AssertionError("Utility class");
    }

    public static void register(GridWorks plugin) {
        CONTROL_INTERFACE = new NamespacedKey(plugin, "control_interface");
        GRIDWORKS_LINKER = new NamespacedKey(plugin, "gridworks_linker");

        RebarBlock.register(CONTROL_INTERFACE, Material.LODESTONE, ControlInterfaceBlock.class);

        CONTROL_INTERFACE_ITEM = ItemStackBuilder.rebar(Material.LODESTONE, CONTROL_INTERFACE).build();
        GRIDWORKS_LINKER_ITEM = ItemStackBuilder.rebar(Material.RECOVERY_COMPASS, GRIDWORKS_LINKER).build();

        RebarItem.register(RebarItem.class, CONTROL_INTERFACE_ITEM, CONTROL_INTERFACE);
        RebarItem.register(ControlLinker.class, GRIDWORKS_LINKER_ITEM);
    }
}
