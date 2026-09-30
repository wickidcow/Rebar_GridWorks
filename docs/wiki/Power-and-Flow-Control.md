# Power and Flow Control

Native Rebar electricity is integrated using the pinned development API `1.0.0-20260929.193904-140` from upstream commit `5e34938`. Use a matching Rebar development server JAR; the stable `0.43.0-26.2` JAR lacks this API. See [Native Electricity](https://github.com/wickidcow/Rebar_GridWorks/blob/main/docs/wiki/Native-Electricity.md).

## Power Grid Sensor

A registered `PowerGridProvider` can expose grid telemetry such as:

- production capacity;
- demand;
- reserve;
- load ratio;
- powered and unpowered consumers;
- powered-consumer ratio.

## Load Shedding Controller

The controller manages three named load tiers:

- **Essential**
- **Normal**
- **Optional**

Default hysteresis policy:

- shed Optional at 90% load;
- restore Optional at 80%;
- severe shedding begins at 100% load or when a consumer is reported unpowered;
- restore Normal after load falls to 90% with no unpowered consumers;
- Essential is never disabled by the policy.

If telemetry disappears, the player can choose:

- Essential Only — default;
- Allow All;
- Hold Last.

## Smart Breaker

Controls one provider-exposed electrical branch.

It accepts circuit or named-address commands and starts in the safe open state.

Desired state persists, while provider/chunk availability is handled without a dedicated polling loop.

## Power Limiter

Controls an optional watt cap on the same provider-neutral branch model.

It starts in **BYPASS** and can be configured from 1 W through 1 TW.

## Fluid Valve

A released-Rebar-API fluid pass-through with a one-bucket transit buffer.

- OPEN: requests compatible fluid and exposes stored fluid.
- CLOSED: requests/supplies 0 mB while retaining buffered fluid.

It defaults CLOSED.

## Cargo Isolator

A one-stack Rebar cargo buffer.

- OPEN: uses the configured transfer rate.
- ISOLATED: rejects new inbound writes and sets outbound transfer rate to zero.
- Existing buffered items remain safe.

It defaults to the safe isolated behavior during active route changes.

## Native Rebar electricity

Native Rebar electricity is integrated using the pinned development API `1.0.0-20260929.193904-140` from upstream commit `5e34938`. Use a matching Rebar development server JAR; the stable `0.43.0-26.2` JAR lacks this API. See [Native Electricity](https://github.com/wickidcow/Rebar_GridWorks/blob/main/docs/wiki/Native-Electricity.md).

Native Rebar electricity is integrated using the pinned development API `1.0.0-20260929.193904-140` from upstream commit `5e34938`. Use a matching Rebar development server JAR; the stable `0.43.0-26.2` JAR lacks this API. See [Native Electricity](https://github.com/wickidcow/Rebar_GridWorks/blob/main/docs/wiki/Native-Electricity.md).
