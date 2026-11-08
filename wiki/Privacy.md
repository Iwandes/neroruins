# Privacy

NeroRuins keeps only what its features need, keeps it in the world save, and lets every player export and
erase their own data. It is designed to meet the GDPR (EU) and POPIA (South Africa). The full disclosure,
including error reporting, is in the repository's `PRIVACY.md`.

## What is stored, per player

| Store | What it holds | How long |
| --- | --- | --- |
| Archive | your UUID, each log ID you have read, and when you first read it | until erasure, or Neroland Core's inactivity purge |
| Vault claims | your UUID, the vault's world and position, and when you claimed it | until erasure, or `privacy.claimRetentionDays` if set |
| Held vault rewards | your UUID, the vault's world and position, its loot table, when the roll was made, and the held items | until delivered, or erased with your claims |
| Expedition Record | your UUID and, for each ruin you entered, its type, family, size, world, when you first entered, and the hub position | entries until erasure; positions dropped after `privacy.expeditionRetentionDays` (default 365), or never stored if `privacy.recordDiscoveryPositions=false` |

None of these stores your name. Ruin positions are shown only to you (Expedition Map). The Surveyor's
Compass works from your own record in memory and stores nothing. Progression milestones live in Neroland
Core's progression store, covered by Core's own privacy page. A world-level vault ledger counts claims per
ruin type only — never who or where.

## Your rights

- **See your data:** `/neroruins privacy export` writes your archive, read times, vault claims, held rewards
  and Expedition Record to `neroruins-exports/<your UUID>.json` in the world folder. The server operator can
  read that file. A new export replaces the old one.
- **Erase your data:** `/neroland data eraseme` (Neroland Core) removes your NeroRuins data — archive,
  claims, held rewards, Expedition Record and your export file — together with your data in other Neroland
  mods. If any part cannot be erased, Core reports that the erasure was incomplete so it can be retried.

## Error reporting

Anonymous error reports (no IP, name, UUID, coordinates or world data) go to Sentry in the EU. On by default;
set `telemetryEnabled=false` in `config/neroruins.properties` to opt out.

[Home](Home.md)
