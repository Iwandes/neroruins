# NeroRuins Wiki

**NeroRuins** fills the Neroland worlds with ruins: abandoned Survey bunkers, labs and colonies, a broken
generation ship, crashed freighters and the temples of the vanished Architects. Each ruin has rooms to
explore, guardians, a puzzle gate, a vault and data logs that tell one story across nine chapters.

> **Status: 0.5.1-beta.1.** Everything on these pages is in the mod and covered by automated tests, but
> most structures have not yet been played through end to end in survival. Please report anything that
> breaks.

## Requirements

- **Minecraft** 1.21.1–1.21.11, 26.1.2, 26.2 or 26.3 on NeoForge, Forge or Fabric.
- **Neroland Core 1.17.4** or later (required).
- **Nerospace** (optional, but needed for most ruins): every family except the Earth bunker generates on
  Nerospace's moons and planets, and the Halcyon Ark generates in its station dimension. Without
  Nerospace only the Earth bunker generates (and the Ark in vanilla void worlds).
- Other Neroland mods are optional. When installed, their items join the loot and NeroCreatures mobs join
  the guardian lists. NeroQuests adds the [Expedition Notes](Expedition-Notes.md) quest chapter.

## Ruins

| Family | Where | Page |
| --- | --- | --- |
| Survey Bunker | rare on Earth's dry land; every moon and planet | [Survey Bunker](Survey-Bunker.md) |
| Lattice Temple | citadel on Nyxara; complexes on Nyxara and Cindara; wayshrines everywhere | [Lattice Temple](Lattice-Temple.md) |
| Halcyon Ark | the station's void; a severed section on Luna | [Halcyon Ark](Halcyon-Ark.md) |
| Survey Lab | citadel under Mars; labs on Greenxertz and Glacira; outposts everywhere | [Survey Lab](Survey-Lab.md) |
| Colony Field | citadel on Glacira; complex on Greenxertz; outposts on Mars, Glacira and Greenxertz | [Colony Field](Colony-Field.md) |
| Crash Site | the *Meridian* on Cindara; wrecks on every moon and planet | [Crash Site](Crash-Site.md) |

Ruins come in up to three sizes: **outposts** (small, common), **complexes** and **citadels** (the
headline ruin of a world, deliberately rare). Each ruin is a hub with outlying satellite pieces; the
layout, room choices and satellites vary between worlds but are fixed for a given seed.

## Systems

- [Puzzles](Puzzles.md) — the six puzzle types, Sealed Doors and vault modes.
- [Guardians](Guardians.md) — Sentinels, Ruin Spawners and Arc Grates.
- [Vaults and loot](Vaults-and-Loot.md) — per-player vault rolls, loot tiers and the nine relics.
- [Expedition tools](Expedition-Tools.md) — the Expedition Map, Survey Scanner and Surveyor's Compass.
- [Data shards and the archive](Data-Shards.md) — logs, terminals and the Codex.
- [Expedition Notes](Expedition-Notes.md) — progression milestones and the quest chapter.
- [Commands](Commands.md) · [Configuration](Configuration.md) · [Privacy](Privacy.md)

## Not yet included

Per-world decay for most families, NeroAgriculture planters in colony farms, named cemetery markers, an
Ark cryopod block, uses for the Reach Thermal Core and Rootglass Vessel beyond their held effects, an admin
placement command, the NeroLink link module and companion-app pages.

## Error reporting

NeroRuins sends anonymous, mod-specific error reports to its Sentry project in the EU by default. Set
`telemetryEnabled=false` in `config/neroruins.properties` and restart to opt out. See [Privacy](Privacy.md).
