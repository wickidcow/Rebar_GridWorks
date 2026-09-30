# Power Grid Sensor

The **Power Grid Sensor** exposes electrical-grid telemetry through GridWorks' provider-neutral power API.

Native Rebar electricity is integrated using the pinned development API `1.0.0-20260929.193904-140` from upstream commit `5e34938`. Use a matching Rebar development server JAR; the stable `0.43.0-26.2` JAR lacks this API. See [Native Electricity](https://github.com/wickidcow/Rebar_GridWorks/blob/main/docs/wiki/Native-Electricity.md).

## Requirements

A compatible addon must register a GridWorks `PowerGridProvider`.

The sensor faces one adjacent loaded block. If the active provider associates that block with a grid, GridWorks publishes the neutral telemetry snapshot.

## Telemetry

A provider can expose measurements including:

- node count;
- producer count;
- consumer count;
- powered consumers;
- unpowered consumers;
- production capacity;
- demand;
- load ratio;
- reserve watts;
- powered-consumer ratio.

## Complete-snapshot marker

Changed power metrics are emitted in deterministic order and conclude with:

`gridworks:power/sample_revision`

Consumers such as [[Load Shedding Controller]] can wait for that marker before evaluating a complete multi-field sample.

## Availability

If:

- the provider disappears;
- the target chunk unloads; or
- the target is no longer recognized as part of a grid,

the sensor publishes unavailable.

Power-aware Factory Controller conditions discard their old numeric observation and return to WAITING rather than using stale grid data.

## Sampling

All loaded Power Grid Sensors share one conservative sampler.

Default:

**20 ticks / 1 second at 20 TPS**

## Related pages

- [[Load Shedding Controller]]
- [[Smart Breaker]]
- [[Power Limiter]]
- [[Power and Flow Control]]
- [[Developer API]]
