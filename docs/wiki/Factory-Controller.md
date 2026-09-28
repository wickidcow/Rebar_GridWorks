# Factory Controller

The **Factory Controller** is GridWorks' general-purpose logic block. It turns sensor telemetry into a boolean command that can drive relays, valves, breakers, isolators, alarms, and other automation devices.

## What it can do

A Factory Controller supports:

- **Condition A**
- optional **Condition B**
- **AND** or **OR**
- selectable metric
- comparison operator
- numeric threshold
- automatic or explicit source selection
- Default/A-D circuit output
- named addressed output
- player-defined controller name
- reusable starting presets

## Basic setup

A simple controller network looks like:

```text
Sensor ---> Factory Controller ---> Actuator
```

Example:

```text
Inventory Sensor ---> Factory Controller ---> Addressed Relay
```

1. Place and configure the sensor.
2. Link the sensor to the Factory Controller with the [[GridWorks Linker|Control Bus and Linking]].
3. Open the Factory Controller.
4. Choose a metric, operator, and threshold.
5. Choose the output route.
6. Link or route the output to the desired actuator.

## Two-condition logic

The optional second condition lets one controller evaluate two measurements.

Example:

```text
Inventory Sensor -----\
                       > Factory Controller (AND) ---> process_ready
Fluid Tank Sensor ----/
```

This can prevent a process from starting until **both** stock and fluid are ready.

With **OR**, either condition may satisfy the controller.

## Source selection

A condition may use automatic source selection or bind to a specific directly linked source.

Explicit binding is useful when the same Control Bus contains multiple sensors of the same type.

Changing a preset returns its relevant sources to AUTO and clears stale observations before evaluation.

## Output routing

The controller can publish to:

- Default
- Circuit A
- Circuit B
- Circuit C
- Circuit D
- a named address

Named addresses are especially useful in larger factories:

```text
ore_line_1
process_ready
tank_fill_enable
optional_loads
```

## Common metrics

Depending on linked sensors/controllers, metrics include:

- inventory counts and occupancy
- fluid amount, capacity, and fill ratio
- redstone strength
- machine progress
- process time
- ticks remaining
- observed machine cycles
- power capacity
- power demand
- power reserve
- power load ratio
- powered-consumer ratio
- unpowered-consumer count
- batch cycles/minute
- batch ETA seconds

Batch pace metrics return the condition to **WAITING** whenever rate data is unavailable.

## Presets

Factory Controller includes reusable presets for common starting points. Presets are intended as templates; changing their rule fields makes the configuration custom again.

Examples include stock/fluid readiness, redstone thresholds, machine progress, power/load conditions, and production pace conditions.

## WAITING state

A controller should not make a decision from stale or unavailable measurements.

If a required source disappears or a special metric is currently unavailable, the condition can return to **WAITING** until valid telemetry is received again.

## Example: stop a line when inventory fills

```text
Inventory Sensor
    |
    v
Factory Controller
Rule: occupancy >= chosen threshold
    |
    v
Address: line_full
    |
    +--> Addressed Relay
    +--> Cargo Isolator
```

## Related pages

- [[Sensors and Telemetry]]
- [[Factory Monitor]]
- [[Factory Automation]]
- [[Control Bus and Linking]]
