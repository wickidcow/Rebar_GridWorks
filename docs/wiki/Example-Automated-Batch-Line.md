# Example: Automated Batch Line

This example combines Machine Sensors, Batch Controller, and optional sequence control into a repeatable production line.

## Goal

Run one or more machines until a chosen number of observed cycles has completed, then signal that the batch is finished.

## Parts

- one or more [[Machine Sensor]] devices
- 1 [[Batch Controller]]
- 1 [[GridWorks Linker]]
- optional [[Addressed Relay]]
- optional [[Sequence Controller]]
- optional [[Alarm Indicator]]

## Layout

```text
Machine A ---> Machine Sensor A ----\
                                      > Batch Controller ---> batch_done
Machine B ---> Machine Sensor B ----/          |
                                                +--> batch_fault
```

## Configure the sensors

Place each Machine Sensor facing a supported Rebar processor.

Verify that the sensor reports:

- available;
- processing state;
- progress;
- observed cycles.

## Link the Batch Controller

Link each Machine Sensor directly to the Batch Controller.

The first cumulative cycle count received from each source becomes its baseline.

That means existing historical cycles are not incorrectly counted as part of the new batch.

## Set a target

Choose a batch target such as:

```text
64 cycles
```

When enough new cycle deltas accumulate, completion latches ON.

## Add a watchdog

For unattended production, enable the optional no-progress watchdog.

If production stalls beyond its configured deadline, the Batch Controller:

- enters FAULT;
- turns completion OFF;
- asserts the separate fault address.

You can route the fault into an [[Alarm Indicator]].

## Automatically start the next batch

The Batch Controller has a named Reset / Start Input.

A [[Sequence Controller]] can pulse that address at the appropriate stage:

```text
Sequence stage output ---> batch_reset
                              |
                              v
                       Batch Controller
```

The reset route is rising-edge driven and replay-safe.

## Add production pacing

Batch Controller derives pace from real progress events without adding another sampler.

Positive deltas reported within 50 ms are coalesced into one burst, so multiple directly linked Machine Sensors sampled back-to-back do not create an artificial rate spike. The first distinct burst establishes timing; the next distinct burst closes that interval and makes pace available.

Batch Controller can then expose:

- cycles/minute;
- ETA seconds.

Display these with [[Factory Monitor]] or use them in [[Factory Controller]] logic. Pace returns to unavailable after a new batch, reload/reactivation, watchdog fault, or relevant Machine Sensor connect/disconnect until two new distinct bursts are observed.

## Important limitation

Machine Sensor observed cycles represent observed processing activity.

They are **not guaranteed crafted-item accounting**.

Use this example for process control, not economy-grade output auditing.
