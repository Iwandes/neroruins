# Changelog

All notable changes to **NeroRuins** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Data shards, an erasure-aware personal archive, and a book-style Codex.
  Prototype bunker chests can contain Earth shards, but the bunker does not
  generate naturally.
- A prototype log terminal with a paginated screen and per-player archive action.
  The command-placeable bunker archive room can contain a terminal; natural bunker
  generation remains in development. A rotating shard marks the terminal.
- A command-placeable vault-cache prototype and personal claim ledger. Puzzle locking,
  survival placement, multiplayer and crash-recovery verification remain unfinished.
- A distinct Survey-style data shard texture.
- Survey Hull and Rusted Survey Hull blocks, with procedural weathering in the
  bunker prototype.
- A self-service archive export, including first-read times; Core erasure also
  removes the player's export file.
- Seed-bound hub and satellite placement with temporary test templates. The
  finished ruin families and their gameplay are not yet implemented.

### Changed

- The Codex checks client support before sending its screen payload and gives
  an explanation when the client cannot receive it.
- The archive stores reader UUIDs, log IDs and first-read times in the world
  save and recovery backup. Core's erasure hook removes a player's entries.

## [0.4.1-alpha.1] - 2026-10-31

### Changed

- Release uploads now target CurseForge and GitHub Releases only.
- Removed automatic project description and gallery uploads.
- Renamed the website overview source to `art/description.md` and refreshed public documentation links.

## [0.4.0-alpha.1] - 2026-10-30

Builds for Minecraft 1.21.x. Requires Neroland Core 1.17.0.

### Added

- **Minecraft 1.21.x builds** on NeoForge, Fabric and Forge, next to the 26.1.2, 26.2 and 26.3 builds: one
  build each for 1.21.1, 1.21.4, 1.21.5, 1.21.8 (also runs on 1.21.7), 1.21.10 (also runs on 1.21.9) and
  1.21.11. The 1.21.x builds need Java 21. Everything works the same on every version.

### Changed

- Now requires Neroland Core 1.17.0 or later.

## [0.3.0-alpha.1] - 2026-10-11

Progression seams and a quest pack, ahead of the structures. Requires **Neroland Core 1.15.0**. No
gameplay content yet: the quests below become completable once ruins, vaults, puzzle doors, guardians
and relics exist.

### Added

- **NeroQuests quest pack.** Ships chapter `neroruins:expedition_notes` "Expedition Notes", 7 quests
  (5 player, 2 server-scope world-log), that appears in the quest book when NeroQuests is installed;
  inert otherwise, no dependency. Quests read the record gates and channel below, plus a
  `collect_item` on the `neroruins:relics` tag. Rewards are experience, a few plain items and two
  `mod_hook` rewards.
- **Record gates** (`data/neroruins/neroland_gates/`, player scope, no `requires`, never checked for
  access): `first_log`, `first_vault`, `puzzle_solver`, `temple_cleared`, and the hook gates
  `hook/reveal_ruin` and `hook/unlock_log`.
- **`progression.RuinMilestones`** — the one place future structure code reports progress:
  `firstLog` / `firstVault` / `puzzleSolver` / `templeCleared(ServerPlayer)` open the record gates, and
  `vaultClaimed(server, structureTypeId)` counts claimed vaults per structure type and fires the
  threshold channel **`neroruins:vault_claimed`** (scope = structure type id, rungs 1 / 10 / 50) on
  Neroland Core's bus. Counts persist in the new `neroruins:vault_ledger` saved-data store, loaded
  through Core's crash-safe `SavedDataRecovery`.
- **`progression.QuestHooks`** — listens on Core's gate events for the two hook gates, delivers to the
  online player (or on their next join, via each loader's player-join hook) and re-arms the gate.
  Placeholder effect until the content exists: one translatable chat line per hook. The survey-map
  (`reveal_ruin`) and lore-archive (`unlock_log`) effects land with the structures.
- **`lifecycle.ServerState`** — records the running server from each loader's start/stop events so
  common code reacting to Core events can find it (and forgets it between single-player worlds).
- Item tag `neroruins:relics` (empty until relic items exist) and the mod's first `en_us.json`.
- Wiki page [`Expedition-Notes.md`](wiki/Expedition-Notes.md).

### Changed

- Neroland Core pin `1.13.0` → **`1.15.0`** (offline-capable `ProgressionGates.open/close/isOpen` and
  sourced `ThresholdEvents.fire`); the loader manifests' floor follows it automatically.
- Privacy: no new personal data. Personal milestones live in Core's player-scope gate store (covered
  by Core's privacy notes and erasure hook); the vault ledger holds structure type ids and counts
  only; the channel scope is a structure type, never a player or a position. `PRIVACY.md` notes it.

## [0.2.0-alpha.1] - 2026-09-24

EMI compatibility. No gameplay, id, tag or config change.

### Added

- **EMI support.** NeroRuins adds no recipes of its own, so nothing needed a plugin. The build now
  compiles against the community EMI Unofficial Port (Unstable), the only EMI build for Minecraft 26.x,
  and dev clients load it with `-PwithEmi` (default runs stay JEI-only). EMI stays optional.

## [0.1.0-alpha.1] - 2026-09-20

Minecraft **26.3** support. No gameplay, id, tag or config change.

### Added

- **Minecraft 26.3** as a new Stonecutter node on every loader — NeoForge `26.3.0.7-beta`,
  Forge `26.3-66.0.2` and Fabric (fabric-api `0.161.0+26.3`, NeoForm `26.3-1`) — built alongside
  26.1.2 and 26.2, so every release now ships **nine** loader × version jars.

### Changed

- VS Code run/debug configurations (`.vscode/launch.json`, `.vscode/tasks.json`) gain the three
  26.3 cells; the "Build all" task now builds all nine.
- CI (`multiloader.yml`, `publish.yml`) builds, attaches and publishes the 26.3 jars.
- JEI pins moved to the newest published builds on each Minecraft version: `29.40.0.101` (26.1.2), `30.35.0.223` (26.2) and `31.3.0.18` (26.3). Compile-time API only — JEI remains a soft dependency and the shipped jar gains no hard requirement.

### 26.3 port notes

- Build: the shared `common/` Java source is now preprocessed by Stonecutter for every non-active node (`stonecutterProcessCommon`), so common code can carry `//? if >=26.3 {` blocks, and `common/src/main/resources-<mc>` overlay folders are merged over the shared resources for matching nodes (`mergeCommonResources`). The active node still compiles the raw `common/` folder.
- Build plugins aligned with Neroland Core: ModDevGradle `2.0.147` (the older 2.0.141 cannot set up NeoForge 26.3), ForgeGradle `7.0.40`, Stonecutter `0.9.8`.
