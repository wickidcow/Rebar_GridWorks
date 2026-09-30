# Roadmap and Compatibility

## Current target

| Component | Current development target |
| --- | --- |
| GridWorks | 0.4.0-SNAPSHOT |
| Minecraft / Paper | 26.2 |
| Java | 25 |
| Rebar | 1.0.0-20260929.193904-140 |

The repository's Gradle properties and CI are authoritative if this page ever trails a newer development commit.

## What GridWorks is

GridWorks is the **factory automation, control, and operations layer** for Rebar.

A useful mental model is:

```text
Rebar / Pylon machines -------- do the work
cargo / fluids / electricity -- move resources and power
GridWorks sensors ------------- observe
GridWorks controllers --------- decide
GridWorks actuators ----------- apply safe commands
GridWorks monitors/alarms ----- operate and diagnose the factory
```

GridWorks should grow toward a Minecraft version of a lightweight **PLC + SCADA** system: programmable-enough control behavior, production coordination, safety interlocks, telemetry, alarms, and operator visibility without becoming a general-purpose scripting engine.

## Deliberate boundary around electricity

GridWorks does **not** intend to replace Seggan/Rebar electricity.

Electricity generation, storage, cables/conductors, network simulation, electrical losses, and the underlying power graph belong to Rebar's electricity system or another power provider.

GridWorks owns the automation around that system:

- observe grid health;
- decide when loads should run;
- shed or restore priorities;
- open/close controllable branches;
- apply branch limits;
- alarm on shortages;
- coordinate production with available power.

Native Rebar electricity is integrated using the pinned development API `1.0.0-20260929.193904-140` from upstream commit `5e34938`. Use a matching Rebar development server JAR; the stable `0.43.0-26.2` JAR lacks this API. See [Native Electricity](https://github.com/wickidcow/Rebar_GridWorks/blob/main/docs/wiki/Native-Electricity.md).

## Implemented foundation

The current development line includes:

- persistent Control Bus topology;
- Default/A/B/C/D circuits and addressed commands;
- redstone, inventory, fluid, machine, and provider-neutral power sensing;
- Factory Controller rules and Factory Monitor;
- alarms and Alarm Console;
- Machine Sensor cycle telemetry;
- Batch Controller target/watchdog/pace/ETA;
- four-stage Sequence Controller;
- Stock Controller low/high inventory hysteresis;
- provider-neutral power telemetry and branch APIs;
- Load Shedding Controller;
- Smart Breaker and Power Limiter;
- Fluid Valve and Cargo Isolator;
- first-class `/rebar guide` category;
- survival recipe visibility and content-integrity validation;
- live Paper + Rebar CI smoke testing;
- `/gridworks doctor`.

## Next control devices

The strongest remaining additions are deliberately control-oriented rather than more processing machines.

### Timer / Clock Controller

Generate replay-safe scheduled control events:

- one-shot delay;
- repeating interval;
- duty cycle;
- optional Minecraft-time window;
- named/circuit output.

It should own only the minimum delayed task required by its current schedule and must never replay a false timer edge after restart.

### Counter Controller

A general event counter for rising edges and compatible integer telemetry:

- target count;
- reset/start input;
- rollover or latch mode;
- completion output;
- persisted count/history.

Batch Controller remains machine-production-specific; Counter Controller would be the reusable primitive.

### Rate Controller

Derive event rate from existing signals without creating another global sampler:

- events/minute;
- minimum/maximum acceptable rate;
- stalled/slow/over-speed output;
- bounded smoothing window.

Useful for farms, cargo pulse lines, production cells, and later electricity-aware factory throttling.

### Production Router / Priority Controller

Select which one of several production lines should run based on resource demand and priority.

Examples:

- iron low -> enable iron line;
- iron satisfied, copper low -> switch to copper;
- critical input shortage -> pause optional products.

The goal is deterministic demand scheduling, not an autocrafter replacement.

### Resource Interlock

A specialized controller that requires input stock **and** output capacity before enabling a line. It can compose Inventory/Fluid/Machine availability into one safe production permission without users rebuilding the same rule repeatedly.

## Operations layer

After the control primitives are mature, GridWorks can add richer operations tooling without turning into a database-heavy plugin.

### Factory Historian

Record **meaningful state transitions**, not every sample:

- batch completions;
- sequence completions/faults;
- alarms;
- production start/stop;
- stock-controller demand transitions;
- power shortage/load-shed events.

History should be bounded, append-light, and chunk-independent.

### Production Dashboard

Extend Factory Monitor with:

- current line state;
- stock target status;
- recent batch duration;
- completed runs;
- current alarms;
- event-derived production rates.

### Maintenance / Runtime Counters

Optional machine-cycle/runtime summaries can support service intervals and operator reminders without pretending to modify Rebar machine durability.

## Things GridWorks should not become

To preserve a clear ecosystem role, GridWorks should normally avoid adding:

- ore processors, furnaces, crushers, farms, or generic crafting machines;
- generators, batteries, cables, or an electricity simulation;
- a general digital storage network;
- chunk loaders;
- world-wide item or machine scanners;
- arbitrary scripting capable of bypassing the safe Control Bus model.

Those are better served by Rebar/Pylon or separate focused addons.

## Separate-addon opportunities

Some ideas pair well with GridWorks but should remain separate projects. A digital-storage/crafting addon is one example: it can expose stock telemetry and automation endpoints to GridWorks without putting storage ownership inside GridWorks.

## Development rules

New systems should continue to preserve these project principles:

- no unnecessary force-loaded chunks;
- no world scans for ordinary automation;
- events before polling;
- shared samplers when polling is unavoidable;
- publish only changed snapshots;
- replay-safe edge semantics;
- fail-safe physical route changes;
- explicit provider contracts;
- bounded persisted history;
- regression tests for false triggers, dupes, lifecycle failures, and unsafe startup state.
