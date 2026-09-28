# Example: Simple Redstone Network

This is the easiest practical GridWorks build and a good way to learn the [[Control Bus and Linking]] model.

## Goal

Read a vanilla redstone signal somewhere in the factory and reproduce it somewhere else through GridWorks.

## Parts

- 1 [[Redstone Sensor]]
- 1 [[GridWorks Linker]]
- 1 [[Status Light]] or [[Control Relay]]

## Layout

```text
Vanilla redstone
      |
      v
Redstone Sensor ===== Control Bus ===== Control Relay
                                          |
                                          v
                                   vanilla redstone
```

## Build it

1. Place the Redstone Sensor where it can observe the desired vanilla signal.
2. Place the Control Relay near the device you want to operate.
3. Use the GridWorks Linker on the sensor.
4. Use the Linker on the relay to create the link.
5. Apply and remove the input redstone signal.
6. Confirm the relay follows the boolean state.

For a visual-only test, use a Status Light instead of a relay.

## Add analog logic

The Redstone Sensor also publishes strength from 0 through 15.

Add a [[Factory Controller]]:

```text
Redstone Sensor ---> Factory Controller ---> Control Relay
```

Example rule:

```text
redstone strength >= 8
```

Now the relay only activates above the chosen analog threshold.

## Upgrade to named control

For a larger network:

```text
Redstone Sensor
      |
Factory Controller
Address: workshop_enable
      |
Addressed Relay
```

This makes the command self-describing and avoids consuming one of the five shared simple circuits.

## What this example teaches

- persistent physical linking;
- telemetry versus commands;
- circuit routing;
- numeric controller rules;
- addressed output.
