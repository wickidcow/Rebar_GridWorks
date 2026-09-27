package io.github.wickidcow.gridworks.power;

import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.api.power.PowerBranchProvider;
import io.github.wickidcow.gridworks.content.block.SmartBreakerBlock;
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

public final class SmartBreakerManager implements Listener, AutoCloseable {
    private final GridWorks plugin;
    private final Set<SmartBreakerBlock> breakers =
            Collections.newSetFromMap(new IdentityHashMap<>());

    public SmartBreakerManager(GridWorks plugin) {
        this.plugin = plugin;
    }

    public void register(SmartBreakerBlock breaker) {
        breakers.add(breaker);
        breaker.reconcileBranch();
    }

    public void unregister(SmartBreakerBlock breaker) {
        breakers.remove(breaker);
    }

    public int loadedBreakerCount() {
        return breakers.size();
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
        for (SmartBreakerBlock breaker : List.copyOf(breakers)) {
            breaker.reconcileBranch();
        }
    }

    private void reconcileTargeting(Chunk chunk) {
        UUID worldId = chunk.getWorld().getUID();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        for (SmartBreakerBlock breaker : List.copyOf(breakers)) {
            if (breaker.targetsChunk(worldId, chunkX, chunkZ)) {
                breaker.reconcileBranch();
            }
        }
    }

    private void markTargetUnavailable(Chunk chunk) {
        UUID worldId = chunk.getWorld().getUID();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        for (SmartBreakerBlock breaker : List.copyOf(breakers)) {
            if (breaker.targetsChunk(worldId, chunkX, chunkZ)) {
                breaker.markTargetUnavailable("Target chunk unloaded");
            }
        }
    }

    @Override
    public void close() {
        breakers.clear();
    }
}
