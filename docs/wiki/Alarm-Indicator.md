# Alarm Indicator

The **Alarm Indicator** is GridWorks' latched industrial-style fault display.

Unlike a simple status lamp, an alarm remembers that a real fault occurred until it has been acknowledged or safely cleared.

## Alarm behavior

On a real false-to-true fault:

- the indicator latches;
- occurrence history increments;
- last-trigger time updates;
- an optional local bell can ring once.

If the underlying condition clears before acknowledgement, the alarm remains latched so the event is not lost.

If acknowledged while the condition is still active, it remains visibly acknowledged and clears automatically when the condition later disappears.

## Name and severity

Each alarm can have:

- a player-defined name;
- **Info**
- **Warning**
- **Critical**

Older placed alarms without a stored severity load as Warning.

## Escalation

Optional one-step timed escalation can raise an unacknowledged latched alarm:

- Info -> Warning
- Warning -> Critical

Supported delay range is 5 seconds through 30 minutes.

Acknowledgement cancels escalation.

If the chunk/server reloads during a pending escalation, GridWorks reconstructs the remaining delay from the persisted trigger timestamp rather than simply starting the timer over.

## History

Alarm Indicator stores a lightweight summary:

- total real occurrences;
- most recent real trigger time.

Startup and Control Bus state replay do not create false history entries.

## Telemetry

Alarm telemetry includes:

- name
- severity
- condition active
- latched
- acknowledged
- occurrence count
- last-trigger time

Because Alarm Indicator provides current state replay, a later-joining [[Alarm Console]] or [[Factory Monitor]] can obtain the current alarm state without polling the block.

## Related pages

- [[Alarm Console]]
- [[Factory Monitor]]
- [[Factory Automation]]
