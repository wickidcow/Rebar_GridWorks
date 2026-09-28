# Load Shedding Controller

The **Load Shedding Controller** automatically manages power consumers in three priority tiers.

It is designed to shed less-important loads before critical factory infrastructure.

## Tiers

The controller publishes three independently editable addressed outputs:

- **Essential**
- **Normal**
- **Optional**

Optional loads are shed first.

Under severe grid stress, Normal and Optional loads are disabled while Essential remains enabled.

## Telemetry source

The controller binds to one directly linked [[Power Grid Sensor]].

It waits for the sensor's complete `power/sample_revision` marker before evaluating a changed snapshot.

This prevents a decision from being made halfway through a multi-channel power update.

## Default hysteresis

Default behavior:

| Event | Threshold |
| --- | ---: |
| Shed Optional | 90% load |
| Restore Optional | 80% load |
| Severe shedding | 100% load or any unpowered consumer |
| Restore Normal | 90% load with no unpowered consumers |

Hysteresis prevents rapid on/off cycling around one threshold.

## Editing thresholds

All four hysteresis thresholds are configurable from the GUI.

Edits are constrained so an invalid threshold ordering cannot be created.

Corrupt or incomplete persisted threshold state falls back to the safe default set atomically.

## Missing telemetry fail-safe

Choose how the controller behaves if power telemetry disappears:

- **Essential Only** — default
- **Allow All**
- **Hold Last**

The last hysteresis stage persists across reloads.

## Typical network

```text
Power Grid Sensor
       |
       v
Load Shedding Controller
   |       |        |
   |       |        +--> optional_loads ---> Smart Breakers
   |       +-----------> normal_loads ----> Smart Breakers
   +-------------------> essential_loads -> Smart Breakers
```

## Related pages

- [[Power Grid Sensor]]
- [[Smart Breaker]]
- [[Power Limiter]]
- [[Power and Flow Control]]
