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

Electricity integration is intentionally not compiled into the current build. Rebar's electricity implementation is still evolving upstream. When its addon-facing contract stabilizes, GridWorks will add a bridge for grid measurements, smart breakers, branch limits, load shedding, and power-aware factory rules without changing the control-bus core.


## Controller rules

Numeric controller logic is represented independently from GUI and block code by `NumericControlRule`. Rules match one exact `ControlChannel`, require a numeric value, and apply a `ComparisonOperator` to the observed value and configured threshold.

Controller output uses `gridworks:control/enabled` rather than re-emitting a sensor channel. This keeps measurement channels and command channels separate and avoids accidental feedback loops when controllers share a Control Bus with their sources.


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
