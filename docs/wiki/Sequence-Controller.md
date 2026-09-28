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

## Stage timeout

Optional **Stage Timeout** defaults OFF.

When enabled, GridWorks schedules one delayed task for the active stage.

If the stage fails to advance before the deadline:

- sequence enters persisted FAULT;
- the timed-out stage is remembered;
- stage/completion outputs are cleared;
- fault output is asserted.

Changing the timeout while RUNNING restarts the current stage deadline.

After server reload, a running timeout receives a fresh full deadline rather than treating offline time as elapsed fault time.

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

## Persistence

Current phase and active stage persist across reloads.

Fault cause is persisted separately for diagnostics.

## Related pages

- [[Batch Controller]]
- [[Fluid Valve]]
- [[Cargo Isolator]]
- [[Production Control]]
- [[Factory Monitor]]
