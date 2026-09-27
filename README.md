# Rebar GridWorks

**Industrial automation, smart power management, and factory control systems for Pylon/Rebar.**

GridWorks is a Rebar addon focused on making factories smarter rather than simply making machines faster. Its long-term goal is to connect sensors, controllers, power systems, cargo, fluids, machines, and redstone through a common automation layer.

> GridWorks is in early development. The Control Bus, persistent physical network, redstone I/O, sensors, controllers, presets, and Factory Monitor are implemented.

## Current systems

The Control Bus provides namespaced typed signals, cycle-safe graph propagation, a configurable safety cap, receiver isolation, and a Bukkit service API for other addons.

One physical Control Bus can carry five simple boolean command circuits: **Default, A, B, C, and D**. Factory Controllers can also switch to **addressed output**, publishing to a normalized channel such as `gridworks:control/address/ore_line_1`. The Addressed Relay listens to one human-readable address; multiple relays may deliberately share an address to create a multicast control group.

Existing boolean actuators keep their Linker-selectable Legacy/Redstone/Default/A-D routing unchanged, so addressed control is additive and does not alter placed devices.

The physical layer provides persistent node UUIDs, atomic link persistence, a GridWorks Linker, chunk-safe live topology, peer-availability callbacks, and reconnection without world scans or forced chunk loads.

### Redstone automation

```text
                     +--> Status Light
                     +--> Alarm Indicator --> visible + one-shot local alert
Redstone Sensor -----|
                     +--> Control Relay --> steady vanilla redstone
Factory Controller --> Addressed Relay --> addressed/group redstone
                     +--> Pulse Relay   --> timed vanilla redstone pulse
                     +--> Delay Relay   --> delayed/debounced redstone
```

The Redstone Sensor is event-driven and publishes analog strength plus boolean powered state. The Status Light displays the boolean state, while the Control Relay produces steady vanilla redstone output. The Addressed Relay adds human-readable routing for factories that need more independent outputs than Default/A-D.

The **Pulse Relay** produces a configurable 1-tick to 60-second redstone pulse on an observed false-to-true edge. Its first replayed state after a load/relink only establishes a baseline, preventing server restarts from accidentally re-triggering one-shot machinery. A new rising edge during an active pulse restarts the timer from that edge.

The **Delay Relay** has independently configurable ON and OFF delays from instant to 60 seconds. If the input reverses before a delayed transition fires, the stale task is cancelled. This makes it useful for startup delays, shutdown grace periods, debounce behavior, and preventing rapid machine cycling.

The **Alarm Indicator** is the first non-redstone action device and now uses industrial-style acknowledgement. A new false-to-true fault latches the indicator and optionally rings a local bell once. If the condition clears before an operator acknowledges it, the lamp stays latched so the event is not lost. If the alarm is acknowledged while the condition is still active, it remains visibly acknowledged and clears automatically when the fault later disappears. Replayed state after restart/topology recovery is silent.

Alarm Indicators can be given player-defined names and a persisted **Critical / Warning / Info** configured severity. Existing placed alarms with no stored severity load as **Warning**. Optional one-step timed escalation can raise an unacknowledged latched alarm from Info→Warning or Warning→Critical after a configurable 5-second to 30-minute delay. Acknowledgement cancels escalation. After chunk/server reload, the remaining delay is reconstructed from the persisted last-trigger timestamp rather than restarted.

Each indicator also keeps a lightweight persisted history summary: total real alarm occurrences and the timestamp of the most recent real false-to-true trigger. Startup/replay does not increment history.

They publish bounded telemetry on `gridworks:alarm/name`, `gridworks:alarm/severity`, `gridworks:alarm/condition_active`, `gridworks:alarm/latched`, `gridworks:alarm/acknowledged`, `gridworks:alarm/occurrences`, and `gridworks:alarm/last_triggered_epoch_ms`. They implement `ControlStateSource`, so a Factory Monitor or Alarm Console joining later receives current state and history without polling alarm blocks.

### Alarm Console

The **Alarm Console** aggregates alarm telemetry by source UUID across its currently loaded Control Bus component. It sorts latched/unacknowledged faults first and then by severity, displays up to 18 at once, provides per-alarm acknowledgement, and has an explicit **Acknowledge All** action. A persisted filter cycles through **All, Warning+, Critical only, Latched only, and Unacknowledged only**.

Acknowledgement itself is a Control Bus command (`gridworks:alarm/acknowledge`) carrying either a target alarm UUID or `*`. That means the console does not need to reach into another block's implementation. Refresh prunes sources that are no longer in the loaded component and requests normal state replay; it never scans the world or forces chunks to load.

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

Controllers publish their boolean result either on a selected command circuit (Default or A-D) or on a human-readable addressed channel. Legacy actuators continue accepting the original Default command plus direct Redstone Sensor input until their routing is changed.

## Factory Controller

The Factory Controller is a configurable logic block. It supports **Condition A** plus an optional **Condition B**, combined with AND or OR, and publishes its result either on a selectable Default/A-D command circuit or an addressed output.

Its Rebar/InvUI screen configures:

- metric, comparison, threshold, and source for Condition A;
- optional Condition B with its own metric, comparison, threshold, and source;
- AND/OR combination;
- output routing mode, circuit/address, and current output state;
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

The Factory Monitor is a passive diagnostic node. It displays the latest built-in redstone, command-circuit, inventory, fluid, controller-output, and alarm-state signals seen on its loaded Control Bus component, including the source node ID and signal sequence. It does not poll machines.

Stateful devices implement `ControlStateSource`. When loaded topology expands, GridWorks replays current state from every loaded state source in that component. This means a monitor or actuator joining through an intermediate Control Interface receives current state even when the original sensor is several hops away.

## Threading safety

Control Bus callbacks deliberately run on the publisher's thread. Physical GridWorks nodes therefore marshal all Bukkit world mutations and InvUI refreshes onto the primary server thread and verify the node is still active before delayed work executes. This lets third-party addons publish asynchronously without making GridWorks actuators touch unloaded chunks or Bukkit state off-thread.

## Planned systems

The next work is power-grid integration behind the existing adapter boundary as Rebar's electricity API stabilizes, plus broader addressed actuators where that remains useful.

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
