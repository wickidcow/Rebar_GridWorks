package io.github.wickidcow.gridworks.command;

import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.api.control.ControlBus;
import io.github.wickidcow.gridworks.content.GridWorksContentCatalog;
import io.github.wickidcow.gridworks.content.GridWorksRecipes;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GridWorksCommand implements CommandExecutor, TabCompleter {
    private final GridWorks plugin;

    public GridWorksCommand(GridWorks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 1 && args[0].equalsIgnoreCase("doctor")) {
            sendDoctor(sender);
            return true;
        }

        sender.sendMessage(Component.text(
                "Usage: /" + label + " doctor",
                NamedTextColor.YELLOW
        ));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length != 1) {
            return List.of();
        }

        String prefix = args[0].toLowerCase(Locale.ROOT);
        return "doctor".startsWith(prefix)
                ? List.of("doctor")
                : List.of();
    }

    private void sendDoctor(CommandSender sender) {
        ControlBus bus = plugin.getControlBus();
        var physical = plugin.getPhysicalControlNetwork();

        List<String> registeredRecipeIds = GridWorksRecipes.registeredIds();
        Set<String> uniqueRecipeIds = new LinkedHashSet<>(registeredRecipeIds);
        boolean contentHealthy =
                registeredRecipeIds.size() == GridWorksContentCatalog.ALL_IDS.size()
                        && uniqueRecipeIds.equals(GridWorksContentCatalog.ALL_ID_SET);

        boolean serviceHealthy =
                Bukkit.getServicesManager().load(ControlBus.class) == bus;

        boolean sensorSchedulersHealthy =
                plugin.getInventorySensorManager().isScheduled()
                        && plugin.getFluidSensorManager().isScheduled()
                        && plugin.getMachineSensorManager().isScheduled()
                        && plugin.getPowerGridSensorManager().isScheduled();

        boolean instanceHealthy = GridWorks.getInstance() == plugin;

        long scheduledTasks = Bukkit.getScheduler()
                .getPendingTasks()
                .stream()
                .filter(task -> task.getOwner().equals(plugin))
                .count();

        boolean healthy =
                contentHealthy
                        && serviceHealthy
                        && sensorSchedulersHealthy
                        && instanceHealthy;

        sender.sendMessage(Component.text(
                "GridWorks Doctor — " + plugin.getPluginMeta().getVersion(),
                NamedTextColor.GOLD
        ));
        checkLine(
                sender,
                "Content",
                contentHealthy,
                registeredRecipeIds.size()
                        + "/"
                        + GridWorksContentCatalog.ALL_IDS.size()
                        + " recipes match catalog"
        );
        checkLine(
                sender,
                "Control Bus service",
                serviceHealthy,
                serviceHealthy
                        ? "registered service points to the live bus"
                        : "Bukkit service does not point to the live bus"
        );
        checkLine(
                sender,
                "Sensor schedulers",
                sensorSchedulersHealthy,
                sensorSchedulersHealthy
                        ? "inventory/fluid/machine/power samplers scheduled"
                        : "one or more shared sensor samplers are cancelled"
        );
        checkLine(
                sender,
                "Plugin instance",
                instanceHealthy,
                instanceHealthy
                        ? "static instance matches enabled plugin"
                        : "static instance does not match enabled plugin"
        );
        line(
                sender,
                "Control Bus",
                bus.nodeCount()
                        + " active nodes, "
                        + bus.connectionCount()
                        + " live links, "
                        + physical.persistentConnectionCount()
                        + " persisted links"
        );
        line(
                sender,
                "Sensors",
                "inventory "
                        + plugin.getInventorySensorManager().loadedSensorCount()
                        + ", fluid "
                        + plugin.getFluidSensorManager().loadedSensorCount()
                        + ", machine "
                        + plugin.getMachineSensorManager().loadedSensorCount()
                        + ", power "
                        + plugin.getPowerGridSensorManager().loadedSensorCount()
        );
        line(
                sender,
                "Power grid provider",
                availability(
                        plugin.getPowerGridBridge().isAvailable(),
                        plugin.getPowerGridBridge().status()
                )
        );
        line(
                sender,
                "Power branch provider",
                availability(
                        plugin.getPowerBranchBridge().isAvailable(),
                        plugin.getPowerBranchBridge().status()
                )
        );
        line(
                sender,
                "Branch devices",
                plugin.getPowerBranchDeviceManager().loadedDeviceCount()
                        + " loaded"
        );
        line(sender, "Plugin scheduler", scheduledTasks + " pending tasks");

        sender.sendMessage(Component.text(
                "GridWorks Doctor result: " + (healthy ? "PASS" : "FAIL"),
                healthy ? NamedTextColor.GREEN : NamedTextColor.RED
        ));
        sender.sendMessage(Component.text(
                "GridWorks Doctor complete.",
                healthy ? NamedTextColor.GREEN : NamedTextColor.RED
        ));
    }

    private static void checkLine(
            CommandSender sender,
            String label,
            boolean healthy,
            String value
    ) {
        sender.sendMessage(
                Component.text(
                                (healthy ? "[PASS] " : "[FAIL] ") + label + ": ",
                                healthy ? NamedTextColor.GREEN : NamedTextColor.RED
                        )
                        .append(Component.text(value, NamedTextColor.WHITE))
        );
    }

    private static void line(
            CommandSender sender,
            String label,
            String value
    ) {
        sender.sendMessage(
                Component.text(label + ": ", NamedTextColor.GRAY)
                        .append(Component.text(value, NamedTextColor.WHITE))
        );
    }

    private static String availability(boolean available, String status) {
        return (available ? "available" : "not registered") + " — " + status;
    }
}
