package io.github.wickidcow.gridworks.inventory;

import io.github.pylonmc.rebar.block.BlockStorage;
import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.block.interfaces.LogisticRebarBlock;
import io.github.pylonmc.rebar.block.interfaces.NoVanillaInventoryRebarBlock;
import io.github.pylonmc.rebar.block.interfaces.VirtualInventoryRebarBlock;
import io.github.pylonmc.rebar.logistics.LogisticGroup;
import io.github.pylonmc.rebar.logistics.slot.LogisticSlot;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.inventory.VirtualInventory;

public final class InventoryProbe {
    private InventoryProbe() {
        throw new AssertionError("Utility class");
    }

    public static InventorySnapshot snapshot(Block block) {
        RebarBlock rebarBlock = BlockStorage.get(block);

        if (rebarBlock instanceof VirtualInventoryRebarBlock virtualInventoryBlock) {
            InventorySnapshot snapshot = snapshotVirtualInventories(
                    virtualInventoryBlock.getVirtualInventories()
            );
            if (snapshot.totalSlots() > 0) {
                return snapshot;
            }
        }

        if (rebarBlock instanceof LogisticRebarBlock logisticBlock) {
            InventorySnapshot snapshot = snapshotLogisticGroups(logisticBlock.getLogisticGroups());
            if (snapshot.totalSlots() > 0) {
                return snapshot;
            }
        }

        if (rebarBlock instanceof NoVanillaInventoryRebarBlock) {
            return InventorySnapshot.unavailable();
        }

        BlockState state = block.getState(false);
        if (!(state instanceof InventoryHolder holder)) {
            return InventorySnapshot.unavailable();
        }

        return snapshotBukkitInventory(holder.getInventory());
    }

    private static InventorySnapshot snapshotVirtualInventories(
            Map<String, VirtualInventory> inventories
    ) {
        Set<VirtualInventory> unique = Collections.newSetFromMap(new IdentityHashMap<>());
        unique.addAll(inventories.values());

        long items = 0;
        int occupiedSlots = 0;
        int totalSlots = 0;

        for (VirtualInventory inventory : unique) {
            ItemStack[] stacks = inventory.getUnsafeItems();
            totalSlots += inventory.getSize();

            for (ItemStack stack : stacks) {
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                occupiedSlots++;
                items += stack.getAmount();
            }
        }

        return new InventorySnapshot(true, items, occupiedSlots, totalSlots);
    }

    private static InventorySnapshot snapshotLogisticGroups(
            Map<String, LogisticGroup> groups
    ) {
        Set<LogisticSlot> unique = Collections.newSetFromMap(new IdentityHashMap<>());
        for (LogisticGroup group : groups.values()) {
            unique.addAll(group.getSlots());
        }

        long items = 0;
        int occupiedSlots = 0;

        for (LogisticSlot slot : unique) {
            long amount = Math.max(0L, slot.getAmount());
            if (amount <= 0L) {
                continue;
            }

            occupiedSlots++;
            if (Long.MAX_VALUE - items < amount) {
                items = Long.MAX_VALUE;
            } else {
                items += amount;
            }
        }

        return new InventorySnapshot(true, items, occupiedSlots, unique.size());
    }

    private static InventorySnapshot snapshotBukkitInventory(Inventory inventory) {
        long items = 0;
        int occupiedSlots = 0;
        ItemStack[] contents = inventory.getStorageContents();

        for (ItemStack stack : contents) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            occupiedSlots++;
            items += stack.getAmount();
        }

        return new InventorySnapshot(true, items, occupiedSlots, contents.length);
    }
}
