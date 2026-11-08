"""Authoring script for the crash_site family.

Writes the template descriptions (tools/templates/crash_site.json) and every crash_site data file (pools,
structures, structure sets, processor lists, biome tags, loot tables, spawn table). Run it, then
tools/build_templates.py, whenever the family changes. Everything it writes is deterministic.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SPECS = ROOT / "tools" / "templates" / "crash_site.json"
DATA = ROOT / "common" / "src" / "main" / "resources" / "data"
DATA_263 = ROOT / "common" / "src" / "main" / "resources-26.3" / "data"
PUZZLES = ROOT / "tools" / "templates" / "puzzles.json"

HULL = "neroruins:survey_hull"
RUST = "neroruins:rusted_survey_hull"
AIR = "minecraft:air"
NS = "neroruins:"
ENTRY = NS + "crash_site_entry"
EXIT = NS + "crash_site_exit"
CORRIDORS = NS + "crash_site_corridors"
ROOMS = NS + "crash_site_rooms"
CAPS = NS + "crash_site_caps"
WEATHERING = NS + "crash_site_weathering"
EMPTY = "minecraft:empty"

MIRROR_FACING = {"east": "west", "west": "east", "north": "north", "south": "south"}
CLOCKWISE = {"north": "east", "east": "south", "south": "west", "west": "north"}


class Piece:
    def __init__(self, name, size):
        self.name = name
        self.size = list(size)
        self.fills = []
        self.connectors = []
        self.containers = []
        self.vaults = []
        self.spawners = []
        self.terminals = []

    def fill(self, a, b, block):
        self.fills.append({"from": list(a), "to": list(b), "block": block})

    def set(self, pos, block):
        self.fill(pos, pos, block)

    def shell(self, a, b, block=HULL):
        """A solid box with its interior hollowed out."""
        self.fill(a, b, block)
        self.fill((a[0] + 1, a[1] + 1, a[2] + 1), (b[0] - 1, b[1] - 1, b[2] - 1), AIR)

    def opening(self, pos, height=2):
        self.fill(pos, (pos[0], pos[1] + height - 1, pos[2]), AIR)

    def connector(self, pos, orientation, name, target, pool, final_state=AIR):
        self.connectors.append({"pos": list(pos), "orientation": orientation, "name": name, "target": target,
                                "pool": pool, "final_state": final_state})

    def exit(self, pos, orientation, pool):
        self.opening(pos)
        self.connector(pos, orientation, EXIT, ENTRY, pool)

    def entry(self, pos, orientation):
        self.opening(pos)
        self.connector(pos, orientation, ENTRY, EXIT, EMPTY)

    def chest(self, pos, table):
        self.containers.append({"pos": list(pos), "loot_table": NS + "chests/" + table})

    def vault(self, pos, table, structure):
        self.vaults.append({"pos": list(pos), "loot_table": NS + "chests/" + table, "structure": NS + structure})

    def spawner(self, pos, tier):
        self.spawners.append({"pos": list(pos), "table": NS + "crash_site", "tier": tier})

    def terminal(self, pos, log, facing):
        self.terminals.append({"pos": list(pos), "log_id": NS + "cindara." + str(log), "facing": facing})

    def mirrored(self, name):
        """The same piece reflected across x, with every directional block state turned to match."""
        width = self.size[0]
        other = Piece(name, self.size)

        def px(pos):
            return [width - 1 - pos[0], pos[1], pos[2]]

        def block(value):
            if not isinstance(value, dict):
                return value
            props = dict(value.get("properties", {}))
            if "facing" in props:
                if value["id"] == "neroruins:conduit" and props.get("corner") == "true":
                    props["facing"] = MIRROR_FACING[CLOCKWISE[props["facing"]]]
                else:
                    props["facing"] = MIRROR_FACING[props["facing"]]
            return {"id": value["id"], "properties": props}

        for f in self.fills:
            a, b = px(f["from"]), px(f["to"])
            other.fills.append({"from": [min(a[0], b[0]), a[1], a[2]], "to": [max(a[0], b[0]), b[1], b[2]],
                                "block": block(f["block"])})
        for c in self.connectors:
            direction, up = c["orientation"].split("_")
            other.connectors.append(dict(c, pos=px(c["pos"]),
                                         orientation=MIRROR_FACING[direction] + "_" + up))
        for c in self.containers:
            other.containers.append(dict(c, pos=px(c["pos"])))
        for v in self.vaults:
            other.vaults.append(dict(v, pos=px(v["pos"])))
        for s in self.spawners:
            other.spawners.append(dict(s, pos=px(s["pos"])))
        for t in self.terminals:
            other.terminals.append(dict(t, pos=px(t["pos"]), facing=MIRROR_FACING[t["facing"]]))
        return other

    def spec(self):
        out = {"name": self.name, "size": self.size, "fills": self.fills, "connectors": self.connectors}
        for key in ("containers", "vaults", "spawners", "terminals"):
            if getattr(self, key):
                out[key] = getattr(self, key)
        return out


def door(piece, x, y, z, facing):
    piece.set((x, y, z), {"id": "neroruins:sealed_door",
                          "properties": {"facing": facing, "half": "lower", "open": "false"}})
    piece.set((x, y + 1, z), {"id": "neroruins:sealed_door",
                              "properties": {"facing": facing, "half": "upper", "open": "false"}})


def conduit_loop(piece, ox, y, oz):
    """The authored, solved conduit loop from the puzzle fixture, at the given offset. Returns the lock."""
    room = next(t for t in json.loads(PUZZLES.read_text(encoding="utf-8"))["templates"]
                if t["name"] == "puzzle_conduit")
    lock = None
    for f in room["fills"]:
        block = f["block"]
        if isinstance(block, dict) and block["id"] in ("neroruins:conduit", "neroruins:conduit_source",
                                                       "neroruins:conduit_lock"):
            x, _, z = f["from"]
            pos = (ox + x, y, oz + z)
            piece.set(pos, block)
            if block["id"] == "neroruins:conduit_lock":
                lock = pos
    return lock


def hump(piece, width, length, roof_y, profile):
    """The broken spine above the roof: one stepped height per section of the hull, so the hull reads as tilted."""
    sections = len(profile)
    for i, height in enumerate(profile):
        z0 = i * length // sections
        z1 = (i + 1) * length // sections - 1
        if height > 0:
            piece.fill((2, roof_y + 1, z0), (width - 3, roof_y + height, z1), HULL)


def partition(piece, width, z, y0, y1, gap_x):
    piece.fill((1, y0, z), (width - 2, y1, z), HULL)
    piece.fill((gap_x - 1, y0, z), (gap_x + 1, y0 + 2, z), AIR)


# ---------------------------------------------------------------- citadel: the Meridian


def meridian(name, breach, profile, crates):
    """The Meridian's surviving hull: 27 x 64, half buried. Interior floor at y 6 so the walking level is the
    surface; the keel ring below is buried. Bow (z 0) is the cockpit, stern (z 63) the breach hall."""
    W, L, F, Y = 27, 64, 6, 7          # width, length, floor y, walking y
    roof = 12
    p = Piece(name, (W, roof + max(profile) + 1, L))
    # keel ring and walls, floor, roof
    p.fill((0, 0, 0), (W - 1, roof, L - 1), HULL)
    p.fill((1, 0, 1), (W - 2, F - 1, L - 2), AIR)          # buried keel is hollow, carved from the ground
    p.fill((1, Y, 1), (W - 2, roof - 1, L - 2), AIR)
    hump(p, W, L, roof, profile)
    c = W // 2                                             # 13
    # cockpit z 1-4, inner bay z 6-10, gate wall z 11, guardian z 12-21, crew z 23-29, hold z 31-48, breach 50-62
    partition(p, W, 5, Y, roof - 1, c)
    p.fill((1, Y, 11), (W - 2, roof - 1, 11), HULL)
    door(p, c, Y, 11, "south")
    p.set((c + 2, Y + 1, 11), {"id": "neroruins:relic_socket",
                               "properties": {"facing": "south", "chapter": "5", "lit": "false"}})
    p.set((c + 2, Y + 2, 11), {"id": "neroruins:glyph_stone", "properties": {"glyph": "4"}})
    p.set((c - 2, Y + 1, 11), {"id": "neroruins:glyph_stone", "properties": {"glyph": "11"}})
    partition(p, W, 22, Y, roof - 1, c)
    partition(p, W, 30, Y, roof - 1, c)
    partition(p, W, 49, Y, roof - 1, c)
    # cockpit: consoles along the bow, the vault, the last log
    p.fill((3, Y, 1), (W - 4, Y, 1), "minecraft:polished_deepslate")
    p.set((c - 3, Y + 1, 1), "minecraft:light_blue_stained_glass")
    p.set((c + 3, Y + 1, 1), "minecraft:light_blue_stained_glass")
    p.vault((c, Y, 3), "crash_site_meridian_vault", "crash_site_meridian")
    p.terminal((c - 4, Y, 3), 6, "south")
    # inner cargo bay behind the gate
    p.chest((2, Y, 8), "crash_site_salvage")
    p.chest((W - 3, Y, 8), "crash_site_cabin")
    p.fill((2, Y, 6), (4, Y + 1, 6), RUST)
    # guardian approach
    p.spawner((c, F, 17), 2)
    p.spawner((c - 6, F, 14), 3)
    p.terminal((1, Y, 16), 4, "east")
    p.terminal((c + 4, Y, 12), 5, "south")
    # crew deck: bunks, medical locker, third log
    for z in (24, 27):
        p.fill((W - 6, Y, z), (W - 3, Y, z), "minecraft:white_wool")
    p.chest((1, Y, 26), "crash_site_cabin")
    p.chest((1, Y, 28), "crash_site_logs")
    p.terminal((W - 2, Y, 29), 3, "west")
    # cargo hold: the conduit loop (the inner loop in the second log), the keystone locker
    lock = conduit_loop(p, 8, Y, 36)
    p.terminal((lock[0] + 1, Y, lock[2]), 2, "north")
    p.fill((20, Y, 39), (20, roof - 1, 47), HULL)
    p.fill((20, Y, 39), (W - 2, roof - 1, 39), HULL)
    p.fill((20, Y, 47), (W - 2, roof - 1, 47), HULL)
    door(p, 20, Y, 43, "west")
    p.vault((23, Y, 43), "crash_site_keystone", "crash_site_meridian")
    for pos in crates:
        p.chest(pos, "crash_site_salvage")
    # breach hall: the way in, the first log
    if breach == "side":
        p.fill((0, Y, 54), (0, Y + 2, 57), AIR)
        p.fill((0, Y + 3, 55), (0, Y + 3, 56), AIR)
        p.fill((1, Y, 58), (2, Y, 59), RUST)
        p.terminal((1, Y, 52), 1, "east")
        p.exit((c, Y, L - 1), "south_up", CORRIDORS)
    else:
        p.fill((c - 1, Y, L - 1), (c + 1, Y + 2, L - 1), AIR)
        p.fill((c, Y + 3, L - 1), (c, Y + 3, L - 1), AIR)
        p.terminal((c - 3, Y, L - 2), 1, "north")
        p.exit((0, Y, 56), "west_up", CORRIDORS)
    p.chest((W - 2, Y, 61), "crash_site_salvage")
    p.exit((W - 1, Y, 26), "east_up", CORRIDORS)
    p.exit((W - 1, Y, 34), "east_up", CORRIDORS)
    p.exit((W - 1, Y, 56), "east_up", CORRIDORS)
    return p


# ---------------------------------------------------------------- complex: a wrecked Survey hauler


def hauler(name, breach, profile):
    W, L, F, Y = 19, 40, 4, 5
    roof = 10
    p = Piece(name, (W, roof + max(profile) + 1, L))
    p.fill((0, 0, 0), (W - 1, roof, L - 1), HULL)
    p.fill((1, 0, 1), (W - 2, F - 1, L - 2), AIR)
    p.fill((1, Y, 1), (W - 2, roof - 1, L - 2), AIR)
    hump(p, W, L, roof, profile)
    c = W // 2                                             # 9
    # cockpit z 1-7, gate wall z 8, guardian + conduit z 9-17, hold z 19-28, breach z 30-38
    p.fill((1, Y, 8), (W - 2, roof - 1, 8), HULL)
    door(p, c, Y, 8, "south")
    p.vault((c, Y, 6), "crash_site_vault", "crash_site_complex")
    p.chest((2, Y, 2), "crash_site_salvage")
    p.fill((4, Y, 1), (W - 5, Y, 1), "minecraft:polished_deepslate")
    conduit_loop(p, 4, Y, 8)
    p.spawner((c, F, 15), 2)
    partition(p, W, 18, Y, roof - 1, c)
    partition(p, W, 29, Y, roof - 1, c)
    p.chest((1, Y, 20), "crash_site_salvage")
    p.chest((W - 2, Y, 27), "crash_site_logs")
    p.fill((2, Y, 24), (4, Y + 1, 26), RUST)
    if breach == "side":
        p.fill((0, Y, 32), (0, Y + 2, 35), AIR)
        p.exit((c, Y, L - 1), "south_up", CORRIDORS)
    else:
        p.fill((c - 1, Y, L - 1), (c + 1, Y + 2, L - 1), AIR)
        p.exit((0, Y, 34), "west_up", CORRIDORS)
    p.chest((W - 2, Y, 37), "crash_site_salvage")
    p.exit((W - 1, Y, 23), "east_up", CORRIDORS)
    p.exit((W - 1, Y, 34), "east_up", CORRIDORS)
    return p


# ---------------------------------------------------------------- outpost: a cargo pod


def pod(name, breach, profile):
    W, L, F, Y = 13, 21, 2, 3
    roof = 7
    p = Piece(name, (W, roof + max(profile) + 1, L))
    p.fill((0, 0, 0), (W - 1, roof, L - 1), HULL)
    p.fill((1, 0, 1), (W - 2, F - 1, L - 2), AIR)
    p.fill((1, Y, 1), (W - 2, roof - 1, L - 2), AIR)
    hump(p, W, L, roof, profile)
    c = W // 2                                             # 6
    p.fill((1, Y, 10), (W - 2, roof - 1, 10), HULL)
    door(p, c, Y, 10, "south")
    p.set((c + 2, Y + 1, 10), {"id": "neroruins:card_reader",
                               "properties": {"facing": "south", "tier": "1", "accepted": "false"}})
    p.vault((c, Y, 5), "crash_site_outpost_vault", "crash_site_outpost")
    p.chest((2, Y, 2), "crash_site_salvage")
    p.spawner((c, F, 14), 1)
    p.chest((1, Y, 12), "crash_site_keycard")
    p.chest((W - 2, Y, 18), "crash_site_logs")
    if breach == "side":
        p.fill((0, Y, 15), (0, Y + 2, 17), AIR)
        p.exit((W - 1, Y, 15), "east_up", CORRIDORS)
    else:
        p.fill((c - 1, Y, L - 1), (c + 1, Y + 2, L - 1), AIR)
        p.exit((0, Y, 15), "west_up", CORRIDORS)
    return p


# ---------------------------------------------------------------- branches


def corridor_straight():
    p = Piece("crash_site_corridor_straight", (5, 5, 9))
    p.shell((0, 0, 0), (4, 4, 8))
    p.entry((2, 1, 0), "north_up")
    p.exit((2, 1, 8), "south_up", ROOMS)
    p.set((1, 1, 4), RUST)
    return p


def corridor_turn():
    p = Piece("crash_site_corridor_turn", (7, 5, 7))
    p.shell((0, 0, 0), (6, 4, 6))
    p.entry((3, 1, 0), "north_up")
    p.exit((6, 1, 4), "east_up", ROOMS)
    p.fill((1, 1, 5), (2, 2, 5), RUST)
    return p


def corridor_jog():
    p = Piece("crash_site_corridor_jog", (7, 5, 11))
    p.shell((0, 0, 0), (6, 4, 10))
    p.fill((1, 1, 4), (3, 3, 6), HULL)
    p.entry((5, 1, 0), "north_up")
    p.exit((1, 1, 10), "south_up", ROOMS)
    return p


def cabin(name, variant):
    if variant == 0:
        p = Piece(name, (9, 5, 9))
        p.shell((0, 0, 0), (8, 4, 8))
        p.fill((1, 1, 6), (3, 1, 7), "minecraft:white_wool")
        p.fill((5, 1, 6), (7, 1, 7), "minecraft:white_wool")
        p.chest((7, 1, 2), "crash_site_cabin")
        p.chest((1, 1, 2), "crash_site_logs")
        p.entry((4, 1, 0), "north_up")
    elif variant == 1:
        p = Piece(name, (11, 5, 9))
        p.shell((0, 0, 0), (10, 4, 8))
        for x in (1, 4, 7):
            p.fill((x, 1, 7), (x + 1, 1, 7), "minecraft:light_gray_wool")
        p.chest((1, 1, 1), "crash_site_cabin")
        p.entry((5, 1, 0), "north_up")
        p.exit((10, 1, 4), "east_up", CORRIDORS)
    else:
        p = Piece(name, (9, 5, 11))
        p.shell((0, 0, 0), (8, 4, 10))
        p.fill((1, 0, 1), (7, 0, 9), "minecraft:white_concrete")
        p.fill((1, 1, 8), (2, 1, 9), "minecraft:white_wool")
        p.fill((6, 1, 8), (7, 1, 9), "minecraft:white_wool")
        p.chest((7, 1, 3), "crash_site_cabin")
        p.chest((1, 1, 3), "crash_site_logs")
        p.entry((4, 1, 0), "north_up")
    return p


def hold(name, variant):
    if variant == 0:
        p = Piece(name, (13, 6, 13))
        p.shell((0, 0, 0), (12, 5, 12))
        for x, z in ((2, 3), (2, 8), (9, 3), (9, 8)):
            p.fill((x, 1, z), (x + 1, 2, z + 1), "minecraft:barrel")
        p.chest((6, 1, 6), "crash_site_salvage")
        p.entry((6, 1, 0), "north_up")
        p.exit((6, 1, 12), "south_up", CORRIDORS)
    elif variant == 1:
        p = Piece(name, (11, 6, 15))
        p.shell((0, 0, 0), (10, 5, 14))
        p.fill((1, 1, 9), (9, 3, 13), RUST)
        p.fill((4, 1, 9), (6, 2, 10), AIR)
        p.chest((5, 1, 9), "crash_site_salvage")
        p.chest((1, 1, 4), "crash_site_salvage")
        p.entry((5, 1, 0), "north_up")
    else:
        p = Piece(name, (13, 6, 11))
        p.shell((0, 0, 0), (12, 5, 10))
        for x in (1, 4, 8):
            p.fill((x, 1, 6), (x + 2, 3, 9), RUST if x == 4 else HULL)
        p.chest((11, 1, 3), "crash_site_salvage")
        p.chest((1, 1, 3), "crash_site_logs")
        p.entry((6, 1, 0), "north_up")
    return p


def cap():
    p = Piece("crash_site_cap", (1, 2, 1))
    p.set((0, 1, 0), HULL)
    p.connector((0, 0, 0), "north_up", ENTRY, EXIT, EMPTY, HULL)
    return p


def debris(variant):
    if variant == 0:
        p = Piece("crash_site_debris_plates", (7, 3, 5))
        p.fill((0, 0, 0), (6, 0, 4), RUST)
        p.fill((1, 1, 1), (4, 1, 2), HULL)
        p.set((5, 2, 3), RUST)
    elif variant == 1:
        p = Piece("crash_site_debris_container", (5, 4, 9))
        p.shell((0, 0, 0), (4, 3, 8), RUST)
        p.fill((1, 1, 0), (3, 2, 0), AIR)
        p.chest((2, 1, 7), "crash_site_salvage")
    elif variant == 2:
        p = Piece("crash_site_debris_pod", (5, 5, 7))
        p.shell((0, 0, 0), (4, 4, 6))
        p.set((2, 3, 0), "minecraft:light_blue_stained_glass")
        p.fill((2, 1, 6), (2, 2, 6), AIR)
        p.chest((2, 1, 1), "crash_site_cabin")
    else:
        p = Piece("crash_site_debris_engine", (7, 6, 7))
        p.fill((1, 0, 1), (5, 4, 5), "minecraft:polished_basalt")
        p.fill((2, 1, 2), (4, 3, 4), "minecraft:magma_block")
        p.fill((0, 0, 3), (6, 1, 3), RUST)
        p.chest((6, 0, 6), "crash_site_salvage")
    return p


def templates():
    a = meridian("crash_site_meridian_a", "side", [0, 1, 2, 2, 3, 4, 4, 6],
                 [(2, 7, 32), (2, 7, 47), (10, 7, 46)])
    b = meridian("crash_site_meridian_b", "side", [0, 0, 2, 3, 3, 5, 6, 5],
                 [(2, 7, 33), (6, 7, 47), (12, 7, 31)]).mirrored("crash_site_meridian_b")
    c = meridian("crash_site_meridian_c", "stern", [0, 2, 2, 3, 4, 6, 5, 3],
                 [(2, 7, 32), (3, 7, 47), (18, 7, 46)])
    complex_a = hauler("crash_site_complex_a", "side", [0, 1, 2, 3, 4])
    complex_b = hauler("crash_site_complex_b", "side", [0, 2, 2, 4, 3]).mirrored("crash_site_complex_b")
    complex_c = hauler("crash_site_complex_c", "stern", [1, 1, 3, 4, 2])
    pod_a = pod("crash_site_outpost_a", "side", [0, 1, 2])
    pod_b = pod("crash_site_outpost_b", "side", [0, 2, 1]).mirrored("crash_site_outpost_b")
    pod_c = pod("crash_site_outpost_c", "stern", [1, 1, 2])
    pieces = [a, b, c, complex_a, complex_b, complex_c, pod_a, pod_b, pod_c,
              corridor_straight(), corridor_turn(), corridor_jog(),
              cabin("crash_site_cabin_bunks", 0), cabin("crash_site_cabin_quarters", 1),
              cabin("crash_site_cabin_medbay", 2),
              hold("crash_site_hold_crates", 0), hold("crash_site_hold_collapsed", 1),
              hold("crash_site_hold_stacks", 2), cap()] + [debris(i) for i in range(4)]
    return [piece.spec() for piece in pieces]


# ---------------------------------------------------------------- data


HUBS = {
    "meridian": {"structure": "crash_site_meridian", "size_class": "citadel", "pool": "crash_site_meridian_hulls",
                 "pieces": ["crash_site_meridian_a", "crash_site_meridian_b", "crash_site_meridian_c"],
                 "depths": [(3, 4), (2, 3)], "start_height": -6, "distance": 100,
                 "biomes": "crash_site_meridian", "spacing": 64, "separation": 28, "salt": 1857303761,
                 "ground": {"radius": 32, "max_height_delta": 6, "require_dry": True, "min_depth": 8,
                            "headroom": 20},
                 "satellites": {"count": 14, "always": 8, "chance": 50, "min": 3, "max": 19}},
    "complex": {"structure": "crash_site_complex", "size_class": "complex", "pool": "crash_site_complex_hulls",
                "pieces": ["crash_site_complex_a", "crash_site_complex_b", "crash_site_complex_c"],
                "depths": [(3, 3), (2, 2)], "start_height": -4, "distance": 80,
                "biomes": "crash_site", "spacing": 36, "separation": 14, "salt": 1857304261,
                "ground": {"radius": 20, "max_height_delta": 4, "require_dry": True, "min_depth": 8,
                           "headroom": 16},
                "satellites": {"count": 6, "always": 4, "chance": 50, "min": 2, "max": 10}},
    "outpost": {"structure": "crash_site_outpost", "size_class": "outpost", "pool": "crash_site_outpost_pods",
                "pieces": ["crash_site_outpost_a", "crash_site_outpost_b", "crash_site_outpost_c"],
                "depths": [(3, 2), (2, 1)], "start_height": -2, "distance": 48,
                "biomes": "crash_site", "spacing": 22, "separation": 9, "salt": 1857304761,
                "ground": {"radius": 10, "max_height_delta": 2, "require_dry": True, "min_depth": 8,
                           "headroom": 12},
                "satellites": {"count": 2, "always": 0, "chance": 60, "min": 2, "max": 5}},
}

DEBRIS = ["crash_site_debris_plates", "crash_site_debris_container", "crash_site_debris_pod",
          "crash_site_debris_engine"]
ROOM_PIECES = ["crash_site_cabin_bunks", "crash_site_cabin_quarters", "crash_site_cabin_medbay",
               "crash_site_hold_crates", "crash_site_hold_collapsed", "crash_site_hold_stacks"]
CORRIDOR_PIECES = ["crash_site_corridor_straight", "crash_site_corridor_turn", "crash_site_corridor_jog"]


def element(location):
    return {"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": NS + location,
                                     "processors": WEATHERING, "projection": "rigid"}}


def pool(pieces, fallback):
    return {"fallback": fallback, "elements": [element(p) for p in pieces]}


def jigsaw(biomes, pool_name, depth, start_height, distance):
    return {"biomes": "#neroruins:has_structure/" + biomes, "spawn_overrides": {}, "step": "surface_structures",
            "terrain_adaptation": "none", "max_distance_from_center": distance,
            "project_start_to_heightmap": "WORLD_SURFACE_WG", "size": depth,
            "start_height": {"absolute": start_height}, "start_pool": NS + pool_name, "use_expansion_hack": False}


def data_files():
    files = {}
    pools = DATA / "neroruins" / "worldgen" / "template_pool"
    files[pools / "crash_site_corridors.json"] = pool(CORRIDOR_PIECES, CAPS)
    files[pools / "crash_site_rooms.json"] = pool(ROOM_PIECES, CAPS)
    files[pools / "crash_site_caps.json"] = pool(["crash_site_cap"], EMPTY)
    files[pools / "crash_site_debris.json"] = pool(DEBRIS, EMPTY)
    structures = DATA / "neroruins" / "worldgen" / "structure"
    sets = DATA / "neroruins" / "worldgen" / "structure_set"
    for hub in HUBS.values():
        files[pools / (hub["pool"] + ".json")] = pool(hub["pieces"], EMPTY)
        files[structures / (hub["structure"] + ".json")] = {
            "type": "neroruins:complex_hub", "biomes": "#neroruins:has_structure/" + hub["biomes"],
            "spawn_overrides": {}, "step": "surface_structures", "terrain_adaptation": "none",
            "family": "crash_site", "size_class": hub["size_class"],
            "layouts": [{"weight": weight, "layout": jigsaw(hub["biomes"], hub["pool"], depth, hub["start_height"],
                                                            hub["distance"])}
                        for weight, depth in hub["depths"]],
            "ground_check": hub["ground"]}
        files[sets / (hub["structure"] + ".json")] = {
            "structures": [{"structure": NS + hub["structure"], "weight": 1}],
            "placement": {"type": "minecraft:random_spread", "spacing": hub["spacing"],
                          "separation": hub["separation"], "salt": hub["salt"]}}
        sat = hub["satellites"]
        for index in range(sat["count"]):
            name = hub["structure"] + "_debris_" + str(index)
            placement = {"hub_set": NS + hub["structure"], "family": "crash_site", "index": index,
                         "min_radius": sat["min"], "max_radius": sat["max"],
                         "chance_percent": 100 if index < sat["always"] else sat["chance"]}
            files[structures / (name + ".json")] = {
                "type": "neroruins:satellite", "biomes": "#neroruins:has_structure/" + hub["biomes"],
                "spawn_overrides": {}, "step": "surface_structures", "terrain_adaptation": "none",
                "placement": placement,
                "layout": jigsaw(hub["biomes"], "crash_site_debris", 1, -1, 16)}
            files[sets / (name + ".json")] = {"structures": [{"structure": NS + name, "weight": 1}],
                                              "placement": dict(placement, type="neroruins:satellite")}
    tags = DATA / "neroruins" / "tags" / "worldgen" / "biome" / "has_structure"
    cindara = ["nerospace:cindara", "nerospace:cindara_ember_plains", "nerospace:cindara_sulfur_vents"]
    files[tags / "crash_site_meridian.json"] = {"replace": False,
                                                "values": [{"id": b, "required": False} for b in cindara]}
    worlds = ["nerospace:luna_crater_field", "nerospace:luna_highlands", "nerospace:luna_maria",
              "nerospace:mars_canyons", "nerospace:mars_dunes", "nerospace:mars_polar"] + cindara + [
              "nerospace:glacira", "nerospace:glacira_methane_shore", "nerospace:glacira_spires",
              "nerospace:greenxertz", "nerospace:greenxertz_fungal_depths", "nerospace:greenxertz_spore_marsh",
              "nerospace:nyxara_dayside", "nerospace:nyxara_nightside", "nerospace:nyxara_twilight_band"]
    files[tags / "crash_site.json"] = {"replace": False, "values":
        [{"id": "#neroland:space/planet_biomes", "required": False},
         {"id": "#neroland:space/moon_biomes", "required": False}] + [{"id": b, "required": False} for b in worlds]}
    rules = [("neroruins:survey_hull", 0.3, RUST), ("neroruins:survey_hull", 0.06, "minecraft:blackstone")]

    def weathering(new_format):
        return {"processors": [
            {"processor_type": "minecraft:rule", "rules": [
                {"input_predicate": {"predicate_type": "minecraft:random_block_match", "block": block,
                                     "probability": chance},
                 "location_predicate": {"predicate_type": "minecraft:always_true"},
                 "output_state": output if new_format else {"Name": output}}
                for block, chance, output in rules]},
            {"processor_type": "neroruins:puzzle_seed"}]}
    files[DATA / "neroruins" / "worldgen" / "processor_list" / "crash_site_weathering.json"] = weathering(False)
    files[DATA_263 / "neroruins" / "worldgen" / "processor_list" / "crash_site_weathering.json"] = weathering(True)

    loot = DATA / "neroruins" / "loot_table" / "chests"
    loot_263 = DATA_263 / "neroruins" / "loot_table" / "chests"

    def tiers(*pools_):
        return {"type": "minecraft:chest", "pools": [
            {"rolls": rolls, "entries": [{"type": "minecraft:loot_table", "value": NS + "tier/" + str(t)}]}
            for t, rolls in pools_]}
    files[loot / "crash_site_salvage.json"] = tiers((0, 1), (1, {"type": "minecraft:uniform", "min": 0, "max": 1}))
    files[loot / "crash_site_cabin.json"] = tiers((1, {"type": "minecraft:uniform", "min": 1, "max": 2}))
    files[loot / "crash_site_meridian_vault.json"] = tiers((1, 2), (2, 2), (3, 1), (4, 1))
    files[loot / "crash_site_vault.json"] = tiers((1, 2), (2, 1), (3, 1), (4, 1))
    files[loot / "crash_site_outpost_vault.json"] = tiers((1, 2), (2, 1))
    files[loot / "crash_site_keystone.json"] = {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": NS + "meridian_keystone"}]},
        {"rolls": 1, "entries": [{"type": "minecraft:loot_table", "value": NS + "tier/2"}]}]}
    files[loot / "crash_site_keycard.json"] = {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": NS + "survey_keycard"}]},
        {"rolls": 1, "entries": [{"type": "minecraft:loot_table", "value": NS + "tier/0"}]}]}

    def logs(new_format):
        def shard(n):
            components = {"neroruins:log_id": NS + "cindara." + str(n)}
            if new_format:
                return {"type": "minecraft:item", "name": NS + "data_shard",
                        "modifier": {"components": components, "type": "minecraft:set_components"}}
            return {"type": "minecraft:item", "name": NS + "data_shard",
                    "functions": [{"function": "minecraft:set_components", "components": components}]}
        salvage = ({"type": "minecraft:tag", "expand": True, "items": "#neroruins:loot/salvage_metal"} if new_format
                   else {"type": "minecraft:tag", "name": "neroruins:loot/salvage_metal", "expand": True})
        return {"type": "minecraft:chest", "pools": [
            {"rolls": 1, "entries": [shard(n) for n in range(1, 7)]},
            {"rolls": 1, "entries": [salvage, {"type": "minecraft:item", "name": HULL}]}]}
    files[loot / "crash_site_logs.json"] = logs(False)
    files[loot_263 / "crash_site_logs.json"] = logs(True)

    files[DATA / "neroruins" / "spawn_tables" / "crash_site.json"] = {"entries": [
        {"entity": NS + "sentinel_mk1", "weight": 5, "min_tier": 1, "max_tier": 2},
        {"entity": NS + "sentinel_mk2", "weight": 5, "min_tier": 2, "max_tier": 3},
        {"entity": NS + "sentinel_mk3", "weight": 3, "min_tier": 3, "max_tier": 3},
        {"entity": "nerocreatures:space_pirate", "weight": 3, "min_tier": 1, "max_tier": 3, "required": False},
        {"entity": "nerocreatures:rogue_android", "weight": 2, "min_tier": 2, "max_tier": 3, "required": False},
        {"entity": "nerocreatures:rogue_drone", "weight": 2, "min_tier": 1, "max_tier": 3, "required": False},
        {"entity": "nerocreatures:plasma_slime", "weight": 2, "min_tier": 1, "max_tier": 2, "required": False},
        {"entity": "minecraft:husk", "weight": 2, "min_tier": 1, "max_tier": 1},
        {"entity": "minecraft:skeleton", "weight": 1, "min_tier": 1, "max_tier": 2}]}
    return files


def write(path, value):
    text = json.dumps(value, indent=2, ensure_ascii=False) + "\n"
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists() and path.read_text(encoding="utf-8") == text:
        return
    with open(path, "w", encoding="utf-8", newline="\n") as out:
        out.write(text)


def main():
    write(SPECS, {"templates": templates()})
    for path, value in data_files().items():
        write(path, value)


if __name__ == "__main__":
    main()
