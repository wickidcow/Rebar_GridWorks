# Factory Monitor

The **Factory Monitor** is GridWorks' central live telemetry display.

It observes Control Bus state without directly polling every machine itself.

## Current pages

The monitor is organized into bounded pages:

- **Automation**
- **Resources**
- **Production**
- **Power & Alarms**

Each page can show an overview or focus on individual source nodes.

## How it works

Devices such as sensors, alarms, and production controllers publish telemetry onto the Control Bus.

The Factory Monitor consumes that existing state:

```text
Machine Sensor -----\
Fluid Sensor --------+--> Control Bus --> Factory Monitor
Alarm Indicator -----+
Batch Controller ----/
```

This avoids creating a second machine-polling system just for the display.

## Refresh behavior

Refresh:

- removes sources that are no longer part of the currently loaded component;
- requests normal state replay from loaded state sources;
- does **not** scan the world;
- does **not** force chunks to load.

## Useful with

Factory Monitor becomes especially valuable in networks containing:

- multiple Machine Sensors;
- Batch Controllers;
- Sequence Controllers;
- Alarm Indicators;
- Power Grid Sensors;
- Factory Controllers.

## Troubleshooting

If a source is missing from the monitor:

1. Verify both devices are loaded.
2. Inspect the Control Bus with the GridWorks Linker.
3. Confirm the source is actually linked into the same live component.
4. Refresh the monitor.
5. Check whether the source reports unavailable.

## Related pages

- [[Factory Controller]]
- [[Machine Sensor]]
- [[Batch Controller]]
- [[Alarm Indicator]]
- [[Power Grid Sensor]]
