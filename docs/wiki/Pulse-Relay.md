# Pulse Relay

The **Pulse Relay** turns a GridWorks rising edge into a timed vanilla redstone pulse.

Use it when another machine needs a momentary trigger rather than a steady ON state.

## Trigger behavior

A pulse occurs on a real:

```text
false ---> true
```

transition.

The pulse duration can be configured from:

**1 tick through 60 seconds**

## Replay safety

The first replayed input state after:

- restart;
- chunk reload;
- relink;
- topology recovery

establishes a baseline only.

If the input was already ON before the replay, GridWorks does **not** manufacture a new pulse.

This is important for buttons, one-shot machine triggers, reset lines, and other actions where duplicate activation would be undesirable.

## Re-triggering during an active pulse

If a new real rising edge occurs while a pulse is already active, the timer restarts from that new edge.

## Example

```text
Factory Controller ---> Pulse Relay ---> redstone-triggered machine
```

or:

```text
Addressed/Control signal ---> Pulse Relay ---> one-shot door / mechanism
```

## Pulse Relay versus Control Relay

- [[Control Relay]] mirrors steady boolean state.
- **Pulse Relay** converts a rising edge into a timed pulse.
- [[Delay Relay]] delays/debounces state transitions.

## Related pages

- [[Control Relay]]
- [[Delay Relay]]
- [[Redstone Sensor]]
- [[Control Bus and Linking]]
