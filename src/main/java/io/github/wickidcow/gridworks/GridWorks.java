package io.github.wickidcow.gridworks;

import io.github.pylonmc.rebar.addon.RebarAddon;
import io.github.wickidcow.gridworks.api.control.ControlBus;
import io.github.wickidcow.gridworks.control.GraphControlBus;
import io.github.wickidcow.gridworks.content.GridWorksContent;
import io.github.wickidcow.gridworks.fluid.FluidSensorManager;
import io.github.wickidcow.gridworks.inventory.InventorySensorManager;
import io.github.wickidcow.gridworks.machine.MachineSensorManager;
import io.github.wickidcow.gridworks.physical.PersistentConnectionStore;
import io.github.wickidcow.gridworks.physical.PhysicalControlNetwork;
import io.github.wickidcow.gridworks.power.PowerBranchBridge;
import io.github.wickidcow.gridworks.power.PowerGridBridge;
import io.github.wickidcow.gridworks.power.PowerGridSensorManager;
import io.github.wickidcow.gridworks.power.ServicePowerBranchBridge;
import io.github.wickidcow.gridworks.power.ServicePowerGridBridge;
import io.github.wickidcow.gridworks.power.SmartBreakerManager;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class GridWorks extends JavaPlugin implements RebarAddon {
    private static GridWorks instance;

    private GraphControlBus controlBus;
    private PhysicalControlNetwork physicalControlNetwork;
    private InventorySensorManager inventorySensorManager;
    private FluidSensorManager fluidSensorManager;
    private MachineSensorManager machineSensorManager;
    private PowerGridBridge powerGridBridge;
    private PowerGridSensorManager powerGridSensorManager;
    private PowerBranchBridge powerBranchBridge;
    private SmartBreakerManager smartBreakerManager;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        registerWithRebar();

        saveDefaultConfig();
        int maxPropagationNodes = Math.max(
                1,
                getConfig().getInt("control-bus.max-propagation-nodes", 4096)
        );

        controlBus = new GraphControlBus(maxPropagationNodes);

        try {
            PersistentConnectionStore connectionStore = new PersistentConnectionStore(
                    getDataFolder().toPath().resolve("control-network.txt")
            );
            physicalControlNetwork = new PhysicalControlNetwork(
                    controlBus,
                    connectionStore,
                    exception -> getLogger().log(
                            Level.SEVERE,
                            "A GridWorks physical-node availability callback failed",
                            exception
                    )
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load GridWorks control-network data", exception);
        }

        long inventorySampleInterval = Math.max(
                1L,
                getConfig().getLong("sensors.inventory.sample-interval-ticks", 10L)
        );
        inventorySensorManager = new InventorySensorManager(this, inventorySampleInterval);

        long fluidSampleInterval = Math.max(
                1L,
                getConfig().getLong("sensors.fluid.sample-interval-ticks", 10L)
        );
        fluidSensorManager = new FluidSensorManager(this, fluidSampleInterval);

        long machineSampleInterval = Math.max(
                1L,
                getConfig().getLong("sensors.machine.sample-interval-ticks", 20L)
        );
        machineSensorManager = new MachineSensorManager(
                this,
                machineSampleInterval
        );

        // Resolve power data through Bukkit services. Released Rebar does not
        // yet provide electricity, but third-party addons and the future native
        // Rebar adapter can register PowerGridProvider without changing core.
        powerGridBridge = new ServicePowerGridBridge(
                Bukkit.getServicesManager(),
                "No PowerGridProvider is registered"
        );

        long powerSampleInterval = Math.max(
                1L,
                getConfig().getLong("sensors.power.sample-interval-ticks", 20L)
        );
        powerGridSensorManager = new PowerGridSensorManager(
                this,
                powerSampleInterval
        );

        powerBranchBridge = new ServicePowerBranchBridge(
                Bukkit.getServicesManager(),
                "No PowerBranchProvider is registered"
        );
        smartBreakerManager = new SmartBreakerManager(this);
        getServer().getPluginManager().registerEvents(smartBreakerManager, this);

        GridWorksContent.register(this);

        Bukkit.getServicesManager().register(ControlBus.class, controlBus, this, ServicePriority.Normal);
        getLogger().info("GridWorks control bus initialized (max propagation: " + maxPropagationNodes + " nodes).");
    }

    @Override
    public void onDisable() {
        Bukkit.getServicesManager().unregisterAll(this);

        if (fluidSensorManager != null) {
            fluidSensorManager.close();
            fluidSensorManager = null;
        }

        if (inventorySensorManager != null) {
            inventorySensorManager.close();
            inventorySensorManager = null;
        }

        if (machineSensorManager != null) {
            machineSensorManager.close();
            machineSensorManager = null;
        }

        if (powerGridSensorManager != null) {
            powerGridSensorManager.close();
            powerGridSensorManager = null;
        }

        if (smartBreakerManager != null) {
            smartBreakerManager.close();
            smartBreakerManager = null;
        }

        powerBranchBridge = null;
        powerGridBridge = null;

        if (physicalControlNetwork != null) {
            physicalControlNetwork.close();
            physicalControlNetwork = null;
        }

        if (controlBus != null) {
            controlBus.clear();
            controlBus = null;
        }

        instance = null;
    }

    public static @NotNull GridWorks getInstance() {
        GridWorks current = instance;
        if (current == null) {
            throw new IllegalStateException("GridWorks is not loaded");
        }
        return current;
    }

    public @NotNull ControlBus getControlBus() {
        if (controlBus == null) {
            throw new IllegalStateException("GridWorks is not enabled");
        }
        return controlBus;
    }

    public @NotNull PhysicalControlNetwork getPhysicalControlNetwork() {
        if (physicalControlNetwork == null) {
            throw new IllegalStateException("GridWorks is not enabled");
        }
        return physicalControlNetwork;
    }

    public @NotNull InventorySensorManager getInventorySensorManager() {
        if (inventorySensorManager == null) {
            throw new IllegalStateException("GridWorks is not enabled");
        }
        return inventorySensorManager;
    }

    public @NotNull MachineSensorManager getMachineSensorManager() {
        if (machineSensorManager == null) {
            throw new IllegalStateException("GridWorks is not enabled");
        }
        return machineSensorManager;
    }

    public @NotNull SmartBreakerManager getSmartBreakerManager() {
        if (smartBreakerManager == null) {
            throw new IllegalStateException("GridWorks is not enabled");
        }
        return smartBreakerManager;
    }

    public @NotNull PowerBranchBridge getPowerBranchBridge() {
        if (powerBranchBridge == null) {
            throw new IllegalStateException("GridWorks is not enabled");
        }
        return powerBranchBridge;
    }

    public @NotNull PowerGridSensorManager getPowerGridSensorManager() {
        if (powerGridSensorManager == null) {
            throw new IllegalStateException("GridWorks is not enabled");
        }
        return powerGridSensorManager;
    }

    public @NotNull PowerGridBridge getPowerGridBridge() {
        if (powerGridBridge == null) {
            throw new IllegalStateException("GridWorks is not enabled");
        }
        return powerGridBridge;
    }

    public @NotNull FluidSensorManager getFluidSensorManager() {
        if (fluidSensorManager == null) {
            throw new IllegalStateException("GridWorks is not enabled");
        }
        return fluidSensorManager;
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
