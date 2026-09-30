# Performance and Optimization

GridWorks is designed so **hundreds or thousands of placed devices can exist on one server** without every device owning a repeating task or every sensor population waking on the same tick.

The important distinction is **placed devices vs one giant live Control Bus component**. Thousands of devices distributed across factories are a normal design target. A single component containing thousands of receivers can still be expensive because a broadcast command intentionally has to consider that component.

## Event-driven first

Where the upstream API exposes a useful event, GridWorks prefers it.

Examples include:

- redstone sensing;
- topology/provider lifecycle;
- Control Bus propagation;
- branch-device reconciliation;
- Batch/Sequence production state;
- alarms and controller outputs.

## Distributed shared samplers

Inventory, fluid, machine, and provider-neutral power state do not have one universal event source across every supported target.

GridWorks therefore uses **one round-robin sampler per loaded sensor type**.

Sensors are distributed across ticks instead of all waking on the same scheduler tick.

Current defaults:

| Sensor | Desired sweep | Max probes/tick |
| --- | ---: | ---: |
| Inventory Sensor | 10 ticks | 128 |
| Fluid Tank Sensor | 10 ticks | 128 |
| Machine Sensor | 20 ticks | 64 |
| Power Grid Sensor | 20 ticks | 32 |

A newly loaded sensor receives initial sample credit, but the per-tick ceiling still applies. If a population is too large to meet the desired interval, GridWorks stretches the effective sweep instead of violating the tick budget.

Approximate minimum sweep time under overload:

```text
ceil(loaded sensors / max-samples-per-tick)
```

Examples:

- 1,000 Inventory Sensors: 100 probes/tick to maintain the 10-tick target.
- 2,000 Inventory Sensors: capped at 128/tick, roughly a 16-tick sweep.
- 5,000 Machine Sensors: capped at 64/tick, roughly a 79-tick sweep.
- 1,000 Power Grid Sensors: capped at 32/tick, roughly a 32-tick sweep.

Snapshots still publish only when they change.

### No-provider power behavior

If no power provider is registered, Power Grid Sensors perform only a bounded transition sweep to publish unavailable state and then stop probing. They resume with a bounded full sweep when a provider becomes available.

## Batched snapshot publication

One sensor sample may contain several fields.

For example, an Inventory Sensor can publish availability, total items, occupied slots, total slots, and fill ratio.

GridWorks now sends those fields through `ControlBus.publishBatch`. The graph resolves the recipient route **once for the whole snapshot** rather than rebuilding a candidate list for every field.

The public API keeps a default fallback so third-party Control Bus implementations remain source-compatible.

## Cached Control Bus routes

The live graph caches bounded breadth-first dispatch routes per source while topology is unchanged.

A sensor repeatedly publishing changed telemetry therefore does not repeat graph traversal for every signal.

A real link/unlink/unload mutation invalidates the route caches. Merely registering a new isolated node does not invalidate unrelated hot routes.

The existing `control-bus.max-propagation-nodes` safety cap still bounds the first route build and every cached dispatch.

## Indexed persistent topology

`control-network.txt` remains the durable link source, but runtime neighbor/component lookup now uses an in-memory adjacency index.

This changes common topology operations from repeated whole-link scans to graph-local work:

- neighbor lookup: proportional to node degree;
- component traversal: proportional to the component's nodes + edges;
- component edge count: proportional to adjacency inside that component.

The file format is unchanged.

## Coalesced state replay

Chunk activation and route changes request current-state replay.

In production, replay requests within the same tick are coalesced. Hundreds of blocks loading together therefore create **one replay pass per resulting loaded component**, rather than hundreds of overlapping component-wide passes.

Sensor state replay never forces an unscheduled target probe. If a sensor has not received its bounded sampler turn yet, consumers remain fail-safe until current telemetry arrives.

## Coalesced monitor/console UI work

Factory Monitor batches window notifications to one scheduled refresh per tick instead of notifying GUI items for every field in one sensor snapshot.

Alarm Console similarly coalesces alarm-state changes before rebuilding/sorting its visible alarm list.

This keeps rich operator UIs from multiplying backend telemetry cost.

## No ordinary world scans

Normal automation does not scan the world to locate devices.

Physical links are persistent and loaded topology reconnects from known state.

## No forced chunk loading

GridWorks does not force-load another chunk merely because a sensor or actuator wants to inspect a target.

Unavailable targets become unavailable state until the normal server lifecycle makes them available again.

## Control Bus propagation cap

Default:

`control-bus.max-propagation-nodes: 4096`

This protects one dispatch from unbounded work in malformed or unexpectedly large graphs.

It is a safety cap, **not a target component size**.

For very large servers, prefer several logical factory components over one server-wide Control Bus component. Named addresses are multicast inside a component; they are not intended to turn every GridWorks block on the server into one broadcast domain.

## Tuning sensor budgets

For most servers, adjust **max samples per tick before lowering the desired interval**.

If tick spikes occur:

1. use `/gridworks doctor`;
2. check loaded sensor counts;
3. check the reported estimated sweep ticks;
4. lower the relevant `max-samples-per-tick` gradually;
5. accept a longer telemetry sweep instead of concentrating work into one tick.

If faster response is genuinely required and profiling shows spare headroom, increase the budget or lower the desired interval carefully.

## Production controllers

Batch Controller derives production rate/ETA from progress events instead of adding another sampler. Parallel progress updates are burst-coalesced.

Sequence Controller is event-driven. Only the active stage can own one optional one-shot timeout task.

Stock Controller consumes Inventory Sensor telemetry and owns no sampler of its own.

## /gridworks doctor

Use:

`/gridworks doctor`

to inspect:

- loaded sensor counts;
- estimated sensor sweep times and per-tick budgets;
- active Control Bus nodes/links;
- pending GridWorks scheduler tasks;
- active settings;
- provider availability.

## Large-server design guidance

For hundreds/thousands of devices:

- keep physically unrelated factories in separate Control Bus components;
- avoid attaching one Factory Monitor to an enormous server-wide component unless global telemetry is actually needed;
- use addressed outputs within a factory instead of one universal component;
- leave sensor budgets bounded;
- prefer Stock/Batch/Sequence controllers over rapid redstone clocking;
- profile provider callbacks separately from GridWorks itself.

## Related pages

- [[Configuration]]
- [[Commands and Permissions]]
- [[Troubleshooting]]
