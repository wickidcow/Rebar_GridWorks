# Machine Sensor

The **Machine Sensor** observes an adjacent supported Rebar machine and publishes processing telemetry onto the Control Bus.

## Supported machine contracts

GridWorks uses released Rebar processor APIs, including supported processor and recipe-processor contracts.

Unsupported blocks report unavailable instead of being guessed through reflection or internal APIs.

## Telemetry

Machine Sensor publishes:

- availability
- machine kind
- processing state
- normalized progress
- process time in ticks
- ticks remaining
- observed cycles
- last observed cycle timestamp

Progress is normalized to **0.0 through 1.0 completed** across supported processor styles.

## Placement

The Machine Sensor watches the adjacent machine it faces.

A typical setup:

```text
[Machine] [Machine Sensor] ---- Control Bus
```

## Observed Cycles

GridWorks maintains a persisted **Observed Cycles** count.

A cycle increments only when a continuously available target is observed moving from:

```text
processing ---> idle
```

This makes it useful for production automation without requiring GridWorks to own the machine's recipe lifecycle.

### Important limitation

**Observed Cycles is activity telemetry, not guaranteed crafted-output accounting.**

A processing-to-idle observation does not prove a specific output quantity was produced.

## Resetting the cycle count

Right click the Machine Sensor to open its cycle panel.

**Shift + right click Reset Observed Cycles** starts a new count by clearing the persisted count/timestamp while preserving the current processing baseline.

If the machine is already running when reset, that in-progress job can become cycle 1 when it next reaches idle.

## Sampling

All loaded Machine Sensors share one sampler.

Default:

**20 ticks / 1 second at 20 TPS**

Snapshots are published only when they change.

## Batch Controller integration

Machine Sensor is the normal source for [[Batch Controller]].

```text
Machine Sensor A ----\
                      > Batch Controller
Machine Sensor B ----/
```

The Batch Controller establishes a baseline from each source and counts only later positive cycle deltas.

## Factory Controller integration

Observed cycles, progress, process time, and ticks remaining can be used as Factory Controller metrics.

Example:

```text
Machine Sensor ---> Factory Controller
Rule: machine progress >= 90%
```

## Related pages

- [[Batch Controller]]
- [[Factory Controller]]
- [[Production Control]]
- [[Sensors and Telemetry]]
