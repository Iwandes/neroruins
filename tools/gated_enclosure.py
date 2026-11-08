"""Shared check for decision 039: rooms behind a puzzle gate cannot be mined into.

A family test builds a block map (position -> block, every position the structure owns) and asks whether a
player who mines everything breakable could get from the gated side of a door to anywhere it must not reach: out
of the structure (open air or terrain) or back to the door's near side. Only unbreakable blocks stop them.
"""

from collections import deque

UNBREAKABLE = frozenset((
    "neroruins:reinforced_survey_hull", "neroruins:reinforced_rusted_survey_hull",
    "neroruins:reinforced_latticework", "neroruins:reinforced_lattice_floor",
    "neroruins:sealed_door", "neroruins:vault_cache", "neroruins:ruin_spawner", "neroruins:ballast_plate",
    "neroruins:glyph_clue", "neroruins:glyph_lock", "neroruins:conduit", "neroruins:conduit_source",
    "neroruins:conduit_lock", "neroruins:resonance_pylon", "neroruins:resonance_clue", "neroruins:card_reader",
    "neroruins:relic_socket",
))


def block_id(block):
    return block["id"] if isinstance(block, dict) else block


def breach(blocks, start, forbidden=()):
    """The first position a miner starting at `start` reaches outside the structure or in `forbidden`, else None."""
    forbidden = set(forbidden)
    if block_id(blocks.get(start)) in UNBREAKABLE:
        raise ValueError("the start of an enclosure check must be inside the room, not in its wall")
    seen = {start}
    queue = deque([start])
    while queue:
        x, y, z = queue.popleft()
        for pos in ((x - 1, y, z), (x + 1, y, z), (x, y - 1, z), (x, y + 1, z), (x, y, z - 1), (x, y, z + 1)):
            if pos in seen:
                continue
            if pos not in blocks or pos in forbidden:
                return pos
            if block_id(blocks[pos]) in UNBREAKABLE:
                continue
            seen.add(pos)
            queue.append(pos)
    return None
