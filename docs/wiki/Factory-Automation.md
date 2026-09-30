# Factory Automation

## Factory Controller

The Factory Controller converts sensor telemetry into a boolean decision.

It supports:

- Condition A;
- optional Condition B;
- AND or OR;
- metric selection;
- comparison operator;
- finite threshold;
- automatic or explicit linked-source selection;
- Default/A-D or named-address output;
- player-defined controller names;
- reusable starting presets.

## Example: require stock and fluid

```text
Inventory Sensor -----\
                       > Factory Controller (AND) ---> process_ready
Fluid Tank Sensor ----/
```

The controller can require both conditions before publishing the command.

## Addressed output

For large factories, named output is often clearer than a shared circuit.

Examples:

- `process_ready`
- `ore_line_1`
- `tank_fill_enable`
- `optional_loads`

Multiple receivers can intentionally listen to one address.

## Stock Controller

The [[Stock Controller]] is a purpose-built inventory regulator with hysteresis.

Unlike a single Factory Controller comparison, it has separate **start-low** and **stop-high** thresholds. Once a refill line starts, it remains ON through the middle band until the high target is reached. Once stopped, it remains OFF until stock falls back to the low target.

```text
Inventory Sensor ---> Stock Controller ---> production_enable
        256 low           |
       1024 high          +--> ON while refilling
```

That makes it ideal for self-maintaining material buffers without rapid ON/OFF cycling.

## Factory Monitor

The Factory Monitor displays Control Bus telemetry without directly polling machines.

Current pages include:

- Automation
- Resources
- Production
- Power & Alarms

It can display an overview or individual source nodes.

## Alarm automation

Alarm Indicators latch real false-to-true fault events until they are safely acknowledged/cleared.

They support:

- custom names;
- Info, Warning, or Critical severity;
- optional one-level timed escalation;
- occurrence count;
- last-trigger timestamp;
- replay-safe history.

The Alarm Console aggregates alarms across the currently loaded Control Bus component and supports per-alarm or Acknowledge All actions.

For production targets and multi-step workflows, continue with [[Production Control]].
