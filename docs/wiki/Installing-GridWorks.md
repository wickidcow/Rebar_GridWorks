# Installing GridWorks

## Requirements

The current GridWorks development line requires:

| Software | Version |
| --- | --- |
| Paper | 26.2 |
| Java | 25 |
| Rebar | 0.44.4-26.2 |
| GridWorks | 0.4.x development line |

Pylon is the primary ecosystem GridWorks is designed to complement, but GridWorks currently depends directly on Rebar.

## Installation

1. Stop the server normally.
2. Install the official electricity-enabled Rebar 0.44.4-26.2 server JAR.
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

GridWorks development builds are tested on real Paper 26.2 with the SHA-256-verified Rebar 0.44.4 release before publishing.

Next: [[Getting Started]]
