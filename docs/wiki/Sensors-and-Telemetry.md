# Sensors and Telemetry

GridWorks sensors turn nearby state into stable Control Bus telemetry.

Where a public event exists, GridWorks prefers event-driven updates. Where there is no universal event, loaded sensors share one conservative sampler and publish only when their snapshot changes.

## Redstone Sensor

Publishes powered state and analog signal strength from 0-15.

## Inventory Sensor

Faces an adjacent inventory and publishes:

- availability;
- total item count;
- occupied slots;
- total slots;
- occupied-slot ratio.

It supports Rebar virtual/logistic inventories before falling back to ordinary Bukkit containers.

Default shared sample interval: **10 ticks**.

## Fluid Tank Sensor

Faces an adjacent block using Rebar's released fluid-tank API.

Publishes fluid availability, presence, type, amount, capacity, and fill ratio.

Default shared sample interval: **10 ticks**.

## Machine Sensor

Faces an adjacent supported Rebar processor.

Publishes processing state, normalized progress, process time, ticks remaining, observed cycles, and last-cycle time.

### Observed Cycles

A cycle increments when a continuously available target is observed changing from processing to idle.

**Observed Cycles is activity telemetry, not guaranteed crafted-output accounting.**

Right click the Machine Sensor to inspect it. Shift + right click **Reset Observed Cycles** to begin a new operator-defined batch count while preserving the current processing baseline.

Default shared sample interval: **20 ticks**.

## Power Grid Sensor

Uses a registered GridWorks `PowerGridProvider` to report provider-neutral electrical telemetry.

A changed multi-field sample ends with `gridworks:power/sample_revision` so controllers can evaluate a complete snapshot.

Default shared sample interval: **20 ticks**.

If a provider or target becomes unavailable, GridWorks marks the telemetry unavailable instead of holding stale values.
