# Rebar GridWorks

**Industrial automation, smart power management, and factory control systems for Pylon/Rebar.**

GridWorks is a Rebar addon focused on making factories smarter rather than simply making machines faster. Its long-term goal is to connect sensors, controllers, power systems, cargo, fluids, machines, and redstone through a common automation layer.

> GridWorks is in early development. The Control Bus, persistent physical network, and first end-to-end redstone automation devices are implemented.

## Current foundation

The Control Bus provides namespaced and typed signals, cycle-safe graph propagation, a configurable safety cap, receiver isolation, and a Bukkit service API for other addons.

The physical layer provides persistent node UUIDs, atomic link persistence, a GridWorks Linker, chunk-safe live topology, and reconnection without world scans or forced chunk loads.

## Redstone automation

GridWorks now supports both display and real redstone output:

```text
                     +--> Status Light
Redstone Sensor -----|
                     +--> Control Relay --> vanilla redstone
```

The **Redstone Sensor** publishes:

- `gridworks:redstone/strength` — numeric value from 0 through 15;
- `gridworks:redstone/powered` — boolean state.

The **Status Light** subscribes to the boolean channel and persists its last commanded state. It is kept under Control Bus ownership rather than allowing adjacent vanilla redstone to take over its state.\n\nThe **Control Relay** subscribes to the same boolean channel and drives a lever-backed Rebar block, producing actual vanilla redstone output. Manual lever use is blocked so the Control Bus remains the source of truth.

Physical nodes are notified when a persisted linked peer becomes available. State-producing sensors use that hook to re-publish their current value, so a receiver that loads later does not have to wait for another redstone transition.

## Planned systems

The next gameplay milestones are Inventory Sensor and Fluid Sensor, followed by configurable logic and a survival-friendly Factory Controller. Smart power meters, breakers, branch limits, priority/load shedding, and factory-wide monitoring will follow the Rebar electricity API as it stabilizes.

See [Architecture](docs/ARCHITECTURE.md) for the design rules and API boundaries.

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
