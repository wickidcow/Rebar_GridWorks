# Developer API

GridWorks exposes service boundaries so other addons can integrate without depending on GridWorks internals.

## ControlBus

GridWorks registers its Control Bus through Bukkit's ServicesManager.

The bus carries namespaced typed signals across the live linked topology and supports state replay when components reconnect.

## PowerGridProvider

Third-party electricity systems can register a `PowerGridProvider`.

The provider associates a loaded adjacent block with a grid and returns provider-neutral telemetry. Bukkit service priority selects the active provider.

## PowerBranchProvider

A `PowerBranchProvider` exposes a controllable electrical branch on a loaded block face.

A provider may support:

- open/closed branch state;
- optional watt limiting.

GridWorks surfaces capability differences rather than pretending an unsupported command succeeded.

## Stable telemetry families

Examples include:

- `gridworks:redstone/*`
- `gridworks:machine/*`
- `gridworks:fluid/*`
- `gridworks:alarm/*`
- `gridworks:batch/*`
- `gridworks:sequence/*`
- `gridworks:power/*`

Changed power snapshots end with `gridworks:power/sample_revision`.

## Released Rebar API boundary

GridWorks deliberately avoids known internal/unreleased boundaries including:

- Rebar internal FluidManager;
- internal CargoRoutes;
- using TickingRebarBlock.isTicking as a processing-state proxy;
Native Rebar electricity is integrated using the pinned development API `1.0.0-20260929.193904-140` from upstream commit `5e34938`. Use a matching Rebar development server JAR; the stable `0.43.0-26.2` JAR lacks this API. See [Native Electricity](https://github.com/wickidcow/Rebar_GridWorks/blob/main/docs/wiki/Native-Electricity.md).

CI checks production source for these boundaries.

## Building

```bash
./gradlew build
```

Paper and Rebar remain server-provided compile-only dependencies and are not intended to be shaded into the GridWorks JAR.
