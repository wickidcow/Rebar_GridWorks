# Rebar GridWorks

**Industrial automation, smart power management, and factory control systems for Pylon/Rebar.**

GridWorks is a Rebar addon focused on making factories smarter rather than simply making machines faster. Its long-term goal is to connect sensors, controllers, power systems, cargo, fluids, machines, and redstone through a common automation layer.

> GridWorks is in early development. The Control Bus, persistent physical network, redstone I/O, and first inventory/fluid sensors are implemented.

## Current systems

The Control Bus provides namespaced typed signals, cycle-safe graph propagation, a configurable safety cap, receiver isolation, and a Bukkit service API for other addons.

The physical layer provides persistent node UUIDs, atomic link persistence, a GridWorks Linker, chunk-safe live topology, peer-availability callbacks, and reconnection without world scans or forced chunk loads.

### Redstone automation

```text
                     +--> Status Light
Redstone Sensor -----|
                     +--> Control Relay --> vanilla redstone
```

The Redstone Sensor is event-driven and publishes analog strength plus boolean powered state. The Status Light displays the boolean state, while the Control Relay produces real vanilla redstone output.

### Inventory sensing

The Inventory Sensor faces an adjacent inventory and publishes availability, total item count, occupied slots, total slots, and occupied-slot ratio. It understands Rebar virtual/logistic inventories before falling back to ordinary Bukkit container inventories.

There is no universal inventory-change event across every supported inventory type, so all loaded Inventory Sensors share one configurable sampler and publish only when their snapshot changes.

### Fluid sensing

The Fluid Tank Sensor faces an adjacent block implementing Rebar's released `FluidTankRebarBlock` API and publishes:

- `gridworks:fluid/available`
- `gridworks:fluid/present`
- `gridworks:fluid/type`
- `gridworks:fluid/amount`
- `gridworks:fluid/capacity`
- `gridworks:fluid/fill_ratio`

It samples loaded sensor targets only and never loads another chunk. The first implementation intentionally targets Rebar fluid tanks rather than using reflection to inspect arbitrary internal multi-fluid buffers that Rebar does not publicly enumerate.

## Logic foundation

GridWorks now has a tested numeric-rule API for controller logic. A rule consists of an input channel, comparison operator, and finite threshold. Rules ignore unrelated channels and non-numeric values rather than coercing data unexpectedly.

Controllers will publish their boolean result on `gridworks:control/enabled`. The existing Status Light and Control Relay already accept that channel in addition to direct Redstone Sensor input, so controller logic does not need to masquerade as redstone input.

## Factory Controller

The Factory Controller is a configurable logic block. It now supports **Condition A** plus an optional **Condition B**, combined with AND or OR, and publishes its result on `gridworks:control/enabled`.

Its Rebar/InvUI screen configures:

- metric, comparison, threshold, and source for Condition A;
- optional Condition B with its own metric, comparison, threshold, and source;
- AND/OR combination;
- current output state.

Existing placed controllers remain compatible: their old single rule loads as Condition A and Condition B starts disabled.

Each condition can use **AUTO** source binding or explicitly cycle through directly linked, currently loaded nodes. AUTO still binds only after receiving the configured metric from a directly linked source, so unrelated sensors elsewhere in the same connected Control Bus cannot take ownership.

The logic uses safe three-state evaluation. Known results can short-circuit (`false AND ? = false`, `true OR ? = true`). If the result genuinely cannot be known yet, the controller displays `WAITING` and sends an OFF fail-safe command so actuators do not remain stuck in an old ON state.

Example:

```text
Inventory Sensor ----\
                      > Factory Controller --AND--> Control Relay
Fluid Tank Sensor ---/       A: items >= 64
                              B: fluid fill >= 25%
```

## Planned systems

The next controller work is player-defined rule names, more than two conditions, and reusable controller presets. After that, GridWorks can connect inventory/fluid/redstone measurements to relays and eventually to smart electricity controls as Rebar's electricity API stabilizes.

## Requirements

- Paper 26.2
- Java 25
- Rebar 0.43.0-26.2

Pylon is the primary gameplay ecosystem GridWorks is being designed to complement, but the foundation depends only on Rebar.

## Building

```bash
./gradlew build
```

The plugin jar is written to `build/libs/`.

## License

GridWorks is licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE).
