# GridWorks Glossary

## Address

A human-readable named Control Bus command route, such as:

`ore_line_1`

Addresses allow more independent commands than the five simple circuits and may intentionally have multiple listeners.

## Actuator

A device that performs an action from a Control Bus command.

Examples include relays, Fluid Valve, Cargo Isolator, Smart Breaker, and Power Limiter.

## Baseline

An initial observed state used to distinguish existing state from a **new event**.

Baselines are especially important for replay-safe edge detection and Batch Controller cycle accounting.

## Circuit

One of GridWorks' five simple boolean command routes:

Default, A, B, C, or D.

## Control Bus

GridWorks' typed automation communication system connecting linked nodes.

It carries sensor telemetry, state, and commands.

## Control Interface

A foundational crafting component used in GridWorks progression.

## Edge / Rising Edge

A state transition.

A **rising edge** means:

`false -> true`

Pulse Relay, reset inputs, and sequence triggers use rising-edge semantics where appropriate.

## Fail-safe

A deliberately safe physical state used when configuration/routing or upstream availability is uncertain.

Examples include a Fluid Valve closing or a Power Limiter entering bypass during an active route change.

## Link

A persistent physical GridWorks Control Bus relationship created with the GridWorks Linker.

## Provider

Another addon/service implementation that supplies GridWorks with a neutral integration.

Examples:

- `PowerGridProvider`
- `PowerBranchProvider`

## Replay

Re-sending current state when topology or a consumer reconnects.

Replay restores state but should not be confused with a new button press, pulse, alarm occurrence, cycle, or stage trigger.

## Sampler

A shared repeating task that measures loaded devices where no universal upstream change event exists.

GridWorks shares samplers per sensor class rather than creating one repeating task per placed sensor.

## Sensor

A GridWorks device that observes state and publishes telemetry.

Examples include Inventory Sensor, Machine Sensor, Fluid Tank Sensor, and Power Grid Sensor.

## Telemetry

Descriptive state published onto the Control Bus.

Telemetry tells the network **what is happening**.

Commands tell actuators **what to do**.

## WAITING

A controller state where required telemetry is unavailable or not yet valid enough to evaluate safely.

## Watchdog

A timer that detects lack of expected progress.

Batch Controller's optional no-progress watchdog can place a stalled batch into FAULT.
