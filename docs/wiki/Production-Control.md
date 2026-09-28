# Production Control

## Batch Controller

The Batch Controller aggregates **new cycle deltas** from directly linked Machine Sensors.

The first cumulative count received from a source becomes its baseline. Historical work is not backfilled.

### Batch behavior

- progress and target persist;
- multiple sensors can contribute parallel production;
- reconnecting a source establishes a fresh baseline;
- reaching the target latches completion ON;
- **Start New Batch** resets progress while preserving live source baselines.

### No-Progress Watchdog

The optional watchdog defaults **OFF**.

If enabled and progress stalls beyond its deadline:

- the controller enters persisted FAULT;
- completion is forced OFF;
- a separate fault address is asserted;
- cycles observed during fault update baselines but do not count toward progress.

Start New Batch acts as deliberate fault acknowledgement.

### Pace and ETA

Production pace is derived from real progress events rather than a new polling task.

Progress deltas arriving within 50 ms are coalesced into one burst so multiple linked Machine Sensors sampled back-to-back do not create unrealistic microsecond rate intervals. The next distinct burst closes the prior interval and updates cycles/minute and ETA.

The controller exposes an explicit availability state so unknown pace is never misrepresented as zero.

## Sequence Controller

The Sequence Controller provides a persistent **four-stage** workflow.

Each stage has:

- one named output;
- one named trigger.

Starting activates Stage 1. A real rising edge on the current stage's trigger advances to the next stage.

The controller also has:

- Start Input;
- Completion Output;
- Fault Output;
- Fault Interlock Input;
- Abort / Reset Input.

### Example

```text
Stage 1 ---> fill valve
tank_full -----------------> Stage 1 trigger

Stage 2 ---> machine line
batch_done ----------------> Stage 2 trigger

Stage 3 ---> drain valve
tank_empty ----------------> Stage 3 trigger

Stage 4 ---> cargo release
output_ready --------------> Stage 4 trigger
```

Each stage has its own optional timeout and can place the sequence into persistent FAULT. Only the active stage owns a delayed timeout task.

Fault Interlock is level-sensitive, making a Batch Controller fault a reliable direct production-stop path even after replay/restart.

Sequence Controller also persists completed-run history and last-completion time. Operators can reset that history independently of sequence state, and Factory Controller can use **Sequence Completed Runs** as a numeric metric for maintenance or downstream automation.
