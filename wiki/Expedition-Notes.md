# Expedition Notes (progression and quests)

NeroRuins reports what explorers do through **Neroland Core** progression signals, and ships a quest
chapter for **NeroQuests** that reads them. NeroQuests is optional: without it the quest files are inert.

## The quest chapter

When NeroQuests is installed, the chapter **Expedition Notes** appears in the quest book. Its quests follow
an expedition from the first bunker to the Lattice's last room:

- **First steps** — find a Survey Bunker, read your first data log, defeat a Sentinel.
- **Ruin craft** — open a locked vault, solve a puzzle door yourself, find a keycard, recover a relic and
  then three.
- **The families** — enter each kind of ruin: Lattice Temple, Halcyon Ark, Survey Lab, Colony Field and
  Crash Site.
- **The story** — complete each chapter of the Codex by archiving all six of its logs.
- **Server milestones** — shared quests that complete when someone on the server claims the first vault of
  each family, the first vault anywhere, and fifty vaults of one ruin type.

Rewards are experience and expedition gear. The book shows each quest's objective and reward.

## Personal milestones

Player-scope record gates in `data/neroruins/neroland_gates/`. They record that a player did something;
they never lock anything (vault locks and puzzle doors keep their own logic).

| Gate | Opens when |
| --- | --- |
| `neroruins:first_log` | you read a data log for the first time |
| `neroruins:first_vault` | you claim a vault that was sealed behind a puzzle |
| `neroruins:puzzle_solver` | you solve a puzzle |
| `neroruins:codex/<chapter>` | you have archived all six logs of that chapter |

## World milestone

`neroruins:vault_claimed` fires on Core's threshold channel each time the number of claimed vaults of one
**ruin type** reaches 1, 10 or 50 in a world. Its scope is the ruin type (for example
`neroruins:lattice_temple`) and its value the new count. It never carries a player, a UUID or a position:
it says how much has been plundered, never where or by whom.

## Tags for other mods

- `neroruins:relics` — the nine relics.
- `neroruins:keycards` — the three keycards.

[Home](Home.md)
