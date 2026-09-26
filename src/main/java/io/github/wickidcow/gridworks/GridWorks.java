package io.github.wickidcow.gridworks;

import io.github.pylonmc.rebar.addon.RebarAddon;
import io.github.wickidcow.gridworks.api.control.ControlBus;
import io.github.wickidcow.gridworks.control.GraphControlBus;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class GridWorks extends JavaPlugin implements RebarAddon {
    private GraphControlBus controlBus;

    @Override
    public void onEnable() {
        registerWithRebar();

        saveDefaultConfig();
        int maxPropagationNodes = Math.max(
                1,
                getConfig().getInt("control-bus.max-propagation-nodes", 4096)
        );

        controlBus = new GraphControlBus(maxPropagationNodes);
        Bukkit.getServicesManager().register(ControlBus.class, controlBus, this, ServicePriority.Normal);

        getLogger().info("GridWorks control bus initialized (max propagation: " + maxPropagationNodes + " nodes).");
    }

    @Override
    public void onDisable() {
        Bukkit.getServicesManager().unregisterAll(this);
        if (controlBus != null) {
            controlBus.clear();
        }
    }

    public @NotNull ControlBus getControlBus() {
        if (controlBus == null) {
            throw new IllegalStateException("GridWorks is not enabled");
        }
        return controlBus;
    }

    @Override
    public @NotNull JavaPlugin getJavaPlugin() {
        return this;
    }

    @Override
    public @NotNull Material getMaterial() {
        return Material.COMPARATOR;
    }

    @Override
    public @NotNull Locale getDefaultLanguage() {
        return Locale.ENGLISH;
    }
}
