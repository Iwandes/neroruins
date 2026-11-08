# Guardians

Before every puzzle gate there is a guardian zone. You cannot reach the gate without passing through it.

## Sentinel

NeroRuins' mechanical guard, in three marks.

| Mark | Health | Melee | Charged shot | Armour |
| --- | --- | --- | --- | --- |
| Sentinel Mk I | 24 | 3 | 2 | 4 |
| Sentinel Mk II | 40 | 5 | 4 | 8 |
| Sentinel Mk III | 64 | 7 | 6 | 12 |

Up close it strikes with a claw. Between 4 and 16 blocks, with a clear line of sight, it stops, sparks for
one second and fires an arc that hits only its target. The shot never damages blocks or starts fires and
recharges after three seconds. Sentinels drop salvage, better for higher marks. Sentinels from a Ruin
Spawner never despawn; spawn eggs exist for all three marks.

## Ruin Spawner

- Wakes only for players in survival or adventure mode within 8 blocks. Creative and spectator players do
  not wake it.
- Spawns one guardian every five seconds while fewer than four hostile mobs are nearby, and nothing on
  Peaceful.
- Has a limited budget — 4, 6 or 8 guardians for tiers 1, 2 and 3 — then goes dark for good.
- Cannot be broken in survival.

## Arc Grate

A floor trap. When charged it shocks a survival player who steps on it for 3 damage, then goes dark for
three seconds. Mobs cross it unharmed. Arc Grates can be mined.

## Spawn tables

What a spawner summons comes from data files in `data/<namespace>/spawn_tables/`, one per family, which
data packs can change. Each entry is an entity id with a weight and a tier range. Entries for other mods —
for example NeroCreatures' Rogue Androids, Rogue Drones, Void Crawlers, Lunar Stalkers and Crystal Golems —
are optional: when the mod is not installed they are skipped. Zombies and skeletons are the fallback.

[Home](Home.md)
