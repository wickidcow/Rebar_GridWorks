# Roadmap and Compatibility

## Current target

| Component | Current development target |
| --- | --- |
| GridWorks | 0.3.0-SNAPSHOT |
| Minecraft / Paper | 26.2 |
| Java | 25 |
| Rebar | 0.43.0-26.2 |

The repository's Gradle properties and CI are authoritative if this page ever trails a newer development commit.

## Implemented foundation

The current development line already includes:

- persistent Control Bus topology;
- circuits and addressed commands;
- redstone, inventory, fluid, machine, and power sensing;
- Factory Controller and Factory Monitor;
- alarms and Alarm Console;
- Machine Sensor cycle telemetry;
- Batch Controller target/watchdog/pace/ETA;
- four-stage Sequence Controller;
- provider-neutral power telemetry and branch APIs;
- load shedding;
- Smart Breaker and Power Limiter;
- Fluid Valve and Cargo Isolator;
- survival recipe visibility;
- content-integrity validation;
- live Paper + Rebar CI smoke testing;
- `/gridworks doctor`.

## Native Rebar electricity adapter

The major intentionally deferred integration is the native Rebar electricity adapter.

GridWorks already defines provider-neutral `PowerGridProvider` and `PowerBranchProvider` contracts.

The adapter can be implemented after Rebar ships its electricity API in a released dependency. GridWorks will not bind production code to unreleased electricity classes just to get there sooner.

## Development rules

New systems should continue to preserve these project principles:

- no unnecessary force-loaded chunks;
- no world scans for ordinary automation;
- events before polling;
- shared samplers when polling is unavoidable;
- publish only changed snapshots;
- replay-safe edge semantics;
- fail-safe physical route changes;
- explicit provider contracts;
- regression tests for false triggers, dupes, lifecycle failures, and unsafe startup state.
