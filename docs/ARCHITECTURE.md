# GridWorks Architecture

GridWorks is designed as an automation layer for the Rebar ecosystem rather than as another collection of faster machines.

## Design rules

1. **Keep the control core independent.** The public control-bus API and graph engine do not depend on Bukkit, Rebar, Pylon, electricity, cargo, or fluids.
2. **Integrate through adapters.** Rebar blocks and future electricity, logistics, fluid, and machine integrations sit at the edge of the control system.
3. **Do not call addon code while holding topology locks.** Dispatch snapshots its route first, then invokes receivers after releasing the graph lock.
4. **Bound all propagation.** A configurable hard cap prevents a single signal from traversing an unexpectedly huge network in one dispatch.
5. **Suppress graph duplicates.** A node receives a publication at most once even when the network contains cycles.
6. **Prefer events over global polling.** Sensors use native change events where available and use configurable scheduled sampling only when the source has no event model.
7. **Do not force chunk loads.** Physical networks operate only on currently loaded Rebar blocks and recover when chunks load naturally.
8. **Keep upstream-sensitive code isolated.** Rebar electricity is under active development, so electricity-specific code belongs behind a bridge instead of leaking into the core API.
9. **Persist topology separately from live routing.** A saved physical link can exist while one or both endpoint chunks are unloaded; the live graph contains loaded endpoints only.
10. **Centralize block lifecycle behavior.** Physical GridWorks node blocks inherit UUID persistence, activation, unload, break cleanup, and last-signal capture from one base class.

## Layers

```text
Rebar/Pylon machines, inventories, tanks, redstone, electricity
                          |
                    adapter layer
                          |
                GridWorks Control Bus
                 /        |        \
             sensors   controllers  actuators
```

## Control bus

The core implementation is an undirected graph of registered `ControlNode` endpoints. A publication contains:

- a UUID identifying its source node;
- a namespaced `ControlChannel`;
- a typed `ControlValue`;
- a monotonically assigned sequence number for that server session.

A breadth-first traversal snapshots reachable recipients. The source is not sent its own signal. Cycles are de-duplicated by node UUID. Receiver exceptions are isolated and reported in `ControlDispatchResult` without preventing delivery to healthy recipients.

## Physical control network

Every physical GridWorks node inherits `PhysicalControlNodeBlock`. The base class owns a UUID persisted in its Rebar block PDC and handles live graph activation, chunk unload, block break cleanup, and last-signal capture.

Connections between UUIDs are stored in `control-network.txt`.

The persistent connection store and the live graph intentionally represent different things:

- **Persistent store:** intended topology, including endpoints in unloaded chunks.
- **Live graph:** only nodes whose Rebar blocks are currently loaded.

When a control node loads, it registers and reconnects only to persistent neighbors that are already active. When it unloads, it is removed from the live graph without deleting persistent links. No lookup path loads a chunk.

Connection-file writes use a temporary sibling file and atomic replacement where the filesystem supports it. Mutations are rolled back in memory if a write fails.

## Sensors

Sensors should publish only on meaningful state changes whenever an event exists. The Redstone Sensor is the reference implementation: it listens to `BlockRedstoneEvent` and publishes both analog strength and boolean powered state. It does not participate in a per-tick sensor loop.

## Public API

Public addon contracts live under `io.github.wickidcow.gridworks.api`.

GridWorks publishes its `ControlBus` through Bukkit's `ServicesManager`, allowing another plugin to obtain the service without depending on the concrete graph implementation. Stable built-in channel names live in `GridWorksChannels`.

## Electricity

Electricity integration remains isolated behind `PowerGridBridge`. The public/provider-neutral `PowerGridSnapshot` contains only GridWorks measurements: node/producer/consumer counts, powered consumers, production capacity, and demand, with derived load/reserve/powered ratios.

The released Rebar dependency does not contain the electricity package currently present on the upstream `seggan/feature/elektrikity` branch, so the production build installs `UnavailablePowerGridBridge`. No reflection is used to bind unreleased internals and no dead Power Sensor is registered.

When electricity reaches a released Rebar artifact, a Rebar-specific bridge can translate `ElectricNetwork` / producer / consumer state into `PowerGridSnapshot`. Controllers, Control Bus channels, and future player-facing power devices do not need to depend directly on upstream electricity classes.


## Controller rules

Numeric controller logic is represented independently from GUI and block code by `NumericControlRule`. Rules match one exact `ControlChannel`, require a numeric value, and apply a `ComparisonOperator` to the observed value and configured threshold.

Controller output uses a selected command circuit (Default or A-D) rather than re-emitting a sensor channel. This keeps measurement channels and command channels separate and avoids accidental feedback loops when controllers share a Control Bus with their sources.


## Multi-condition controllers

Factory Controllers support one required condition and one optional second condition. Existing single-condition persistent keys remain Condition A, so upgrades do not change existing placed-controller behavior.

Each condition tracks its own metric, comparison, threshold, source binding, last observation, and last boolean result. Explicit source selection only enumerates directly linked nodes that are already loaded; it never causes chunk loads.

AND/OR uses three-state logic. Unknown input is preserved as unknown unless the other condition determines the result by short-circuiting. A genuinely unknown combined result is fail-safe: the controller publishes `control/enabled=false` while exposing a WAITING state in its GUI.


## Controller naming and reconnect safety

Factory Controller names are stored in Rebar block PDC and edited through InvUI's native AnvilWindow text input; GridWorks does not capture global chat messages for naming.

A WAITING controller is logically unknown but electrically fail-safe OFF. When a non-input peer becomes available, the controller always republishes its current wire-level output, including false while WAITING. This prevents a persisted relay that was previously ON from remaining ON after a partial chunk reload.


## Controller presets

Factory Controller presets are configuration templates, not alternate execution paths. Applying a preset writes the same condition/operator/threshold state used by Custom controllers, resets both sources to AUTO, clears prior observations, and forces fresh sensor data before the rule can become known.

Manual edits to rule semantics mark the controller Custom again. Source selection and controller naming do not, because they do not change the preset's logical rule.


## Component-wide state replay

Stateful nodes implement the public `ControlStateSource` contract. On node activation or link creation, the physical network snapshots all loaded state sources in the affected live component and invokes them outside its synchronization boundary.

This is not a polling loop. It only runs on topology changes and prevents multi-hop receivers from waiting indefinitely for an otherwise unchanged sensor value.

## Factory Monitor

The Factory Monitor subscribes only to a bounded set of built-in GridWorks channels and keeps one latest signal per channel. Its memory use therefore does not grow with event volume. GUI refreshes are marshalled onto the primary server thread when a signal is published asynchronously.


## Physical-node thread boundary

The public Control Bus remains thread-agnostic and invokes recipients on the publisher thread. Physical GridWorks blocks use `runOnServerThreadIfActive` before touching Bukkit world state or InvUI. The guard re-checks live topology before executing delayed work, so an asynchronous signal cannot mutate an unloaded node or force a chunk back into memory.

Status Light, steady Control Relay, Pulse Relay, Factory Controller GUI updates, and Factory Monitor GUI updates all follow this boundary.

## Pulse Relay

Pulse Relay is edge-triggered rather than level-triggered. A small pure `RisingEdgeTrigger` treats the first observed state as a baseline and only fires on a later false-to-true transition. This deliberately prevents topology replay or server restart from manufacturing a fake rising edge.

The relay uses Bukkit's delayed scheduler only when a pulse is active; there is no repeating timer. Retriggering after an intervening false state cancels the previous shutoff task and schedules a new one from the latest edge.


## Delay Relay

Delay Relay uses a pure `DelayedBooleanTransition` state machine plus at most one Bukkit delayed task. ON and OFF delays are configured independently. Repeated identical inputs do not schedule duplicate work, and reversing an input before its pending transition fires cancels that stale transition.

The output starts fail-safe OFF after load; current component state replay then establishes the desired level and applies the configured delay. No repeating ticker is used.


## Alarm Indicator

Alarm Indicator separates level state from event notification. The lamp follows the current boolean input, while `RisingEdgeTrigger` controls the audible bell. The first replayed value is only a baseline, so restart/reconnect state restoration is silent even when the alarm condition is already active.

Its lamp is protected from ordinary redstone changes in the same listener used by Status Light; the Control Bus remains the authoritative state.


## Alarm acknowledgement

`AlarmLatch` is a pure state machine independent of Bukkit. It tracks current fault condition, latched state, and operator acknowledgement.

An unacknowledged fault remains latched after its source condition clears. Acknowledging an already-cleared fault clears the latch immediately. Acknowledging an active fault marks it acknowledged while preserving the visible alarm; it auto-clears when the condition later becomes false. Persisted latch/acknowledgement survives chunk unload and restart, while the live input observation is re-established through normal Control Bus state replay.


## Named command circuits

The physical Control Bus is transport; command circuits are logical channels carried across it. GridWorks currently exposes Default plus A-D. Factory Controllers publish one selected command channel, while boolean actuators can filter for one circuit.

Backward compatibility is explicit: an actuator with no stored routing setting loads as `BooleanInputMode.LEGACY`, accepting `redstone/powered` and the original `control/enabled` channel. Existing Factory Controllers default to the Default command circuit.

Changing a controller's output circuit first publishes OFF on the old circuit before publishing its current state on the new one. This prevents downstream devices on the old circuit from being stranded ON.

Changing an actuator input circuit resets transient output/edge/timer state to a safe baseline, then requests component-wide state replay. No chunk loads or polling are introduced.


## Alarm telemetry

Alarm Indicator is also a `ControlStateSource`. It publishes three read-only boolean telemetry channels: current condition, latched state, and acknowledgement state.

Telemetry is emitted when the alarm condition changes, when an operator acknowledges it, and during component state replay. These channels are separate from command circuits, so observing alarm state cannot accidentally drive an actuator configured for Default/A-D commands.

Factory Monitor subscribes to these bounded channels like any other built-in measurement; no direct block lookup or alarm polling is required.


## Alarm Console and acknowledgement routing

Alarm Indicators publish their name plus condition/latch/acknowledgement state as telemetry and accept a separate `alarm/acknowledge` text command. An acknowledgement value is either one alarm node UUID or `*` for all alarm nodes reachable in the current Control Bus component.

The Alarm Console aggregates telemetry by source UUID rather than by channel alone. Per-field sequence numbers prevent an out-of-order asynchronous callback from overwriting newer state. Its source map is pruned against `activeComponentNodes` whenever telemetry is rebuilt or the operator presses Refresh.

The console does not enumerate worlds, query unloaded Rebar blocks, or keep a polling task. Refresh invokes the same component-wide `ControlStateSource` replay used by other topology recovery paths.


## Alarm severity and console filtering

Alarm severity is explicit telemetry with three levels: Critical, Warning, and Info. The persistent default is Warning so existing placed Alarm Indicators upgrade conservatively without becoming either silently informational or unexpectedly critical.

The Alarm Console applies filtering to its already-bounded in-memory source snapshots; filtering never changes bus subscriptions and never causes additional sensor reads. Available filters are All, Warning+, Critical only, Latched only, and Unacknowledged only.

Sorting remains operational rather than cosmetic: latched alarms appear before clear alarms, unacknowledged before acknowledged, then higher severity before lower severity, followed by active-condition state and stable name/source ordering.


## Lightweight alarm history

Alarm history is intentionally aggregate rather than an event database. Each Alarm Indicator persists two values in its Rebar block data: occurrence count and last-triggered epoch milliseconds.

The occurrence counter advances only when `AlarmLatch.observe` reports a real false-to-true transition. Initial state replay, chunk reload, and server restart therefore cannot manufacture incidents. The counter saturates at `Long.MAX_VALUE` rather than overflowing, and the last-triggered timestamp never moves backward if the host clock is adjusted.

History is also published as bounded telemetry and displayed by the Alarm Console, Factory Monitor, and Linker inspection. No history polling task or global storage file is introduced.

## CI Git initialization

The build job supplies `init.defaultBranch=main` through Git's environment-based configuration before `actions/checkout` runs. This suppresses the runner's default-branch migration hint at its source without adding an extra shell step or changing repository behavior.


## Alarm escalation

Alarm escalation is a derived one-step policy, not another persisted severity state. Alarm Indicators persist only the configured severity, escalation-enabled flag, escalation delay, and normal alarm history.

While an alarm is latched and unacknowledged, an optional one-shot Bukkit task waits until the configured deadline. Info escalates to Warning and Warning escalates to Critical; Critical has no further escalation. Acknowledgement or latch clearance cancels the task.

On chunk/server reload, `AlarmEscalationPolicy` compares the persisted last-trigger epoch with the current clock and either schedules only the remaining delay or applies escalation immediately when the deadline already passed. There is no repeating escalation ticker, and host-clock rollback cannot move the persisted last-trigger timestamp backward.


## Addressed command routing

Default/A-D remain the compact routing model for simple factories. Addressed commands are additive and use canonical channels under `gridworks:control/address/<address>`.

`ControlAddress` normalizes player input to a lower-case, channel-safe identifier capped at 32 characters. Addresses are scoped by physical Control Bus reachability, not globally registered. Sharing an address is therefore intentional multicast: every Addressed Relay with that address in the same loaded component receives the command.

Factory Controllers persist both their legacy circuit choice and their output mode/address. Switching output mode, circuit, or active address explicitly publishes OFF on the old active channel before publishing the current state on the new route, preventing abandoned receivers from remaining ON.

Addressed Relay starts fail-safe OFF after construction/load and relies on normal `ControlStateSource` replay from the controller to restore current state. Changing a relay address also resets OFF and requests component state replay. No address lookup scans worlds or loads chunks.

Factory Monitor keeps only the most recent addressed command in one dedicated diagnostic slot, preserving bounded memory despite the dynamic channel namespace.


## Power telemetry contract

`PowerGridTelemetry` is the provider-neutral conversion between a `PowerGridSnapshot` and typed Control Bus values. Stable channels live under `gridworks:power/*`, covering availability, topology counts, capacity, demand, reserve, load ratio, powered-consumer ratio, and powered/unpowered consumer counts.

An unavailable provider publishes only `power/available=false`; it does not invent zero capacity/demand values that could be mistaken for a real empty grid.

Factory Monitor reserves its sixth row for a bounded subset of these power channels. Until a released Rebar electricity adapter exists, those slots remain in WAITING state and no dead player-facing Power Grid Sensor is registered.


## Power-aware controller rules

Factory Controller's metric registry includes the provider-neutral power telemetry channels for production capacity, demand, reserve watts, load ratio, powered-consumer ratio, and unpowered-consumer count.

Power metrics are appended to the existing registry. Persistent controller rules store the full channel identifier rather than a metric-list index, so adding these metrics does not reinterpret existing placed controllers.

The built-in Power Load, Power Shortage, and Load Shed Trigger presets use the same `NumericControlRule` path as every other controller preset. If no linked source publishes the configured power metric, the rule remains unknown and the controller preserves its existing fail-safe OFF wire behavior. No special electricity execution path exists inside Factory Controller.
