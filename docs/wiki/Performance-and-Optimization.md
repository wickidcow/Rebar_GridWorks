# Performance and Optimization

GridWorks is designed to make large automation systems practical without creating one repeating task for every device.

## Event-driven first

Where the upstream API exposes a useful event, GridWorks prefers it.

Examples include:

- redstone sensing;
- topology/provider lifecycle;
- Control Bus propagation;
- branch-device reconciliation.

## Shared samplers

Some supported systems do not expose one universal change event.

GridWorks therefore uses **one shared sampler per loaded sensor type** rather than one repeating scheduler task per placed sensor.

Current defaults:

| Sensor | Interval |
| --- | ---: |
| Inventory Sensor | 10 ticks |
| Fluid Tank Sensor | 10 ticks |
| Machine Sensor | 20 ticks |
| Power Grid Sensor | 20 ticks |

Snapshots publish only when they change.

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

Do not raise it simply because a factory has many machines. First verify that a real propagation-cap issue exists.

## Tuning sensor intervals

If a server needs lower sensor overhead, increase the relevant shared sample interval gradually.

Avoid dramatically lowering intervals for a more responsive GUI. Even change-only publication still requires the measurement itself to occur.

## Production controllers

Batch Controller derives production rate/ETA from progress events instead of adding another sampler.

Sequence Controller is event-driven; its only scheduler use is the optional single stage-timeout task while running.

## /gridworks doctor

Use:

`/gridworks doctor`

to inspect:

- loaded sensor counts;
- active Control Bus nodes/links;
- pending GridWorks scheduler tasks;
- active settings;
- provider availability.

## Performance troubleshooting checklist

If GridWorks appears expensive:

1. Run `/gridworks doctor`.
2. Count loaded sensors by type.
3. Check custom sample intervals.
4. Look for unusually large Control Bus components.
5. Verify another addon/provider is not doing expensive work inside a GridWorks provider callback.
6. Compare server profiling before and after the specific automation is loaded.

## Related pages

- [[Configuration]]
- [[Commands and Permissions]]
- [[Troubleshooting]]
