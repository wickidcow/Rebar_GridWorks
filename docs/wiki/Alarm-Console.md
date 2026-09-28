# Alarm Console

The **Alarm Console** is the central GridWorks alarm-management interface.

It aggregates [[Alarm Indicator]] telemetry across the currently loaded Control Bus component.

## What it shows

The console groups alarm state by source UUID and prioritizes important entries.

Latched/unacknowledged faults are sorted first, then severity is considered.

The current interface displays up to 18 alarms at once.

## Filters

The persisted filter cycles through:

- All
- Warning+
- Critical only
- Latched only
- Unacknowledged only

## Acknowledgement

The console supports:

- acknowledging one alarm;
- **Acknowledge All**.

Acknowledgement is sent as a Control Bus command rather than by reaching directly into another block's implementation.

The command can target:

- one alarm UUID;
- `*` for all appropriate alarms.

## Refresh behavior

Refresh:

- prunes sources no longer in the loaded Control Bus component;
- requests normal state replay;
- does not scan the world;
- does not force chunks to load.

## Example

```text
Alarm Indicator: "Low Coolant" ----\
Alarm Indicator: "Line Jam" --------+--> Alarm Console
Alarm Indicator: "Grid Overload" --/
```

## Related pages

- [[Alarm Indicator]]
- [[Factory Monitor]]
- [[Factory Automation]]
