package io.github.wickidcow.gridworks.content.listener;

import io.github.pylonmc.rebar.block.BlockStorage;
import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.wickidcow.gridworks.content.block.RedstoneSensorBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;

public final class RedstoneSensorListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRedstoneChanged(BlockRedstoneEvent event) {
        RebarBlock rebarBlock = BlockStorage.get(event.getBlock());
        if (rebarBlock instanceof RedstoneSensorBlock sensor) {
            sensor.updatePower(event.getNewCurrent());
        }
    }
}
