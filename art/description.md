[![NeroLink App — Beta](https://img.shields.io/badge/NeroLink_App-Now_in_Beta-60d4e8?style=for-the-badge)](https://nerolandmc.net/nerolink/#beta) [![Explore the Neroland ecosystem](https://img.shields.io/badge/Explore-The_Neroland_Ecosystem-1a5a6c?style=for-the-badge)](https://nerolandmc.net/ecosystem/)

> 📱 **NeroLink App Beta — your Neroland world on your phone.** Check energy, alerts and machines live, claim quest rewards and search your storage without logging in. **[Join the beta at nerolandmc.net →](https://nerolandmc.net/nerolink/#beta)**
>
> 🌌 **Explore the Neroland ecosystem.** See how NeroRuins fits together with the rest of the Nero mods — every mod, wiki and changelog in one place. **[View the ecosystem at nerolandmc.net →](https://nerolandmc.net/ecosystem/)** · [NeroRuins on the website](https://nerolandmc.net/mods/neroruins/)

---

# NeroRuins

**Explore it, loot it, uncover it — abandoned labs, crashed ships, alien temples and station wrecks that give every world a reason to be landed on.**

NeroRuins is the **lore & exploration structures** mod of the Neroland ecosystem. It scatters abandoned research labs, crashed ships, ancient alien temples, underground bunkers, station wrecks and lost colony ruins across Nerospace planets and derelict orbital stations, then fills them with discoverable lore, gated vaults, puzzle doors, unique relics and genuine danger — turning "fly to a planet, mine ore, fly home" into an adventure worth the trip.

Built on **Neroland Core**, so its progression gates (loot and structure tiering), shared `c:` material and space compat tags, and item framework are shared with the rest of the lineup. *(Planned — in design; not yet released.)*

---

## What you can explore

1. **Abandoned Research Labs.** Modular surface or sub-surface complexes of labs, server rooms and storage, themed around the precursor science that shaped the setting — tiered loot, data logs and the occasional sealed wing behind a puzzle door.
2. **Crashed Ships.** Broken hull sections half-buried in terrain, with intact compartments holding salvage, fuel and ship-grade relics, a debris field around the core wreck, and surviving security drones on patrol.
3. **Ancient Alien Temples.** The marquee destination — large set-pieces with the most elaborate puzzles, the deepest lore and the rarest relics, gated by puzzle doors, locked vaults and guardian encounters.
4. **Underground Bunkers.** Compact, hatch-accessed shelters hidden beneath planet surfaces (and rarely on Earth), holding survival caches, schematics and short lore vignettes behind a single vault.
5. **Space Station Wrecks.** Derelict orbital structures that generate on stations and in orbital dimensions rather than on planet surfaces, explored in zero-or-low gravity, holding salvage and station relics.
6. **Lost Colony Ruins.** Sprawling, lower-density fields of collapsed habs, dead farms and broken machines — the cautionary story of those who came before, told in scattered loot and logs.

## Systems that make it worth it

- 📖 **Story through space** — **data logs** (readable items and terminal blocks) deliver the narrative in fragments and register as discoveries, ready to hand off to NeroQuests' journal.
- 🔐 **Risk gates reward** — **locked vaults** need keycards, relics or quest progress to open; **puzzle doors** are solved with redstone/logic, item placement, sequences or environmental clues.
- 💎 **Unique relic loot** — distinctive, named artifacts flagged with Core relic tags: crafting catalysts, economy treasures and quest items rather than generic vendor trash.
- 👾 **Guardians & hazards** — embedded spawners, turrets and trap blocks pull entities from NeroCreatures when present, with Core/vanilla fallbacks otherwise, so looting is earned.
- 🧩 **Re-discoverable, not repetitive** — jigsaw/structure-set worldgen assembles ruins from modular rooms, corridors and props with loot variation, so the tenth lab still surprises.
- 🪐 **Planet-appropriate** — structures register against biome and dimension tags, so a ruin on a lava world differs from one on an ice moon.

## Built for servers

- 🎲 **Deterministic per seed** — every player shares the same ruins; puzzle and vault state is server-authoritative and persists.
- ⚖️ **Fair loot** — vaults support per-player or one-time-claim modes so busy servers avoid first-come-takes-all.
- 🎛️ **Tune anything** — structure spawn rates per type and dimension, loot rarity weighting, guardian toggle/difficulty, and a relic-rarity dial the downstream economy depends on, all server-authoritative.

## Privacy (POPIA / GDPR)

NeroRuins stores **no personal data by default**. Discovery and vault state, where used, is gameplay progress tied to the world save — never identity data, never keyed by player name.

> **Telemetry notice:** NeroRuins sends anonymous error reports (stack trace + mod/game
> versions only — never IPs, usernames, UUIDs, or world data) to the developers via Sentry
> (EU servers) so crashes can be fixed. On by default — opt out any time by setting `telemetryEnabled = false`
> in `config/neroruins.properties`. Full details:
> [PRIVACY.md](https://github.com/Neroland/neroruins/blob/main/PRIVACY.md).

## Why it fits the ecosystem

- 🧩 **Built on Neroland Core** — one progression arc, one item framework, shared `c:` material, relic and space compat tags. NeroRuins ships in its own creative tab.
- 🔌 **Interoperates, never hard-depends** — it feeds lore into **NeroQuests**, relics into **NeroEconomy** and **Nerotech**, guardians from **NeroCreatures**, and templates into **NeroEvents**, all optional. No Nero mod hard-depends on NeroRuins; all treat it as a content provider.
- 🚀 **Soft synergy with Nerospace** — its planets, biomes and orbital/station dimensions are where ruins come into their own. With Nerospace absent, NeroRuins simply has fewer places to generate.
- 🌌 **External interop via Core tags** — Create, AE2, Mekanism, Ad Astra and Energized Power salvage can surface in loot tables through Core material and space compat tags, never a direct dependency.
- 🧱 **Cross-loader** — NeoForge, Forge, and Fabric on Minecraft **26.1.2** and **26.2**.

## Requirements & compatibility

- **Requires [Neroland Core](https://nerolandmc.net/mods/neroland-core/)** — install it alongside NeroRuins (it loads first).
- Optional but recommended: **[Nerospace](https://nerolandmc.net/mods/nerospace/)** for the planets and orbital stations where ruins generate.
- Conventional `c:` material and space compat tags mean Create, AE2, Mekanism, Ad Astra and Energized Power interoperate as the 26.x ecosystem fills in — no hard dependency on any of them.
- **Modpacks are allowed and encouraged** — any platform, no need to ask. Use the official files and credit *NeroRuins by Neroland* with links to the [CurseForge page](https://www.curseforge.com/minecraft/mc-mods/neroruins) and the [GitHub repository](https://github.com/Neroland/neroruins). Full terms: [LICENSE](https://github.com/Neroland/neroruins/blob/main/LICENSE).

## Links

- 📖 **[Wiki](https://github.com/Neroland/neroruins/wiki)** — every structure, system and loot tier documented.
- 💬 **[Discord](https://discord.gg/ArPXvYUzJG)** — chat, help, and sneak peeks.
- 🐞 **[Issues](https://github.com/Neroland/neroruins/issues)** — bug reports and feature requests.
- 🗒️ **[Changelog](https://github.com/Neroland/neroruins/blob/main/CHANGELOG.md)**
- 🔥 **[Also on CurseForge](https://www.curseforge.com/minecraft/mc-mods/neroruins)**

---

*Created by Neroland. The project logo was made with the help of AI image tools; in-game art is generated by the project's own tooling and refined by hand.*
