# Prototype structures

The bunker outpost prototype is available for testing in a disposable creative
world with `/place structure neroruins:bunker_prototype`. It assembles a hub,
corridors and rooms from weighted pools. Turns and junctions can produce
branching layouts. The world seed and location influence the selected pieces
and their orientation.

The rooms use Survey Hull and Rusted Survey Hull, with some panels weathered
during placement. Both blocks are available in the NeroRuins creative tab and
can be recovered with a pickaxe.
Terminal rooms also contain a chest with a randomly selected Earth data shard.
The chest is shared like a vanilla chest; it is not a player-specific vault.
The archive-room variant also contains a readable log terminal.

The prototype does not generate naturally and is not a finished bunker. Its
terrain integration, lighting, access, loot balance, guardians, puzzles and
vault are still under development. It is intended for testing, not survival
progression.

## Vault cache prototype

A separately placed Vault Cache can be assigned a loot table by an operator for testing. The
default policy lets each player claim once, provided the rewards fit in their inventory. A full
inventory does not consume the claim. `vaults.mode` can also be set to `first_come` or
`timed(hours)`, such as `timed(24)`. The cache remains a prototype: it has no puzzle lock or
survival placement, and multiplayer and crash-recovery behavior are still being verified.

[Home](Home.md)
