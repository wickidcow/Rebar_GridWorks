package io.github.wickidcow.gridworks.power;

import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.api.power.PowerBranchProvider;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public final class PowerBranchDeviceManager implements Listener, AutoCloseable {
    private final GridWorks plugin;
    private final Set<PowerBranchDevice> devices =
            Collections.newSetFromMap(new IdentityHashMap<>());

    public PowerBranchDeviceManager(GridWorks plugin) {
        this.plugin = plugin;
    }

    public void register(PowerBranchDevice device) {
        devices.add(device);
        device.reconcileBranch();
    }

    public void unregister(PowerBranchDevice device) {
        devices.remove(device);
    }

    public int loadedDeviceCount() {
        return devices.size();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServiceRegister(ServiceRegisterEvent event) {
        if (event.getProvider().getService() != PowerBranchProvider.class) {
            return;
        }
        reconcileNextTick();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServiceUnregister(ServiceUnregisterEvent event) {
        if (event.getProvider().getService() != PowerBranchProvider.class) {
            return;
        }
        reconcileNextTick();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        reconcileTargeting(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        markTargetUnavailable(event.getChunk());
    }

    private void reconcileNextTick() {
        plugin.getServer().getScheduler().runTask(plugin, this::reconcileAll);
    }

    private void reconcileAll() {
        for (PowerBranchDevice device : List.copyOf(devices)) {
            device.reconcileBranch();
        }
    }

    private void reconcileTargeting(Chunk chunk) {
        UUID worldId = chunk.getWorld().getUID();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        for (PowerBranchDevice device : List.copyOf(devices)) {
            if (device.targetsChunk(worldId, chunkX, chunkZ)) {
                device.reconcileBranch();
            }
        }
    }

    private void markTargetUnavailable(Chunk chunk) {
        UUID worldId = chunk.getWorld().getUID();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        for (PowerBranchDevice device : List.copyOf(devices)) {
            if (device.targetsChunk(worldId, chunkX, chunkZ)) {
                device.markTargetUnavailable("Target chunk unloaded");
            }
        }
    }

    @Override
    public void close() {
        devices.clear();
    }
}
