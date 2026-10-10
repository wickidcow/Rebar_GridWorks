# Troubleshooting

## GridWorks does not enable

Check:

1. Paper is on the supported 26.2 line.
2. Java 25 is being used.
3. Rebar 0.44.4-26.2 is installed and enabled.
4. The GridWorks JAR matches the current development line.
5. `config.yml` does not contain an invalid numeric value.

GridWorks intentionally fails visibly rather than enabling with a broken content catalog or invalid runtime configuration.

## A linked device does nothing

Verify:

- both nodes are loaded;
- the Linker shows the intended link;
- the actuator is listening to the intended circuit/address;
- the controller is publishing on that route;
- the sensor/source is available instead of WAITING.

## Pulse Relay fired after restart

Current replay-safe behavior should prevent a previously-ON line from manufacturing a new rising edge after restart/relink.

If you can reproduce this on a current build, include the exact commit/build and topology in the bug report.

## Sequence will not advance

Check:

- only the current stage trigger can advance;
- the trigger must produce a real false-to-true edge;
- a replayed already-ON trigger establishes only a baseline;
- Fault Interlock is not active;
- stage timeout has not placed the controller in FAULT.

## Batch did not count older machine cycles

This is intentional.

A newly linked/reconnected Machine Sensor establishes its current cumulative count as a baseline. Only later positive deltas count toward the active batch.

## Machine cycles do not equal output items

Observed Cycles tracks observed processing-to-idle activity. It is not guaranteed crafted-output accounting.

## Power device says provider unavailable

GridWorks includes a native Rebar electricity provider plus a provider-neutral service contract. Check that Rebar 0.44.4-26.2 is installed, and confirm the target is a loaded electrical block on one unambiguous network. A separate electricity content addon may be needed for survival generators and wires.

## What to include in a bug report

- GridWorks build or commit;
- Paper version;
- Rebar version;
- `/gridworks doctor` output;
- relevant log lines;
- exact devices and routes;
- reproduction steps;
- whether the issue survives a clean restart.
