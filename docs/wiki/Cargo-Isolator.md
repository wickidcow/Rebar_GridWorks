# Cargo Isolator

The **Cargo Isolator** is GridWorks' automation-controlled cargo shutoff device.

It is intentionally different from Pylon's Cargo Gate, which is a threshold-based left/right splitter.

## Physical behavior

Cargo Isolator is a one-stack public Rebar cargo buffer with:

- rear input;
- facing-side output.

## OPEN

When OPEN, the isolator uses the configured cargo transfer rate.

Default server setting:

`cargo.isolator.transfer-rate: 1`

## ISOLATED

When isolated, it:

- rejects new inbound writes at its public logistic slot;
- sets its own outbound cargo transfer rate to zero;
- safely retains any stack already buffered inside.

Items therefore do not cross either side while isolated.

## Inputs

Cargo Isolator accepts:

- Default/A-D circuits; or
- a named addressed command.

## Fail-safe behavior

If the active input route changes, Cargo Isolator moves to **ISOLATED** before normal Control Bus replay establishes the new route's command.

Editing an inactive saved address does not unnecessarily interrupt cargo.

## Production example

```text
Sequence Stage 4 ---> cargo_release ---> Cargo Isolator ---> output storage
```

This lets a Sequence Controller hold completed items until the final production stage.

## Related pages

- [[Sequence Controller]]
- [[Factory Controller]]
- [[Power and Flow Control]]
- [[Configuration]]
