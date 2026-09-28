# Recipes and Progression

GridWorks is designed as a progression rather than a flat list of unrelated machines.

The early automation components use familiar Minecraft materials, while advanced devices build on earlier GridWorks parts.

## Viewing recipes

Use the installed **Rebar/Pylon guide** for the exact recipe in your current build.

GridWorks deliberately keeps recipe grids out of the Wiki while the addon is still changing rapidly, because the in-game guide is the authoritative player-facing recipe source.

## General progression

A typical progression looks like:

```text
Vanilla copper / redstone / quartz
              |
              v
      Control Interface
              |
              +--> basic sensors
              +--> lights and relays
              +--> GridWorks Linker
              |
              v
      advanced automation
              |
              +--> Factory Controller
              +--> Factory Monitor
              +--> Alarm system
              +--> Batch Controller
              +--> Sequence Controller
              +--> power / flow devices
```

Advanced devices intentionally reuse earlier GridWorks components.

## Recommended first crafts

For a new player, a sensible order is:

1. [[Control Interface]]
2. [[GridWorks Linker]]
3. [[Redstone Sensor]]
4. [[Status Light]] or [[Control Relay]]
5. [[Inventory Sensor]]
6. [[Factory Controller]]
7. whichever specialized sensors/actuators your factory needs

## Production progression

Once basic automation is understood:

1. Add a [[Machine Sensor]].
2. Use its cycle telemetry with a [[Batch Controller]].
3. Add a [[Factory Monitor]].
4. Add alarms for important fault states.
5. Use a [[Sequence Controller]] when multiple operations must occur in order.

## Power and flow progression

When a compatible power provider is available:

1. [[Power Grid Sensor]]
2. [[Load Shedding Controller]]
3. [[Smart Breaker]]
4. [[Power Limiter]]

For fluids/cargo:

- [[Fluid Tank Sensor]]
- [[Fluid Valve]]
- [[Cargo Isolator]]

## Content integrity

GridWorks CI checks the canonical player-facing content catalog.

For every current content entry, the build verifies that the addon includes:

- survival recipe registration;
- bundled English item metadata.

A mismatch stops validation rather than leaving an item silently inaccessible.

## Related pages

- [[Getting Started]]
- [[Devices and Machines]]
- [[Factory Automation]]
