# Commands and Permissions

GridWorks intentionally keeps its administrative command surface small.

## /gridworks doctor

```text
/gridworks doctor
```

Permission:

`gridworks.admin`

Default: **OP**

Doctor reports a compact live health snapshot including:

- plugin version;
- content/recipe registration;
- active Control Bus nodes;
- live and persisted links;
- loaded inventory, fluid, machine, and power sensors;
- power-grid provider availability;
- branch-provider availability;
- loaded branch-control devices;
- pending GridWorks scheduler tasks;
- validated runtime settings.

A healthy runtime ends with:

```text
GridWorks Doctor result: PASS
```

## What PASS/FAIL means

Doctor separates required runtime invariants from optional integrations.

Current required checks include GridWorks' content/recipe integrity, Control Bus service identity, shared sensor samplers, and enabled plugin singleton.

A missing power provider is informational because power integrations are optional.

## Why doctor matters

The same doctor command is executed during GridWorks' real Paper + Rebar CI smoke test before the rolling development JAR is published.
