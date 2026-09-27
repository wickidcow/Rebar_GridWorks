# Rebar GridWorks

**Industrial automation, smart power management, and factory control systems for Pylon/Rebar.**

GridWorks is a Rebar addon focused on making factories smarter rather than simply making machines faster. Its long-term goal is to connect sensors, controllers, power systems, cargo, fluids, machines, and redstone through a common automation layer.

> **Part 2 automation foundation is complete on the 0.2.x development line.** The Control Bus, persistent physical network, redstone I/O, inventory/fluid/machine/power sensing, controllers, addressed routing, alarms, load shedding, presets, and Factory Monitor are implemented. Native Rebar electricity switching remains upstream-blocked until its electricity API ships in a released dependency.

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

### Machine sensing

The **Machine Sensor** faces an adjacent loaded Rebar block and uses the released `ProcessorRebarBlock` and `RecipeProcessorRebarBlock` contracts. It publishes:

- `gridworks:machine/available`
- `gridworks:machine/kind`
- `gridworks:machine/processing`
- `gridworks:machine/progress`
- `gridworks:machine/process_time_ticks`
- `gridworks:machine/ticks_remaining`

Progress is normalized to **0.0 → 1.0 completed** for both processor styles, rather than leaking the different raw progress semantics of the two Rebar interfaces. Idle machines publish zero process measurements. Unsupported blocks publish unavailable.

All loaded Machine Sensors share one configurable sampler (1 second by default) and publish only when the snapshot changes. GridWorks deliberately does **not** use Rebar's internal `TickingRebarBlock.isTicking` helper as a proxy for “machine running”; scheduled ticking is not the same thing as active processing.

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

Controllers can also use `gridworks:redstone/strength` as a numeric 0–15 input, so analog vanilla redstone can participate in threshold logic. The controller metric list now also understands the stable power telemetry contract: capacity, demand, reserve, load ratio, powered-consumer ratio, and unpowered-consumer count.

Machine telemetry is also available as numeric controller metrics for **progress**, **process time**, and **ticks remaining**, with a **Machine >= 90%** preset for completion-driven automation. Power-aware presets remain **Power Load >= 90%**, **Power Shortage**, and **Load Shed Trigger**.

Inventory, fluid, machine, and power measurements all respect their domain availability channel. If a bound sensor reports unavailable, the controller discards that cached measurement and returns to three-state `WAITING` instead of interpreting unavailable as a legitimate numeric zero.

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

The Factory Monitor is a passive diagnostic node. It displays the latest built-in redstone, command-circuit, inventory, fluid, machine-processing, power, controller-output, and alarm-state signals seen on its loaded Control Bus component, including the source node ID and signal sequence. It does not poll machines itself; Machine Sensors own their shared sampler.

Stateful devices implement `ControlStateSource`. When loaded topology expands, GridWorks replays current state from every loaded state source in that component. This means a monitor or actuator joining through an intermediate Control Interface receives current state even when the original sensor is several hops away.

Direct-link lifecycle is symmetric: controllers are notified when a bound peer unloads or is explicitly unlinked. Cached measurements from that source are invalidated immediately, so a missing sensor cannot leave automation running forever on stale data.

## Threading safety

Control Bus callbacks deliberately run on the publisher's thread. Physical GridWorks nodes therefore marshal all Bukkit world mutations and InvUI refreshes onto the primary server thread and verify the node is still active before delayed work executes. This lets third-party addons publish asynchronously without making GridWorks actuators touch unloaded chunks or Bukkit state off-thread.

## Power-grid bridge status

GridWorks now has a provider-neutral `PowerGridSnapshot` model, an internal `PowerGridBridge`, and a public `PowerGridProvider` Bukkit service contract. The released Rebar dependency (`0.43.0-26.2`) does **not** contain the electricity API currently being developed on Rebar's `seggan/feature/elektrikity` branch, so GridWorks does not compile against unreleased classes.

The neutral snapshot already defines the measurements GridWorks needs: node/producer/consumer counts, powered/unpowered consumers, production capacity, demand, load ratio, reserve watts, and powered-consumer ratio. `PowerGridTelemetry` maps those values onto stable `gridworks:power/*` Control Bus channels, and the Factory Monitor already has bounded display slots for the most useful power signals.

Any addon can register `PowerGridProvider` through Bukkit's `ServicesManager`; Bukkit's normal service priority selects the active provider. That gives third-party electricity systems an integration path now, while a future native Rebar adapter can implement the same contract later.

### Power Grid Sensor

The **Power Grid Sensor** is provider-neutral and faces one adjacent loaded block. If the active `PowerGridProvider` associates that block with a grid, the sensor publishes the stable `gridworks:power/*` telemetry set. A single shared sampler handles all loaded Power Grid Sensors, defaults to once per second, and publishes only when the snapshot changes.

Power metrics are emitted in deterministic order and each changed snapshot ends with `gridworks:power/sample_revision`. Multi-channel consumers can therefore wait for the revision marker before evaluating a complete sample instead of reacting to intermediate field updates.

When the provider disappears, the target chunk unloads, or the target is no longer a known power grid, the sensor publishes only `power/available=false`. Power-aware Factory Controller conditions bound to that sensor immediately discard their previous numeric observation and return to `WAITING`, preventing stale power data from holding a load-shed command ON or OFF.

## Smart-grid automation

The load-shedding core uses hysteresis with three stages: Normal, Shed Optional, and Shed Normal + Optional. Default thresholds shed optional loads at 90% load and restore them at 80%; severe shedding begins at 100% load or whenever the provider reports any unpowered consumer, and normal loads recover only after load falls to 90% with no unpowered consumers. Essential loads are never disabled by the policy.

### Load Shedding Controller

The **Load Shedding Controller** turns that policy into player-facing automation. It binds to one directly linked power telemetry source and waits for the sensor's `power/sample_revision` marker before evaluating, so it never makes a decision from a half-updated snapshot.

It publishes three independently editable addressed outputs: **Essential**, **Normal**, and **Optional**. Optional loads are disabled first; severe grid stress disables both Normal and Optional while Essential remains enabled. Tier addresses must be distinct, and moving an address explicitly clears the old route before applying the current tier state to the new route.

If telemetry disappears, the controller uses an explicit player-selected fail-safe mode: **Essential Only** (default), **Allow All**, or **Hold Last**. The last hysteresis stage is persisted so Hold Last and recovery behavior survive reloads.

All four hysteresis thresholds are configurable in the controller GUI: optional shed/restore and normal shed/restore. Normal clicks adjust by 5 percentage points and shift-clicks by 25. Edits clamp against neighboring thresholds so an invalid hysteresis ordering cannot be created. Corrupt or partial persisted threshold data falls back atomically to the safe defaults instead of mixing old and new values.

Saved tier routes are also repaired through a deterministic collision-safe route set. Even if legacy/corrupt PDC contains duplicate addresses—or one duplicate happens to equal the first generated fallback—the controller generates a distinct replacement before it can publish conflicting Essential/Normal/Optional states.

The remaining native-electricity step is the actual Rebar adapter once its electricity API lands in a released dependency. Power sensing, addressed load groups, controller metrics, load shedding, Smart Breaker, and Power Limiter are already implemented behind provider-neutral contracts.

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


## Part 3 — Smart electrical branch control

The **0.3.x development line** begins the native-electricity control layer without coupling GridWorks core to Rebar's unreleased electricity package.

GridWorks now exposes a second Bukkit service contract, `PowerBranchProvider`, for controllable electrical branches. A provider resolves a branch on a specific loaded block face and can apply an idempotent open/closed state. The snapshot model also reserves a validated optional watt-limit value so the future Power Limiter can use the same provider boundary.

### Smart Breaker

The **Smart Breaker** faces one adjacent provider-exposed electrical branch. `true` means the branch is closed/enabled; `false` means open/disabled. It can listen to Default/A-D command circuits or a human-readable addressed command, so the existing Load Shedding Controller can drive groups of Smart Breakers directly without converting through vanilla redstone.

The desired branch state is persisted. If the provider or target chunk is temporarily unavailable, the desired command is retained and reapplied when it returns. Provider registration/unregistration and target chunk load/unload are handled with Bukkit events; there is no Smart Breaker polling task.

The current released Rebar build still does not provide `PowerBranchProvider`. The future native adapter can map this contract onto Rebar's developing `ElectricNetwork.Edge` model, where connections already expose mutable per-edge power limits. GridWorks does not compile against that development branch.


### Power Limiter

The **Power Limiter** uses the same `PowerBranchProvider` target model as Smart Breaker but controls the branch's watt cap instead of open/closed state. It starts in **BYPASS** so placing one cannot unexpectedly throttle a grid.

The configured cap is persisted and adjustable from 1 W through 1 TW. A Control Bus boolean chooses whether the cap is active: `true` applies the configured limit and `false` requests provider-neutral unlimited/bypass (`Double.MAX_VALUE`). Circuit and addressed input routing match Smart Breaker.

Providers may support switching without supporting limits; the limiter surfaces that capability cleanly rather than pretending the command worked. Smart Breaker and Power Limiter now share one event-driven `PowerBranchDeviceManager` for provider hot-plug and target-chunk lifecycle.


### Fail-safe input-route changes

Smart Breaker and Power Limiter now clear transient command state before switching their Control Bus input route. Changing a breaker from one circuit/address to another first requests **OPEN**; changing a limiter route first requests **BYPASS**. Component state replay then establishes the new route's actual command if a state source exists.

New Smart Breakers also start OPEN rather than automatically closing an adjacent branch on placement. This prevents configuration work or a missing upstream command from energizing a branch implicitly.


### Fluid Valve

The **Fluid Valve** is implemented entirely on released Rebar fluid interfaces; it does not call the internal `FluidManager`. It creates a public fluid input on its rear face and output on its facing side, backed by a one-bucket transit tank.

When OPEN, the valve requests compatible fluid up to its remaining buffer space and exposes stored fluid to the output network. When CLOSED, it requests and supplies exactly 0 mB while preserving whatever fluid is already inside. This gives the valve a real isolation behavior without disconnecting or rewriting Rebar's pipe graph.

Fluid Valve accepts Default/A-D circuits or an addressed boolean command and defaults CLOSED. Input-route changes close it before normal state replay, matching the fail-safe behavior of Smart Breaker and Power Limiter.


### Cargo Isolator

Pylon already has a **Cargo Gate**, but that machine is a threshold-driven left/right splitter. GridWorks therefore uses the distinct **Cargo Isolator** name for its automation-controlled shutoff device.

Cargo Isolator is a one-stack public Rebar cargo buffer with rear input and facing-side output. OPEN uses the configured cargo transfer rate. ISOLATED rejects new inbound writes at the public logistic slot and sets its own outbound cargo transfer rate to zero, so items do not cross either side while closed. A stack already inside remains buffered safely.

Like Fluid Valve, it accepts Default/A-D circuits or an addressed boolean command and fails safe ISOLATED when its input route changes. The implementation uses `CargoRebarBlock`, `VirtualInventoryRebarBlock`, and `VirtualInventoryLogisticSlot`; it never calls the internal `CargoRoutes` cache.


## Survival crafting and recipe visibility

GridWorks now registers shaped crafting recipes through Rebar's `RecipeType.VANILLA_SHAPED` rather than only registering item schemas. That makes the automation set craftable in survival and visible through Rebar's normal recipe/guide machinery.

The progression intentionally starts with vanilla copper/redstone/quartz components for Control Interfaces, the Linker, basic sensors, lights, and relays. Advanced devices then consume earlier GridWorks components: timed/addressed relays build from Control Relay, Factory Controller/Monitor build from Control Interface, alarm infrastructure builds from Status Light/Control Interface, and the Part 3 power/flow devices build from the established control components.

Recipes are registered under the `gridworks` crafting-book group and are explicitly removed from Rebar's recipe type when GridWorks disables, avoiding stale recipe registrations during plugin lifecycle changes.


### Shared actuator input routing

Smart Breaker, Power Limiter, Fluid Valve, and Cargo Isolator now use one immutable `ControlInputRoute` model for Circuit vs Address routing. The model owns safe parsing of persisted mode/circuit/address values, active-channel selection, circuit cycling, address replacement, and whether a configuration edit actually changes the active input route.

Each physical actuator still owns its domain-specific fail-safe action, but it only applies that action when `RouteChange.activeRouteChanged()` is true. Editing an inactive saved address therefore no longer causes unnecessary physical switching, while toggling route mode or changing the active circuit/address always triggers the correct safe state before replay.


## Part 3 implementation boundary

The current 0.3.x development line now includes the provider-neutral electrical branch-control API, Smart Breaker, Power Limiter, released-API Fluid Valve, released-API Cargo Isolator, unified actuator input routing, and survival crafting recipes for the full current GridWorks set.

The intentionally unresolved piece is the **native Rebar electricity adapter**. Rebar 0.43.0-26.2 still does not ship the electricity package being developed upstream, so GridWorks will not compile against those unreleased classes. When that API is released, the adapter can implement the existing `PowerGridProvider` and `PowerBranchProvider` contracts without redesigning sensors, controllers, breakers, limiters, or load shedding.


## Development build download

Successful builds from `main` publish a rolling **GridWorks Development Build** prerelease. The release asset is the raw `.jar` file itself (for example `Rebar_GridWorks-0.3.0-SNAPSHOT.jar`), so server owners can download it directly without unpacking a GitHub Actions ZIP.

Pull-request builds still run the full compile/test gate but do not publish a downloadable development JAR.


## Content integrity checks

GridWorks now has a canonical catalog for all 21 player-facing content IDs. Startup validates that every catalog entry has a registered survival recipe and bundled English item metadata; a mismatch stops enablement with the exact missing/extra IDs instead of leaving a partially usable addon.

CI also source-checks the actual `NamespacedKey` registrations, recipe declarations, and `lang/en.yml` item keys against that same catalog. Adding a future machine while forgetting its recipe or language entry therefore fails tests before the rolling development JAR is published.


## Deterministic startup rollback

GridWorks now owns its partial-startup cleanup instead of relying solely on the server to invoke disable after an enable exception. If config loading, network persistence, content registration, recipe validation, or any later initialization step fails, GridWorks immediately rolls back registered recipes/services/listeners, closes every initialized sensor/device manager, cancels remaining plugin scheduler tasks, clears the live physical/control graphs, and releases the static plugin instance before rethrowing the original failure.

Normal plugin disable uses the same idempotent cleanup path, so cleanup behavior does not drift between failed startup and ordinary shutdown.


## Real-server CI smoke gate

Successful `main` builds now boot a real Paper 26.2 test server through run-paper with released Rebar and the just-built GridWorks JAR before the rolling raw JAR is published. CI requires GridWorks to complete its 21-entry content/recipe validation and reach the Control Bus initialized state, then shuts the server down cleanly.

This catches enable-time API/linkage problems, Rebar registration failures, bundled-language/recipe validation failures, and startup lifecycle regressions that unit tests alone cannot see. Pull requests continue to run compile/unit tests without publishing or replacing the development JAR.


## `/gridworks doctor`

Operators with `gridworks.admin` can run `/gridworks doctor` for a compact live health snapshot. It reports the plugin version, registered recipe/content count, active Control Bus nodes/live links/persisted links, loaded inventory/fluid/machine/power sensors, power-grid and branch-provider availability, loaded branch-control devices, and the number of pending GridWorks scheduler tasks.

The real-server CI smoke test now executes the doctor command after startup and requires it to complete before shutdown. After `stop`, CI also scans the log for GridWorks cleanup/disable errors before the raw development JAR is allowed to publish.


The rolling `dev-build` release metadata is updated to the exact smoke-tested commit on every successful publish, in addition to moving the tag and replacing the raw JAR. This keeps the release page, tag target, build provenance, and downloadable file aligned.


## Paper startup smoke test

Successful `main` builds now go beyond compilation and unit tests: CI starts a real Paper 26.2 server through `run-paper`, loads the freshly built GridWorks JAR alongside Rebar, waits for Paper's normal `Done` marker, verifies GridWorks' 21-entry content validation and Control Bus initialization, executes the live `gridworks doctor` command, and then requires a clean shutdown.

The rolling raw development JAR is published only after this single integration gate succeeds. Pull requests still run the compile/unit-test gate without publishing a development JAR. The smoke gate catches classloading, plugin-enable, Rebar registration, bundled-resource, recipe-registration, command, scheduler/listener startup, and shutdown failures that ordinary unit tests cannot exercise.


## Doctor health result

`/gridworks doctor` now distinguishes informational telemetry from runtime invariants. The command reports PASS/FAIL checks for exact recipe/catalog registration, the live Bukkit `ControlBus` service identity, all four shared sensor samplers, and the enabled GridWorks singleton. Power-provider absence remains informational because electricity integrations are optional.

CI requires the explicit `GridWorks Doctor result: PASS` marker before it accepts the live Paper smoke test or publishes the rolling raw JAR.


## CI permission and release-race hardening

The compile/test/smoke job now runs with **read-only repository contents permission**, including pull requests. Raw release publication is isolated in a second job with `contents: write`, and that job only runs for successful `main` pushes or manual runs after the full build + Paper smoke gate has passed.

Workflow concurrency also cancels superseded runs for the same ref. An older build can no longer finish late and move the rolling `dev-build` tag/assets backward over a newer tested commit.


## Configuration validation

GridWorks now loads its runtime numeric settings through one validated `GridWorksSettings` snapshot. Existing configs that predate a key remain compatible because a missing value uses the documented default. If an administrator explicitly supplies a non-numeric, fractional, zero, negative, or out-of-range integer value, startup fails with the exact config path instead of silently clamping the mistake.

The validated snapshot drives the Control Bus propagation cap, all four shared sensor sampling intervals, and Cargo Isolator transfer rate. `/gridworks doctor` prints the active snapshot so the live values are visible without rereading YAML.


## Released Rebar API boundary

CI now exhaustively scans every production Java source for the Rebar internals GridWorks has deliberately avoided: internal `FluidManager`, internal `CargoRoutes`, the internal `TickingRebarBlock.isTicking` helper, and the unreleased `io.github.pylonmc.rebar.electricity` package.

The same regression test requires `rebar.version` to be a released artifact on the same Minecraft line as `minecraft.version` and keeps Rebar as a `compileOnly` server dependency. When Rebar eventually releases electricity publicly, this guard is the intentional place to review and update that boundary rather than quietly binding to a development branch.


## Byte-for-byte development release verification

GridWorks archive tasks are reproducible: archive entry order is fixed and source file timestamps are not preserved. After the live Paper smoke test, CI records the SHA-256 of the exact JAR that was exercised. The isolated publisher rebuilds from the exact tested commit and refuses to publish unless its JAR has the identical SHA-256.

After upload, CI reads GitHub's release-asset digest back and requires it to match the same verified raw JAR. The rolling `dev-build` asset is therefore byte-for-byte tied to the binary that passed the live server gate, while the user-facing download remains a raw `.jar`.
