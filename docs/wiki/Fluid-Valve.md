# Fluid Valve

The **Fluid Valve** is a Control Bus-operated fluid pass-through built entirely on released Rebar fluid interfaces.

## Physical layout

The valve uses:

- rear face -> public fluid input
- facing side -> fluid output
- internal one-bucket transit buffer

## OPEN

When OPEN, the valve:

- requests compatible fluid up to remaining buffer space;
- exposes stored fluid to the output network.

## CLOSED

When CLOSED, the valve:

- requests 0 mB;
- supplies 0 mB;
- keeps already-buffered fluid safely stored.

This creates a real isolation behavior without disconnecting or rewriting Rebar's pipe graph.

## Inputs

Fluid Valve accepts:

- Default/A-D circuit commands; or
- named addressed commands.

## Fail-safe behavior

Fluid Valve defaults CLOSED.

If its active input route changes, it closes first and then allows state replay from the new route to establish the desired state.

## Sequence example

```text
Sequence Controller Stage 1 ---> fill_valve
                                  |
                                  v
                              Fluid Valve
                                  |
                                  v
                                 Tank
```

A Fluid Tank Sensor and Factory Controller can publish `tank_full` back to the sequence trigger.

## Related pages

- [[Sequence Controller]]
- [[Factory Controller]]
- [[Power and Flow Control]]
- [[Sensors and Telemetry]]
