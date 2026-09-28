# GridWorks Linker

The **GridWorks Linker** is the main tool used to create, remove, inspect, and configure Control Bus connections.

If you are building a GridWorks network, this is one of the first tools you should carry.

## Basic controls

| Action | Result |
| --- | --- |
| Right click a control node | Select that node |
| Right click another control node | Link or unlink the selected pair |
| Sneak + right click | Inspect the network/device |
| Sneak + left click a supported actuator | Cycle its input circuit |

## Creating a link

1. Hold the GridWorks Linker.
2. Right click the first GridWorks control node.
3. Right click the second node.
4. GridWorks creates the persistent link.

Repeat the process on an already linked pair to unlink it.

## Inspecting a network

Sneak + right click is useful when troubleshooting.

Inspection can help confirm:

- what node you are looking at;
- its current Control Bus relationships;
- relevant device state;
- active routing information where supported;
- live telemetry exposed by the device.

## Cycling actuator circuits

Supported boolean actuators can listen to the simple command circuits:

- Default
- A
- B
- C
- D

Sneak + left click with the Linker cycles that input circuit on compatible devices.

Address-capable devices may also expose named routing through their own configuration interface.

## Persistence

GridWorks links use persistent node UUIDs and persistent link storage.

Loaded topology reconnects without:

- scanning the entire world;
- force-loading unrelated chunks.

## Example

```text
Redstone Sensor ----link----> Control Relay
```

or:

```text
Machine Sensor ----link----> Factory Controller
```

The physical Control Bus can carry multiple telemetry channels and command routes across the same linked component.

## Related pages

- [[Control Bus and Linking]]
- [[Getting Started]]
- [[Devices and Machines]]
