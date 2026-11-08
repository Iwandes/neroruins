# Commands

| Command | Who | What it does |
| --- | --- | --- |
| `/neroruins codex` | any player | Opens your archive as a book. |
| `/neroruins codex <log ID>` | any player | Rereads one of your saved logs in chat. |
| `/neroruins codex chapter <chapter>` | any player | Shows a chapter entry once all six of its logs are archived. |
| `/neroruins privacy export` | any player | Writes your own NeroRuins data to a file in the world folder (see [Privacy](Privacy.md)). |
| `/neroruins gallery [family]` | operator (level 2), in creative | Builds a showcase around you: every NeroRuins block, the Ballast Plate targets, all sixteen glyphs, one of each Sentinel mark, every item in frames, and structure pieces. With no family it shows the bunker and puzzle rooms; with a family (`bunker`, `puzzle`, `lattice_temple`, `halcyon_ark`, `survey_lab`, `colony_field`, `crash_site`) it shows that family's pieces. Puzzles in the pieces are seeded and solvable; vaults in the gallery are for demonstration. |
| `/neroruins gallery clear [family]` | operator (level 2), in creative | Removes the gallery built from this spot. Name the same family you built with. |
| `/neroruins bench <structure> [samples]` | operator (level 2) | Measures how long a NeroRuins structure takes to generate in the current dimension, without placing anything. Reports how many sampled positions produced the structure, mean / 95th-percentile / maximum time, piece counts and the largest size. `samples` is 1–256 (default 32). The server pauses while it runs. |
| `/neroland data eraseme` | any player | Neroland Core's erasure request; removes your NeroRuins data along with other Neroland mods'. |

Build the gallery in a flat, empty area: it clears the space it uses.

[Home](Home.md)
