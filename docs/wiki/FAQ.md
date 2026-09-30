# Frequently Asked Questions

## Does GridWorks require Pylon?

GridWorks is designed for the Pylon/Rebar ecosystem, but the current hard gameplay dependency is Rebar.

## Why does GridWorks use a Control Bus instead of only redstone?

The Control Bus can carry typed telemetry, multiple boolean circuits, and named addresses across one persistent automation topology.

Vanilla redstone remains useful at the edges through relays and sensors.

## How many simple command circuits are there?

Five:

- Default
- A
- B
- C
- D

For larger systems, use named addressed commands.

## Can several devices listen to one address?

Yes.

That is an intended way to build multicast groups such as:

- `optional_loads`
- `emergency_stop`
- `ore_line_1`

## Does GridWorks force-load chunks?

Normal automation is designed not to force-load target chunks.

If a target is unavailable, devices wait for normal lifecycle recovery.

## Why do some sensors sample instead of use events?

There is no one universal event for every supported inventory, Rebar fluid tank, machine processor, or external power provider.

GridWorks shares samplers by sensor type and only publishes changed snapshots.

## Does Machine Sensor count actual crafted items?

No.

Observed Cycles records observed processing-to-idle activity. It is useful for production control but is not guaranteed item-output accounting.

## Why did Batch Controller ignore old Machine Sensor cycles?

That is intentional.

The first count after link/relink becomes a baseline. Only later positive deltas count toward the current batch.

## Can restart trigger my Pulse Relay?

Current replay-safe behavior is specifically designed to prevent an already-ON replayed state from manufacturing a false rising edge.

## Can restart advance my Sequence Controller?

A replayed already-ON stage trigger establishes a baseline only. A real false-to-true transition is required to advance.

## Why is my Smart Breaker or Power Grid Sensor unavailable?

GridWorks' electricity layer is provider-neutral.

Native Rebar electricity is integrated using the pinned development API `1.0.0-20260929.193904-140` from upstream commit `5e34938`. Use a matching Rebar development server JAR; the stable `0.43.0-26.2` JAR lacks this API. See [Native Electricity](https://github.com/wickidcow/Rebar_GridWorks/blob/main/docs/wiki/Native-Electricity.md).

## Is GridWorks safe to update during development?

Use a normal server stop/start and keep backups as you would for any actively developed automation plugin.

The rolling development JAR passes compilation/tests plus a real Paper + pinned Rebar electricity smoke gate before publication.

## Where do I find recipes?

Use the installed Rebar/Pylon guide.

See [[Recipes and Progression]].

## How do I check whether GridWorks itself is healthy?

Run:

`/gridworks doctor`

A healthy runtime ends with:

`GridWorks Doctor result: PASS`
