# Puzzles

Every vault worth having sits behind a puzzle. Each puzzle can be solved from what is in the room, and its
answer is randomised per ruin from the world seed, so no two temples share a solution. Puzzle blocks —
locks, clues, doors, plates, pylons, conduits, readers and sockets — cannot be broken in survival.

## Solving opens what is nearby

When a lock is solved it opens every **Sealed Door** and unseals every sealed **Vault Cache** within
8 blocks sideways and 4 blocks up or down of it. A sealed vault tells you so when you try to claim it.

## The six puzzle types

| Puzzle | Clue in the room | What you do |
| --- | --- | --- |
| **Ballast Plates** | Each plate shows 1–8 lights | Put exactly that many items on every plate near the vault, then claim. Too many or too few keeps it sealed. |
| **Glyph sequence** | Four glyphs carved on the wall | Open the Glyph Lock keypad and enter the four glyphs, read left to right as you face the wall. A wrong entry resets the lock for a few seconds. |
| **Conduit routing** | Conduit pieces on the floor, a Conduit Source and a Conduit Lock | Use pieces to turn them until power flows from the source to the lock. Pieces light up as power reaches them. |
| **Resonance tuning** | A Tuning Crystal shows the target colour | Use each Resonance Pylon to step through its eight pitches until every pylon matches. Each pylon plays its note. |
| **Keycards** | A Card Reader shows its tier as lit bars | Use the reader holding a Survey (tier 1), Security (tier 2) or Command (tier 3) Keycard. A higher tier opens a lower-tier reader. The card is never taken. |
| **Relic socket** | The socket names the relic it wants | Use it holding that chapter's relic. The socket reads the relic and gives it back. |

Keycards come from the ruins themselves — usually a chest found before the reader — and from tier-3 loot.
They cannot be sold through NeroEconomy.

## Vault modes

Server operators choose how vaults are shared with `vaults.mode` ([Configuration](Configuration.md)):

| Mode | Behaviour |
| --- | --- |
| `per_player` (default) | Every player can claim each vault once, with their own roll. |
| `first_come` | The first claim takes the vault. When a puzzle unseals a vault, the solver's roll is reserved, so someone waiting beside the vault cannot take it. Others are told the vault was opened first. |
| `timed(hours)` | Each player can claim again after the given number of hours, e.g. `timed(24)`. |

See [Vaults and loot](Vaults-and-Loot.md).

[Home](Home.md)
