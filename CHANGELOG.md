# Changelog

All notable changes to **NeroRuins** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.5.1-beta.1] - 2026-11-09

Log Terminals no longer drop as empty terminals, plus a guard that keeps loot-only items uncraftable. Requires
**Neroland Core 1.17.4**.

### Added

- The wiki's *Vaults and Loot* page lists where every NeroRuins item comes from.
- A data check keeps relics, keycards, data shards, vault and puzzle blocks, reinforced walls and spawn eggs
  uncraftable, and keeps the unbreakable fixtures from dropping themselves.
- `tools/gen_recipe_unlocks.py` is ready for the first recipes (it generates their recipe-book unlocks;
  `--check` verifies). NeroRuins still adds no recipes.

### Changed

- **A mined Log Terminal drops Survey Hull** instead of an empty terminal, which kept no log.

## [0.5.0-beta.1] - 2026-11-08

The ruins arrive: six structure families, puzzles, guardians, vaults, relics and expedition tools. Requires
**Neroland Core 1.17.4**. This is a beta: everything below is built and covered by automated tests, but
most structures have not yet been played through in a survival world. Please report anything that breaks.

### Added

- **Six ruin families**, each a hub with seeded satellite structures, so layouts, room choices and
  outlying pieces differ between worlds but stay the same for a given seed. Apart from the Earth bunker,
  they generate in Nerospace's worlds, so install Nerospace to find them (the Halcyon Ark also appears in
  vanilla void worlds):
  - **Survey Bunker** — a hatch-and-ladder shelter on every world, and rarely on Earth's dry land
    (roughly one every 1–1.5 km).
  - **Lattice Temple** — the Architects' precincts. The Lattice Citadel on Nyxara descends two spiral
    wells to a sanctum about 86 blocks below its plaza; complexes on Nyxara and Cindara, wayshrines everywhere.
  - **Halcyon Ark** — the Survey's broken generation ship drifting in the station's void, with a severed
    hull section on Luna.
  - **Survey Lab** — a research complex that goes down: twelve floors under Mars, smaller labs on
    Greenxertz and Glacira, and outposts everywhere.
  - **Colony Field** — a lost colony's sprawl, with its citadel on Glacira and a complex on Greenxertz.
  - **Crash Site** — the half-buried freighter *Meridian* on Cindara with a debris field, and wrecks on
    every moon and planet.
- **Puzzles**, all solvable from clues in the room and randomised per ruin: Ballast Plates (weight),
  glyph sequences with a keypad screen, conduit routing, resonance tuning, three keycard tiers with card
  readers, and relic sockets that hand the relic back. Sealed Doors open when a nearby puzzle is solved.
- **Guardians**: the Sentinel in three marks, with melee and a charged arc shot that never damages
  blocks; the Ruin Spawner, which wakes for nearby players and has a limited budget; and the Arc Grate
  floor trap. Spawn lists are data-driven and include NeroCreatures mobs when that mod is installed.
- **Vault caches** that roll their loot per player. A roll that does not fit in your inventory is held for
  you and delivered exactly when you return. Vaults behind a puzzle stay sealed until it is solved; with
  `first_come` vault mode the solver's roll is reserved.
- **Protected gated rooms**: every room behind a puzzle gate is enclosed in reinforced, unbreakable and
  blast-proof hull or lattice, so gates cannot be bypassed by mining. Shells outside gates stay mineable.
- **Loot tiers 0–4** with nine named relics (one per chapter). Each relic grants a small passive while held;
  the Surveyor's Compass points to your nearest unexplored ruin. Sibling-mod items join the loot only
  when those mods are installed.
- **Expedition Record**, **Expedition Map** and **Survey Scanner**. The map lists the ruins you have
  entered; the scanner gives the direction and rough distance of the nearest ruin, never coordinates.
- **Data logs** placed through the structures in story order, readable from terminals and shards, collected
  in the Codex.
- **Expedition Notes quests** (NeroQuests, optional): 29 quests that walk you through the release — the
  first bunker and log, Sentinels, vaults, puzzles, keycards and relics, finding each ruin family, finishing
  each Codex chapter, and server milestones for claiming each citadel's vault. Completing a Codex chapter now
  opens a progression gate other mods and quest packs can use.
- Blocks: Lattice palette (Latticework, Lattice Floor, Resonance Crystal Block, sixteen Glyph Stones),
  puzzle fixtures, reinforced variants, and a facing Log Terminal.
- Commands for operators: `/neroruins gallery [family]` builds a showcase of the mod's blocks, items,
  Sentinels and structure pieces, and `/neroruins bench <structure> [samples]` measures generation cost.
- `/neroruins privacy export` now also lists held vault rewards and your Expedition Record.

### Changed

- Now requires Neroland Core 1.17.4 or later, which reports incomplete player-data erasure.
- Survey Bunker hubs sit deeper, so rooms on slopes stay buried and the hatch still reaches the surface.
- The Codex checks client support before sending its screen payload and explains when the client cannot
  receive it.

### Privacy

- New personal data (held vault rewards, the Expedition Record) is included in export and in Core's
  erasure. Expedition positions are dropped after `privacy.expeditionRetentionDays`.

### Not yet included

- Most structures have not been walked end to end in survival; generation cost is measured and within budget.
- Per-world decay for most families, NeroAgriculture planters in colony farms, named cemetery markers,
  an Ark cryopod block, uses for the Reach Thermal Core and Rootglass Vessel, an admin placement command,
  the NeroLink link module and companion-app pages, and push notifications.
- The `guardians.enabled`, `traps.enabled`, `privacy.archiveRetention` and `pushNotifications.enabled`
  settings are reserved and have no effect yet.

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
