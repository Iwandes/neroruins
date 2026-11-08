# Vaults and loot

## Vault Caches

A Vault Cache rolls its loot **for you, at the moment you claim it**, so a late arrival gets the same chance
as the first explorer (in the default `per_player` mode; see [Puzzles](Puzzles.md#vault-modes)).

- Open the cache and choose **Claim rewards**.
- If the roll does not fit in your inventory, it is **held for you**: the exact items are kept and delivered
  the next time you claim with enough room. Claiming again never rerolls.
- A cache behind a puzzle stays sealed (and looks sealed) until its puzzle is solved. Vault Caches cannot
  be broken in survival.

## Loot tiers

Ruin containers draw from five tiers. Families mix them by room: salvage in outer rooms, better tiers in
vaults.

| Tier | Contents |
| --- | --- |
| 0 — Salvage | scrap metal and circuitry, iron, redstone, Survey Hull |
| 1 — Supplies | food, medical and oxygen supplies, seeds, colony goods; rarely an **Expedition Map** |
| 2 — Components | machine, storage and power components, Nero alloy plates; rarely a **Survey Scanner** |
| 3 — Schematics and keycards | **keycards**, currency, faction and event tokens, rare exotics, void crystal, starsteel, diamonds |
| 4 — Relics | a chance at one of the nine relics, plus void crystal, diamonds or starsteel |

**Items from other Neroland mods** (Nerospace, Nerotech, NeroLogistics, NeroEconomy, NeroCreatures and
others) appear only when those mods are installed. Without them, NeroRuins, Neroland Core and vanilla items
fill the same slots, and nothing is ever missing.

## Relics

Nine named relics, one per chapter of the story. They are trophies, keys for **relic sockets** (the socket
gives the relic back) and give a small effect while held in either hand. Effects do not stack.

| Relic | While held |
| --- | --- |
| Surveyor's Compass | Points toward the nearest ruin you have not entered yet |
| Pale Survey Lens | Night vision |
| Director's Seal | Luck |
| Field Recorder | Shows how many logs of your archive you have recorded |
| Meridian Keystone | Fire resistance |
| Reach Thermal Core | You do not freeze |
| Rootglass Vessel | Slow regeneration below half health |
| Twilight Prism | Nearby hostile creatures glow (for everyone) |
| Resonance Heart | Slowly repairs a worn item you are wearing or holding |

Some vaults always contain a specific relic — the one a nearby socket needs — so no ruin depends on a rare
drop. Relics cannot be sold through NeroEconomy.

### Relic drop rate: the shared balance lever

Tier-4 loot rolls its relic pool with a **base chance of 25%**, set in the loot table so data packs can
change it. The server setting **`loot.relicRarity`** (default `1.0`, range `0.25`–`4.0`) multiplies that
chance: `0.5` halves it, `2.0` doubles it (capped at 100%). The loot condition behind it is
`neroruins:relic_chance`, which other data packs can use too. Guaranteed relics in specific vaults are not
affected.

[Home](Home.md)
