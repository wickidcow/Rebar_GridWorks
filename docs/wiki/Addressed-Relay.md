# Addressed Relay

The **Addressed Relay** converts one human-readable GridWorks addressed command into vanilla redstone output.

It is the bridge between named factory routes and ordinary Minecraft redstone.

## Why addresses matter

The simple Control Bus provides five boolean circuits:

- Default
- A
- B
- C
- D

That is enough for small systems, but large factories benefit from meaningful names.

Examples:

```text
ore_line_1
smelter_batch_done
tank_fill_enable
optional_loads
```

## Basic use

```text
Factory Controller
Output address: ore_line_1
          |
          v
Addressed Relay ---> vanilla redstone
```

## Multicast groups

Multiple Addressed Relays may deliberately listen to the same address.

Example:

```text
Address: emergency_stop
       |
       +--> Relay A
       +--> Relay B
       +--> Relay C
```

This creates a simple multicast control group without requiring a new physical command circuit for each receiver.

## Address normalization

Players configure the human-readable address. GridWorks maps it into its namespaced addressed-control channel internally.

## When to use one

Addressed Relay is especially useful for:

- named production lines;
- remote stage outputs;
- grouped redstone shutdown;
- large factories with many independent commands.

## Related pages

- [[Control Relay]]
- [[Factory Controller]]
- [[Sequence Controller]]
- [[Control Bus and Linking]]
