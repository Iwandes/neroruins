"""Authoring script for the lattice_temple family. Writes the template spec and every worldgen/data file.

Run from the repository root: python3 tools/lattice_temple_layout.py .
Then regenerate the structure NBT with tools/build_templates.py.
"""
import json
import sys
from collections import deque
from pathlib import Path

L = "neroruins:latticework"
F = "neroruins:lattice_floor"
C = "neroruins:resonance_crystal_block"
A = "minecraft:air"
DT = "minecraft:polished_deepslate"
DTILE = "minecraft:deepslate_tiles"
GRATE = "neroruins:arc_grate"
ENTRY = "neroruins:lattice_temple_entry"
EXIT = "neroruins:lattice_temple_exit"
NS = "neroruins:"

# Decision 039: rooms behind a puzzle gate are sealed by unbreakable, blast-proof reinforced blocks.
REINFORCED = {L: "neroruins:reinforced_latticework", F: "neroruins:reinforced_lattice_floor"}
REINFORCED_DEFAULT = "neroruins:reinforced_latticework"
UNBREAKABLE = {
    "neroruins:reinforced_latticework", "neroruins:reinforced_lattice_floor", "neroruins:reinforced_survey_hull",
    "neroruins:reinforced_rusted_survey_hull", "neroruins:sealed_door", "neroruins:glyph_clue", "neroruins:glyph_lock",
    "neroruins:resonance_pylon", "neroruins:resonance_clue", "neroruins:relic_socket", "neroruins:vault_cache",
    "neroruins:ruin_spawner", "neroruins:card_reader", "neroruins:conduit", "neroruins:conduit_source",
    "neroruins:conduit_lock"}
# Pools whose pieces lie wholly behind the citadel's glyph gate.
GATED_POOLS = {"lattice_temple_citadel_organs", "lattice_temple_citadel_finals"}


def G(n):
    return {"id": "neroruins:glyph_stone", "properties": {"glyph": str(n % 16)}}


def stair(facing):
    return {"id": "minecraft:deepslate_tile_stairs",
            "properties": {"facing": facing, "half": "bottom", "shape": "straight", "waterlogged": "false"}}


ORIENT = {"n": "north_up", "s": "south_up", "w": "west_up", "e": "east_up"}


class T:
    def __init__(self, name, size):
        self.name = name
        self.size = list(size)
        self.fills = []
        self.connectors = []
        self.vaults = []
        self.spawners = []
        self.terminals = []
        self.containers = []

    def box(self, a, b, block):
        self.fills.append({"from": list(a), "to": list(b), "block": block})

    def room(self, wall=L, floor=F, base=0):
        w, h, d = self.size
        self.box((0, 0, 0), (w - 1, h - 1, d - 1), wall)
        self.box((1, base + 1, 1), (w - 2, h - 2, d - 2), A)
        self.box((1, base, 1), (w - 2, base, d - 2), floor)

    def connector_pos(self, face, along, y):
        w, h, d = self.size
        return {"n": (along, y, 0), "s": (along, y, d - 1), "w": (0, y, along), "e": (w - 1, y, along)}[face]

    def opening(self, face, along, y, width=3, height=4, depth=1):
        w, h, d = self.size
        half = width // 2
        for k in range(depth):
            if face == "n":
                self.box((along - half, y, k), (along + half, y + height - 1, k), A)
            elif face == "s":
                self.box((along - half, y, d - 1 - k), (along + half, y + height - 1, d - 1 - k), A)
            elif face == "w":
                self.box((k, y, along - half), (k, y + height - 1, along + half), A)
            else:
                self.box((w - 1 - k, y, along - half), (w - 1 - k, y + height - 1, along + half), A)

    def entry(self, face="n", along=None, y=1, depth=1):
        along = self.size[0] // 2 if along is None else along
        self.opening(face, along, y, depth=depth)
        self.connectors.append({"pos": list(self.connector_pos(face, along, y)), "orientation": ORIENT[face],
                                "name": ENTRY, "target": "minecraft:empty", "pool": "minecraft:empty",
                                "final_state": "minecraft:air"})

    def exit(self, face, along, pool, y=1, priority=None, depth=1):
        self.opening(face, along, y, depth=depth)
        c = {"pos": list(self.connector_pos(face, along, y)), "orientation": ORIENT[face],
             "name": EXIT, "target": ENTRY, "pool": NS + pool, "final_state": "minecraft:air"}
        if priority:
            c["priority"] = priority
        self.connectors.append(c)

    def terminal(self, pos, log, facing):
        if log:
            self.terminals.append({"pos": list(pos), "log_id": NS + log, "facing": facing})

    def chest(self, pos, table):
        self.containers.append({"pos": list(pos), "loot_table": NS + "chests/" + table})

    def vault(self, pos, table, structure):
        self.vaults.append({"pos": list(pos), "loot_table": NS + "chests/" + table, "structure": NS + structure})

    def spawner(self, pos, tier):
        self.spawners.append({"pos": list(pos), "table": NS + "lattice_temple", "tier": tier})

    def spec(self):
        out = {"name": self.name, "size": self.size, "fills": self.fills}
        for key in ("connectors", "vaults", "spawners", "terminals", "containers"):
            if getattr(self, key):
                out[key] = getattr(self, key)
        return out


# ---------------------------------------------------------------- decision 039: sealing gated rooms

def block_name(block):
    return block["id"] if isinstance(block, dict) else block


def grid_of(t):
    grid = {}
    for fill in t.fills:
        (x0, y0, z0), (x1, y1, z1) = fill["from"], fill["to"]
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    grid[(x, y, z)] = block_name(fill["block"])
    for key, value in (("containers", "minecraft:chest"), ("vaults", "neroruins:vault_cache"),
                       ("spawners", "neroruins:ruin_spawner"), ("terminals", "neroruins:log_terminal")):
        for entry in getattr(t, key):
            grid[tuple(entry["pos"])] = value
    for connector in t.connectors:
        grid[tuple(connector["pos"])] = connector["final_state"]
    return grid


SIDES = ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))


def neighbours(pos):
    x, y, z = pos
    return [(x + dx, y + dy, z + dz) for dx, dy, dz in SIDES]


def opening_cells(size, connector):
    """The face cells a connector's doorway occupies: three wide, four tall."""
    w, h, d = size
    x, y, z = connector["pos"]
    face = connector["orientation"]
    cells = set()
    for a in (-1, 0, 1):
        for dy in range(4):
            cells.add({"north_up": (x + a, y + dy, 0), "south_up": (x + a, y + dy, d - 1),
                       "west_up": (0, y + dy, z + a), "east_up": (w - 1, y + dy, z + a)}[face])
    return cells


def gated_air(size, grid, gated_openings):
    """Air a player cannot walk into from outside the piece while every sealed door is shut."""
    w, h, d = size
    inside = lambda p: 0 <= p[0] < w and 0 <= p[1] < h and 0 <= p[2] < d
    air = {p for p, b in grid.items() if b == A}
    face = lambda p: p[0] in (0, w - 1) or p[1] in (0, h - 1) or p[2] in (0, d - 1)
    seeds = [p for p in air if face(p) and p not in gated_openings]
    seen = set(seeds)
    queue = deque(seeds)
    while queue:
        for n in neighbours(queue.popleft()):
            if n in air and n not in seen and inside(n):
                seen.add(n)
                queue.append(n)
    return air - seen


def breaches(size, grid, gated, gated_openings):
    """Mineable cells a player tunnelling in from outside reaches next to gated air."""
    w, h, d = size
    inside = lambda p: 0 <= p[0] < w and 0 <= p[1] < h and 0 <= p[2] < d
    passable = lambda p: p not in gated and grid.get(p) not in UNBREAKABLE
    outside = [p for x in range(-1, w + 1) for y in range(-1, h + 1) for z in range(-1, d + 1)
               for p in [(x, y, z)] if not inside(p)]
    seen = set(outside)
    queue = deque(outside)
    found = set()
    while queue:
        cur = queue.popleft()
        for n in neighbours(cur):
            if n in seen or not inside(n) or not passable(n) or n in gated_openings:
                continue
            seen.add(n)
            if grid.get(n) != A and any(m in gated for m in neighbours(n)):
                found.add(n)
                continue
            queue.append(n)
    return found


def seal(t, gated_entry=False):
    """Swap every mineable block on the skin of the piece's gated rooms for its reinforced form."""
    grid = grid_of(t)
    openings = set()
    for c in t.connectors:
        pool = c["pool"].split(":", 1)[1] if c["name"] == EXIT else None
        if (c["name"] == ENTRY and gated_entry) or (pool in GATED_POOLS):
            openings |= opening_cells(t.size, c)
    gated = gated_air(t.size, grid, openings)
    fixed = {tuple(e["pos"]) for key in ("containers", "terminals") for e in getattr(t, key)}
    for pos in sorted(breaches(t.size, grid, gated, openings)):
        assert pos not in fixed, f"{t.name}: a container or terminal sits in the gated room's shell at {pos}"
        block = REINFORCED.get(grid.get(pos), REINFORCED_DEFAULT)
        t.box(pos, pos, block)
    return t


# ---------------------------------------------------------------- decoration helpers

def pillar(t, x, z, y0, y1, glyph=None, cap=None):
    t.box((x, y0, z), (x, y1, z), L)
    if glyph is not None:
        t.box((x, y0 + 2, z), (x, y0 + 2, z), G(glyph))
    if cap:
        t.box((x, y1, z), (x, y1, z), cap)


def frieze(t, axis, fixed, start, end, y, seed):
    for i, p in enumerate(range(start, end + 1)):
        if (i + seed) % 3 == 0:
            pos = (p, y, fixed) if axis == "x" else (fixed, y, p)
            t.box(pos, pos, G(seed * 5 + i))


# ---------------------------------------------------------------- spine pieces

def citadel_hub(v, log):
    """The precinct ziggurat. Plaza at local south, ziggurat touching the north face; spine exit north."""
    t = T(f"lattice_temple_citadel_hub_{v}", (45, 28, 45))
    base = 3
    # Foundation and plaza.
    t.box((0, 0, 0), (44, base - 1, 44), DT)
    t.box((0, base, 0), (44, base, 44), F)
    t.box((0, base + 1, 0), (44, base + 12, 44), A)
    # Ziggurat tiers over x 7..37, z 0..30.
    t.box((7, base + 1, 0), (37, base + 7, 30), L)
    t.box((11, base + 8, 3), (33, base + 13, 26), L)
    t.box((15, base + 14, 7), (29, base + 19, 22), L)
    t.box((19, base + 20, 11), (25, base + 22, 18), L)
    t.box((22, base + 23, 14), (22, base + 24, 15), C)
    # Foyer inside the first tier.
    t.box((9, base + 1, 2), (35, base + 6, 28), A)
    t.box((9, base, 2), (35, base, 28), F)
    # Entrance from the plaza (south wall z=30), three wide.
    t.box((21, base + 1, 30), (23, base + 4, 30), A)
    # Pillars and lights; variants change the colonnade.
    spacing = (4, 6, 5)[v]
    for x in range(12, 34, spacing):
        for z in (6, 14, 22):
            if abs(x - 22) > 2:
                pillar(t, x, z, base + 1, base + 6, glyph=x + z + v, cap=C if (x + z + v) % 2 else None)
    frieze(t, "x", 2, 10, 34, base + 4, v + 1)
    frieze(t, "x", 28, 10, 20, base + 4, v + 2)
    frieze(t, "x", 28, 24, 34, base + 4, v + 3)
    # The first stele, outside the central hall: lattice.1.
    t.box((22, base + 1, 36), (22, base + 4, 36), G(1 + v))
    t.box((22, base + 5, 36), (22, base + 5, 36), C)
    t.terminal((22, base + 1, 35), log, "north")
    # Plaza obelisks (variant positions).
    for (x, z) in ((3, 33), (41, 33), (3, 41), (41, 41), (14, 40), (30, 40))[: 4 + v]:
        t.box((x, base + 1, z), (x, base + 6 + v, z), L)
        t.box((x, base + 3, z), (x, base + 3, z), G(x * z + v))
        t.box((x, base + 7 + v, z), (x, base + 7 + v, z), C)
    # Salvage chests in the foyer.
    t.chest((10, base + 1, 3 + v), "lattice_temple_salvage")
    t.chest((34, base + 1, 27 - v), "lattice_temple_salvage")
    # Spine exit through the north wall (two thick: the ziggurat starts at z=0).
    t.exit("n", 22, "lattice_temple_citadel_outer_halls", y=base + 1, priority=1, depth=2)
    # Precinct exits on the plaza edges.
    for z in (35, 41):
        t.exit("e", z, "lattice_temple_precinct_corridors", y=base + 1)
        t.exit("w", z, "lattice_temple_precinct_corridors", y=base + 1)
    for x in (8, 36):
        t.exit("s", x, "lattice_temple_precinct_corridors", y=base + 1)
    return t


def complex_hub(v, log, prefix, next_pool):
    """A smaller temple court for the complex class."""
    t = T(f"{prefix}_hub_{v}", (33, 20, 33))
    base = 2
    t.box((0, 0, 0), (32, base - 1, 32), DT)
    t.box((0, base, 0), (32, base, 32), F)
    t.box((0, base + 1, 0), (32, base + 10, 32), A)
    t.box((5, base + 1, 0), (27, base + 6, 20), L)
    t.box((9, base + 7, 3), (23, base + 11, 16), L)
    t.box((14, base + 12, 7), (18, base + 14, 12), L)
    t.box((16, base + 15, 9), (16, base + 16, 10), C)
    t.box((7, base + 1, 2), (25, base + 5, 18), A)
    t.box((7, base, 2), (25, base, 18), F)
    t.box((15, base + 1, 20), (17, base + 4, 20), A)
    for x in range(10, 24, (4, 5, 3)[v]):
        for z in (6, 13):
            if abs(x - 16) > 2:
                pillar(t, x, z, base + 1, base + 5, glyph=x + z + v, cap=C if (x + v) % 2 else None)
    frieze(t, "x", 2, 8, 24, base + 3, v + 4)
    t.box((16, base + 1, 25), (16, base + 4, 25), G(8 + v))
    t.box((16, base + 5, 25), (16, base + 5, 25), C)
    t.terminal((16, base + 1, 24), log, "north")
    for (x, z) in ((3, 24), (29, 24), (3, 30), (29, 30))[: 2 + v]:
        t.box((x, base + 1, z), (x, base + 5 + v, z), L)
        t.box((x, base + 3, z), (x, base + 3, z), G(x + z + v))
        t.box((x, base + 6 + v, z), (x, base + 6 + v, z), C)
    t.chest((8, base + 1, 3 + v), "lattice_temple_salvage")
    t.exit("n", 16, next_pool, y=base + 1, priority=1, depth=2)
    t.exit("e", 26, "lattice_temple_precinct_corridors", y=base + 1)
    t.exit("w", 26, "lattice_temple_precinct_corridors", y=base + 1)
    t.exit("s", 6, "lattice_temple_precinct_corridors", y=base + 1)
    t.exit("s", 26, "lattice_temple_precinct_corridors", y=base + 1)
    return t


def outer_hall(name, v, log, next_pool):
    t = T(name, (25, 12, 31))
    t.room()
    t.entry("n", 12)
    t.exit("s", 12, next_pool, priority=1)
    t.exit("e", 15, "lattice_temple_precinct_corridors")
    t.exit("w", 15, "lattice_temple_precinct_corridors")
    step = (4, 5, 6)[v]
    for z in range(4, 28, step):
        for x in (6, 18):
            pillar(t, x, z, 1, 10, glyph=z + x + v, cap=None)
            t.box((x, 9, z), (x, 9, z), C)
    frieze(t, "z", 1, 2, 28, 5, v)
    frieze(t, "z", 23, 2, 28, 5, v + 1)
    if v == 1:
        t.box((10, 1, 13), (14, 1, 17), DTILE)
        t.box((12, 2, 15), (12, 3, 15), C)
    if v == 2:
        for x in range(3, 22, 3):
            t.box((x, 10, 2), (x, 10, 28), DTILE)
    t.box((12, 1, 20), (12, 2, 20), L)
    t.terminal((12, 1, 19), log, "north")
    t.chest((2, 1, 2 + v), "lattice_temple_salvage")
    t.chest((22, 1, 28 - v), "lattice_temple_cache")
    return t


def descent(name, v, log, next_pool, turn):
    """A stair hall dropping 21 blocks. Entry at the top on the north face, exit at the bottom."""
    w, h, d = 11, 27, 31
    t = T(name, (w, h, d))
    t.box((0, 0, 0), (w - 1, h - 1, d - 1), L)
    top = 21
    t.box((1, top, 1), (w - 2, top, 5), F)
    t.box((1, top + 1, 1), (w - 2, top + 4, 5), A)
    for i in range(21):
        z = 6 + i
        y = top - i
        t.box((1, y, z), (w - 2, y, z), stair("north"))
        t.box((1, y + 1, z), (w - 2, y + 5, z), A)
    t.box((1, 0, 27), (w - 2, 0, d - 2), F)
    t.box((1, 1, 27), (w - 2, 4, d - 2), A)
    # Light: crystals set into the side walls every few steps.
    for i in range(0, 21, (4, 5, 3)[v]):
        y = top - i + 3
        t.box((0, y, 6 + i), (0, y, 6 + i), C)
        t.box((w - 1, y, 6 + i), (w - 1, y, 6 + i), C if v != 1 else G(i))
    t.entry("n", 5, y=top + 1)
    t.terminal((1, top + 1, 3), log, "east")
    if turn:
        t.exit("e", 28, next_pool, priority=1)
    else:
        t.exit("s", 5, next_pool, priority=1)
    return t


# A square spiral stair around a 5 x 5 core. One block down per step; 24 steps per turn.
SPIRAL_W = 13


def spiral_units():
    """One turn of the ring, clockwise from the north-east corner: (key, cells, stair facing or None)."""
    units = [(("ne",), [(x, z) for x in range(9, 12) for z in range(1, 4)], None)]
    units += [(("e", z), [(x, z) for x in range(9, 12)], "north") for z in range(4, 9)]
    units += [(("se",), [(x, z) for x in range(9, 12) for z in range(9, 12)], None)]
    units += [(("s", x), [(x, z) for z in range(9, 12)], "east") for x in range(8, 3, -1)]
    units += [(("sw",), [(x, z) for x in range(1, 4) for z in range(9, 12)], None)]
    units += [(("w", z), [(x, z) for x in range(1, 4)], "south") for z in range(8, 3, -1)]
    units += [(("nw",), [(x, z) for x in range(1, 4) for z in range(1, 4)], None)]
    units += [(("n", x), [(x, z) for z in range(1, 4)], "west") for x in range(4, 9)]
    return units


def spiral_well(name, v, log, next_pool, exit_face, drop):
    """Entry at the top of the north face onto a landing; the stair winds down `drop` blocks and leaves through
    the middle of `exit_face`. Valid drops: west 16 + 24k, north 22 + 24k."""
    stop = {"w": ("w", 6), "n": ("n", 6)}[exit_face]
    top = drop + 1
    t = T(name, (SPIRAL_W, top + 6, SPIRAL_W))
    w, h, d = t.size
    t.box((0, 0, 0), (w - 1, h - 1, d - 1), L)

    def tread(cells, y, block):
        for (x, z) in cells:
            t.box((x, y, z), (x, y, z), block)
            t.box((x, y + 1, z), (x, y + 5, z), A)

    tread([(x, z) for x in range(4, 9) for z in range(1, 4)], top, F)
    ring = spiral_units()
    step, k, last = 0, 0, None
    while True:
        key, cells, facing = ring[k % len(ring)]
        step += 1
        k += 1
        tread(cells, top - step, stair(facing) if facing else F)
        if step == drop:
            assert key == stop, f"{name}: a {drop}-block drop does not end at the {exit_face} face"
            last = k
            break
    # The landing: the rest of the exit segment at the bottom floor.
    while True:
        key, cells, facing = ring[last % len(ring)]
        if key[0] != stop[0]:
            break
        tread(cells, 1, F)
        last += 1
    # Light and lore in the core: crystal or glyph inlays every few blocks, per variant.
    for y in range(4, top, (5, 4, 6)[v]):
        for (x, z) in ((6, 4), (8, 6), (6, 8), (4, 6)):
            t.box((x, y, z), (x, y, z), C if (y + v) % 2 == 0 or v == 0 else G(y + x + z))
    t.box((6, top + 5, 6), (6, top + 5, 6), C)
    t.entry("n", 6, y=top + 1)
    t.terminal((4, top + 1, 2), log, "east")
    t.exit(exit_face, 6, next_pool, y=2, priority=1)
    return t


def guardian_hall(name, v, log, next_pool, tiers):
    t = T(name, (21, 10, 27))
    t.room()
    t.entry("n", 10)
    t.exit("s", 10, next_pool, priority=1)
    t.spawner((6, 0, 13), tiers[0])
    t.spawner((14, 0, 13), tiers[1])
    for x in range(3, 18, (5, 4, 7)[v]):
        t.box((x, 0, 19), (x, 0, 19), GRATE)
    for z in (5, 13, 21):
        pillar(t, 3, z, 1, 8, glyph=z + v)
        pillar(t, 17, z, 1, 8, glyph=z + v + 3)
    t.box((10, 8, 13), (10, 8, 13), C)
    frieze(t, "x", 1, 2, 18, 4, v + 7)
    t.terminal((5, 1, 2), log, "south")
    return t


def gate(name, v, log, vault_log, next_pool, vault_table, structure):
    """Glyph-sequence gate: clues over the door, keypad lock beside it, sealed vault behind."""
    t = T(name, (21, 10, 27))
    t.room()
    t.entry("n", 10)
    # Gate wall.
    t.box((1, 1, 12), (19, 8, 12), L)
    t.box((10, 1, 12), (10, 1, 12),
          {"id": "neroruins:sealed_door", "properties": {"facing": "north", "half": "lower", "open": "false"}})
    t.box((10, 2, 12), (10, 2, 12),
          {"id": "neroruins:sealed_door", "properties": {"facing": "north", "half": "upper", "open": "false"}})
    for index, x in enumerate((12, 11, 10, 9)):
        t.box((x, 3, 12), (x, 3, 12), {"id": "neroruins:glyph_clue", "properties": {"index": str(index), "glyph": "0"}})
    t.box((13, 2, 12), (13, 2, 12), {"id": "neroruins:glyph_lock", "properties": {"facing": "north"}})
    # Obelisk faces either side of the gate, purely decorative glyphs.
    for x in (4, 16):
        t.box((x, 1, 11), (x, 6, 11), L)
        t.box((x, 4, 11), (x, 4, 11), G(x + v * 3))
        t.box((x, 7, 11), (x, 7, 11), C)
    t.terminal((4, 1, 6), log, "east")
    # Vault room.
    t.vault((10, 1, 19), vault_table, structure)
    t.box((7, 1, 22), (13, 1, 22), DTILE if v == 0 else L)
    t.box((10, 8, 19), (10, 8, 19), C)
    for x in (5, 15):
        pillar(t, x, 18 + v, 1, 8, glyph=x + v)
    t.terminal((14, 1, 20), vault_log, "west")
    if next_pool:
        t.exit("s", 10, next_pool, priority=1)
    return seal(t)


def organ(name, v, next_pool):
    """The Sanctum organ: four resonance pylons and their crystals, then a sealed door."""
    t = T(name, (21, 12, 27))
    t.room()
    t.entry("n", 10)
    for index, x in enumerate((7, 9, 11, 13)):
        t.box((x, 1, 7), (x, 1, 7), {"id": "neroruins:resonance_pylon",
                                     "properties": {"index": str(index), "pitch": "0", "target": "0"}})
        t.box((x, 2, 7), (x, 2, 7), {"id": "neroruins:resonance_clue", "properties": {"index": str(index), "tone": "0"}})
    # Organ pipes: crystal and lattice columns along the side walls.
    for z in range(2, 12, (2, 3, 2)[v]):
        t.box((1, 1, z), (1, 6 + (z % 4), z), L)
        t.box((19, 1, z), (19, 6 + ((z + v) % 4), z), L)
        t.box((1, 7 + (z % 4), z), (1, 7 + (z % 4), z), C)
        t.box((19, 7 + ((z + v) % 4), z), (19, 7 + ((z + v) % 4), z), C)
    t.box((1, 1, 13), (19, 10, 13), L)
    t.box((10, 1, 13), (10, 1, 13),
          {"id": "neroruins:sealed_door", "properties": {"facing": "north", "half": "lower", "open": "false"}})
    t.box((10, 2, 13), (10, 2, 13),
          {"id": "neroruins:sealed_door", "properties": {"facing": "north", "half": "upper", "open": "false"}})
    frieze(t, "x", 12, 2, 18, 5, v + 11)
    for z in range(16, 25, 4):
        pillar(t, 4, z, 1, 10, glyph=z + v)
        pillar(t, 16, z, 1, 10, glyph=z + v + 1)
    t.exit("s", 10, next_pool, priority=1)
    return seal(t, gated_entry=True)


def final_room(name, v, log, structure):
    """What Was Left Open: a relic socket opens the last chamber with the final log and the sanctum vault."""
    t = T(name, (21, 14, 27))
    t.room()
    t.entry("n", 10)
    t.box((1, 1, 12), (19, 12, 12), L)
    t.box((10, 1, 12), (10, 1, 12),
          {"id": "neroruins:sealed_door", "properties": {"facing": "north", "half": "lower", "open": "false"}})
    t.box((10, 2, 12), (10, 2, 12),
          {"id": "neroruins:sealed_door", "properties": {"facing": "north", "half": "upper", "open": "false"}})
    # The socket takes the chapter 8 relic, the Twilight Prism carried out of the Nyxara complex.
    t.box((13, 2, 12), (13, 2, 12),
          {"id": "neroruins:relic_socket", "properties": {"facing": "north", "chapter": "8", "lit": "false"}})
    # Paired glyphs: binding then release.
    t.box((7, 3, 12), (7, 3, 12), G(4))
    t.box((8, 3, 12), (8, 3, 12), G(5))
    # The chamber.
    t.box((7, 0, 16), (13, 0, 22), DTILE)
    t.box((10, 1, 16), (10, 6 + v, 16), C)
    for z in (14, 18, 22, 25):
        pillar(t, 3, z, 1, 12, glyph=z + v)
        pillar(t, 17, z, 1, 12, glyph=z + v + 2)
    t.terminal((10, 1, 20), log, "north")
    t.vault((7, 1, 18), "lattice_temple_sanctum", structure)
    return seal(t, gated_entry=True)


# ---------------------------------------------------------------- side branches

def corridor(v):
    if v == 2:
        t = T("lattice_temple_precinct_turn", (9, 7, 9))
        t.room()
        t.entry("n", 4)
        t.exit("e", 5, "lattice_temple_precinct_halls")
        t.box((4, 5, 4), (4, 5, 4), C)
        return t
    w, d = (7, 15) if v == 0 else (9, 17)
    t = T(f"lattice_temple_precinct_corridor_{v}", (w, 7, d))
    t.room()
    t.entry("n", w // 2)
    t.exit("s", w // 2, "lattice_temple_precinct_halls")
    for z in range(3, d - 2, 4):
        t.box((1, 5, z), (1, 5, z), C if v == 0 else G(z))
        t.box((w - 2, 5, z), (w - 2, 5, z), G(z + 1))
    if v == 1:
        for z in range(2, d - 1, 3):
            t.box((1, 1, z), (1, 4, z), L)
            t.box((w - 2, 1, z), (w - 2, 4, z), L)
    return t


def hall(v):
    t = T(f"lattice_temple_precinct_hall_{v}", (19, 10, 21))
    t.room()
    t.entry("n", 9)
    t.exit("e", 10, "lattice_temple_annex_corridors")
    t.exit("s", 9, "lattice_temple_annex_corridors")
    if v == 0:  # obelisk court
        for (x, z) in ((5, 6), (13, 6), (5, 14), (13, 14)):
            t.box((x, 1, z), (x, 6, z), L)
            t.box((x, 3, z), (x, 3, z), G(x + z))
            t.box((x, 7, z), (x, 7, z), C)
    elif v == 1:  # library of glyph shelves
        for z in range(3, 18, 3):
            t.box((2, 1, z), (6, 3, z), G(z))
            t.box((12, 1, z), (16, 3, z), G(z + 2))
        t.box((9, 8, 10), (9, 8, 10), C)
    else:  # crystal garden
        for (x, z) in ((4, 4), (14, 4), (9, 10), (4, 16), (14, 16)):
            t.box((x, 1, z), (x, 1 + (x + z) % 3, z), C)
        t.box((1, 0, 1), (17, 0, 19), DTILE)
        t.box((9, 0, 1), (9, 0, 19), F)
    frieze(t, "x", 1, 2, 16, 6, v + 2)
    t.chest((2, 1, 18), "lattice_temple_cache")
    return t


def annex_corridor(v):
    t = T(f"lattice_temple_annex_corridor_{v}", (7, 6, 9))
    t.room()
    t.entry("n", 3)
    t.exit("s", 3, "lattice_temple_annexes")
    t.box((3, 4, 4), (3, 4, 4), C if v == 0 else G(6))
    if v == 1:
        t.box((1, 1, 4), (1, 3, 4), L)
        t.box((5, 1, 4), (5, 3, 4), L)
    return t


def annex(v):
    t = T(f"lattice_temple_annex_{v}", (11, 7, 11))
    t.room()
    t.entry("n", 5)
    if v == 0:
        t.box((1, 1, 9), (9, 3, 9), G(9))
        t.chest((5, 1, 8), "lattice_temple_salvage")
    elif v == 1:
        t.box((5, 1, 6), (5, 3, 6), C)
        t.chest((2, 1, 8), "lattice_temple_cache")
    else:
        for x in (2, 8):
            t.box((x, 1, 3), (x, 5, 3), L)
            t.box((x, 1, 8), (x, 5, 8), L)
        t.box((5, 5, 5), (5, 5, 5), C)
        t.chest((5, 1, 9), "lattice_temple_salvage")
    return t


def cap():
    t = T("lattice_temple_cap", (3, 5, 1))
    t.box((0, 0, 0), (2, 4, 0), L)
    t.connectors.append({"pos": [1, 1, 0], "orientation": "north_up", "name": ENTRY, "target": "minecraft:empty",
                         "pool": "minecraft:empty", "final_state": L})
    return t


# ---------------------------------------------------------------- outposts

def outpost_hub(v):
    t = T(f"lattice_temple_outpost_hub_{v}", (23, 10, 27))
    t.box((0, 0, 0), (22, 0, 26), F)
    t.box((0, 1, 0), (22, 9, 26), A)
    # Shrine over z 9..26.
    t.box((2, 0, 9), (20, 9, 26), L)
    t.box((3, 1, 10), (19, 7, 25), A)
    t.box((3, 0, 10), (19, 0, 25), F)
    t.box((10, 1, 9), (12, 4, 9), A)
    t.box((3, 1, 18), (19, 7, 18), L)
    t.box((11, 1, 18), (11, 1, 18),
          {"id": "neroruins:sealed_door", "properties": {"facing": "north", "half": "lower", "open": "false"}})
    t.box((11, 2, 18), (11, 2, 18),
          {"id": "neroruins:sealed_door", "properties": {"facing": "north", "half": "upper", "open": "false"}})
    for index, x in enumerate((13, 12, 11, 10)):
        t.box((x, 3, 18), (x, 3, 18), {"id": "neroruins:glyph_clue", "properties": {"index": str(index), "glyph": "0"}})
    t.box((14, 2, 18), (14, 2, 18), {"id": "neroruins:glyph_lock", "properties": {"facing": "north"}})
    t.spawner((11, 0, 13), 1)
    t.vault((11, 1, 22), "lattice_temple_outpost_vault", "lattice_temple_outpost")
    t.box((11, 7, 22), (11, 7, 22), C)
    # Plaza obelisks.
    for (x, z) in ((3, 3), (19, 3), (11, 2))[: 2 + (v > 0)]:
        t.box((x, 1, z), (x, 4 + v, z), L)
        t.box((x, 2, z), (x, 2, z), G(x + z + v))
        t.box((x, 5 + v, z), (x, 5 + v, z), C)
    for x in (5, 17):
        t.box((x, 1, 15 - v), (x, 5, 15 - v), L)
        t.box((x, 6, 15 - v), (x, 6, 15 - v), C)
    t.chest((4, 1, 11), "lattice_temple_salvage")
    t.exit("e", 4, "lattice_temple_annex_corridors")
    t.exit("w", 4, "lattice_temple_annex_corridors")
    return seal(t)


# ---------------------------------------------------------------- satellites

def obelisk_field(v):
    t = T(f"lattice_temple_obelisk_field_{v}", (21, 14, 21))
    t.box((0, 0, 0), (20, 0, 20), F)
    spots = [((3, 3), (17, 4), (10, 10), (4, 16), (16, 16)),
             ((5, 5), (15, 5), (5, 15), (15, 15)),
             ((10, 3), (3, 10), (17, 10), (10, 17), (10, 10), (6, 6))][v]
    for i, (x, z) in enumerate(spots):
        top = 6 + (i * 3 + v) % 6
        t.box((x, 1, z), (x + 1, top, z + 1), L)
        for y in range(2, top, 3):
            t.box((x, y, z), (x, y, z), G(x + y + z + v))
            t.box((x + 1, y, z + 1), (x + 1, y, z + 1), G(x * y + z))
        t.box((x, top + 1, z), (x, top + 1, z), C)
    return t


def ruin_hall(v):
    t = T(f"lattice_temple_ruin_hall_{v}", (17, 9, 17))
    t.box((0, 0, 0), (16, 0, 16), F)
    t.box((0, 1, 0), (16, 8, 16), A)
    for z in range(0, 17, 4):
        t.box((0, 1, z), (0, 3 + (z + v) % 5, z), L)
        t.box((16, 1, z), (16, 2 + (z * 3 + v) % 6, z), L)
    for x in range(0, 17, 4):
        t.box((x, 1, 0), (x, 2 + (x + v) % 5, 0), L)
        if x != 8:
            t.box((x, 1, 16), (x, 1 + (x + v * 2) % 4, 16), L)
    t.box((8, 1, 8), (8, 1 + v, 8), G(3 + v))
    t.box((5, 1, 11), (5, 1, 11), C)
    t.chest((11, 1, 11 - v), "lattice_temple_salvage")
    return t


def spire(v):
    t = T(f"lattice_temple_spire_{v}", (9, 20, 9))
    t.box((0, 0, 0), (8, 0, 8), F)
    t.box((2, 1, 2), (6, 16 - v * 2, 6), L)
    t.box((3, 1, 3), (5, 3, 5), A)
    t.box((4, 1, 2), (4, 3, 2), A)
    t.box((4, 17 - v * 2, 4), (4, 18 - v * 2, 4), C)
    for y in range(5, 15 - v * 2, 3):
        t.box((2, y, 4), (2, y, 4), G(y + v))
        t.box((6, y, 4), (6, y, 4), G(y + v + 7))
    t.chest((4, 1, 4), "lattice_temple_cache")
    return t


# ---------------------------------------------------------------- assembly

SPINES = {
    # structure id: list of (pool, [templates]) in spine order
}


def build():
    templates = []
    pools = {}

    def pool(name, items, fallback="lattice_temple_caps"):
        pools[name] = {"fallback": NS + fallback if fallback else "minecraft:empty", "items": [t.name for t in items]}
        templates.extend(items)

    # Citadel spine.
    pool("lattice_temple_citadel_hubs", [citadel_hub(v, "lattice.1") for v in range(3)], fallback=None)
    pool("lattice_temple_citadel_outer_halls",
         [outer_hall(f"lattice_temple_citadel_outer_hall_{v}", v, "lattice.2", "lattice_temple_citadel_approaches")
          for v in range(3)])
    # Two spiral wells: 40 blocks down leaving west, then 46 blocks down leaving north, which turns the rest of
    # the spine back underneath the precinct and keeps it inside the 128-block bound.
    pool("lattice_temple_citadel_approaches",
         [spiral_well(f"lattice_temple_citadel_approach_{v}", v, "lattice.3", "lattice_temple_citadel_deep_stairs",
                      "w", 40) for v in range(3)])
    pool("lattice_temple_citadel_deep_stairs",
         [spiral_well(f"lattice_temple_citadel_deep_stair_{v}", v, None, "lattice_temple_citadel_guardians",
                      "n", 46) for v in range(2)])
    pool("lattice_temple_citadel_guardians",
         [guardian_hall(f"lattice_temple_citadel_guardian_{v}", v, "lattice.4", "lattice_temple_citadel_gates", (2, 3))
          for v in range(3)])
    pool("lattice_temple_citadel_gates",
         [gate(f"lattice_temple_citadel_gate_{v}", v, "lattice.5", None, "lattice_temple_citadel_organs",
               "lattice_temple_citadel_vault", "lattice_temple") for v in range(3)])
    pool("lattice_temple_citadel_organs",
         [organ(f"lattice_temple_citadel_organ_{v}", v, "lattice_temple_citadel_finals") for v in range(3)])
    pool("lattice_temple_citadel_finals",
         [final_room(f"lattice_temple_citadel_final_{v}", v, "lattice.6", "lattice_temple") for v in range(3)])

    # Complex spines: Nyxara carries chapter 8, Cindara carries no unique logs.
    for prefix, logs, vault_table, structure in (
            ("lattice_temple_complex", ("nyxara.1", "nyxara.2", "nyxara.3", "nyxara.4", "nyxara.5", "nyxara.6"),
             "lattice_temple_nyxara_vault", "lattice_temple_complex"),
            ("lattice_temple_cindara", (None,) * 6, "lattice_temple_complex_vault", "lattice_temple_cindara")):
        pool(f"{prefix}_hubs", [complex_hub(v, logs[0], prefix, f"{prefix}_outer_halls") for v in range(3)],
             fallback=None)
        pool(f"{prefix}_outer_halls",
             [outer_hall(f"{prefix}_outer_hall_{v}", v, logs[1], f"{prefix}_approaches") for v in range(3)])
        pool(f"{prefix}_approaches",
             [descent(f"{prefix}_approach_{v}", v, logs[2], f"{prefix}_guardians", True) for v in range(3)])
        pool(f"{prefix}_guardians",
             [guardian_hall(f"{prefix}_guardian_{v}", v, logs[3], f"{prefix}_gates", (1, 2)) for v in range(3)])
        pool(f"{prefix}_gates",
             [gate(f"{prefix}_gate_{v}", v, logs[4], logs[5], None, vault_table, structure) for v in range(3)])

    pool("lattice_temple_outpost_hubs", [outpost_hub(v) for v in range(3)], fallback=None)
    pool("lattice_temple_precinct_corridors", [corridor(v) for v in range(3)])
    pool("lattice_temple_precinct_halls", [hall(v) for v in range(3)])
    pool("lattice_temple_annex_corridors", [annex_corridor(v) for v in range(2)])
    pool("lattice_temple_annexes", [annex(v) for v in range(3)])
    pool("lattice_temple_caps", [cap()], fallback=None)
    pool("lattice_temple_obelisk_fields", [obelisk_field(v) for v in range(3)], fallback=None)
    pool("lattice_temple_ruin_halls", [ruin_hall(v) for v in range(3)], fallback=None)
    pool("lattice_temple_spires", [spire(v) for v in range(2)], fallback=None)
    return templates, pools


# Structures: id -> (size class, hub pool, layouts [(weight, depth)], max distance, biome tag, ground check,
# terrain adaptation, start height, spacing, separation, salt, satellites [(pool, chance)], sat radii)
STRUCTURES = {
    "lattice_temple": dict(size_class="citadel", start_pool="lattice_temple_citadel_hubs", layouts=[(2, 8), (3, 9)],
                           max_distance=128, biomes="lattice_temple", adaptation="none", start=-3,
                           ground={"radius": 32, "max_height_delta": 8, "require_dry": True, "min_depth": 104,
                                   "headroom": 30},
                           spacing=96, separation=40, salt=1873402917,
                           satellites=[("lattice_temple_obelisk_fields", 85)] * 4
                           + [("lattice_temple_ruin_halls", 80)] * 3 + [("lattice_temple_spires", 70)] * 2,
                           radii=(9, 13)),
    "lattice_temple_complex": dict(size_class="complex", start_pool="lattice_temple_complex_hubs",
                                   layouts=[(1, 4), (2, 5)], max_distance=96, biomes="lattice_temple_complex",
                                   adaptation="none", start=-2,
                                   ground={"radius": 24, "max_height_delta": 8, "require_dry": True, "min_depth": 40,
                                           "headroom": 22},
                                   spacing=40, separation=16, salt=1873402923,
                                   satellites=[("lattice_temple_obelisk_fields", 80)] * 2
                                   + [("lattice_temple_ruin_halls", 80), ("lattice_temple_spires", 80)],
                                   radii=(7, 10)),
    "lattice_temple_cindara": dict(size_class="complex", start_pool="lattice_temple_cindara_hubs",
                                   layouts=[(1, 4), (2, 5)], max_distance=96, biomes="lattice_temple_cindara",
                                   adaptation="none", start=-2,
                                   ground={"radius": 24, "max_height_delta": 8, "require_dry": True, "min_depth": 40,
                                           "headroom": 22},
                                   spacing=40, separation=16, salt=1873402931,
                                   satellites=[("lattice_temple_obelisk_fields", 80)] * 2
                                   + [("lattice_temple_ruin_halls", 80), ("lattice_temple_spires", 80)],
                                   radii=(7, 10)),
    "lattice_temple_outpost": dict(size_class="outpost", start_pool="lattice_temple_outpost_hubs",
                                   layouts=[(1, 1), (2, 2)], max_distance=48, biomes="lattice_temple_outpost",
                                   adaptation="beard_thin", start=0,
                                   ground={"radius": 12, "max_height_delta": 6, "require_dry": True, "min_depth": 8,
                                           "headroom": 12},
                                   spacing=16, separation=6, salt=1873402943,
                                   satellites=[("lattice_temple_obelisk_fields", 60)] * 2,
                                   radii=(4, 6)),
}

SPAWN_TABLE = {"entries": [
    {"entity": "neroruins:sentinel_mk1", "weight": 5, "min_tier": 1, "max_tier": 2},
    {"entity": "neroruins:sentinel_mk2", "weight": 5, "min_tier": 2, "max_tier": 3},
    {"entity": "neroruins:sentinel_mk3", "weight": 3, "min_tier": 3, "max_tier": 3},
    {"entity": "nerocreatures:crystal_golem", "weight": 2, "min_tier": 2, "max_tier": 3, "required": False},
    {"entity": "nerocreatures:void_crawler", "weight": 3, "min_tier": 1, "max_tier": 3, "required": False},
    {"entity": "nerocreatures:rogue_drone", "weight": 2, "min_tier": 1, "max_tier": 2, "required": False},
    {"entity": "nerocreatures:rogue_android", "weight": 2, "min_tier": 2, "max_tier": 3, "required": False},
    {"entity": "minecraft:skeleton", "weight": 2, "min_tier": 1, "max_tier": 2},
    {"entity": "minecraft:zombie", "weight": 1, "min_tier": 1, "max_tier": 1},
]}


def tiers(*rolls):
    return {"type": "minecraft:chest", "pools": [
        {"rolls": n, "entries": [{"type": "minecraft:loot_table", "value": f"neroruins:tier/{tier}"}]}
        for tier, n in rolls]}


LOOT = {
    "lattice_temple_salvage": tiers((0, 2), (1, 1)),
    "lattice_temple_cache": tiers((1, 2), (2, 1)),
    "lattice_temple_outpost_vault": tiers((1, 2), (2, 1), (3, 1)),
    "lattice_temple_complex_vault": tiers((1, 2), (2, 2), (3, 1), (4, 1)),
    "lattice_temple_citadel_vault": tiers((2, 2), (3, 2), (4, 1)),
}


def with_relic(table, relic):
    table = json.loads(json.dumps(table))
    table["pools"].append({"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"neroruins:{relic}"}]})
    return table


LOOT["lattice_temple_nyxara_vault"] = with_relic(LOOT["lattice_temple_complex_vault"], "twilight_prism")
LOOT["lattice_temple_sanctum"] = with_relic(tiers((3, 2), (4, 1)), "resonance_heart")

BIOMES = {
    "lattice_temple": [("nerospace:nyxara_dayside", False), ("nerospace:nyxara_nightside", False),
                       ("nerospace:nyxara_twilight_band", False)],
    "lattice_temple_complex": [("nerospace:nyxara_dayside", False), ("nerospace:nyxara_nightside", False),
                               ("nerospace:nyxara_twilight_band", False)],
    "lattice_temple_cindara": [("nerospace:cindara", False), ("nerospace:cindara_ember_plains", False),
                               ("nerospace:cindara_sulfur_vents", False)],
    "lattice_temple_outpost": [("#neroland:space/planet_biomes", False), ("#neroland:space/moon_biomes", False)],
}


def dump(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def element(location):
    return {"element_type": "minecraft:single_pool_element", "location": NS + location,
            "processors": "neroruins:lattice_temple", "projection": "rigid"}


def jigsaw(biomes, start_pool, depth, max_distance, adaptation, start):
    return {"biomes": "#neroruins:has_structure/" + biomes, "spawn_overrides": {}, "step": "surface_structures",
            "terrain_adaptation": adaptation, "max_distance_from_center": max_distance,
            "project_start_to_heightmap": "WORLD_SURFACE_WG", "size": depth,
            "start_height": {"absolute": start}, "start_pool": NS + start_pool, "use_expansion_hack": False}


def main(root):
    root = Path(root)
    templates, pools = build()
    data = root / "common/src/main/resources/data/neroruins"
    dump(root / "tools/templates/lattice_temple.json", {"templates": [t.spec() for t in templates]})
    for name, p in pools.items():
        dump(data / "worldgen/template_pool" / f"{name}.json",
             {"fallback": p["fallback"], "elements": [{"weight": 1, "element": element(i)} for i in p["items"]]})
    dump(data / "worldgen/processor_list/lattice_temple.json",
         {"processors": [{"processor_type": "neroruins:puzzle_seed"}]})
    for sid, s in STRUCTURES.items():
        dump(data / "worldgen/structure" / f"{sid}.json", {
            "type": "neroruins:complex_hub", "biomes": "#neroruins:has_structure/" + s["biomes"],
            "spawn_overrides": {}, "step": "surface_structures", "terrain_adaptation": s["adaptation"],
            "family": "lattice_temple", "size_class": s["size_class"],
            "layouts": [{"weight": w, "layout": jigsaw(s["biomes"], s["start_pool"], d, s["max_distance"],
                                                        s["adaptation"], s["start"])} for w, d in s["layouts"]],
            "ground_check": s["ground"]})
        dump(data / "worldgen/structure_set" / f"{sid}.json", {
            "structures": [{"structure": NS + sid, "weight": 1}],
            "placement": {"type": "minecraft:random_spread", "spacing": s["spacing"],
                          "separation": s["separation"], "salt": s["salt"]}})
        for index, (sat_pool, chance) in enumerate(s["satellites"]):
            sat = f"{sid}_satellite_{index}"
            placement = {"hub_set": NS + sid, "family": "lattice_temple", "index": index,
                         "min_radius": s["radii"][0], "max_radius": s["radii"][1], "chance_percent": chance}
            dump(data / "worldgen/structure" / f"{sat}.json", {
                "type": "neroruins:satellite", "biomes": "#neroruins:has_structure/" + s["biomes"],
                "spawn_overrides": {}, "step": "surface_structures", "terrain_adaptation": "beard_thin",
                "placement": placement,
                "layout": jigsaw(s["biomes"], sat_pool, 1, 24, "beard_thin", 0)})
            dump(data / "worldgen/structure_set" / f"{sat}.json", {
                "structures": [{"structure": NS + sat, "weight": 1}],
                "placement": dict(placement, type="neroruins:satellite")})
    for tag, values in BIOMES.items():
        dump(data / "tags/worldgen/biome/has_structure" / f"{tag}.json",
             {"replace": False, "values": [{"id": i, "required": r} for i, r in values]})
    dump(data / "spawn_tables/lattice_temple.json", SPAWN_TABLE)
    for name, table in LOOT.items():
        dump(data / "loot_table/chests" / f"{name}.json", table)


if __name__ == "__main__":
    main(sys.argv[1])
