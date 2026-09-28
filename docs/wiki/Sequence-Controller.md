# Sequence Controller

The **Sequence Controller** runs a persistent four-stage, event-driven factory workflow.

Use it when a process must happen in a defined order rather than from one continuous boolean condition.

## Stage model

Each of four stages has:

- one named **Output**
- one named **Trigger**

Starting the sequence activates Stage 1's output.

A real false-to-true edge on the **current** stage trigger advances the sequence.

After Stage 4 completes, stage outputs are cleared and completion latches ON.

## Other routes

Sequence Controller also has:

- Start Input
- Completion Output
- Fault Output
- Fault Interlock Input
- Abort / Reset Input

All endpoints are kept distinct so the controller cannot be configured to feed its own input accidentally.

## Example production sequence

```text
Stage 1 output ---> fill valve
tank_full -----------------> Stage 1 trigger

Stage 2 output ---> machine line
batch_done ----------------> Stage 2 trigger

Stage 3 output ---> drain valve
tank_empty ----------------> Stage 3 trigger

Stage 4 output ---> cargo release
output_ready --------------> Stage 4 trigger
```

## Replay-safe triggers

Stage triggers use edge-safe replay semantics.

If a trigger is already ON when the sequence starts, reloads, reconnects, or advances to that stage, its first observed value establishes a baseline.

It must become false and then rise true again before it advances the stage.

This prevents restart/reconnect from skipping work.

## Per-stage timeouts

Each of the four stages has its own optional **Stage Timeout**, and all four default **OFF**.

That allows different safety windows for filling, processing, draining, and transfer stages while keeping the runtime bounded: only the currently active stage can own one delayed task.

If the active stage fails to advance before its deadline:

- sequence enters persisted FAULT;
- the timed-out stage is remembered;
- stage/completion outputs are cleared;
- fault output is asserted.

Editing the **active** stage timeout while RUNNING restarts only that stage's deadline. Editing a future stage changes only the value that will be used when that stage becomes active.

Existing placed controllers using the older single timeout migrate automatically: the old value becomes the fallback for any stage that does not yet have a per-stage value.

After server reload, a running stage receives a fresh full deadline rather than treating offline time as elapsed fault time.

## Fault Interlock

Fault Interlock is **level-sensitive**.

If it is ON while the sequence is running, the current stage faults immediately.

This is intentionally different from edge-triggered stage commands and makes safety state survive replay.

Example:

```text
Batch Controller fault ---> Sequence Controller Fault Interlock
```

If the batch fault is still ON after restart, the sequence still sees the unsafe state.

## Abort / Reset

Abort / Reset is rising-edge driven.

A real false-to-true command returns RUNNING, COMPLETE, or FAULT to IDLE and clears sequence outputs/fault cause.

Its first replayed value is a baseline, so a reset line left ON during restart cannot silently erase sequence state.

## Run history

Sequence Controller persists:

- **Completed Runs**
- **Last Completion**

The run counter increments only on the real **Stage 4 -> COMPLETE** transition. Restart, abort, fault, replay, and earlier stage advances never increment it.

The **Sequence Run History** GUI item shows both values. **Shift + right click** resets the count and timestamp without changing the current sequence phase or outputs.

`gridworks:sequence/completed_runs` is also available as a Factory Controller numeric metric, so maintenance or downstream rules can trigger after a chosen number of complete process cycles.

## Persistence

Current phase and active stage persist across reloads.

Fault cause, completed-run count, last-completion timestamp, and all four stage timeout values persist separately for diagnostics and recovery.

## Related pages

- [[Batch Controller]]
- [[Fluid Valve]]
- [[Cargo Isolator]]
- [[Production Control]]
- [[Factory Monitor]]
