# Delay Relay

The **Delay Relay** delays ON and OFF transitions independently before producing vanilla redstone output.

It is useful for timing, debounce, and preventing rapid machine cycling.

## Configurable delays

ON and OFF delays can each be configured independently from:

**instant through 60 seconds**

Example:

```text
Input turns ON
     |
     |  ON delay
     v
Relay output turns ON
```

The OFF transition can use a completely different delay.

## Stale-transition cancellation

If the input reverses before a delayed transition executes, GridWorks cancels the stale pending task.

Example:

1. Input turns ON.
2. A 5-second ON delay starts.
3. Input turns OFF after 2 seconds.
4. The old pending ON transition is cancelled.

This prevents delayed actions from firing after the condition that requested them has already disappeared.

## Good uses

Delay Relay can provide:

- startup delay;
- shutdown grace period;
- debounce;
- minimum reaction spacing;
- protection from rapid on/off cycling.

## Example

```text
Factory Controller ---> Delay Relay ---> vanilla redstone machine
```

A noisy threshold can therefore be prevented from instantly switching downstream equipment.

## Related pages

- [[Control Relay]]
- [[Pulse Relay]]
- [[Factory Controller]]
- [[Control Bus and Linking]]
