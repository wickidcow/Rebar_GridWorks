# Timer Controller

The **Timer Controller** creates timed GridWorks Control Bus commands without ticking on every server tick. The physical block looks like a copper bulb and opens a 2-row settings GUI.

## Modes

| Mode | Behavior |
| --- | --- |
| **One-shot** | After an optional start delay, output ON once for the ON duration, then finish OFF |
| **Repeating pulse** | Emit one-tick pulses separated by the chosen OFF interval |
| **Duty cycle** | Alternate chosen ON/OFF durations until stopped |

Each mode needs a **new real OFF-to-ON input transition** to start. The first observed/replayed ON input after a chunk load only sets a baseline. **Manual Start** is also available.

## Menu

| Control | Operation |
| --- | --- |
| Mode | Left/right cycles One-shot, Repeating Pulse, Duty Cycle |
| Input | Select Redstone, Default, A, B, C or D input circuit |
| Input source | Select AUTO or a directly linked loaded peer; selected UUID persists through chunk unload |
| Output Mode | Choose Circuit or Address |
| Output Circuit | Choose Default or A-D |
| Output Address | Edit a name such as \`farm_timer\` when Address mode is selected |
| Start Delay | 0 to 3,600 seconds |
| ON Duration | 1 tick to 3,600 seconds; fixed at 1 tick in Pulse mode |
| OFF Duration | 1 tick to 3,600 seconds |
| Manual Start | Restart the timer now |
| Stop / Reset | Cancel all scheduled work and output OFF |

Duration clicks: **left +5 ticks, right −5 ticks, Shift uses 20-tick steps.** One Minecraft second is 20 ticks at 20 TPS.

## Recipe

\`\`\`text
Copper Ingot     Repeater          Copper Ingot
Clock            Factory Controller Clock
Copper Ingot     Repeater          Copper Ingot
\`\`\`

## Linking

Link a Redstone Sensor or another compatible source directly to the Timer Controller with your GridWorks Linker. Then link the Timer Controller to a Control Relay or Addressed Relay to power a vanilla redstone machine. The Timer publishes Control Bus commands; its copper bulb visual is **not** a redstone output.

The controller binds to **one directly linked, loaded input source** so competing sensor sources cannot accidentally fight over one schedule.

## Safety and performance

- Restart, unload, break, and source loss cancel the old schedule and reset output OFF.
- Replayed ON state after a restart never starts a new timer by itself.
- Delayed wakeups skip overdue transitions instead of producing catch-up pulses.
- At most one delayed Bukkit task exists for a running loaded timer.
- No global polling or forced chunk loads.

Related: [[Control Bus and Linking]], [[Factory Automation]], [[Production Control]].
