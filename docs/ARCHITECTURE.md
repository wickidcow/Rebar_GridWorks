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

The released Rebar dependency does not contain the electricity package currently present on the upstream `seggan/feature/elektrikity` branch, so no reflection is used to bind unreleased internals and no dead Power Sensor is registered.

Instead, GridWorks exposes the public `PowerGridProvider` service contract. `ServicePowerGridBridge` asks Bukkit's `ServicesManager` for the highest-priority currently registered provider each time a snapshot is needed. Provider registration therefore follows Bukkit's standard plugin lifecycle and automatically disappears when the owning plugin is disabled.

When electricity reaches a released Rebar artifact, a Rebar-specific provider can translate `ElectricNetwork` / producer / consumer state into `PowerGridSnapshot`. Controllers, Control Bus channels, and future player-facing power devices do not need to depend directly on upstream electricity classes.


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

### Activation/replay ordering

Physical control blocks now have explicit pre- and post-activation lifecycle hooks. `beforeActivated()` runs after Rebar block initialization but before the node joins the live graph, while `afterActivated()` runs only after activation and component state replay finish.

Replay-sensitive devices reset transient input baselines in the pre-activation hook. This is critical for Pulse Relay, Delay Relay, Alarm Indicator, Sequence Controller, and Batch Controller reset input: replay establishes their first observed state, and the post-activation hook must not erase that baseline. Otherwise the first real post-restart transition could be missed or a delayed transition scheduled from replay could be invalidated.

Sampler-backed Inventory, Fluid, Machine, and Power Grid sensors also clear only their transient cached snapshots in `beforeActivated()`. Their first replay therefore reads the current adjacent target rather than re-emitting a snapshot retained by a reactivated block instance. Machine Sensor additionally clears only `ObservedMachineCycleCounter`'s transient previous-processing observation while preserving persisted cycle totals and timestamp history; this prevents an old pre-unload processing state from pairing with a new idle sample and manufacturing a cycle.

## Factory Monitor

The Factory Monitor subscribes only to a bounded set of built-in GridWorks channels and keeps one latest signal per source/channel pair. A dedicated source selector switches between an Overview (newest source for each channel) and one observed source, so identical metrics from multiple machines no longer overwrite each other. Addressed commands retain only the latest command per source, keeping the dynamic address namespace bounded.

The GUI is category-paged rather than one permanently mapped 54-slot matrix. Automation, Resources, Production, and Power & Alarms each provide a bounded channel list, while the addressed-command diagnostic, source selector, refresh action, and page navigation stay in a fixed header. Page selection is viewer-local and does not alter telemetry subscription state.

Each channel maps to one page-local slot index. A signal update therefore refreshes only the corresponding generic page-slot item rather than repainting every telemetry item; viewers on other pages may re-render that same slot index but no full-page refresh occurs. Page changes intentionally refresh the bounded page-slot set. Static initialization verifies every built-in monitor definition appears on exactly one page and that no page exceeds the available slot count.

Per-key sequence checks reject late asynchronous callbacks that would otherwise overwrite newer state. Refresh prunes the cache against activeComponentNodes and requests normal component-wide state replay; it never scans worlds or loads chunks. GUI refreshes remain marshalled onto the primary server thread.


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

Factory Monitor keeps only the most recent addressed command per observed source. The dedicated diagnostic slot shows the newest command in Overview or the selected source's latest command, preserving bounded memory despite the dynamic channel namespace.


## Power telemetry contract

`PowerGridTelemetry` is the provider-neutral conversion between a `PowerGridSnapshot` and typed Control Bus values. Stable channels live under `gridworks:power/*`, covering availability, topology counts, capacity, demand, reserve, load ratio, powered-consumer ratio, and powered/unpowered consumer counts.

An unavailable provider publishes only `power/available=false`; it does not invent zero capacity/demand values that could be mistaken for a real empty grid.

Factory Monitor reserves its sixth row for a bounded subset of these power channels. When no provider is registered, the player-facing Power Grid Sensor reports unavailable and those measurements remain in WAITING state rather than inventing grid values.


## Power-aware controller rules

Factory Controller's metric registry includes the provider-neutral power telemetry channels for production capacity, demand, reserve watts, load ratio, powered-consumer ratio, and unpowered-consumer count.

Power metrics are appended to the existing registry. Persistent controller rules store the full channel identifier rather than a metric-list index, so adding these metrics does not reinterpret existing placed controllers.

The built-in Power Load, Power Shortage, and Load Shed Trigger presets use the same `NumericControlRule` path as every other controller preset. If no linked source publishes the configured power metric, the rule remains unknown and the controller preserves its existing fail-safe OFF wire behavior. No special electricity execution path exists inside Factory Controller.


## Load shedding policy

`LoadSheddingPolicy` is provider-neutral and event-driven. It consumes a `PowerGridSnapshot` (or equivalent load ratio/unpowered-consumer values) only when telemetry changes; it has no scheduler or poll loop.

The policy exposes three stages:

- `NORMAL`: essential, normal, and optional loads are allowed;
- `SHED_OPTIONAL`: optional loads are disabled, essential and normal remain allowed;
- `SHED_NORMAL_AND_OPTIONAL`: only essential loads remain allowed.

Default hysteresis is 90%/80% for optional shed/restore and 100%/90% for normal shed/restore. Any reported unpowered consumer forces the severe stage even when aggregate capacity-minus-demand appears healthy, because Rebar branch/edge limits can strand consumers independently of total capacity.

Recovery is deliberately staged: severe -> optional-shed -> normal. This prevents a marginal grid from restoring all loads at once and immediately collapsing again.


## Third-party power providers

External addons can implement `io.github.wickidcow.gridworks.api.power.PowerGridProvider` and register it with Bukkit's `ServicesManager`. GridWorks intentionally does not cache the provider object across calls; service resolution follows Bukkit's highest-priority registration semantics, so provider enable/disable or replacement does not leave a stale reference inside GridWorks.

A provider must return snapshots only for already-loaded blocks and must never force chunk loads. Returning `Optional.empty()` means the queried block is not associated with a known grid. Returning `null` is treated as a provider contract violation.


## Power Grid Sensor

Power Grid Sensor is the first player-facing consumer of the provider-neutral electricity API. It faces one adjacent block and asks the currently selected `PowerGridProvider` for a snapshot only when that adjacent chunk is already loaded.

All loaded sensors share one configurable sampler (20 ticks by default). Sensors compare record snapshots and publish only on meaningful changes. When no provider is registered, the manager does not repeatedly walk every sensor; it publishes the transition to unavailable once and waits until a provider becomes available again.

An unavailable sensor publishes only `power/available=false`. It does not publish synthetic zero capacity/demand values.

Factory Controller treats that availability channel specially for power metrics. If the unavailable signal comes from the sensor currently bound to a power condition, only the condition's cached observation/result is cleared; its selected source remains bound. The controller then recomputes through its normal three-state logic and uses fail-safe OFF when the result becomes genuinely unknown.


## Peer unavailability

The physical network now emits both peer-available and peer-unavailable callbacks for directly linked loaded nodes. Unavailability is sent after an unload removes the peer from the live graph and after an explicit unlink disconnects two loaded peers. Callbacks execute outside the topology monitor and failures are isolated through the existing callback failure handler.

Factory Controller uses this lifecycle signal for every metric type, not only power. If its bound source unloads or is unlinked, the controller preserves the selected source UUID but clears the cached observation/result and immediately recomputes through three-state logic. This prevents stale inventory, fluid, redstone, or power measurements from continuing to drive automation while the source is absent.


## Power snapshot publication boundary

`PowerGridTelemetry.fromSnapshot` returns an unmodifiable insertion-ordered map rather than `Map.copyOf`, because downstream power logic may care about deterministic publication order.

Power Grid Sensor increments a per-sensor revision only when its snapshot actually changes. It publishes all telemetry fields first and then publishes `power/sample_revision` last. The revision stays within the exactly representable integer range of IEEE-754 doubles and wraps back to 1 only after that extremely large boundary.

Consumers that depend on multiple power fields should cache individual values and evaluate only when the revision marker arrives. Single-metric Factory Controller rules can continue reacting directly to their configured channel.


## Load Shedding Controller

Load Shedding Controller is a `ControlStateSource` that consumes `power/available`, `power/load_ratio`, `power/unpowered_consumers`, and `power/sample_revision` from one directly linked source.

Load/unpowered values are cached as they arrive, but the policy is evaluated only when the sample-revision marker arrives. This prevents a mixed old/new snapshot from causing a transient shed/restore decision.

The controller publishes three addressed boolean outputs for Essential, Normal, and Optional tiers. Their addresses are independent and enforced to be distinct. Re-addressing a tier first publishes OFF to the old address and then publishes the tier's current state to the new one.

Missing telemetry is explicit policy rather than implicit behavior. Essential Only, Allow All, and Hold Last are available. The default Essential Only keeps priority-one loads on while disabling lower priorities when the monitoring path disappears.

The last known hysteresis stage is persisted. Provider loss or source chunk unload does not reset it; when telemetry returns, hysteresis resumes from the prior stage. Manually choosing a different source resets the stage to Normal because it may represent a different power grid.


## Configurable load-shedding thresholds

`LoadSheddingThresholds` is the validated persistent configuration for the four hysteresis boundaries. Its invariants guarantee:

`optionalRestore <= optionalShed <= normalShed`

and

`optionalRestore <= normalRestore <= normalShed`.

Player edits clamp to those relationships rather than silently shifting another threshold. Persisted data is loaded as one atomic configuration; missing, non-finite, out-of-range, or internally inconsistent values fall back to the complete default set.

Changing thresholds reconstructs `LoadSheddingPolicy` with the current persisted stage, then immediately reevaluates the most recent complete telemetry sample when one is known. This preserves hysteresis state while allowing the new settings to take effect without polling.

## Load-shedding route repair

`LoadSheddingRoutes` validates that Essential, Normal, and Optional addresses are distinct. During block load it preserves Essential first, then repairs conflicting lower-priority routes with deterministic node-derived fallback addresses. The fallback search handles the edge case where a persisted address already equals the first generated fallback.


## Machine Sensor

Machine Sensor targets only released Rebar machine contracts: `ProcessorRebarBlock` and `RecipeProcessorRebarBlock`. `MachineProbe` reads their public processing/time fields and converts both forms into the same immutable `MachineSnapshot`.

The normalized progress contract is completion progress from 0.0 to 1.0. Recipe processors already expose that direction; generic processors expose remaining/total semantics, so GridWorks derives completion from their tick counts instead of forwarding the raw `processProgress` property.

GridWorks does not use `TickingRebarBlock.isTicking` as machine state. That method is internal and only indicates that a scheduled ticking job remains active, not that useful work is being processed.

All loaded Machine Sensors share one configurable sampler (20 ticks by default), never load adjacent chunks, and publish only when the immutable snapshot changes. Unsupported targets publish a zero-valued unavailable snapshot rather than inferred failure reasons.

Machine Sensor also owns a persisted `ObservedMachineCycleCounter`. The first available snapshot establishes a baseline. Only a continuously available processing -> idle transition increments the counter; an unavailable snapshot clears the baseline, so chunk unload, target replacement, reload, and topology replay cannot manufacture a cycle. The count saturates at the largest integer exactly representable by the Control Bus double transport. The last-cycle timestamp is monotonic and persisted with the block.

The resulting `machine/observed_cycles` and `machine/last_cycle_epoch_ms` telemetry is replayable state. Observed Cycles is intentionally defined as activity telemetry, not a guaranteed recipe-output count, because released Rebar has no universal completion event spanning both processor interfaces GridWorks supports.

Machine Sensor exposes this state through a small GUI. Resetting the observed-cycle count clears only the persisted total and last-cycle timestamp; it deliberately preserves the current processing baseline. This means resetting during an active job does not lose that job from the new batch. The reset immediately republishes telemetry, so downstream Factory Controllers fall below their prior cycle threshold without requiring topology churn or a sensor reload.


## Part 4 Batch Controller

`BatchProgressTracker` converts cumulative Machine Sensor cycle counters into persisted batch progress without polling. It maintains only runtime last-seen counts per source. A source's first value is a baseline; only positive deltas add to progress. Counter decreases re-baseline rather than subtracting progress. Its explicit `rebaseline` operation can advance source history without changing progress.

Batch Controller accepts cycle telemetry only when the signal's source UUID is a currently loaded **direct peer**. Peer-unavailable callbacks forget that source baseline. When the source reconnects, its current cumulative count becomes a fresh baseline, preventing work performed while disconnected from being backfilled into the batch.

Batch progress, target, watchdog configuration, fault state, and fault route are persistent. Source baselines are deliberately runtime-only: after controller reload, the first value from every source is baseline state. Resetting a batch clears progress and a latched fault while preserving live baselines, so new cycles begin counting immediately and already-observed historical totals do not.

The completion state is derived from `progress >= target` and suppressed while a fault is latched. Completion routing reuses `ControlOutputMode`, `ControlCommandChannel`, and `ControlAddress`. Fault has a separate addressed output that is validated against the saved completion address; route edits explicitly clear the prior output before publishing the new state.

The optional no-progress watchdog is disabled by default. When enabled on an incomplete, non-faulted batch, exactly one Bukkit delayed task captures the current progress. Every positive cycle delta replaces that task with a fresh deadline. A callback faults only if progress still equals the captured value, so stale callbacks cannot fault a batch that has advanced. Completion, fault, unload, or removal cancels the task. Reload reconstructs a fresh deadline rather than treating server-offline time as a stall.

A watchdog fault is intentionally latched. While faulted, source telemetry calls `rebaseline` instead of `observe`, keeping live cumulative source totals current without counting fault-period production. When the operator chooses **Start New Batch**, progress and fault reset together while those current baselines remain, preventing hidden backfill.

The same new-batch transition is exposed through one collision-safe addressed Reset/Start input. Its `RisingEdgeTrigger` is reset in `beforeActivated()`, so activation replay establishes baseline state instead of manufacturing a reset. Changing the reset address resets that edge detector before requesting component replay. The saved completion address, fault address, and reset input are mutually exclusive even when completion output mode is currently Circuit, preventing a later mode switch from creating a feedback collision.

`BatchPaceTracker` is runtime-only and event-driven. The first positive applied progress delta records a monotonic `System.nanoTime()` baseline; each later positive delta computes cycles/minute from only that delta and the elapsed monotonic interval. ETA is derived from remaining cycles at publication time. The tracker resets on controller reactivation, new-batch reset, unload/removal, watchdog fault, and direct Machine Sensor peer availability changes so a rate measured against one production topology is never carried into another. `batch/rate_available=false` is published whenever the estimate is unknown; numeric rate/ETA channels are only refreshed when a valid pace exists, avoiding synthetic zero telemetry.

The implementation remains bounded by the largest integer exactly representable by Control Bus numeric transport. Outside the optional one-shot watchdog it has no repeating scheduler, and it performs no world scan or chunk loading.


## Part 4 Sequence Controller

`SequenceStateMachine` is a pure persisted state machine with IDLE, RUNNING, COMPLETE, and FAULT phases. RUNNING and FAULT retain exactly one stage from 1 through 4. Start/restart selects Stage 1, advance moves through stages in order, the fourth advance enters COMPLETE, timeout faults preserve the stage that failed, and abort returns to IDLE.

`SequenceRoutes` owns the ten normal routes: start input, completion output, four stage triggers, and four stage outputs. Construction and edits reject collisions across that set. Sequence Controller additionally owns a configurable fault output, a fault-interlock input, and an abort/reset input. All three are validated against the ten-route set and against one another before edits or persisted state are accepted. Corrupt/legacy collisions fall back to deterministic node-derived addresses instead of allowing output-to-input feedback.

Only the current stage trigger is accepted while RUNNING. Start has its own independent rising-edge detector. Entering a stage resets the stage edge detector and requests normal component state replay. The first current trigger value after start/advance/reload therefore establishes a baseline rather than advancing; only a later false-to-true transition advances the sequence.

The fault-interlock input is intentionally level-sensitive and is accepted in every phase so replay can carry its current state. The latest observed level is cached as transient controller state. A true value transitions only a RUNNING sequence into FAULT, preserving the current stage. If Start is requested while that cached level is already true, the state machine enters FAULT before publishing a running stage output. This means a persisted/latched upstream fault cannot be bypassed by server restart, topology replay, or by restarting the sequence while the fault source remains true.

Abort/reset uses an independent `RisingEdgeTrigger`. Its transient edge state is reset before activation and whenever the reset address changes, then normal state replay establishes the current level as baseline. Only a later false-to-true transition invokes the same abort transition as the GUI: phase -> IDLE, fault reason -> NONE, timeout cancelled, stage edge cleared, and all derived outputs republished OFF.

State publication is complete and idempotent: running/stage/complete/fault telemetry, persisted fault reason, live fault-interlock level, active-stage timeout ticks, completed-run count, and last-completion timestamp are published. All four stage outputs are explicitly written true/false, and completion/fault outputs are derived from the persisted phase. Restart and abort therefore clear abandoned outputs without a scan.

`SequenceCompletionHistory` persists bounded completion count and last-completion epoch time. The controller records history only when `SequenceStateMachine.advance()` returns the exact RUNNING Stage 4 -> COMPLETE transition. The counter saturates at the largest integer exactly representable by Control Bus numeric transport; timestamps never move backward. Missing/corrupt persisted values are sanitized before construction so history cannot prevent an otherwise valid controller from loading.

Fault reason is persisted independently from phase. TIMEOUT is recorded only by the validated stage-timeout callback, INTERLOCK is recorded by the level-sensitive fault input or by a Start attempt while that interlock is already active, and abort/start clear the reason back to NONE before any new transition. A loaded FAULT with missing, invalid, or NONE reason maps to UNKNOWN rather than inventing a cause.

Stage timeouts are stored per stage and default to OFF. `SequenceStageTimeouts` owns the bounded 0..72,000 tick configuration and migration from the legacy single timeout: any missing per-stage value inherits the old saved value, while a new controller starts with all four stages OFF. New saves persist all four values and update the legacy key to the maximum configured timeout as a conservative downgrade fallback.

Exactly one Bukkit delayed task can exist while RUNNING. Starting or advancing reads only the active stage's configured timeout and schedules that deadline when nonzero; advancing, aborting, faulting, unloading, or removing cancels the prior task. A timeout verifies that the expected stage is still active before transitioning to FAULT, so a stale delayed callback cannot fault a later stage. Editing the active stage timeout while RUNNING deliberately restarts that deadline; editing any other stage does not touch the current task. Reloading a RUNNING controller reconstructs a fresh full deadline for the active stage after component replay; server-offline time is not counted as a production fault. `sequence/timeout_ticks` remains the bounded runtime telemetry channel and now reports the retained/current stage's configured timeout, or 0 in IDLE.

Route edits are disallowed while RUNNING. When an output, completion, or fault-output address is edited while IDLE/COMPLETE/FAULT, the previous output is explicitly cleared before current state is published to the new route. Start-address and abort/reset-input edits reset their edge baselines and request replay. Fault-interlock edits request replay immediately so the newly selected level is applied. The controller has no repeating ticker, world scan, or chunk-loading behavior.


## Unified metric availability semantics

Inventory, fluid, machine, power, and runtime Batch pace telemetry share one controller rule: unavailable measurement data is unknown, not numeric zero.

`MetricAvailability` maps each numeric measurement channel to its domain's boolean availability channel. Factory Controller subscribes to that availability channel whenever a configured metric has one. When the bound source publishes `available=false`, only that condition's cached observation/result is cleared; source selection remains intact so normal replay can resume when the target returns.

Built-in Inventory, Fluid, and Machine Sensors publish the availability transition and stop there when unavailable. They no longer follow `available=false` with synthetic zero measurement packets that could immediately re-establish a false numeric reading.

Machine Sensor's numeric progress/process-time/ticks-remaining channels are first-class Factory Controller metrics. Batch Controller's rate/minute and ETA-seconds telemetry are appended as additional numeric metrics and both map to `batch/rate_available`. When that availability becomes false, only the affected condition's cached observation/result is invalidated and it returns to WAITING until fresh pace telemetry arrives. Production presets use those same channels for **Batch Rate <= 30/min** and **Batch ETA >= 60s**; because availability is shared, neither preset evaluates against startup/reset placeholders. Appending the metrics and presets preserves existing persisted channel-based controller configurations.

## Part 2 completion boundary

The 0.2.x development line completes the provider-neutral automation layer: persistent Control Bus topology, sensor state replay, inventory/fluid/machine/redstone/power telemetry, multi-condition Factory Controller logic, addressed routing, relays/timers, alarm operations, power-provider services, Power Grid Sensor, and hysteresis-based load shedding.

Part 2 deliberately stopped at provider-neutral power telemetry because released Rebar 0.43.0-26.2 does not expose the upstream electricity package. Part 3 has since added provider-neutral branch control, Smart Breaker, and Power Limiter without changing those stable control, telemetry, or rule contracts.


## Part 3 branch-control boundary

Read-only grid telemetry and active branch control are intentionally separate services. `PowerGridProvider` supplies network snapshots; `PowerBranchProvider` controls one branch exposed on a loaded block face.

`PowerBranchSnapshot` contains a provider-local branch identifier, display name, enabled state, and an optional finite non-negative watt limit. Providers that do not support limits must report the limit as unsupported/0. The `setPowerLimitWatts` method has a default UNSUPPORTED implementation so a switch-only provider remains valid when limiting is added later.

`ServicePowerBranchBridge` resolves the highest-priority provider dynamically through Bukkit's `ServicesManager` on every operation. GridWorks never holds a stale provider reference.

## Smart Breaker lifecycle

Smart Breaker persists desired branch state and input routing, not a provider-specific edge/node identifier. Its physical target is the adjacent block face it points at. This keeps worlds portable across provider implementation changes and avoids serializing unreleased Rebar internals.

A Smart Breaker can receive either a compact Default/A-D command circuit or an addressed boolean command. This lets Load Shedding Controller route Essential/Normal/Optional group states directly to branch breakers.

`PowerBranchDeviceManager` has no repeating task. It reconciles loaded branch devices on block activation, PowerBranchProvider registration/unregistration, and matching target-chunk load events. Target chunk unload marks the readback unavailable while keeping the desired state. When the target/provider returns, the desired state is applied before readback is accepted as synchronized.

The provider contract requires an APPLIED result to be immediately visible in a subsequent snapshot. GridWorks verifies that readback and surfaces mismatches instead of assuming the command worked.

The current upstream Rebar electricity development branch represents connections as `ElectricNetwork.Edge` objects with mutable `powerLimit` and `unidirectional` properties. A future native adapter can therefore implement Smart Breaker by translating logical open/closed state into stable edge behavior while keeping that translation out of GridWorks core.


## Power Limiter

Power Limiter is the second `PowerBranchDevice`. It uses the same target-face and provider lifecycle as Smart Breaker, but its desired state is `PowerLimitSetting`: a validated positive configured watt cap plus an enabled/bypass flag.

The provider-neutral bypass representation is `Double.MAX_VALUE`, matching the current upstream Rebar development branch's default unlimited `ElectricNetwork.Edge.powerLimit`. A provider that has no limiting capability may still implement branch switching; its default `setPowerLimitWatts` response is UNSUPPORTED.

Limiter readback uses a small relative floating-point tolerance for finite limits and exact comparison for the unlimited sentinel. APPLIED commands are verified through a fresh branch snapshot.

## Shared power-branch device lifecycle

Smart Breaker and Power Limiter implement the internal `PowerBranchDevice` contract and register with one `PowerBranchDeviceManager`. Provider registration/unregistration and target chunk load/unload events therefore have one reconciliation path and no duplicate listeners or polling tasks.


## Branch-device route-change fail-safe

Control input routing is configuration state, not an implicit command. Smart Breaker therefore resets its desired state to OPEN before changing route mode, circuit, or active address. Power Limiter resets to BYPASS before the equivalent route change. After the safe state has been reconciled to the provider, normal component state replay may establish a command from the newly selected route.

A newly placed Smart Breaker also defaults OPEN. Missing persistent desired-state data loads OPEN, which gives migrations and partially configured devices the same fail-safe baseline.


## Fluid Valve on released Rebar APIs

Fluid Valve deliberately avoids Rebar's internal `FluidManager`. It implements the public `FluidTankRebarBlock` contract, uses public fluid connection points, and lets Rebar's own segment/ticker implementation move fluid into and out of its one-bucket transit tank.

The flow policy is simple and testable: CLOSED means requested amount = 0 and supplied amount = 0; OPEN means request only compatible fluid up to remaining tank space and supply the currently stored amount. Closing does not discard buffered fluid.

The input point is on the rear face and output point on the valve's facing side. Because the valve is an endpoint bridge rather than an in-place pipe mutation, no CargoRoutes/FluidManager internals or chunk-forcing behavior are required.


## Cargo Isolator versus Pylon Cargo Gate

Pylon's existing `CargoGate` is a threshold-based splitter that alternates item batches between left and right paths. GridWorks does not duplicate that machine.

Cargo Isolator is a controlled inline shutoff. It implements the public `CargoRebarBlock` and `VirtualInventoryRebarBlock` contracts with one persisted virtual inventory slot. The inbound logistic slot dynamically rejects writes while isolated, and the block's public cargo transfer rate is set to 0 so its own outbound ticker cannot move buffered items.

When open, the configured positive transfer rate is restored. Route changes force the isolator closed before state replay. No internal `CargoRoutes` calls, cache invalidation, route scanning, or custom cargo ticker is used.


## Recipe registration

GridWorks recipes are registered through Rebar's released vanilla-shaped recipe type by converting Bukkit `ShapedRecipe` instances with `ShapedRebarRecipe.fromVanilla`. This gives Minecraft recipe registration and Rebar recipe/guide discovery one authoritative recipe definition.

Foundational recipes use vanilla materials only. Higher-tier recipes may use exact GridWorks `ItemStack` ingredients, allowing Rebar's item-aware recipe matcher to preserve custom item identity rather than accepting any vanilla item with the same base material.

`GridWorksRecipes` tracks every registered key and removes those recipes on plugin disable. All recipe keys use the GridWorks plugin namespace and a shared `gridworks` crafting-book group.


## Shared ControlInputRoute state model

Part 3 actuators no longer duplicate Circuit/Address routing state machines. `ControlInputRoute` is an immutable value containing mode, compact command circuit, and addressed command. Its nested `RouteChange` explicitly reports both structural configuration change and active-route change.

This distinction matters for physical devices. Changing an address while a device is listening to a circuit is persisted without opening/closing hardware unnecessarily. Changing the active circuit, changing an address while Address mode is active, or toggling mode reports `activeRouteChanged=true`; the owning actuator then applies its own fail-safe state before requesting component state replay.

Persistent parsing continues to use the existing conservative fallbacks: invalid/missing mode -> Circuit, invalid/missing circuit -> Default, invalid/missing address -> the node-derived fallback.


## Part 3 completion boundary

Part 3's GridWorks-owned architecture is now in place: branch switching/limiting contracts, event-driven provider lifecycle, safe Circuit/Address routing, cargo isolation, fluid isolation, and survival recipe registration all compile against released Rebar APIs.

The remaining dependency-specific work is a native Rebar electricity adapter. It must wait for a released Rebar version exposing the electricity graph/edge API. Until then, third-party electricity addons may integrate through the public Bukkit service contracts without requiring GridWorks core changes.


## Content registration integrity

`GridWorksContentCatalog` is the canonical list of player-facing item IDs. Runtime validation occurs immediately after Rebar item/recipe registration and compares the catalog with registered recipe keys and bundled English item metadata.

A JUnit source-consistency check additionally parses `GridWorksContent.java` NamespacedKey assignments and `GridWorksRecipes.java` recipe declarations. This intentionally guards procedural registration code: an item added to one subsystem but omitted from another makes CI fail rather than silently shipping an unobtainable or untranslated device.


## Runtime lifecycle rollback

`GridWorks.onEnable` delegates to a single runtime initializer wrapped in an exception boundary. Any `RuntimeException` or `Error` triggers the same cleanup routine used by normal disable and is then rethrown so Bukkit still records the real enable failure.

Cleanup is intentionally idempotent and step-isolated. One cleanup failure is logged but does not prevent later resources from being released. The sequence removes recipes and Bukkit services, closes initialized sensor/branch managers, cancels all remaining plugin-owned scheduler tasks (including per-device delayed work), unregisters listeners, closes the physical graph, clears the Control Bus, and finally nulls the static plugin instance.


## Real-server smoke validation

The `main` CI path runs unit tests first and then starts Paper 26.2 with released Rebar plus the automatically detected GridWorks project JAR using run-paper. The smoke gate waits for both the runtime content-integrity validation and Control Bus initialization messages.

The server is stopped through its console input after readiness is proven. A premature process exit, enable failure, missing readiness marker, or unclean shutdown fails the workflow. The rolling raw development JAR is published only after this smoke gate succeeds.


## Live doctor diagnostics

`/gridworks doctor` is an operator-only diagnostic surface backed entirely by existing runtime registries; it does not scan worlds or load chunks. The command reports content/recipe integrity, live and persisted Control Bus topology counts, loaded sensor/device counts, provider-service state, and pending plugin scheduler tasks.

The CI smoke server invokes this command as part of every successful main build. This verifies generated command metadata, command registration, all diagnostic manager getters, and Adventure console output in a real Paper environment. CI then performs a clean stop and rejects GridWorks error/severe log lines or explicit cleanup failures.


## Server startup smoke gate

On successful `main` candidates, CI uses the existing `run-paper` task to launch Paper 26.2 with the just-built GridWorks artifact and released Rebar dependency. One FIFO-backed integration gate requires Paper's normal `Done` marker plus GridWorks' content-integrity and Control Bus initialization messages, then executes `gridworks doctor` and requires its completion marker.

The server is stopped through its normal console command and must terminate within 30 seconds; otherwise CI terminates the process and fails the build. Fatal GridWorks startup/shutdown log patterns, missing readiness markers, doctor failure, Gradle failure, or abnormal shutdown all block development release publication.


## Doctor invariants

The live doctor is part of the release gate, so completion alone is not treated as health. It verifies that the registered recipe list exactly matches the canonical content catalog, Bukkit's `ControlBus` service resolves to the current live bus, the inventory/fluid/machine/power shared sampler tasks are still scheduled, and the static GridWorks instance matches the enabled plugin.

Provider availability, loaded node/sensor/device counts, and pending task totals are diagnostic context rather than pass/fail criteria. This keeps a normal installation without an electricity provider healthy while still catching internal lifecycle corruption.


## CI trust boundary

Repository write permission is not exposed to the job that compiles or executes project code. The build/smoke job has `contents: read`; only the dependent main-branch publication job receives `contents: write`, after the tested job succeeds.

The publication job checks out the workflow event SHA explicitly and rebuilds only the raw JAR from that already-tested source revision. Same-ref workflow concurrency cancels superseded runs, preventing stale successful runs from racing a newer commit and rolling the development release backward.


## Validated runtime settings

Runtime numeric settings are parsed once during enable into immutable `GridWorksSettings`. Missing keys use backward-compatible defaults, while explicitly present values must be positive integral numbers representable by the target Java type.

No arbitrary upper gameplay cap is imposed beyond the destination type's range. This avoids silently rewriting administrator intent while still rejecting values that cannot be represented or that would produce invalid zero/negative scheduling and propagation behavior. Runtime code consumes the immutable settings snapshot rather than repeatedly querying mutable YAML.


## Rebar public-API compatibility guard

A source-level CI test protects the architectural adapter boundary. Production Java may not reference Rebar's internal `FluidManager`, internal `CargoRoutes`, internal `TickingRebarBlock.isTicking` helper, or the currently unreleased electricity package.

The guard also checks that the pinned Rebar artifact is release-shaped, targets the same Minecraft line as GridWorks, and remains `compileOnly`. This turns the public-API policy into a regression constraint instead of relying on code-review memory.


## Reproducible raw-JAR release chain

Archive tasks disable source-file timestamps and use reproducible entry ordering. The smoke job exports the SHA-256 of its post-smoke GridWorks JAR. The write-capable publication job rebuilds from the exact event SHA and must reproduce that digest before modifying the rolling release.

After upload, GitHub's own release-asset digest is read back and compared with the verified local SHA-256. This closes the gap between “same tested source” and “same tested binary” without changing the user-facing raw-JAR delivery.


## Packaged artifact integrity gate

After Gradle's normal build, CI inspects the actual GridWorks JAR before launching Paper. Required runtime resources/classes and generated plugin metadata are checked directly from the archive, including command/permission/dependency metadata.

The distributed JAR embeds the project GPLv3 license under `META-INF/LICENSE-GridWorks`. The same gate rejects package entries from Rebar, Paper/Bukkit, and InvUI namespaces so provided dependencies cannot be accidentally shaded into the addon.


## Paper/Rebar runtime smoke gate

The main-branch CI gate now goes beyond compilation. `runServer` boots Paper 26.2 with the released Rebar dependency and the current GridWorks JAR. GridWorks logs a dedicated successful-enable marker only after its complete runtime initializer—including content registration/validation and command registration—has finished.

The workflow waits up to 180 seconds for that marker, checks for fatal enable/classloading failures, sends a normal Paper `stop` command through run-paper's forwarded standard input, and requires shutdown within 60 seconds. Development-release publication occurs only after this smoke gate succeeds.
