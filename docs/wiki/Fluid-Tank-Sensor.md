# Fluid Tank Sensor

The **Fluid Tank Sensor** watches an adjacent Rebar fluid tank and publishes its current state onto the GridWorks Control Bus.

## Supported target

The sensor uses Rebar's **released public fluid tank API**.

GridWorks deliberately does not use reflection to guess at arbitrary internal multi-fluid buffers that Rebar does not publicly expose.

## Telemetry

The sensor publishes:

- availability;
- whether fluid is present;
- fluid type;
- amount;
- capacity;
- fill ratio.

The main channel family is:

`gridworks:fluid/*`

## Placement

The Fluid Tank Sensor watches the adjacent block it faces.

```text
[Rebar Fluid Tank] [Fluid Tank Sensor] ---> Control Bus
```

## Sampling

Rebar's released fluid API exposes tank state cleanly but does not provide one universal change event for every implementation.

All loaded Fluid Tank Sensors therefore share one sampler.

Default:

**10 ticks / 0.5 seconds at 20 TPS**

GridWorks publishes only when the snapshot changes.

## Automation examples

Stop filling when a tank is nearly full:

```text
Fluid Tank Sensor ---> Factory Controller ---> Fluid Valve
Rule: fill ratio >= threshold
```

Or use a tank-full address to advance a [[Sequence Controller]] stage.

## Chunk safety

The sensor only works with its currently available adjacent target and does not force another chunk to load simply to obtain a measurement.

## Related pages

- [[Fluid Valve]]
- [[Factory Controller]]
- [[Sequence Controller]]
- [[Sensors and Telemetry]]
