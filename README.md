# Rebar GridWorks

**Industrial automation, smart power management, and factory control systems for Pylon/Rebar.**

GridWorks is a Rebar addon focused on making factories smarter rather than simply making machines faster. Its long-term goal is to connect sensors, controllers, power systems, cargo, fluids, machines, and redstone through a common automation layer.

> GridWorks is in early development. The Control Bus, persistent physical network, redstone I/O, sensors, controllers, presets, and Factory Monitor are implemented.

## Current systems

The Control Bus provides namespaced typed signals, cycle-safe graph propagation, a configurable safety cap, receiver isolation, and a Bukkit service API for other addons.

One physical Control Bus can carry five independent boolean command circuits: **Default, A, B, C, and D**. Factory Controllers choose an output circuit in their GUI. Boolean actuators use the GridWorks Linker (sneak + left-click) to cycle their input mode between Legacy, Redstone-only, Default, and A-D. Existing blocks remain Legacy-compatible, accepting the original Redstone + Default inputs until changed.

The physical layer provides persistent node UUIDs, atomic link persistence, a GridWorks Linker, chunk-safe live topology, peer-availability callbacks, and reconnection without world scans or forced chunk loads.

### Redstone automation

```text
                     +--> Status Light
                     +--> Alarm Indicator --> visible + one-shot local alert
Redstone Sensor -----|
                     +--> Control Relay --> steady vanilla redstone
                     +--> Pulse Relay   --> timed vanilla redstone pulse
                     +--> Delay Relay   --> delayed/debounced redstone
```

The Redstone Sensor is event-driven and publishes analog strength plus boolean powered state. The Status Light displays the boolean state, while the Control Relay produces steady vanilla redstone output.

The **Pulse Relay** produces a configurable 1-tick to 60-second redstone pulse on an observed false-to-true edge. Its first replayed state after a load/relink only establishes a baseline, preventing server restarts from accidentally re-triggering one-shot machinery. A new rising edge during an active pulse restarts the timer from that edge.

The **Delay Relay** has independently configurable ON and OFF delays from instant to 60 seconds. If the input reverses before a delayed transition fires, the stale task is cancelled. This makes it useful for startup delays, shutdown grace periods, debounce behavior, and preventing rapid machine cycling.

The **Alarm Indicator** is the first non-redstone action device and now uses industrial-style acknowledgement. A new false-to-true fault latches the indicator and optionally rings a local bell once. If the condition clears before an operator acknowledges it, the lamp stays latched so the event is not lost. If the alarm is acknowledged while the condition is still active, it remains visibly acknowledged and clears automatically when the fault later disappears. Replayed state after restart/topology recovery is silent.

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
- current output state;
- a player-defined controller name using InvUI's anvil text input;
- reusable starting presets: Low Tank, Inventory High, Redstone >= 8, Stock + Fluid Ready, and Supply Alert.

Presets reset condition sources to AUTO and clear stale measurements before evaluation. Any manual metric/operator/threshold/AND-OR edit marks the configuration Custom again.

Controllers can also use `gridworks:redstone/strength` as a numeric 0–15 input, so analog vanilla redstone can participate in threshold logic.

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

## Factory Monitor

The Factory Monitor is a passive diagnostic node. It displays the latest built-in redstone, inventory, fluid, and controller-output signals seen on its loaded Control Bus component, including the source node ID and signal sequence. It does not poll machines.

Stateful devices implement `ControlStateSource`. When loaded topology expands, GridWorks replays current state from every loaded state source in that component. This means a monitor or actuator joining through an intermediate Control Interface receives current state even when the original sensor is several hops away.

## Threading safety

Control Bus callbacks deliberately run on the publisher's thread. Physical GridWorks nodes therefore marshal all Bukkit world mutations and InvUI refreshes onto the primary server thread and verify the node is still active before delayed work executes. This lets third-party addons publish asynchronously without making GridWorks actuators touch unloaded chunks or Bukkit state off-thread.

## Planned systems

The next work is alarm routing/aggregation, richer command addressing, and power-grid controls as Rebar's electricity API stabilizes. After that, GridWorks can connect inventory/fluid/redstone measurements to relays and eventually to smart electricity controls as Rebar's electricity API stabilizes.

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
