package io.github.wickidcow.gridworks;

import io.github.pylonmc.rebar.addon.RebarAddon;
import io.github.wickidcow.gridworks.api.control.ControlBus;
import io.github.wickidcow.gridworks.command.GridWorksCommand;
import io.github.wickidcow.gridworks.control.GraphControlBus;
import io.github.wickidcow.gridworks.content.GridWorksContent;
import io.github.wickidcow.gridworks.content.GridWorksRecipes;
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
import io.github.wickidcow.gridworks.power.PowerBranchDeviceManager;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginCommand;
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
    private PowerBranchDeviceManager powerBranchDeviceManager;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        try {
            enableRuntime();
        } catch (RuntimeException | Error failure) {
            getLogger().log(
                    Level.SEVERE,
                    "GridWorks failed to enable; rolling back partial runtime state",
                    failure
            );
            cleanupRuntime();
            throw failure;
        }
    }

    private void enableRuntime() {
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
        powerBranchDeviceManager = new PowerBranchDeviceManager(this);
        getServer().getPluginManager().registerEvents(powerBranchDeviceManager, this);

        GridWorksContent.register(this);

        Bukkit.getServicesManager().register(
                ControlBus.class,
                controlBus,
                this,
                ServicePriority.Normal
        );
        registerCommands();

        getLogger().info(
                "GridWorks control bus initialized (max propagation: "
                        + maxPropagationNodes
                        + " nodes)."
        );
    }

    private void registerCommands() {
        PluginCommand command = getCommand("gridworks");
        if (command == null) {
            throw new IllegalStateException(
                    "Generated plugin metadata is missing the gridworks command"
            );
        }

        GridWorksCommand executor = new GridWorksCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    @Override
    public void onDisable() {
        cleanupRuntime();
    }

    private void cleanupRuntime() {
        cleanupStep("recipes", GridWorksRecipes::unregister);
        cleanupStep(
                "Bukkit services",
                () -> Bukkit.getServicesManager().unregisterAll(this)
        );

        cleanupStep("fluid sensor manager", () -> {
            if (fluidSensorManager != null) {
                fluidSensorManager.close();
                fluidSensorManager = null;
            }
        });

        cleanupStep("inventory sensor manager", () -> {
            if (inventorySensorManager != null) {
                inventorySensorManager.close();
                inventorySensorManager = null;
            }
        });

        cleanupStep("machine sensor manager", () -> {
            if (machineSensorManager != null) {
                machineSensorManager.close();
                machineSensorManager = null;
            }
        });

        cleanupStep("power-grid sensor manager", () -> {
            if (powerGridSensorManager != null) {
                powerGridSensorManager.close();
                powerGridSensorManager = null;
            }
        });

        cleanupStep("power-branch device manager", () -> {
            if (powerBranchDeviceManager != null) {
                powerBranchDeviceManager.close();
                powerBranchDeviceManager = null;
            }
        });

        // Cancel any device-owned delayed tasks (for example alarm escalation)
        // even when startup failed before normal Bukkit disable cleanup runs.
        cleanupStep(
                "remaining scheduled tasks",
                () -> Bukkit.getScheduler().cancelTasks(this)
        );
        cleanupStep(
                "registered listeners",
                () -> HandlerList.unregisterAll(this)
        );

        powerBranchBridge = null;
        powerGridBridge = null;

        cleanupStep("physical control network", () -> {
            if (physicalControlNetwork != null) {
                physicalControlNetwork.close();
                physicalControlNetwork = null;
            }
        });

        cleanupStep("control bus", () -> {
            if (controlBus != null) {
                controlBus.clear();
                controlBus = null;
            }
        });

        instance = null;
    }

    private void cleanupStep(String label, Runnable cleanup) {
        try {
            cleanup.run();
        } catch (RuntimeException exception) {
            getLogger().log(
                    Level.SEVERE,
                    "GridWorks cleanup failed for " + label,
                    exception
            );
        }
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

    public @NotNull PowerBranchDeviceManager getPowerBranchDeviceManager() {
        if (powerBranchDeviceManager == null) {
            throw new IllegalStateException("GridWorks is not enabled");
        }
        return powerBranchDeviceManager;
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
