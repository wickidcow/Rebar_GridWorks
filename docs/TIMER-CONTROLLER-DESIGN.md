# Timer / Clock Controller – event engine (phase 1)

GridWorks' `TimerCycleEngine` is now used by the craftable physical **Timer Controller** (`timer_controller`) introduced on this feature branch. It includes a Rebar block, Control Bus routing, an InvUI menu, English item metadata, a recipe, and a guide entry.

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

## Physical integration

The Timer Controller extends `PhysicalControlNodeBlock` and implements `ControlStateSource`, `GuiRebarBlock` and `BooleanInputConfigurable`.

- One directly linked, currently loaded sensor/controller source drives the selected input channel. Losing that source stops the schedule.
- The GUI configures mode, initial delay, ON/OFF durations, input routing, Default/A-D or named output routing, and manual Start/Stop.
- The physical block never persists an active task: only settings survive a restart/chunk reload, with the command output reset OFF until a fresh rising edge.
- Before unload/break, a shared physical-block lifecycle hook lets the Timer publish OFF **while recipients are still linked**, preventing a previously ON output from being stranded when its source disappears.
- The current schedule owns at most one delayed Bukkit task, guarded against unloaded/broken nodes.
- The copper bulb acts as a status light; vanilla redstone output requires a linked Control Relay or Addressed Relay.

## Required release validation

Unit and catalog tests, production JAR validation, Paper/Rebar startup and `/gridworks doctor` must pass. Also manually verify, on an actual client, GUI interaction, first-on replay, link loss during ON, chunk unload/reload during ON, changing an output address, and in-game recipe display before merging or releasing.
