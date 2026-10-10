# Timer / Clock Controller – event engine (phase 1)

GridWorks' new `TimerCycleEngine` is a **testable scheduling core**, not yet a craftable Rebar block. The current release and player-facing content IDs do not change in this phase.

## Modes and behavior

| Mode | Behavior |
| --- | --- |
| One-shot | A real OFF→ON input edge starts an optional delay, then emits ON for a configured duration and completes OFF |
| Repeating pulse | A real OFF→ON edge starts a train of one-tick pulses separated by a configurable OFF interval |
| Duty cycle | A real OFF→ON edge starts alternating configurable ON/OFF windows |

Every mode permits an initial delay (including zero), with subsequent ON/OFF periods from 1 to 72,000 ticks. A FALSE input cancels pending work and goes immediately OFF. An operator can also start a schedule explicitly.

**Replay and lag safety:**

- The first observed input after load, including a replayed TRUE, only establishes a baseline; it cannot trigger a false rising edge.
- Settings changes and unload reset the transient schedule to OFF.
- A delayed wake-up performs at most one state transition. Overdue cycles are skipped, never replayed in a burst.
- Deadlines are calculated with saturating arithmetic, so long uptimes cannot wrap negative.
- The engine is pure Java; no Bukkit calls, polling loops, global scan, chunk load, or task creation.

## Next physical integration

The eventual Timer Controller adapter should:

1. Extend `PhysicalControlNodeBlock` and implement `ControlStateSource`.
2. Use persisted mode/duration/input routing, but **never persist an active scheduler task** or start a task during component replay.
3. Schedule **only one** Bukkit delayed task for the engine's `nextDueTick`, while loaded and active. Cancel it on unload, break, restart, configuration changes and missing Control Bus peers.
4. Publish OFF on activation before receiving fresh input; propagate ON/OFF only on actual engine transitions. Keep output channels and addressed command routing compatible with existing receivers.
5. Expose mode/duration/manual-start controls through the Rebar GUI; add a recipe, English item metadata, guide entry, and content-catalog integrity check together.
6. Regression-test the live block on Paper/Rebar for restart, chunk reload, sudden peer loss, and heavy lag. Only then introduce it as a craftable item.

This keeps Timer separate from Rebar's actual electricity network simulation: GridWorks decides when control signals switch, while Rebar owns electrical power delivery.
