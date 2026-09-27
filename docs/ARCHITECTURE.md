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
