# Batch Controller

The **Batch Controller** tracks production progress from one or more directly linked [[Machine Sensor|Machine-Sensor]] devices.

It is designed for operator-defined production runs such as:

- make 64 cycles;
- process 128 operations;
- run parallel machines until a shared target is reached.

## Basic setup

```text
Machine Sensor A ----\
                      > Batch Controller ---> batch_done
Machine Sensor B ----/
```

1. Link one or more Machine Sensors directly to the Batch Controller.
2. Set the target.
3. Start a new batch.
4. Let new observed machine cycles accumulate.
5. Use completion output to trigger the next action.

## Baselines

The first cumulative cycle count received from each source becomes a **baseline**.

Historical work is not counted.

If a source unloads or is unlinked, its baseline is discarded. Reconnecting establishes a fresh baseline.

This means cycles performed while the controller could not observe a source are not backfilled later.

## Progress and completion

The controller persists:

- batch progress;
- target;
- fault state.

When progress reaches the target, completion latches ON.

**Start New Batch** resets progress to zero while preserving the current baselines for still-linked sources.

The next new machine cycle therefore becomes the first cycle of the new batch.

## No-Progress Watchdog

The watchdog is optional and defaults **OFF**.

When enabled on an incomplete batch, GridWorks watches for positive progress.

Each positive cycle delta replaces the watchdog deadline.

If the deadline expires without progress:

- the controller enters persisted **FAULT**;
- completion is forced OFF;
- the separate fault address is asserted;
- incoming machine totals update baselines but do not add batch progress.

This prevents work observed during the fault from being silently credited after recovery.

## Fault acknowledgement

**Start New Batch** is the deliberate fault acknowledgement.

It:

- clears progress;
- clears the fault;
- preserves current live source baselines;
- starts a fresh watchdog window when enabled.

## Reset / Start Input

Batch Controller can listen to a named reset/start address.

A real false-to-true edge performs the same safe new-batch action as the GUI.

Replay after restart establishes only the edge baseline, so a reset line left ON through restart cannot erase a batch.

## Pace and ETA

Batch Controller derives runtime production pace from real progress events without adding another sampler.

Positive progress updates arriving within **50 ms** are coalesced into one production burst. This prevents multiple directly linked Machine Sensors from producing an artificial rate spike when the shared sampler publishes their changed cycle totals back-to-back.

It can publish:

- rate availability
- cycles/minute
- ETA seconds

The first distinct progress burst establishes timing. The next distinct burst closes the prior interval and produces the observed rate. Pace becomes unavailable after events that invalidate timing history, including:

- reload or same-instance reactivation;
- Start New Batch;
- watchdog fault;
- relevant Machine Sensor connect/disconnect.

Two distinct progress bursts are therefore required before pace becomes valid again.

## Output routing

Completion can use:

- Default/A-D circuit routing; or
- a named address.

Fault has its own named address.

Completion, fault, and reset routes are kept distinct.

The Production page of the [[Factory Monitor]] exposes batch progress, target, completion/fault state, watchdog timeout, pace availability, cycles/minute, and ETA.

## Example: repeated automated batches

```text
Machine Sensors ---> Batch Controller ---> batch_done
                           |                  |
                           |                  v
                           |          Sequence Controller
                           |
Sequence stage ----------> batch_reset
```

A Sequence Controller can start the next batch by pulsing the Batch Controller's reset/start address.

## Related pages

- [[Machine Sensor]]
- [[Sequence Controller]]
- [[Production Control]]
- [[Factory Monitor]]
