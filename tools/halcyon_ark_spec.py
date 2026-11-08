"""Authoring script for the Halcyon Ark family (decision 035).

Writes the compact template descriptions to tools/templates/halcyon_ark.json and the family's worldgen,
loot and spawn data. Run it, then run build_templates.py to regenerate the NBT. Everything here is
deterministic: the same script always writes the same files.

The Ark is a linear spine of rooms joined end to end along +Z. Every variant of a room role shares its
size and connector positions, so any mix of variants assembles into the same corridor and the order of
the chapter logs cannot change. Side branches leave the spine sideways and never extend past their host
room's length, so they cannot collide with the spine.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SPECS = ROOT / "tools" / "templates" / "halcyon_ark.json"
DATA = ROOT / "common" / "src" / "main" / "resources" / "data" / "neroruins"
OVERLAY = ROOT / "common" / "src" / "main" / "resources-26.3" / "data" / "neroruins"

HULL = "neroruins:survey_hull"
REINFORCED = "neroruins:reinforced_survey_hull"
RUST = "neroruins:rusted_survey_hull"
AIR = "minecraft:air"
LAMP = "minecraft:sea_lantern"
GLASS = "minecraft:light_blue_stained_glass"
WINDOW = "minecraft:light_gray_stained_glass"
DECK = "minecraft:polished_deepslate"
SPINE = "neroruins:halcyon_ark_spine"
SIDE = "neroruins:halcyon_ark_side"
BACK = "neroruins:halcyon_ark_back"
HOST = "neroruins:halcyon_ark_hull"
EMPTY = "minecraft:empty"

W, H, CX = 23, 12, 11
SPAWN_TABLE = "neroruins:halcyon_ark"

# Spine role lengths along Z. Every variant of a role uses the same length.
LENGTHS = {"hub": 19, "cryo": 17, "hydro": 17, "guard": 15, "engineering": 17, "medbay": 13, "captain": 15,
           "bridge": 17}
CITADEL_CHAIN = ["hub", "cryo", "hydro", "guard", "engineering", "medbay", "captain", "bridge"]
LUNA_CHAIN = ["hub", "cryo", "guard", "engineering", "medbay", "captain"]
CITADEL_LOGS = {"hub": 1, "cryo": 2, "hydro": 3, "engineering": 4, "medbay": 5, "bridge": 6}
LUNA_LOGS = {"hub": 1, "cryo": 2, "engineering": 4, "medbay": 5}
# Behind the conduit gate nothing may branch off: side rooms would be mineable ways in (decision 039).
SIDE_DOORS = {"cryo": 8, "hydro": 8}
# Rooms wholly behind a gate, and where the gated part of engineering starts (its bulkhead).
GATED_ROLES = ("medbay", "captain", "bridge")
ENGINEERING_BULKHEAD = 13


class Piece:
    def __init__(self, name, size):
        self.spec = {"name": name, "size": list(size), "fills": [], "connectors": []}

    def fill(self, a, b, block):
        self.spec["fills"].append({"from": list(a), "to": list(b), "block": block})

    def put(self, pos, block):
        self.fill(pos, pos, block)

    def add(self, kind, entry):
        self.spec.setdefault(kind, []).append(entry)

    def connector(self, pos, orientation, name, target, pool, final_state=AIR):
        self.spec["connectors"].append({"pos": list(pos), "orientation": orientation, "name": name,
                                        "target": target, "pool": pool, "final_state": final_state})


def pool_id(prefix, role):
    return "neroruins:%s_%s" % (prefix, role)


def shell(p, length, block=HULL, height=H):
    p.fill((0, 0, 0), (W - 1, height - 1, length - 1), block)
    p.fill((1, 1, 1), (W - 2, height - 2, length - 2), AIR)
    p.fill((1, 0, 1), (W - 2, 0, length - 2), HULL)
    for z in range(3, length - 2, 4):
        for x in (5, 11, 17):
            p.put((x, height - 1, z), LAMP)


def spine_door(p, z):
    p.fill((CX - 1, 1, z), (CX + 1, 3, z), AIR)


def spine_connectors(p, length, exit_pool, entrance=True):
    if entrance:
        spine_door(p, 0)
        p.connector((CX, 1, 0), "north_up", SPINE, BACK, EMPTY)
    if exit_pool:
        spine_door(p, length - 1)
        p.connector((CX, 1, length - 1), "south_up", HOST, SPINE, exit_pool)


def side_doors(p, z, side_pool):
    for x, orientation in ((0, "west_up"), (W - 1, "east_up")):
        p.put((x, 2, z), AIR)
        p.connector((x, 1, z), orientation, HOST, SIDE, side_pool)


def deck_path(p, length, x=CX):
    """Lit deck strip down the middle: the deck signs the boarding log tells players to follow."""
    for z in range(1, length - 1, 2):
        p.put((x, 0, z), LAMP)


def terminal(p, pos, log, facing):
    p.add("terminals", {"pos": list(pos), "log_id": "neroruins:ark.%d" % log, "facing": facing})


def chest(p, pos, table):
    p.add("containers", {"pos": list(pos), "loot_table": "neroruins:chests/" + table})


def vault(p, pos, table, structure):
    p.add("vaults", {"pos": list(pos), "loot_table": "neroruins:chests/" + table, "structure": structure})


def sealed_door(p, x, z, facing="north"):
    p.put((x, 1, z), {"id": "neroruins:sealed_door", "properties": {"facing": facing, "half": "lower", "open": "false"}})
    p.put((x, 2, z), {"id": "neroruins:sealed_door", "properties": {"facing": facing, "half": "upper", "open": "false"}})


# ---- spine rooms ----------------------------------------------------------------------------------

def hub(p, v, luna):
    length = LENGTHS["hub"]
    shell(p, length)
    if luna:
        # A torn breach where the section sheared off; the opening sits at ground level.
        spine_door(p, 0)
        p.fill((CX - 2, 1, 0), (CX + 2, 4, 0), AIR)
        for x, y in ((CX - 3, 1), (CX + 3, 1), (CX - 3, 5), (CX + 3, 2), (CX, 5)):
            p.put((x, y, 0), RUST)
    else:
        # Exterior boarding porch outside the airlock, level with the station platforms.
        p.fill((0, 0, 0), (W - 1, H - 1, 4), AIR)
        p.fill((5, 0, 0), (17, 0, 4), DECK)
        p.fill((5, 1, 0), (5, 1, 4), "minecraft:iron_bars")
        p.fill((17, 1, 0), (17, 1, 4), "minecraft:iron_bars")
        p.fill((6, 1, 0), (16, 1, 0), "minecraft:iron_bars")
        p.fill((CX - 1, 1, 0), (CX + 1, 1, 0), AIR)
        p.fill((CX - 1, 1, 5), (CX + 1, 3, 5), AIR)
    deck_path(p, length)
    if v == 0:  # lockers along both walls
        for z in range(7, length - 2, 2):
            p.put((1, 1, z), "minecraft:barrel")
            p.put((W - 2, 1, z), "minecraft:barrel")
        chest(p, (W - 2, 1, 6), "halcyon_ark_supplies")
    elif v == 1:  # cargo crates in the hold
        for x0, z0 in ((2, 8), (16, 8), (2, 13), (16, 13)):
            p.fill((x0, 1, z0), (x0 + 3, 2, z0 + 2), RUST)
        chest(p, (7, 1, 9), "halcyon_ark_supplies")
    else:  # collapsed ceiling panels
        for x0, z0 in ((3, 9), (15, 12), (4, 14)):
            p.fill((x0, 1, z0), (x0 + 2, 1, z0 + 1), RUST)
            p.fill((x0, H - 1, z0), (x0 + 2, H - 1, z0 + 1), RUST if luna else WINDOW)
        chest(p, (19, 1, 7), "halcyon_ark_supplies")
    terminal(p, (8, 1, 7), 1, "east")


def cryo(p, v):
    length = LENGTHS["cryo"]
    shell(p, length)

    def pod(x, z):
        p.put((x, 1, z), HULL)
        p.fill((x, 2, z), (x, 3, z), GLASS)
        p.put((x, 4, z), LAMP)

    rows = {0: (3, 7, 15, 19), 1: (5, 9, 13, 17), 2: ()}[v]
    for x in rows:
        for z in (2, 4, 6, 10, 12, 14):
            pod(x, z)
    if v == 2:
        # A ring of pods around the central life-support column.
        p.fill((CX, 1, 8), (CX, H - 2, 8), HULL)
        for dx, dz in ((-3, -3), (0, -4), (3, -3), (-4, 0), (4, 0), (-3, 3), (0, 4), (3, 3)):
            pod(CX + dx, 8 + dz)
        for x in (3, 19):
            for z in (3, 13):
                pod(x, z)
    # Pod lights in the floor mark the safe aisle.
    for z in range(1, length - 1, 2):
        p.put((CX - 1 if v != 1 else CX, 0, z), LAMP)
    terminal(p, {0: (10, 1, 11), 1: (2, 1, 11), 2: (6, 1, 11)}[v], 2, "east")
    chest(p, {0: (12, 1, 2), 1: (20, 1, 3), 2: (16, 1, 14)}[v], "halcyon_ark_supplies")


def hydro(p, v):
    length = LENGTHS["hydro"]
    shell(p, length)

    def planter(x0, x1, z0, z1, y=1):
        p.fill((x0, y, z0), (x1, y, z1), "minecraft:coarse_dirt")
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if (x + z) % 2 == 0:
                    p.put((x, y + 1, z), "minecraft:dead_bush")

    if v == 0:  # long rows, broken at the side doors
        for x0 in (3, 7, 14, 18):
            planter(x0, x0 + 1, 2, 6)
            planter(x0, x0 + 1, 10, 14)
    elif v == 1:  # terraced beds along the walls
        planter(1, 3, 2, 6)
        planter(1, 3, 10, 14)
        planter(19, 21, 2, 6)
        planter(19, 21, 10, 14)
        planter(8, 9, 3, 13)
        planter(13, 14, 3, 13)
    else:  # a glass grow tank in the middle
        p.fill((8, 1, 4), (14, 6, 12), "minecraft:glass")
        p.fill((9, 1, 5), (13, 5, 11), AIR)
        p.fill((9, 1, 5), (13, 1, 11), "minecraft:moss_block")
        p.put((11, 2, 8), "minecraft:dead_bush")
    # Lamps hang over the rows in the order the conduit room expects them read.
    for z in range(2, length - 2, 3):
        p.put((CX, H - 2, z), "minecraft:redstone_lamp")
    terminal(p, {0: (CX, 1, 3), 1: (CX, 1, 15), 2: (5, 1, 8)}[v], 3, {0: "south", 1: "north", 2: "east"}[v])
    chest(p, {0: (1, 1, 2), 1: (5, 1, 2), 2: (17, 1, 14)}[v], "halcyon_ark_supplies")


def guard(p, v, tier):
    """The only way through is a channel past the spawner, so every route enters its range."""
    length = LENGTHS["guard"]
    shell(p, length)
    block = {0: "minecraft:barrel", 1: RUST, 2: HULL}[v]
    p.fill((1, 1, 3), (7, H - 2, 11), block)
    p.fill((15, 1, 3), (21, H - 2, 11), block)
    if v == 1:  # pillars inside the channel for cover
        p.fill((9, 1, 5), (9, 3, 5), RUST)
        p.fill((13, 1, 9), (13, 3, 9), RUST)
    if v == 2:  # a fallen bulkhead narrows the channel
        p.fill((8, 1, 7), (8, 2, 7), RUST)
        p.fill((14, 1, 6), (14, 2, 6), RUST)
    p.add("spawners", {"pos": [CX, 0, 7], "table": SPAWN_TABLE, "tier": tier})
    for pos in ({0: ((10, 0, 4), (12, 0, 10)), 1: ((11, 0, 4), (10, 0, 9)), 2: ((12, 0, 5), (10, 0, 10))}[v]):
        p.put(pos, "neroruins:arc_grate")
    chest(p, (2, 1, 13), "halcyon_ark_salvage")


CONDUIT_PATHS = {
    0: [(5, 4), (6, 4), (7, 4), (8, 4), (8, 5), (8, 6), (9, 6), (10, 6), (11, 6), (12, 6), (12, 7), (12, 8),
        (13, 8), (14, 8), (15, 8), (16, 8)],
    1: [(6, 10), (7, 10), (8, 10), (8, 9), (8, 8), (8, 7), (9, 7), (10, 7), (10, 6), (10, 5), (11, 5), (12, 5),
        (13, 5), (13, 6), (13, 7), (14, 7), (15, 7)],
    2: [(16, 4), (15, 4), (14, 4), (13, 4), (13, 5), (13, 6), (12, 6), (11, 6), (10, 6), (9, 6), (9, 7), (9, 8),
        (9, 9), (8, 9), (7, 9), (6, 9)],
}
CONDUIT_DECOYS = {0: [(6, 8), (14, 5), (10, 10)], 1: [(6, 5), (15, 10), (11, 9)], 2: [(15, 8), (6, 5), (11, 9)]}
STEPS = {(0, -1): "north", (0, 1): "south", (1, 0): "east", (-1, 0): "west"}
CLOCKWISE = {"north": "east", "east": "south", "south": "west", "west": "north"}
OPPOSITE = {"north": "south", "south": "north", "east": "west", "west": "east"}


def direction(a, b):
    return STEPS[(b[0] - a[0], b[1] - a[1])]


def conduit_state(back, out):
    """The authored, solved state of a conduit whose openings face `back` and `out`."""
    if OPPOSITE[back] == out:
        return {"facing": out, "corner": "false", "powered": "false"}
    facing = back if CLOCKWISE[back] == out else out
    return {"facing": facing, "corner": "true", "powered": "false"}


def engineering(p, v):
    length = LENGTHS["engineering"]
    shell(p, length)
    # Bulkhead with the gate door; the junction lamps above the door show the powered branch.
    p.fill((1, 1, ENGINEERING_BULKHEAD), (W - 2, H - 2, ENGINEERING_BULKHEAD), REINFORCED)
    sealed_door(p, CX, ENGINEERING_BULKHEAD)
    p.put((CX, 4, ENGINEERING_BULKHEAD - 1), "minecraft:redstone_lamp")
    # Machinery banks along both walls, different per variant.
    machine = {0: "minecraft:blast_furnace", 1: "minecraft:iron_block", 2: "minecraft:smooth_stone"}[v]
    for z in range(2, 12, 2):
        p.fill((1, 1, z), (2, 3, z), machine if v != 0 else HULL)
        p.fill((W - 3, 1, z), (W - 2, 3, z), machine if v != 0 else HULL)
    path = CONDUIT_PATHS[v]
    source, cells, lock = path[0], path[1:-1], path[-1]
    p.put((source[0], 1, source[1]), {"id": "neroruins:conduit_source", "properties": {"facing": direction(source, cells[0])}})
    previous = source
    for i, cell in enumerate(cells):
        following = cells[i + 1] if i + 1 < len(cells) else lock
        p.put((cell[0], 1, cell[1]), {"id": "neroruins:conduit",
                                      "properties": conduit_state(direction(cell, previous), direction(cell, following))})
        previous = cell
    p.put((lock[0], 1, lock[1]), {"id": "neroruins:conduit_lock", "properties": {"powered": "false"}})
    for x, z in CONDUIT_DECOYS[v]:
        p.put((x, 1, z), {"id": "neroruins:conduit", "properties": {"facing": "east", "corner": "true", "powered": "false"}})
    terminal(p, {0: (4, 1, 12), 1: (17, 1, 12), 2: (4, 1, 2)}[v], 4, {0: "north", 1: "north", 2: "south"}[v])


def medbay(p, v):
    length = LENGTHS["medbay"]
    shell(p, length)
    beds = {0: [(2, 2), (2, 4), (2, 8), (2, 10), (19, 2), (19, 4), (19, 8), (19, 10)],
            1: [(7, 2), (7, 4), (7, 8), (7, 10), (14, 2), (14, 4), (14, 8), (14, 10)],
            2: [(3, 2), (6, 2), (15, 2), (18, 2), (3, 10), (6, 10), (15, 10), (18, 10)]}[v]
    for x, z in beds:
        p.fill((x, 1, z), (x + 1, 1, z), "minecraft:white_wool")
    if v == 2:  # curtained treatment bay
        p.fill((8, 1, 4), (8, 3, 8), "minecraft:white_stained_glass")
        p.fill((14, 1, 4), (14, 3, 8), "minecraft:white_stained_glass")
    p.fill((1, 1, length - 2), (4, 2, length - 2), "minecraft:quartz_block")
    # The director's seal: the tier-three card the captain's lock needs.
    chest(p, {0: (9, 1, 10), 1: (11, 1, 11), 2: (11, 1, 2)}[v], "halcyon_ark_medbay")
    terminal(p, {0: (13, 1, 10), 1: (4, 1, 6), 2: (16, 1, 6)}[v], 5, {0: "west", 1: "east", 2: "west"}[v])


def captain(p, v, luna):
    length = LENGTHS["captain"]
    shell(p, length)
    p.add("spawners", {"pos": [CX, 0, 4], "table": SPAWN_TABLE, "tier": 3})
    p.fill((1, 1, 7), (W - 2, H - 2, 7), REINFORCED)
    sealed_door(p, CX, 7)
    p.put((13, 2, 7), {"id": "neroruins:card_reader", "properties": {"facing": "north", "tier": "3", "accepted": "false"}})
    if v == 0:  # the captain's desk
        p.fill((14, 1, 10), (17, 1, 10), "minecraft:dark_oak_planks")
    elif v == 1:  # the chart table
        p.fill((14, 1, 9), (16, 1, 11), "minecraft:cartography_table")
    else:  # trophy cases
        for x in (14, 16, 18):
            p.fill((x, 1, 12), (x, 2, 12), "minecraft:glass")
    vault(p, (6, 1, 12), "halcyon_ark_vault", "neroruins:halcyon_ark_luna" if luna else "neroruins:halcyon_ark")
    if luna:
        # The severed end: buckled plating where the section tore away.
        for x, y in ((CX - 2, 2), (CX + 1, 4), (CX + 3, 1)):
            p.put((x, y, length - 1), RUST)


def bridge(p, v):
    length = LENGTHS["bridge"]
    shell(p, length)
    # The seal alcove at the bow: a relic socket opens it.
    p.fill((7, 1, 12), (15, H - 2, 15), REINFORCED)
    p.fill((8, 1, 13), (14, 4, 15), AIR)
    sealed_door(p, CX, 12)
    p.put((13, 2, 12), {"id": "neroruins:relic_socket", "properties": {"facing": "north", "chapter": "3", "lit": "false"}})
    vault(p, (CX, 1, 15), "halcyon_ark_bridge", "neroruins:halcyon_ark")
    consoles = {0: [(3, 4), (19, 4), (3, 8), (19, 8)], 1: [(6, 3), (16, 3), (CX, 6)], 2: [(2, 6), (20, 6), (8, 9), (14, 9)]}[v]
    for x, z in consoles:
        p.put((x, 1, z), "minecraft:lectern" if v == 1 else "minecraft:smooth_stone_slab")
    terminal(p, (5, 1, 14), 6, "east")


ROLE_BUILDERS = {"cryo": cryo, "hydro": hydro, "engineering": engineering, "medbay": medbay}


def block_at(p, pos):
    for fill in reversed(p.spec["fills"]):
        if all(fill["from"][i] <= pos[i] <= fill["to"][i] for i in range(3)):
            return fill["block"]
    return AIR


def armour(p, length, z0):
    """Wrap the part of a room from z0 on in reinforced plating (decision 039): walls, floor, ceiling, far end.

    Ceiling lamps the plating covers move one block down so the room stays lit. Doorways are cut afterwards by
    spine_connectors, so they stay open."""
    lamps = [(x, z) for z in range(3, length - 2, 4) for x in (5, 11, 17) if z >= z0]
    for a, b in (((0, 0, z0), (0, H - 1, length - 1)), ((W - 1, 0, z0), (W - 1, H - 1, length - 1)),
                 ((0, 0, z0), (W - 1, 0, length - 1)), ((0, H - 1, z0), (W - 1, H - 1, length - 1)),
                 ((0, 0, length - 1), (W - 1, H - 1, length - 1)), ((0, 0, z0), (W - 1, H - 1, z0))):
        if a[2] == b[2] == z0 and z0 > 0:
            continue  # a part-gated room's bulkhead is built (with its door) by the room itself
        p.fill(a, b, REINFORCED)
    for x, z in lamps:
        if block_at(p, (x, H - 2, z)) == AIR:
            p.put((x, H - 2, z), LAMP)


def spine_piece(prefix, chain, role, v, luna):
    length = LENGTHS[role]
    p = Piece("%s_%s_%d" % (prefix, role, v), (W, H, length))
    index = chain.index(role)
    next_pool = pool_id(prefix, chain[index + 1]) if index + 1 < len(chain) else None
    if role == "hub":
        hub(p, v, luna)
    elif role == "guard":
        guard(p, v, 2)
    elif role == "captain":
        captain(p, v, luna)
    elif role == "bridge":
        bridge(p, v)
    else:
        ROLE_BUILDERS[role](p, v)
    if role in GATED_ROLES:
        armour(p, length, 0)
    elif role == "engineering":
        armour(p, length, ENGINEERING_BULKHEAD)
    spine_connectors(p, length, next_pool, entrance=role != "hub")
    if role in SIDE_DOORS:
        side_doors(p, SIDE_DOORS[role], "neroruins:halcyon_ark_side_corridors")
    logs = LUNA_LOGS if luna else CITADEL_LOGS
    if role not in logs:
        p.spec.pop("terminals", None)
    return p.spec


# ---- side branches, caps and satellites ---------------------------------------------------------

def side_corridor(v):
    p = Piece("halcyon_ark_side_corridor_%d" % v, (5, 5, 7))
    p.fill((0, 0, 0), (4, 4, 6), HULL)
    p.fill((1, 1, 1), (3, 3, 5), AIR)
    for z in (0, 6):
        p.put((2, 2, z), AIR)
    p.connector((2, 1, 0), "north_up", SIDE, BACK, EMPTY)
    p.connector((2, 1, 6), "south_up", SIDE, SIDE, "neroruins:halcyon_ark_side_rooms")
    if v == 0:
        p.put((2, 4, 3), LAMP)
    else:
        p.fill((0, 2, 2), (0, 2, 4), "minecraft:iron_bars")
        p.fill((4, 2, 2), (4, 2, 4), "minecraft:iron_bars")
        p.fill((1, 0, 1), (3, 0, 5), "minecraft:iron_block")
    return p.spec


def side_room(v):
    p = Piece("halcyon_ark_side_room_%d" % v, (9, 6, 9))
    p.fill((0, 0, 0), (8, 5, 8), HULL)
    p.fill((1, 1, 1), (7, 4, 7), AIR)
    p.put((4, 2, 0), AIR)
    p.put((4, 5, 4), LAMP)
    p.connector((4, 1, 0), "north_up", SIDE, BACK, EMPTY)
    if v == 0:  # crew quarters
        for z in (2, 5):
            p.fill((1, 1, z), (2, 1, z), "minecraft:white_wool")
            p.fill((6, 1, z), (7, 1, z), "minecraft:white_wool")
        chest(p, (4, 1, 7), "halcyon_ark_salvage")
    elif v == 1:  # stores
        p.fill((1, 1, 6), (7, 2, 7), "minecraft:barrel")
        p.fill((3, 1, 6), (5, 2, 7), AIR)
        chest(p, (4, 1, 7), "halcyon_ark_supplies")
        chest(p, (1, 1, 2), "halcyon_ark_salvage")
    else:  # observation blister
        p.fill((1, 2, 8), (7, 4, 8), WINDOW)
        p.fill((2, 1, 4), (6, 1, 4), "minecraft:smooth_stone_slab")
        chest(p, (7, 1, 1), "halcyon_ark_salvage")
    return p.spec


def caps():
    spine = Piece("halcyon_ark_spine_cap", (3, 3, 1))
    spine.fill((0, 0, 0), (2, 2, 0), HULL)
    spine.connector((1, 0, 0), "north_up", SPINE, BACK, EMPTY, HULL)
    side = Piece("halcyon_ark_side_cap", (1, 2, 1))
    side.fill((0, 0, 0), (0, 1, 0), HULL)
    side.connector((0, 0, 0), "north_up", SIDE, BACK, EMPTY, HULL)
    return [spine.spec, side.spec]


def satellites():
    pieces = []
    for v in range(2):  # the stern, drifting free
        p = Piece("halcyon_ark_stern_%d" % v, (23, 15, 27))
        p.fill((0, 0, 0), (22, 14, 26), HULL)
        p.fill((1, 1, 0), (21, 13, 25), AIR)
        for x, y in ((0, 6), (5, 0), (12, 14), (22, 9), (17, 0)):
            p.fill((x, y, 0), (x, y, 2 + v), AIR)
        for x0 in (3, 15):
            p.fill((x0, 3 + v, 26), (x0 + 4, 7 + v, 26), "minecraft:blackstone")
        p.fill((1, 1, 18), (21, 1, 25), RUST)
        chest(p, (4, 2, 20), "halcyon_ark_salvage")
        chest(p, (18, 2, 22 - v), "halcyon_ark_supplies")
        pieces.append(p.spec)
    for v in range(2):  # the broken midsection with its outer cryo ring
        p = Piece("halcyon_ark_midsection_%d" % v, (23, 12, 21))
        p.fill((0, 0, 0), (22, 11, 20), HULL)
        p.fill((1, 1, 0), (21, 10, 20), AIR)
        for z in range(3, 18, 3):
            p.fill((2, 1, z), (2, 3, z), GLASS if (z + v) % 2 else RUST)
            p.fill((20, 1, z), (20, 3, z), GLASS if (z + v) % 2 == 0 else RUST)
        chest(p, (CX, 1, 10), "halcyon_ark_supplies")
        pieces.append(p.spec)
    for v in range(3):  # escape pods
        p = Piece("halcyon_ark_pod_%d" % v, (5, 5, 7))
        p.fill((0, 0, 0), (4, 4, 6), HULL if v != 1 else RUST)
        p.fill((1, 1, 1), (3, 3, 5), AIR)
        p.fill((1, 2, 6), (3, 3, 6), WINDOW)
        p.put((2, 1, 0), AIR)
        p.put((2, 2, 0), AIR)
        chest(p, (2, 1, 5), "halcyon_ark_salvage")
        pieces.append(p.spec)
    for v in range(3):  # debris clusters
        p = Piece("halcyon_ark_debris_%d" % v, (9, 5, 9))
        plates = {0: [((0, 0, 0), (3, 0, 2)), ((5, 2, 4), (8, 2, 8)), ((2, 4, 6), (4, 4, 7))],
                  1: [((1, 1, 1), (1, 4, 3)), ((4, 0, 5), (8, 1, 6)), ((6, 3, 0), (8, 3, 2))],
                  2: [((0, 2, 3), (2, 2, 8)), ((4, 0, 0), (6, 0, 2)), ((7, 4, 4), (8, 4, 6))]}[v]
        for i, (a, b) in enumerate(plates):
            p.fill(a, b, RUST if i % 2 else HULL)
        if v == 1:
            chest(p, (5, 2, 5), "halcyon_ark_salvage")
        pieces.append(p.spec)
    return pieces


def templates():
    specs = []
    for role in CITADEL_CHAIN:
        for v in range(3):
            specs.append(spine_piece("halcyon_ark", CITADEL_CHAIN, role, v, False))
    for role in LUNA_CHAIN:
        for v in range(3):
            specs.append(spine_piece("halcyon_ark_luna", LUNA_CHAIN, role, v, True))
    specs += [side_corridor(v) for v in range(2)] + [side_room(v) for v in range(3)] + caps() + satellites()
    return specs


# ---- data files ---------------------------------------------------------------------------------

def element(location, processors):
    return {"weight": 1, "element": {"element_type": "minecraft:single_pool_element",
                                      "location": "neroruins:" + location, "processors": processors,
                                      "projection": "rigid"}}


def pool(names, processors, fallback):
    return {"fallback": fallback, "elements": [element(n, processors) for n in names]}


def pools():
    out = {}
    station, luna = "neroruins:halcyon_ark_station", "neroruins:halcyon_ark_luna"
    for prefix, chain, processors in (("halcyon_ark", CITADEL_CHAIN, station), ("halcyon_ark_luna", LUNA_CHAIN, luna)):
        for role in chain:
            fallback = EMPTY if role == "hub" else "neroruins:halcyon_ark_spine_caps"
            out["%s_%s" % (prefix, role)] = pool(["%s_%s_%d" % (prefix, role, v) for v in range(3)], processors, fallback)
    out["halcyon_ark_side_corridors"] = pool(["halcyon_ark_side_corridor_%d" % v for v in range(2)], station,
                                             "neroruins:halcyon_ark_side_caps")
    out["halcyon_ark_side_rooms"] = pool(["halcyon_ark_side_room_%d" % v for v in range(3)], station,
                                         "neroruins:halcyon_ark_side_caps")
    out["halcyon_ark_spine_caps"] = pool(["halcyon_ark_spine_cap"], "minecraft:empty", EMPTY)
    out["halcyon_ark_side_caps"] = pool(["halcyon_ark_side_cap"], "minecraft:empty", EMPTY)
    for name, pool_name, count in SATELLITE_POOLS:
        out[pool_name] = pool(["halcyon_ark_%s_%d" % (name, v) for v in range(count)], station, EMPTY)
    return out


def jigsaw_layout(biomes, pool_name, size, distance, height, project):
    layout = {"biomes": biomes, "spawn_overrides": {}, "step": "surface_structures", "terrain_adaptation": "none",
              "max_distance_from_center": distance, "size": size, "start_height": {"absolute": height},
              "start_pool": pool_name, "use_expansion_hack": False}
    if project:
        layout["project_start_to_heightmap"] = "WORLD_SURFACE_WG"
    return layout


SATELLITE_POOLS = (("stern", "halcyon_ark_sterns", 2), ("midsection", "halcyon_ark_midsections", 2),
                   ("pod", "halcyon_ark_pods", 3), ("debris", "halcyon_ark_debris", 3))
CITADEL_BIOMES = "#neroruins:has_structure/halcyon_ark"
LUNA_BIOMES = "#neroruins:has_structure/halcyon_ark_luna"
# Station platforms stand at y 64; the Ark's deck sits level with them.
CITADEL_START_Y = 63
CITADEL_SATELLITES = [  # index: (name, pool, chance, min radius, max radius, start height)
    ("stern", "halcyon_ark_sterns", 100, 6, 10, 58),
    ("midsection", "halcyon_ark_midsections", 80, 4, 8, 61),
    ("pod_a", "halcyon_ark_pods", 75, 3, 7, 72),
    ("pod_b", "halcyon_ark_pods", 60, 3, 9, 50),
    ("debris_a", "halcyon_ark_debris", 80, 2, 6, 66),
    ("debris_b", "halcyon_ark_debris", 70, 2, 7, 56),
]
LUNA_SATELLITES = [
    ("debris", "halcyon_ark_debris", 70, 2, 5, 0),
    ("pod", "halcyon_ark_pods", 60, 2, 6, 0),
]
SALTS = {"halcyon_ark": 1847263091, "halcyon_ark_luna": 1192837465}


def structures():
    out = {}
    out["halcyon_ark"] = {
        "type": "neroruins:complex_hub", "biomes": CITADEL_BIOMES, "spawn_overrides": {}, "step": "surface_structures",
        "terrain_adaptation": "none", "family": "halcyon_ark", "size_class": "citadel",
        "layouts": [{"weight": 1, "layout": jigsaw_layout(CITADEL_BIOMES, "neroruins:halcyon_ark_hub",
                                                          len(CITADEL_CHAIN), 128, CITADEL_START_Y, False)}]}
    out["halcyon_ark_luna"] = {
        "type": "neroruins:complex_hub", "biomes": LUNA_BIOMES, "spawn_overrides": {}, "step": "surface_structures",
        "terrain_adaptation": "none", "family": "halcyon_ark", "size_class": "complex",
        "layouts": [{"weight": 1, "layout": jigsaw_layout(LUNA_BIOMES, "neroruins:halcyon_ark_luna_hub",
                                                          len(LUNA_CHAIN) + 1, 128, -1, True)}],
        "ground_check": {"radius": 32, "max_height_delta": 8, "require_dry": True, "min_depth": 8, "headroom": 16}}
    for hub_name, biomes, satellites, project in (("halcyon_ark", CITADEL_BIOMES, CITADEL_SATELLITES, False),
                                                  ("halcyon_ark_luna", LUNA_BIOMES, LUNA_SATELLITES, True)):
        for index, (name, pool_name, chance, low, high, height) in enumerate(satellites):
            placement = {"hub_set": "neroruins:" + hub_name, "family": "halcyon_ark", "index": index,
                         "min_radius": low, "max_radius": high, "chance_percent": chance}
            out["%s_%s" % (hub_name, name)] = {
                "type": "neroruins:satellite", "biomes": biomes, "spawn_overrides": {}, "step": "surface_structures",
                "terrain_adaptation": "none", "placement": placement,
                "layout": jigsaw_layout(biomes, "neroruins:" + pool_name, 1, 24, height, project)}
    return out


def structure_sets(structs):
    out = {"halcyon_ark": {"structures": [{"structure": "neroruins:halcyon_ark", "weight": 1}],
                           "placement": {"type": "minecraft:random_spread", "spacing": 80, "separation": 32,
                                         "salt": SALTS["halcyon_ark"]}},
           "halcyon_ark_luna": {"structures": [{"structure": "neroruins:halcyon_ark_luna", "weight": 1}],
                                "placement": {"type": "minecraft:random_spread", "spacing": 48, "separation": 20,
                                              "salt": SALTS["halcyon_ark_luna"]}}}
    for name, data in structs.items():
        if data["type"] == "neroruins:satellite":
            placement = dict(data["placement"])
            placement["type"] = "neroruins:satellite"
            out[name] = {"structures": [{"structure": "neroruins:" + name, "weight": 1}], "placement": placement}
    return out


def processor_lists():
    def rule(probability, overlay):
        output = RUST if overlay else {"Name": RUST}
        return {"processors": [
            {"processor_type": "minecraft:rule", "rules": [{
                "input_predicate": {"predicate_type": "minecraft:random_block_match", "block": HULL,
                                    "probability": probability},
                "location_predicate": {"predicate_type": "minecraft:always_true"},
                "output_state": output}]},
            {"processor_type": "neroruins:puzzle_seed"}]}
    return {False: {"halcyon_ark_station": rule(0.18, False), "halcyon_ark_luna": rule(0.32, False)},
            True: {"halcyon_ark_station": rule(0.18, True), "halcyon_ark_luna": rule(0.32, True)}}


def loot_tables():
    def table(*pools_):
        return {"type": "minecraft:chest", "pools": list(pools_)}

    def tier(n, rolls=1):
        return {"rolls": rolls, "entries": [{"type": "minecraft:loot_table", "value": "neroruins:tier/%d" % n}]}

    return {
        "halcyon_ark_salvage": table(tier(0, 2), tier(1)),
        "halcyon_ark_supplies": table(tier(1, 2), tier(0)),
        "halcyon_ark_medbay": table({"rolls": 1, "entries": [{"type": "minecraft:item",
                                                              "name": "neroruins:command_keycard"}]}, tier(1)),
        "halcyon_ark_vault": table(tier(2, 2), tier(3), tier(4)),
        "halcyon_ark_bridge": table(tier(3), tier(4, 2)),
    }


def spawn_table():
    entries = [
        ("neroruins:sentinel_mk1", 5, 1, 2, True), ("neroruins:sentinel_mk2", 5, 2, 3, True),
        ("neroruins:sentinel_mk3", 3, 3, 3, True),
        ("nerocreatures:rogue_android", 3, 2, 3, False), ("nerocreatures:rogue_drone", 4, 1, 3, False),
        ("nerocreatures:space_pirate", 2, 2, 3, False), ("nerocreatures:void_crawler", 2, 1, 3, False),
        ("nerocreatures:lunar_stalker", 2, 2, 3, False),
        ("minecraft:zombie", 1, 1, 1, True), ("minecraft:skeleton", 1, 1, 2, True),
    ]
    out = []
    for entity, weight, low, high, required in entries:
        entry = {"entity": entity, "weight": weight, "min_tier": low, "max_tier": high}
        if not required:
            entry["required"] = False
        out.append(entry)
    return {"entries": out}


def biome_tags():
    luna = [{"id": "nerospace:" + b, "required": False} for b in ("luna_maria", "luna_highlands", "luna_crater_field")]
    return {"halcyon_ark": {"replace": False, "values": ["minecraft:the_void"]},
            "halcyon_ark_luna": {"replace": False, "values": luna}}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(data, indent=2, ensure_ascii=False) + "\n"
    if path.exists() and path.read_text(encoding="utf-8") == text:
        return
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(text)


def main():
    write(SPECS, {"templates": templates()})
    worldgen = DATA / "worldgen"
    for name, data in pools().items():
        write(worldgen / "template_pool" / (name + ".json"), data)
    structs = structures()
    for name, data in structs.items():
        write(worldgen / "structure" / (name + ".json"), data)
    for name, data in structure_sets(structs).items():
        write(worldgen / "structure_set" / (name + ".json"), data)
    lists = processor_lists()
    for name, data in lists[False].items():
        write(worldgen / "processor_list" / (name + ".json"), data)
    for name, data in lists[True].items():
        write(OVERLAY / "worldgen" / "processor_list" / (name + ".json"), data)
    for name, data in loot_tables().items():
        write(DATA / "loot_table" / "chests" / (name + ".json"), data)
    write(DATA / "spawn_tables" / "halcyon_ark.json", spawn_table())
    for name, data in biome_tags().items():
        write(DATA / "tags" / "worldgen" / "biome" / "has_structure" / (name + ".json"), data)


if __name__ == "__main__":
    main()
