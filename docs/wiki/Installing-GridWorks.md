# Installing GridWorks

## Requirements

The current GridWorks development line requires:

| Software | Version |
| --- | --- |
| Paper | 26.2 |
| Java | 25 |
| Rebar | 0.43.0-26.2 |
| GridWorks | 0.3.x development line |

Pylon is the primary ecosystem GridWorks is designed to complement, but GridWorks currently depends directly on Rebar.

## Installation

1. Stop the server normally.
2. Install the matching released Rebar build.
3. Download the current GridWorks development JAR.
4. Place both JARs in the server's `plugins/` directory.
5. Start the server.
6. Check the startup log.
7. Run `/gridworks doctor` as an operator.

Development builds are published as a raw JAR:

https://github.com/wickidcow/Rebar_GridWorks/releases/tag/dev-build

## Verifying the installation

A healthy runtime should finish:

```text
GridWorks Doctor result: PASS
```

The doctor command verifies important runtime invariants rather than merely printing version information.

## Updating

Use a normal server stop/start when replacing GridWorks or Rebar. Avoid server/plugin reload systems while testing persistent control networks.

GridWorks development builds are smoke-tested on a real Paper server with released Rebar before the rolling development JAR is published.

Next: [[Getting Started]]
