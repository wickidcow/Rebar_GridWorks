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
  fluid:
    sample-interval-ticks: 10
  machine:
    sample-interval-ticks: 20
  power:
    sample-interval-ticks: 20

cargo:
  isolator:
    transfer-rate: 1
```

## Control Bus propagation cap

`control-bus.max-propagation-nodes`

Default: **4096**

This is a safety cap for one signal dispatch, not a recommended network size.

## Sensor sampling

| Sensor | Default |
| --- | ---: |
| Inventory | 10 ticks |
| Fluid | 10 ticks |
| Machine | 20 ticks |
| Power | 20 ticks |

Each sensor type uses one shared sampler for currently loaded sensors.

Shorter intervals create more measurement work even though unchanged snapshots are not republished.

## Cargo Isolator transfer rate

`cargo.isolator.transfer-rate`

Default: **1**

This uses Rebar cargo transfer-rate units. An isolated Cargo Isolator still forces its own outbound transfer rate to zero.

## Confirming the live values

Run:

`/gridworks doctor`

Doctor prints the active validated settings snapshot.
