# Control Relay

The **Control Relay** converts a GridWorks boolean command into steady vanilla redstone output.

## What it does

```text
Control Bus boolean ---> Control Relay ---> vanilla redstone
```

When its selected GridWorks command is ON, the relay produces its controlled redstone output.

When the command is OFF, the output is removed.

## Input routing

Control Relay supports the simple Control Bus command routes used by GridWorks boolean actuators:

- Default
- A
- B
- C
- D

Use the [[GridWorks Linker]] to cycle the selected circuit on supported actuator routing.

## Typical uses

Control Relay is useful when GridWorks needs to drive something that already understands vanilla redstone:

- doors;
- pistons;
- vanilla machinery;
- another plugin's redstone input;
- visual redstone lamps;
- existing factory wiring.

## Control Relay versus Addressed Relay

Use **Control Relay** when the five shared circuits are enough.

Use [[Addressed Relay]] when you want a human-readable named route such as:

`ore_line_1`

Named routing scales better in larger factories.

## Steady output versus pulses

Control Relay represents steady state.

For a one-shot timed pulse on a rising edge, use [[Pulse Relay]].

For delayed/debounced transitions, use [[Delay Relay]].

## Related pages

- [[Addressed Relay]]
- [[Pulse Relay]]
- [[Delay Relay]]
- [[Control Bus and Linking]]
