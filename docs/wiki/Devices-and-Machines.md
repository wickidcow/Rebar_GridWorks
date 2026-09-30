# Devices and Machines

GridWorks currently validates a canonical catalog of **25 player-facing content IDs**. All current devices are intended to participate in normal Rebar recipe/guide visibility.

## Foundation

| Device | Purpose |
| --- | --- |
| [[Control Interface]] | Core automation component used by advanced GridWorks devices |
| [[GridWorks Linker]] | Creates, removes, inspects, and configures Control Bus links |

## Redstone and signaling

| Device | Purpose |
| --- | --- |
| [[Redstone Sensor]] | Reads powered state and analog redstone strength |
| [[Status Light]] | Displays a Control Bus boolean state |
| [[Control Relay]] | Converts a command into steady vanilla redstone |
| [[Addressed Relay]] | Converts one named address into vanilla redstone |
| [[Pulse Relay]] | Produces a timed pulse on a real rising edge |
| [[Delay Relay]] | Adds independent ON/OFF delay and debounce behavior |

## Monitoring and alarms

| Device | Purpose |
| --- | --- |
| [[Alarm Indicator]] | Latched named alarm with severity and acknowledgement |
| [[Alarm Console]] | Central alarm view and acknowledgement interface |
| [[Factory Monitor]] | Live Control Bus telemetry display |

## Sensors

| Device | Purpose |
| --- | --- |
| [[Inventory Sensor]] | Item count, slot use, and occupancy |
| [[Machine Sensor]] | Processing state, progress, timing, and observed cycles |
| [[Fluid Tank Sensor]] | Fluid type, amount, capacity, and fill ratio |
| [[Power Grid Sensor]] | Provider-neutral electrical grid telemetry |

See [[Sensors and Telemetry]] for the shared sensor model.

## Logic and production

| Device | Purpose |
| --- | --- |
| [[Factory Controller]] | Up to two sensor conditions with AND/OR logic |
| [[Stock Controller]] | Maintains inventory stock inside a low/high hysteresis band |
| [[Batch Controller]] | Aggregates new machine cycles toward a target |
| [[Sequence Controller]] | Persistent four-stage production workflow |
| [[Load Shedding Controller]] | Controls Essential, Normal, and Optional power tiers |

## Flow and branch control

| Device | Purpose |
| --- | --- |
| [[Smart Breaker]] | Opens or closes a provider-exposed power branch |
| [[Power Limiter]] | Applies or bypasses a branch watt cap |
| [[Fluid Valve]] | Control Bus-operated Rebar fluid pass-through |
| [[Cargo Isolator]] | Control Bus-operated cargo shutoff/buffer |

## Recipes

Use the installed Rebar/Pylon guide for exact recipes. GridWorks is still under active development, so keeping recipe grids in the Wiki would make them easier to become stale than the in-game guide.

GridWorks CI verifies that every canonical player-facing content entry has:

- a registered survival recipe;
- bundled English item metadata.

## Learn by system

- [[Getting Started]]
- [[Factory Automation]]
- [[Production Control]]
- [[Power and Flow Control]]

## Power Coupler

A native Rebar electrical junction controlled by the existing Smart Breaker and Power Limiter. See [[Native Electricity]] for wiring and power usage.
