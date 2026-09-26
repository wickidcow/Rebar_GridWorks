package io.github.wickidcow.gridworks.content.listener;

import io.github.pylonmc.rebar.block.BlockStorage;
import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.wickidcow.gridworks.content.block.StatusLightBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;

public final class StatusLightListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    public void keepStatusLightUnderControlBusOwnership(BlockRedstoneEvent event) {
        RebarBlock rebarBlock = BlockStorage.get(event.getBlock());
        if (rebarBlock instanceof StatusLightBlock statusLight) {
            event.setNewCurrent(statusLight.isLit() ? 15 : 0);
        }
    }
}
