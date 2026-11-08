# Configuration

Settings live in `config/neroruins.properties`, created on first launch.

| Key | Default | Effect |
| --- | --- | --- |
| `telemetryEnabled` | `true` | Anonymous error reports (client-local). See [Privacy](Privacy.md). |
| `worldgen.bunker.enabled` | `true` | Generate Survey Bunkers (Earth and planets). |
| `worldgen.latticeTemple.enabled` | `true` | Generate Lattice Temples. |
| `worldgen.halcyonArk.enabled` | `true` | Generate the Halcyon Ark. |
| `worldgen.surveyLab.enabled` | `true` | Generate Survey Labs. |
| `worldgen.colonyField.enabled` | `true` | Generate Colony Fields. |
| `worldgen.crashSite.enabled` | `true` | Generate Crash Sites. |
| `loot.relicRarity` | `1.0` | Multiplier for relic drop chances, `0.25`–`4.0`. See [Vaults and loot](Vaults-and-Loot.md#relic-drop-rate-the-shared-balance-lever). |
| `vaults.enabled` | `true` | Allow vault claims. |
| `vaults.mode` | `per_player` | `per_player`, `first_come` or `timed(hours)`. See [Puzzles](Puzzles.md#vault-modes). |
| `archive.keepShards` | `false` | Keep data shards after reading them. |
| `privacy.recordDiscoveryPositions` | `true` | Store the hub position of ruins a player enters in their own Expedition Record. |
| `privacy.expeditionRetentionDays` | `365` | Days to keep discovery positions; `0` keeps them until erasure. |
| `privacy.claimRetentionDays` | `0` | Days to keep vault claims; `0` keeps them until erasure. Expiring a claim makes that vault claimable again. |

Turning a family off stops new generation; ruins already generated stay in the world.

These keys are present but have **no effect yet**: `guardians.enabled`, `traps.enabled`,
`privacy.archiveRetention` and `pushNotifications.enabled`.

Spawn tables, loot tables, biome tags and structure placement are data and can be changed with data packs.

[Home](Home.md)
