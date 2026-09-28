# Redstone Sensor

The **Redstone Sensor** converts vanilla redstone input into GridWorks Control Bus telemetry.

It is the simplest bridge between ordinary Minecraft redstone and a GridWorks automation network.

## What it publishes

The sensor reports:

- boolean powered state;
- analog signal strength from **0 to 15**.

Because strength is numeric, it can also be used by a [[Factory Controller]] for threshold logic.

## Event-driven behavior

Redstone Sensor is event-driven.

It does not need a repeating GridWorks polling task just to notice normal redstone changes.

When a linked peer reconnects, the sensor can replay its current state so the network can recover without waiting for another redstone change.

## Simple setup

```text
Vanilla Redstone
      |
      v
Redstone Sensor ---> Status Light
```

or:

```text
Redstone Sensor ---> Factory Controller ---> Addressed Relay
```

## Analog automation example

A Factory Controller can use redstone strength as a numeric input:

```text
Rule: redstone strength >= 8
```

This allows comparators, weighted signals, and other analog redstone systems to participate in GridWorks logic.

## Replay safety

GridWorks distinguishes **replayed state** from a new edge.

This matters when the same signal later feeds devices such as [[Pulse Relay]], where a server restart must not manufacture a false rising edge.

## Related pages

- [[Status Light]]
- [[Control Relay]]
- [[Factory Controller]]
- [[Control Bus and Linking]]
