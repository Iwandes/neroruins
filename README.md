# NeroRuins

> Part of the Neroland sci-fi Minecraft mod ecosystem, built on **Neroland Core**.

**Status:** multiloader skeleton — version `0.4.0-alpha.1`. No gameplay content yet; the progression
seams and quest pack below are in place for the structures to come.

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
./gradlew :fabric:26.2:build          # one cell
./gradlew :neoforge:26.1.2:build :neoforge:26.2:build :neoforge:26.3:build \
          :forge:26.1.2:build :forge:26.2:build :forge:26.3:build \
          :fabric:26.1.2:build :fabric:26.2:build :fabric:26.3:build   # the nine 26.x cells
```

The 1.21.x cells build the same way, e.g. `./gradlew :neoforge:1.21.1:build`.

## Progression & quests

NeroRuins reports explorer progress through **Neroland Core**: four player-scope record gates
(`neroruins:first_log`, `first_vault`, `puzzle_solver`, `temple_cleared`), the world threshold channel
`neroruins:vault_claimed` (scope = structure type, rungs 1 / 10 / 50 — never a position) and the item
tag `neroruins:relics`. Future structure code calls `progression.RuinMilestones`; nothing opens these
signals yet.

It also ships a **NeroQuests** quest pack — chapter `neroruins:expedition_notes` "Expedition Notes",
7 quests — that appears in the quest book when NeroQuests is installed and is inert otherwise
(NeroQuests is optional; NeroRuins has no dependency on it). The quests become completable once ruins
exist. Two `mod_hook` rewards (`hook/reveal_ruin`, `hook/unlock_log`) are handled by
`progression.QuestHooks`, currently with a placeholder chat line until survey maps and the data-log
archive land. Details: [wiki/Expedition-Notes.md](wiki/Expedition-Notes.md).

## Privacy & telemetry

> **Telemetry notice:** NeroRuins sends anonymous error reports (stack trace + mod/game
> versions only — never IPs, usernames, UUIDs, or world data) to the developers via Sentry
> (EU servers) so crashes can be fixed. On by default — opt out any time by setting `telemetryEnabled = false`
> in `config/neroruins.properties`. Full details:
> [PRIVACY.md](https://github.com/Neroland/neroruins/blob/main/PRIVACY.md).

See [`AGENTS.md`](AGENTS.md) / [`CLAUDE.md`](CLAUDE.md) for agent and contributor context.
