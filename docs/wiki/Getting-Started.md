# Getting Started

This page builds the simplest useful GridWorks network.

## 1. Craft the basic control components

GridWorks registers a dedicated **GridWorks** category on the root of `/rebar guide`. Open the guide and choose **GridWorks** to browse the addon by section:

- Core & Linking
- Sensors
- Logic & Production
- Monitoring & Alarms
- Actuators & Power

Every current GridWorks item is also still available through Rebar's normal guide search and recipe pages.

For a first network, you will want:

- GridWorks Linker
- Redstone Sensor
- Status Light or Control Relay

## 2. Place two control nodes

A simple test network can be:

```text
Redstone Sensor ---> Status Light
```

or:

```text
Redstone Sensor ---> Control Relay ---> vanilla redstone
```

## 3. Link them

With the **GridWorks Linker**:

- Right click the first control node to select it.
- Right click the second control node to link or unlink it.
- Sneak + right click a node to inspect its network.
- Sneak + left click supported actuators to cycle their circuit input.

Links persist across restart.

## 4. Test the signal

Power the Redstone Sensor. The linked device should receive its state through the Control Bus.

GridWorks reconnects loaded topology without scanning the world or force-loading unrelated chunks.

## 5. Move beyond direct signals

Once the basic network works, add a Factory Controller:

```text
Inventory Sensor ---> Factory Controller ---> Addressed Relay
```

The sensor provides telemetry, the controller makes the decision, and the relay performs the action.

For larger factories, named addresses are usually easier to manage than sharing only Default/A-D circuits.

Continue with [[Control Bus and Linking]] and [[Factory Automation]].
