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

## Planned systems

The next major milestone is configurable logic: threshold/comparison rules and a survival-friendly Factory Controller. After that, GridWorks can connect inventory/fluid/redstone measurements to relays and eventually to smart electricity controls as Rebar's electricity API stabilizes.

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
