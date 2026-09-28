# Status Light

The **Status Light** is a simple visual Control Bus output.

It displays a boolean automation state without turning that state into a physical redstone command.

## Basic use

```text
Redstone Sensor ---> Status Light
```

or:

```text
Factory Controller ---> Status Light
```

This makes it useful as a local operator indicator for:

- machine-ready state;
- tank-full state;
- line-enabled state;
- batch completion;
- simple alarms that do not need latching.

## GridWorks-controlled state

Nearby vanilla redstone does not take over the Status Light's Control Bus-controlled state.

The light represents the GridWorks signal it is configured to observe.

## Status Light versus Alarm Indicator

Use **Status Light** when you only need to see the current boolean state.

Use [[Alarm Indicator]] when you need:

- latching;
- acknowledgement;
- severity;
- escalation;
- occurrence history.

## Related pages

- [[Redstone Sensor]]
- [[Control Relay]]
- [[Alarm Indicator]]
- [[Control Bus and Linking]]
