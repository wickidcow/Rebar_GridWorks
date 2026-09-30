package io.github.wickidcow.gridworks.power.nativeapi;

import io.github.pylonmc.rebar.block.BlockStorage;
import io.github.pylonmc.rebar.block.interfaces.ElectricRebarBlock;
import io.github.pylonmc.rebar.electricity.ElectricNetwork;
import io.github.pylonmc.rebar.electricity.nodes.ElectricConsumerNode;
import io.github.pylonmc.rebar.electricity.nodes.ElectricProducerNode;
import io.github.wickidcow.gridworks.api.power.*;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

/** Public Rebar electricity API only. Snapshots are shared across sensors for one server tick. */
public final class RebarPowerProvider implements PowerGridProvider, PowerBranchProvider {
    private final Map<ElectricNetwork, PowerGridSnapshot> cache = new IdentityHashMap<>();
    private int sampledTick = -1;

    @Override public String providerId() { return "Rebar native electricity"; }

    @Override
    public Optional<PowerGridSnapshot> snapshotFor(Block block) {
        requireServerThread();
        if (!loaded(block) || !(BlockStorage.get(block) instanceof ElectricRebarBlock electric)) {
            return Optional.empty();
        }
        int tick = Bukkit.getCurrentTick();
        if (tick != sampledTick) { cache.clear(); sampledTick = tick; }
        ElectricNetwork network = null;
        for (var node : electric.getElectricNodes()) {
            var candidate = node.getNetwork();
            // A block may expose isolated grids. Never merge their readings into an invented grid.
            if (network != null && network != candidate) return Optional.empty();
            network = candidate;
        }
        return network == null ? Optional.empty() : Optional.of(cache.computeIfAbsent(network, this::sample));
    }

    private PowerGridSnapshot sample(ElectricNetwork network) {
        int producers = 0, consumers = 0, powered = 0;
        double supply = 0, demand = 0;
        var nodes = network.getNodes();
        for (var node : nodes) {
            if (node instanceof ElectricProducerNode producer) {
                producers++;
                supply = addPower(supply, producer.getPower());
            } else if (node instanceof ElectricConsumerNode consumer) {
                consumers++;
                demand = addPower(demand, consumer.getRequiredPower());
                if (consumer.isPowered()) powered++;
            }
        }
        return new PowerGridSnapshot(nodes.size(), producers, consumers, powered, supply, demand);
    }

    private static double addPower(double total, double value) {
        if (!Double.isFinite(value) || value < 0) throw new IllegalStateException("Rebar node reports invalid power");
        return total > Double.MAX_VALUE - value ? Double.MAX_VALUE : total + value;
    }

    @Override public Optional<PowerBranchSnapshot> snapshotFor(Block block, BlockFace side) {
        return coupler(block).map(PowerCouplerBlock::snapshot);
    }

    @Override public PowerBranchControlResult setEnabled(Block block, BlockFace side, boolean enabled) {
        var target = coupler(block);
        if (target.isEmpty()) return PowerBranchControlResult.targetNotFound("Aim at a GridWorks Power Coupler");
        target.get().setEnabled(enabled);
        cache.clear();
        return PowerBranchControlResult.applied(enabled ? "Coupler closed" : "Coupler open");
    }

    @Override public PowerBranchControlResult setPowerLimitWatts(Block block, BlockFace side, double watts) {
        if (!Double.isFinite(watts) || watts < 0) return PowerBranchControlResult.rejected("Invalid watt limit");
        var target = coupler(block);
        if (target.isEmpty()) return PowerBranchControlResult.targetNotFound("Aim at a GridWorks Power Coupler");
        target.get().setLimit(watts);
        cache.clear();
        return PowerBranchControlResult.applied("Coupler limit updated");
    }

    private Optional<PowerCouplerBlock> coupler(Block block) {
        requireServerThread();
        return loaded(block) && BlockStorage.get(block) instanceof PowerCouplerBlock coupler
                ? Optional.of(coupler) : Optional.empty();
    }

    private static boolean loaded(Block block) {
        return block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4);
    }

    private static void requireServerThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Rebar power access requires the server thread");
    }
}
