# Control Bus and Linking

The **Control Bus** is GridWorks' shared automation layer.

Sensors publish typed state. Controllers evaluate that state. Actuators consume commands.

## Physical links

GridWorks control nodes use persistent UUIDs and persistent links.

The live topology is designed to be chunk-safe:

- links persist across restarts;
- loaded peers reconnect automatically;
- ordinary operation does not world-scan;
- chunks are not force-loaded just to restore a link;
- devices can react when a linked peer becomes available or unavailable.

## GridWorks Linker

The Linker is the primary network tool.

| Action | Result |
| --- | --- |
| Right click a control node | Select it |
| Right click another node | Link/unlink the pair |
| Sneak + right click | Inspect the device/network |
| Sneak + left click supported actuator | Cycle its command circuit |

## Command circuits

One physical Control Bus can carry five simple boolean command circuits:

- Default
- A
- B
- C
- D

This lets several independent boolean controls share one linked component.

## Addressed commands

Larger factories can use human-readable addresses.

Example:

`ore_line_1`

GridWorks publishes that through its namespaced addressed-control channel.

Multiple receivers may deliberately use the same address. This makes addresses useful for:

- machine groups;
- alarm groups;
- load-shedding tiers;
- batch reset lines;
- sequence stages.

## Replay is not a new event

When topology reconnects, state sources can replay their current state. Edge-triggered devices treat the first replayed state as a baseline.

That prevents a restart from falsely:

- firing a Pulse Relay;
- resetting a Batch Controller;
- advancing a Sequence Controller;
- recording a new alarm occurrence.

## Propagation safety

Control Bus propagation is cycle-safe and has a configurable recipient cap. The default maximum is **4096 nodes per dispatch**.

See [[Configuration]] before changing it.
