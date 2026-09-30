# Stock Controller

The Stock Controller keeps an inventory inside a target operating band without rapidly switching a factory line on and off around one threshold.

It consumes telemetry from a directly linked [[Inventory Sensor]] and publishes a normal GridWorks boolean command. **ON means the connected production line needs to make more stock.**

## Hysteresis

The controller has two thresholds:

- **Start at/below** — when stock falls to or below this value, output turns ON.
- **Stop at/above** — once production is running, output stays ON until stock reaches this value.

Between the two thresholds, the controller keeps its previous demand state.

Example:

```text
Start at/below: 256 items
Stop at/above:  1024 items

0 -------- 256 ========================== 1024 -------->
           START / keep ON                 STOP / OFF
                 hysteresis deadband
```

This is intentionally different from a single Factory Controller comparison. A single threshold can chatter when stock repeatedly moves just above and below the boundary. The Stock Controller creates a stable refill band.

## Metrics

The first implementation supports:

- **Item Count** — total items in the observed inventory.
- **Occupied Slot Fill** — occupied slots as a percentage of total slots.

Changing metric resets the thresholds to sensible defaults and returns source selection to AUTO.

## Source selection

AUTO binds to the first matching **directly linked** Inventory Sensor.

The GUI can also cycle through currently loaded direct links to pin the controller to one source. GridWorks never scans the world and never force-loads another chunk to discover a source.

If the selected source unloads or reports unavailable telemetry, the controller enters **WAITING** and publishes fail-safe OFF.

## Output routing

The refill-demand output supports the same routing model as other GridWorks controllers:

- Default circuit;
- A / B / C / D circuit;
- human-readable Address.

Example:

```text
Inventory Sensor ---> Stock Controller ---> iron_smelter_enable
                                              |
                                              +--> Addressed Relay
                                              +--> Cargo Isolator
                                              +--> another controller
```

## Reload behavior

The hysteresis latch is persisted so a running refill operation does not forget which side of the deadband it was on.

However, persisted demand is **not** blindly replayed after load. The controller first waits for current source telemetry; until then its physical output is fail-safe OFF. Once fresh/replayed telemetry arrives, the persisted latch is used only to resolve values that are inside the low/high deadband.

This avoids both false startup commands and hysteresis state loss.

## Suggested uses

- keep common ingots between a minimum and maximum stock;
- refill machine-input buffers automatically;
- stop farms when their output storage is sufficiently full;
- prevent production from toggling every time one stack is inserted or removed;
- drive addressed production groups from one inventory target.

For arbitrary multi-sensor conditions, use [[Factory Controller]]. For exact production counts, use [[Batch Controller]].
