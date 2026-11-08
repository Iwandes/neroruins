# NeroRuins

> Part of the Neroland sci-fi Minecraft mod ecosystem, built on **Neroland Core**.

**Status:** beta — version `0.5.0-beta.1`. Six ruin families with puzzles, guardians, per-player vaults,
relics, data logs and expedition tools. Requires **Neroland Core 1.17.4**; most ruins generate on
**Nerospace**'s worlds. Player documentation lives in the [wiki](wiki/Home.md).

## Features

- Six ruin families — Survey Bunker, Lattice Temple, Halcyon Ark, Survey Lab, Colony Field and Crash Site —
  in outpost, complex and citadel sizes, each a jigsaw hub with seeded satellite structures
  (`worldgen.structure.ComplexHub`, `worldgen.placement.SatellitePlacement`). Structure pieces are authored
  as JSON specs in `tools/templates/` and built into NBT by `tools/build_templates.py`.
- Six puzzle types seeded per instance by the `neroruins:puzzle_seed` processor (`puzzle/`), Sealed Doors,
  and reinforced unbreakable shells around gated rooms.
- Guardians: the Sentinel entity, the Ruin Spawner with data-driven `spawn_tables/`, and the Arc Grate
  (`guardian/`).
- Vault caches with per-player rolls and held rewards (`vault/`, `data/ClaimLedger`), tiered loot with
  optional sibling-mod entries (`neroruins:mod_loaded`) and nine relics (`neroruins:relic_chance`).
- The data-log archive and Codex, the Expedition Record, Expedition Map and Survey Scanner.
- Operator commands `/neroruins gallery [family]` and `/neroruins bench <structure> [samples]`.

## Build targets

- **Minecraft:** 1.21.1, 1.21.4, 1.21.5, 1.21.8 (also 1.21.7), 1.21.10 (also 1.21.9), 1.21.11, 26.1.2, 26.2 and 26.3
- **Loaders:** NeoForge, MinecraftForge/Forge, Fabric (the "27 cells")
- **Java:** 21 for the 1.21.x builds, 25 for 26.x
- Mod id: `neroruins` · package `za.co.neroland.neroruins`

## Layout

The build is the repo root, with a flattened cross-loader structure driven by Stonecutter:

- `common/` — shared, loader-agnostic source spliced into every loader node
- `fabric/` — Fabric Loom
- `forge/` — ForgeGradle
- `neoforge/` — ModDevGradle
- `stonecutter.gradle` — the real root build script; `build.gradle` is intentionally inert

## Building

```sh
./gradlew :fabric:26.3:build          # current target
./gradlew :neoforge:26.1.2:build :neoforge:26.2:build :neoforge:26.3:build \
          :forge:26.1.2:build :forge:26.2:build :forge:26.3:build \
          :fabric:26.1.2:build :fabric:26.2:build :fabric:26.3:build   # the nine 26.x cells
```

The 1.21.x cells build the same way, e.g. `./gradlew :neoforge:1.21.1:build`.

## Progression & quests

NeroRuins reports explorer progress through **Neroland Core**: player-scope record gates
(`neroruins:first_log`, `first_vault`, `puzzle_solver`, `codex/<chapter>`), the world threshold channel
`neroruins:vault_claimed` (scope = ruin type, rungs 1 / 10 / 50 — never a player or position) and the item
tags `neroruins:relics` and `neroruins:keycards`. It also ships a **NeroQuests** chapter,
`neroruins:expedition_notes` "Expedition Notes", that appears when NeroQuests is installed and is inert
otherwise (NeroQuests is optional). Details: [wiki/Expedition-Notes.md](wiki/Expedition-Notes.md).

## Privacy & telemetry

> **Telemetry notice:** NeroRuins sends anonymous error reports (stack trace + mod/game
> versions only — never IPs, usernames, UUIDs, or world data) to the developers via Sentry
> (EU servers) so crashes can be fixed. On by default — opt out any time by setting `telemetryEnabled = false`
> in `config/neroruins.properties`. Full details:
> [PRIVACY.md](https://github.com/Neroland/neroruins/blob/main/PRIVACY.md).

See [`AGENTS.md`](AGENTS.md) / [`CLAUDE.md`](CLAUDE.md) for agent and contributor context.
