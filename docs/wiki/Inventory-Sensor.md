# Inventory Sensor

The **Inventory Sensor** watches the adjacent inventory it faces and publishes inventory telemetry onto the Control Bus.

## What it measures

The sensor reports:

- availability;
- total item count;
- occupied slots;
- total slots;
- occupied-slot ratio.

## Supported inventories

GridWorks checks Rebar virtual/logistic inventories before falling back to ordinary Bukkit container inventories.

This makes the sensor useful with both Rebar-style logistics and normal Minecraft storage.

## Placement

The Inventory Sensor watches the adjacent inventory in the direction it faces.

Example:

```text
[Chest / Rebar Inventory] [Inventory Sensor] ---> Control Bus
```

## Sampling

There is no one universal inventory-change event covering every inventory type GridWorks supports.

For that reason, all **loaded** Inventory Sensors share one sampler.

Default:

**10 ticks / 0.5 seconds at 20 TPS**

A new snapshot is published only when the measured state changes.

## Factory Controller examples

Stop a production line when output storage becomes crowded:

```text
Inventory Sensor ---> Factory Controller
Rule: occupied ratio >= threshold
```

Start a process only when enough input exists:

```text
Rule: total item count >= required stock
```

## Performance notes

The shared sampler means GridWorks does not schedule one independent repeating task per Inventory Sensor.

Server owners can tune the common interval in [[Configuration]].

## Related pages

- [[Factory Controller]]
- [[Factory Monitor]]
- [[Sensors and Telemetry]]
- [[Configuration]]
