# Smart Breaker

The **Smart Breaker** controls one electrical branch exposed through GridWorks' provider-neutral `PowerBranchProvider`.

## What true and false mean

For the command state:

- **true** -> branch closed/enabled
- **false** -> branch open/disabled

## Placement

The Smart Breaker faces the adjacent provider-exposed branch or power port it controls.

It never force-loads the target chunk just to apply a command.

## Inputs

Smart Breaker can listen to:

- Default/A-D Control Bus circuits; or
- a human-readable addressed command.

This makes it suitable for direct use with [[Load Shedding Controller]].

## Persistence and availability

The desired branch state is persisted.

If the target chunk or provider becomes temporarily unavailable, GridWorks retains the desired command and reconciles it when the target becomes available again.

Provider registration/unregistration and target-chunk lifecycle are handled through events rather than a Smart Breaker polling loop.

## Fail-safe behavior

New Smart Breakers start **OPEN** rather than automatically energizing an adjacent branch.

If the active Control Bus input route changes, GridWorks first requests the safe open state, then lets normal state replay establish the new route's actual command.

Editing an inactive saved address does not unnecessarily switch the branch.

## Provider support

Smart Breaker requires a provider that supports branch switching.

Current GridWorks core provides the neutral contract. Native Rebar electricity support remains pending a released upstream electricity API.

## Related pages

- [[Load Shedding Controller]]
- [[Power Grid Sensor]]
- [[Power Limiter]]
- [[Power and Flow Control]]
