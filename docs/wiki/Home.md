# GridWorks

**Industrial automation, smart power management, and factory control systems for Pylon/Rebar.**

Welcome to the official GridWorks Wiki.

GridWorks is a Rebar addon built around one goal: make Minecraft factories **smarter**, not simply faster. Sensors observe machines and resources, the Control Bus moves telemetry and commands, controllers make decisions, and actuators safely control redstone, cargo, fluids, and electrical branches.

> **Current development line:** 0.3.x  
> **Minecraft / Paper:** 26.2  
> **Java:** 25  
> **Rebar:** 0.43.0-26.2

## Where should I start?

| I want to... | Read |
| --- | --- |
| Install GridWorks | [[Installing GridWorks]] |
| Build my first network | [[Getting Started]] |
| Understand links, circuits, and addresses | [[Control Bus and Linking]] |
| See every current GridWorks device | [[Devices and Machines]] |
| Learn the crafting/progression path | [[Recipes and Progression]] |
| Automate a factory | [[Factory Automation]] |
| Follow a complete example build | [[Example Four Stage Factory]] |
| Build batch or multi-stage production | [[Production Control]] |
| Automate power, fluids, or cargo | [[Power and Flow Control]] |
| Configure a server | [[Configuration]] |
| Check commands and permissions | [[Commands and Permissions]] |
| Diagnose a problem | [[Troubleshooting]] |
| Improve server-side efficiency | [[Performance and Optimization]] |
| Find quick answers | [[Frequently Asked Questions|FAQ]] |
| Integrate another addon with GridWorks | [[Developer API]] |
| See what is coming next | [[Roadmap and Compatibility]] |

## What GridWorks adds

GridWorks currently includes a complete control and automation foundation:

- persistent Control Bus links;
- Default, A, B, C, and D command circuits;
- human-readable addressed commands;
- redstone, inventory, fluid, machine, and power sensors;
- Factory Controller rules and a live Factory Monitor;
- latched alarms and a central Alarm Console;
- Batch Controller production targets, watchdogs, rate, and ETA;
- four-stage Sequence Controller workflows;
- Smart Breaker and Power Limiter provider contracts;
- Fluid Valve and Cargo Isolator;
- load shedding with Essential, Normal, and Optional tiers.

GridWorks is designed to avoid world scans and unnecessary chunk loading. Where polling is unavoidable, loaded devices share conservative samplers and publish only changed snapshots.

## A small example

```text
Machine Sensor ---> Batch Controller ---> batch_done
                         |
                         +---- fault ----> Sequence Controller interlock

Power Grid Sensor ---> Load Shedding Controller
                              |
                              +--> optional_loads ---> Smart Breakers
```

The Wiki focuses on how to **use** these systems. The repository README and source remain the authoritative implementation reference during active development.

---

GridWorks is an independent Minecraft server plugin project and is not affiliated with or endorsed by Mojang or Microsoft.
