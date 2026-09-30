package io.github.wickidcow.gridworks.power.nativeapi;

import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.block.interfaces.ElectricRebarBlock;
import io.github.pylonmc.rebar.electricity.ElectricNetwork;
import io.github.pylonmc.rebar.electricity.nodes.ElectricConnectorNode;
import io.github.pylonmc.rebar.electricity.nodes.ElectricPortSpec;
import io.github.pylonmc.rebar.util.position.BlockPosition;
import io.github.wickidcow.gridworks.api.power.PowerBranchSnapshot;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/** A dedicated internal edge: attached wire ratings always remain owned by Rebar. */
public final class PowerCouplerBlock extends RebarBlock implements ElectricRebarBlock {
    private static final NamespacedKey ENABLED = new NamespacedKey("gridworks", "coupler_enabled");
    private static final NamespacedKey LIMIT = new NamespacedKey("gridworks", "coupler_limit_watts");
    private BranchSettings settings;

    public PowerCouplerBlock(Block block, BlockCreateContext context) {
        super(block, context);
        settings = new BranchSettings(false, Double.MAX_VALUE);
        var a = new ElectricConnectorNode("north", new BlockPosition(block));
        var b = new ElectricConnectorNode("south", new BlockPosition(block));
        addElectricPort(new ElectricPortSpec(a, BlockFace.NORTH));
        addElectricPort(new ElectricPortSpec(b, BlockFace.SOUTH));
        apply();
    }

    public PowerCouplerBlock(Block block, PersistentDataContainer pdc) {
        super(block, pdc);
        double limit = pdc.getOrDefault(LIMIT, PersistentDataType.DOUBLE, Double.MAX_VALUE);
        // Invalid persisted settings fail closed instead of silently bypassing a limit.
        boolean valid = Double.isFinite(limit) && limit >= 0;
        settings = new BranchSettings(valid && pdc.getOrDefault(ENABLED, PersistentDataType.BYTE, (byte) 0) != 0,
                valid ? limit : 0);
    }

    @Override
    public void postInitialise() {
        super.postInitialise();
        apply();
    }

    @Override
    public void write(PersistentDataContainer pdc) {
        pdc.set(ENABLED, PersistentDataType.BYTE, settings.enabled() ? (byte) 1 : (byte) 0);
        pdc.set(LIMIT, PersistentDataType.DOUBLE, settings.limitWatts());
    }

    public PowerBranchSnapshot snapshot() {
        return new PowerBranchSnapshot(getElectricNodeOrThrow("north").getId().toString(),
                "Rebar Power Coupler", settings.enabled(), true, settings.limitWatts());
    }

    public void setEnabled(boolean enabled) {
        settings = settings.withEnabled(enabled);
        apply();
    }

    public void setLimit(double watts) {
        settings = settings.withLimit(watts);
        apply();
    }

    private void apply() {
        var a = getElectricNodeOrThrow("north");
        var b = getElectricNodeOrThrow("south");
        // Rebar build 2064 cannot safely route through a zero-capacity edge.
        // Open the owned internal connection; disconnectFrom clears only its edge data.
        // Wire connections on either port and our independently persisted limit survive.
        if (settings.effectiveWatts() == 0) {
            a.disconnectFrom(b);
            return;
        }
        if (!a.isConnectedTo(b)) a.connect(b);
        setEdgeLimit(new ElectricNetwork.Edge(a, b));
        setEdgeLimit(new ElectricNetwork.Edge(b, a));
    }

    private void setEdgeLimit(ElectricNetwork.Edge edge) {
        double effective = settings.effectiveWatts();
        if (Double.compare(edge.getPowerLimit(), effective) != 0) edge.setPowerLimit(effective);
    }
}
