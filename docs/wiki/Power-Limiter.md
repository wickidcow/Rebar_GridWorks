# Power Limiter

The **Power Limiter** controls a watt cap on one provider-exposed electrical branch.

It uses the same GridWorks `PowerBranchProvider` target model as [[Smart Breaker]].

## Default state

Power Limiter starts in **BYPASS**.

Placing one should not unexpectedly throttle an existing grid.

## Limit range

The configured cap can be adjusted from:

**1 W through 1 TW**

## Command behavior

- **true** -> apply configured watt limit
- **false** -> request provider-neutral unlimited/bypass

Input can use Default/A-D circuits or a named address.

## Provider capability

A provider may support branch switching without supporting watt limits.

GridWorks exposes that capability honestly rather than pretending an unsupported limit was applied.

## Availability

Desired behavior is reconciled with the provider/target lifecycle through the shared branch-device manager.

There is no independent Power Limiter polling loop.

## Fail-safe route changes

When the active input route changes, Power Limiter first requests **BYPASS**.

Normal Control Bus replay then establishes the command on the newly active route.

## Related pages

- [[Smart Breaker]]
- [[Load Shedding Controller]]
- [[Power Grid Sensor]]
- [[Power and Flow Control]]
