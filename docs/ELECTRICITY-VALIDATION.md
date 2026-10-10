> Current development CI runs Paper 26.2 and the SHA-256-pinned released Rebar 0.44.4-26.2 JAR. The earlier Rebar build 2064 validation below is historical.

# Electricity validation — 2026-09-30

Tested with Java 25, Paper 26.2 build 129, and the unmodified upstream Rebar server artifact `rebar-2064.jar`, from successful workflow run 36620712190 at commit `5e34938f044dc63c103213e80b07484bf4994639`.

The upstream download ZIP matched GitHub's SHA-256 `ed9bbaf30511b804beaf2daf1b3024e0612056bac8b3d78f0550460d9b1394d0`.

A disposable Paper server loaded GridWorks and VaultWorks together and placed real Rebar blocks. A test-only producer supplied 100 W to a Power Coupler and a 32 W Powered Vault Cell. Checks passed on first boot and after a clean shutdown/restart:

- New coupler starts open and the cell remains unpowered.
- Both cargo slot write gates reject insertion/extraction during an outage; direct GUI access remains available.
- Closing the coupler with an 80 W cap powers the cell and allows both cargo write gates.
- Grid readings report 100 W capacity, 32 W demand and one powered consumer.
- Reducing the cap to 16 W leaves the cell unpowered without changing its seven stored diamonds.
- After restart, the same seven diamonds, the coupler's closed state and its 80 W setting remain.
- `/gridworks doctor` reports PASS on both runs.

The initial test exposed an upstream infinite routing loop when an electrical edge is connected with a zero-watt limit. The coupler therefore disconnects its owned internal edge while open or capped at zero; closing reconnects it and restores its limit. External wire edges are never rewritten.

These are lifecycle, power-routing and inventory-gate checks, not a large-server throughput benchmark or a complete request-crafting test. VaultWorks' distributed index, terminal and request crafting remain planned.
