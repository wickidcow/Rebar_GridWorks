# Native Electricity

GridWorks 0.4 uses native Rebar electricity via the public API included in [Rebar 0.44.4-26.2](https://github.com/pylonmc/rebar/releases/tag/0.44.4-26.2). The earlier development API from electricity PR #711 is no longer needed for this build.

**Requires Paper 26.2, Java 25, and the released Rebar 0.44.4-26.2 server JAR.** CI downloads that JAR and checks SHA-256 `fd7573a3124b9836641a4e15fc85ff998b3fed894f58f5eb8002eff44d3a884b` before the real-server smoke test. The Maven API JAR is only a compile dependency; do not install it as the Rebar server plugin. Existing GridWorks item IDs and saved controls remain unchanged.

## Connect a powered branch

1. Obtain a Power Coupler from `/rebar guide` → GridWorks → Actuators and Power.
2. Use a Rebar-compatible wire item from an electricity content addon to connect the supply to the coupler's **north** port, and its **south** port to the load. Electricity support in Rebar alone does not provide a survival generator or wire recipe.
3. Place a Smart Breaker facing the coupler. It starts OPEN. Open the breaker menu and close it manually, or link a controller with the GridWorks Linker.
4. Optionally place a Power Limiter facing the same coupler from another side and enter its watt cap. BYPASS removes only the coupler's cap. It does not close an open breaker or raise a wire's rating.
5. Face a Power Grid Sensor toward a generator or another electrical block on the grid. A closed coupler can also be sampled; an open coupler separates two grids and is intentionally reported unavailable. Link it to a Factory Monitor or Load Shedding Controller.

The coupler disconnects its own internal edge while OPEN (or capped at zero). Closing reconnects that edge and restores its configured limit in both directions. This avoids zero-capacity routing loops while preserving Rebar's native electricity graph. Rebar retains cable ratings, directions and network simulation. A separate bypass wire around the coupler bypasses its control, so place all loads you intend to control behind it.

Switch state and watt cap persist independently. A limiter cannot reopen a breaker. The native electricity tick applies power changes on Rebar's next scheduled update. Multiple independent controllers aimed at the same coupler can issue conflicting commands; use one breaker and one limiter per coupler.

## Readings and scale

The native provider reports generation capacity, demand and powered consumers. Generation capacity is available output, not energy already consumed; surplus acceptors do not count as fixed consumer demand. Wire caps can leave a consumer unpowered even when total supply exceeds demand.

Sensors sharing one electrical network reuse a snapshot within the same server tick. The adapter reads only loaded targets, never searches the world and never force-loads chunks. A block connected to multiple independent electrical networks is reported unavailable rather than combining unrelated grids. Existing sensor budgets continue to bound sampling work.

## With VaultWorks

Wire the coupler's load side to the top port of a Powered Vault Cell. Its default demand is 32 W. Cargo enters WEST and exits EAST. Opening the breaker or limiting the available supply below its demand pauses automatic transfer on the next electricity update. Right-clicking the cell still gives direct access to its stored items.

## Check the installation

Run `/gridworks doctor`: both provider lines should show `Rebar native electricity`. If a class-loading error mentions `electricity`, check that the installed Rebar **server** JAR contains PR #711. A sensor reports unavailable for a non-electric block, unloaded target, or ambiguous multi-grid target.
