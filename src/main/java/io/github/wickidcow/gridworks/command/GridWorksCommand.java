package io.github.wickidcow.gridworks.command;

import io.github.wickidcow.gridworks.GridWorks;
import io.github.wickidcow.gridworks.content.GridWorksContentCatalog;
import io.github.wickidcow.gridworks.content.GridWorksRecipes;
import java.util.List;
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
        var bus = plugin.getControlBus();
        var physical = plugin.getPhysicalControlNetwork();

        long scheduledTasks = Bukkit.getScheduler()
                .getPendingTasks()
                .stream()
                .filter(task -> task.getOwner().equals(plugin))
                .count();

        sender.sendMessage(Component.text(
                "GridWorks Doctor — " + plugin.getPluginMeta().getVersion(),
                NamedTextColor.GOLD
        ));
        line(
                sender,
                "Content",
                GridWorksRecipes.registeredIds().size()
                        + "/"
                        + GridWorksContentCatalog.ALL_IDS.size()
                        + " recipes registered"
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
                "GridWorks Doctor complete.",
                NamedTextColor.GREEN
        ));
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
