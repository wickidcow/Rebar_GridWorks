# Rebar GridWorks

**Industrial automation, smart power management, and factory control systems for Pylon/Rebar.**

GridWorks is a Rebar addon focused on making factories smarter rather than simply making machines faster. Its long-term goal is to connect sensors, controllers, power systems, cargo, fluids, machines, and redstone through a common automation layer.

> GridWorks is in early development. The Control Bus foundation and first physical Control Interface/Linker layer are now implemented; sensors and actuators are the next stage.

## Current foundation

The Control Bus provides:

- namespaced signal channels such as `gridworks:power/load`;
- built-in boolean, number, and text signal values;
- an addon-extensible `ControlValue` API;
- graph-based node registration and connections;
- cycle-safe, duplicate-free signal propagation;
- a configurable propagation safety cap;
- isolated receiver failures so one broken endpoint does not stop the rest of a network;
- a Bukkit `ServicesManager` registration so other addons can discover the `ControlBus` API.

The first physical layer adds:

- a Rebar **Control Interface** block with a persistent UUID identity;
- a **GridWorks Linker** for creating/removing Control Bus links;
- sneak-right-click network inspection;
- persistent link storage with atomic file replacement;
- chunk-safe activation/deactivation that never loads a chunk just to reconnect a network;
- automatic reconnection when both endpoints become loaded again;
- separate counts for persistent network size and currently loaded nodes.

The core graph deliberately has no dependency on Bukkit, Pylon, or Rebar. Platform-specific behavior is added through adapters, which keeps the automation system testable and prevents unstable upstream APIs from leaking through GridWorks.

## Planned systems

The next gameplay milestones are Redstone Interface, Inventory Sensor, Fluid Sensor, Status Light, and Relay, followed by a survival-friendly Factory Controller. Smart power meters, breakers, branch limits, priority/load shedding, and factory-wide monitoring will follow the Rebar electricity API as it stabilizes.

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
