# Example: Four-Stage Factory

This example demonstrates a complete event-driven production sequence.

## Goal

Run a process in this order:

1. fill a tank;
2. run production;
3. drain the tank;
4. release finished cargo.

## Parts

A representative setup uses:

- [[Sequence Controller]]
- [[Fluid Valve]]
- [[Fluid Tank Sensor]]
- [[Factory Controller]]
- [[Machine Sensor]]
- [[Batch Controller]]
- [[Cargo Isolator]]
- optional [[Alarm Indicator]]

## Layout

```text
START
  |
  v
Stage 1: Fill
  |
  +--> fill_valve ---> Fluid Valve
  |
Fluid Tank Sensor ---> Factory Controller ---> tank_full
                                             |
                                             v
                                      Stage 1 trigger

Stage 2: Produce
  |
  +--> machine_line
  |
Machine Sensors ---> Batch Controller ---> batch_done
                                      |
                                      v
                               Stage 2 trigger

Stage 3: Drain
  |
  +--> drain_valve
  |
Fluid Tank Sensor ---> Factory Controller ---> tank_empty
                                             |
                                             v
                                      Stage 3 trigger

Stage 4: Release
  |
  +--> cargo_release ---> Cargo Isolator
  |
output_ready -------------------------------> Stage 4 trigger
```

## Stage 1 — fill

Configure the first stage output as a named address such as:

`fill_valve`

Have the Fluid Valve listen to that route.

Use the Fluid Tank Sensor and a Factory Controller to publish:

`tank_full`

when the chosen fill threshold is reached.

That becomes Stage 1's trigger.

## Stage 2 — production

Stage 2 enables the machine line.

Machine Sensors feed a Batch Controller.

When the target is reached, Batch Controller publishes:

`batch_done`

Use that as Stage 2's trigger.

## Stage 3 — drain

Stage 3 opens the drain path.

A fluid threshold controller publishes:

`tank_empty`

when draining is complete.

## Stage 4 — cargo release

Stage 4 opens a Cargo Isolator so finished items can leave the process.

A suitable inventory condition can publish:

`output_ready`

to complete the stage.

## Add safety

Connect the Batch Controller fault address to the Sequence Controller's **Fault Interlock Input**.

```text
batch_fault ---> sequence fault interlock
```

Because the interlock is level-sensitive, an already-active fault remains protective after restart/reconnect.

## Add stage timeouts

Enable Sequence Controller stage timeout when a stage should not be allowed to wait forever.

A timeout moves the workflow into persistent FAULT and records the reason.

## Why this design is efficient

The sequence itself does not poll every block.

Sensors provide state, controllers react to changed telemetry, and the Sequence Controller advances from Control Bus events.
