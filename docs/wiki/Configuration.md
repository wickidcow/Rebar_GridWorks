# Configuration

GridWorks validates runtime numeric settings during startup.

Missing keys use documented defaults for backward compatibility. Explicit invalid values fail startup with the exact configuration path instead of silently clamping the mistake.

## Default configuration

```yaml
control-bus:
  max-propagation-nodes: 4096

sensors:
  inventory:
    sample-interval-ticks: 10
    max-samples-per-tick: 128
  fluid:
    sample-interval-ticks: 10
    max-samples-per-tick: 128
  machine:
    sample-interval-ticks: 20
    max-samples-per-tick: 64
  power:
    sample-interval-ticks: 20
    max-samples-per-tick: 32

cargo:
  isolator:
    transfer-rate: 1
```

## Control Bus propagation cap

`control-bus.max-propagation-nodes`

Default: **4096**

This is a safety cap for one signal dispatch, not a recommended network size.

## Sensor sampling

Each sensor type has two controls:

- `sample-interval-ticks` — desired time for one full sweep under normal load;
- `max-samples-per-tick` — hard per-tick probe ceiling.

| Sensor | Desired sweep | Per-tick budget |
| --- | ---: | ---: |
| Inventory | 10 ticks | 128 |
| Fluid | 10 ticks | 128 |
| Machine | 20 ticks | 64 |
| Power | 20 ticks | 32 |

GridWorks distributes loaded sensors round-robin across ticks.

If the population fits inside the budget, the configured interval is maintained. If it does not, the effective sweep stretches instead of exceeding the per-tick ceiling.

For example, 2,000 Inventory Sensors with the default 128/tick ceiling need roughly 16 ticks for a full sweep even though the requested interval is 10 ticks.

This overload behavior is intentional.

## Choosing budgets

A lower `max-samples-per-tick`:

- reduces worst-tick sensor work;
- increases the time for huge populations to complete a sweep.

A higher value:

- makes very large sensor populations more responsive;
- allows more target probes to land on one tick.

Use profiling and `/gridworks doctor` rather than setting all budgets arbitrarily high.

## Cargo Isolator transfer rate

`cargo.isolator.transfer-rate`

Default: **1**

This uses Rebar cargo transfer-rate units. An isolated Cargo Isolator still forces its own outbound transfer rate to zero.

## Confirming the live values

Run:

`/gridworks doctor`

Doctor prints the active validated settings snapshot plus loaded sensor counts, per-tick budgets, and estimated current sweep times.
