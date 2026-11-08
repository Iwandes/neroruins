"""Generates the Colony Field family: template descriptions and their worldgen data.

Run from the repository root with ``python3 tools/colony_field_layout.py``, then ``python3 tools/build_templates.py``
to turn the descriptions into structure NBT. Everything this writes is derived from this file, so a layout change is
made here and regenerated, never by editing the generated JSON.

Chapters follow the lore bible: the citadel is the Frozen Reach (chapter 6, Glacira), the complex is Roots in the
Hull (chapter 7, Greenxertz), and outposts carry no unique logs. See decision 037.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SPEC_FILE = ROOT / "tools" / "templates" / "colony_field.json"
DATA = ROOT / "common" / "src" / "main" / "resources" / "data" / "neroruins"
DATA_263 = ROOT / "common" / "src" / "main" / "resources-26.3" / "data" / "neroruins"

FAMILY = "colony_field"
LINK = "neroruins:colony_link"
EMPTY = "minecraft:empty"

AIR = "minecraft:air"
HULL = "neroruins:survey_hull"
RUST = "neroruins:rusted_survey_hull"
PLAZA = "minecraft:polished_andesite"
ROOF = "minecraft:smooth_stone"
LIGHT = "minecraft:sea_lantern"
GLASS = "minecraft:white_stained_glass"
SOIL = "minecraft:coarse_dirt"
DEAD = "minecraft:dead_bush"
POST = "minecraft:andesite_wall"
WALLS = ["minecraft:white_concrete", HULL, "minecraft:light_gray_concrete"]
FLAGS = ["minecraft:orange_wool", "minecraft:white_wool", "minecraft:light_blue_wool"]

CLOCKWISE = {"north": "east", "east": "south", "south": "west", "west": "north"}
OPPOSITE = {"north": "south", "south": "north", "east": "west", "west": "east"}
MIRROR = {"east": "west", "west": "east", "north": "north", "south": "south"}

SPAWN_TABLE = "neroruins:colony_field"
PATHS = "neroruins:colony_field_paths"
LOTS = "neroruins:colony_field_lots"
CAPS = "neroruins:colony_field_caps"


def state(block, **properties):
    return {"id": block, "properties": {key: str(value).lower() for key, value in properties.items()}}


class Template:
    def __init__(self, name, size):
        self.name = name
        self.size = list(size)
        self.fills, self.connectors, self.containers = [], [], []
        self.vaults, self.spawners, self.terminals = [], [], []

    def fill(self, a, b, block):
        self.fills.append({"from": [min(p, q) for p, q in zip(a, b)],
                           "to": [max(p, q) for p, q in zip(a, b)], "block": block})

    def put(self, pos, block):
        self.fill(pos, pos, block)

    def exit(self, pos, orientation, pool):
        self.connectors.append({"pos": list(pos), "orientation": orientation, "name": LINK, "target": LINK,
                                "pool": pool, "final_state": AIR})

    def entry(self, pos, orientation):
        self.exit(pos, orientation, EMPTY)

    def chest(self, pos, table):
        self.containers.append({"pos": list(pos), "loot_table": table})

    def terminal(self, pos, log_id, facing):
        self.terminals.append({"pos": list(pos), "log_id": "neroruins:" + log_id, "facing": facing})

    def vault(self, pos, table, structure):
        self.vaults.append({"pos": list(pos), "loot_table": table, "structure": structure})

    def spawner(self, pos):
        self.spawners.append({"pos": list(pos), "table": SPAWN_TABLE, "tier": 1})

    def spec(self):
        spec = {"name": self.name, "size": self.size, "fills": self.fills}
        for key in ("connectors", "containers", "vaults", "spawners", "terminals"):
            if getattr(self, key):
                spec[key] = getattr(self, key)
        return spec


def mirror_x(spec):
    """Mirrors a description east-west, keeping every facing, corner conduit and connector consistent."""
    width = spec["size"][0]

    def flip(pos):
        return [width - 1 - pos[0], pos[1], pos[2]]

    def flip_block(block):
        if not isinstance(block, dict):
            return block
        properties = dict(block.get("properties", {}))
        if "facing" in properties:
            facing = properties["facing"]
            if block["id"] == "neroruins:conduit" and properties.get("corner") == "true":
                opened = {MIRROR[facing], MIRROR[CLOCKWISE[facing]]}
                facing = next(f for f in CLOCKWISE if {f, CLOCKWISE[f]} == opened)
            else:
                facing = MIRROR[facing]
            properties["facing"] = facing
        return {"id": block["id"], "properties": properties}

    out = {"name": spec["name"], "size": spec["size"], "fills": []}
    for fill in spec["fills"]:
        a, b = flip(fill["from"]), flip(fill["to"])
        out["fills"].append({"from": [min(p, q) for p, q in zip(a, b)], "to": [max(p, q) for p, q in zip(a, b)],
                             "block": flip_block(fill["block"])})
    for key in ("connectors", "containers", "vaults", "spawners", "terminals"):
        if key not in spec:
            continue
        out[key] = []
        for entry in spec[key]:
            entry = dict(entry, pos=flip(entry["pos"]))
            if key == "connectors":
                side, up = entry["orientation"].split("_")
                entry["orientation"] = MIRROR[side] + "_" + up
            if key == "terminals":
                entry["facing"] = MIRROR[entry["facing"]]
            out[key].append(entry)
    return out


def building(t, x0, z0, x1, z1, wall, doors, windows=True, height=4):
    """A walled room: hull floor, walls, a lit roof and 1x2 doorways at each (x, z) in ``doors``."""
    t.fill((x0, 0, z0), (x1, 0, z1), HULL)
    t.fill((x0, 1, z0), (x1, height, z1), wall)
    t.fill((x0 + 1, 1, z0 + 1), (x1 - 1, height, z1 - 1), AIR)
    t.fill((x0, height + 1, z0), (x1, height + 1, z1), ROOF)
    t.put(((x0 + x1) // 2, height + 1, (z0 + z1) // 2), LIGHT)
    if windows:
        for x in range(x0 + 2, x1 - 1, 3):
            t.put((x, 2, z0), GLASS)
            t.put((x, 2, z1), GLASS)
        for z in range(z0 + 2, z1 - 1, 3):
            t.put((x0, 2, z), GLASS)
            t.put((x1, 2, z), GLASS)
    for x, z in doors:
        t.fill((x, 1, z), (x, 2, z), AIR)


def furnish(t, x0, z0, x1, z1, items):
    """Lines the north and south inner walls with furniture, leaving the middle rows and both end columns open."""
    row = 0
    for z in (z0 + 1, z1 - 1):
        for x in range(x0 + 2, x1 - 1):
            t.put((x, 1, z), items[row % len(items)])
            row += 1


ROOMS = {
    "hab": [[ "minecraft:smooth_stone_slab", "minecraft:white_wool", "minecraft:barrel"],
            ["minecraft:barrel", "minecraft:smoker", "minecraft:white_wool", "minecraft:crafting_table"],
            ["minecraft:white_wool", "minecraft:spruce_slab", "minecraft:barrel", "minecraft:flower_pot"]],
    "school": [["minecraft:lectern", "minecraft:spruce_slab", "minecraft:bookshelf"],
               ["minecraft:spruce_slab", "minecraft:spruce_slab", "minecraft:lectern"],
               ["minecraft:bookshelf", "minecraft:lectern", "minecraft:birch_slab"]],
    "generator": [["minecraft:blast_furnace", HULL, "minecraft:redstone_lamp"],
                  [HULL, "minecraft:blast_furnace", "minecraft:iron_block", "minecraft:redstone_lamp"],
                  ["minecraft:redstone_lamp", "minecraft:blast_furnace", RUST]],
    "store": [["minecraft:barrel", "minecraft:barrel", HULL],
              ["minecraft:barrel", "minecraft:crafting_table", "minecraft:barrel"],
              [HULL, "minecraft:barrel", "minecraft:barrel", "minecraft:smithing_table"]],
    "farm": [["minecraft:composter", "minecraft:barrel", "minecraft:hay_block"],
             ["minecraft:barrel", "minecraft:composter"],
             ["minecraft:hay_block", "minecraft:barrel", "minecraft:composter", "minecraft:flower_pot"]],
    "records": [["minecraft:bookshelf", "minecraft:lectern", "minecraft:moss_block"],
                ["minecraft:lectern", "minecraft:bookshelf", "minecraft:bookshelf"],
                ["minecraft:moss_block", "minecraft:bookshelf", "minecraft:lectern"]],
}


def room(t, rect, role, variant, wall, door_side, log=None, chest=None):
    """A furnished building whose door faces the street; the log terminal sits on the far wall facing the door."""
    x0, z0, x1, z1 = rect
    mid = (z0 + z1) // 2
    door_x = x1 if door_side == "east" else x0
    building(t, x0, z0, x1, z1, wall, [(door_x, mid), (door_x, mid + 1)])
    furnish(t, x0, z0, x1, z1, ROOMS[role][variant])
    far = x0 + 1 if door_side == "east" else x1 - 1
    if log:
        t.terminal((far, 1, mid - 2), log, door_side)
    if chest:
        t.chest((far, 1, mid + 3), chest)
    if role == "generator":
        # The thermal core's input (orange) and heat (blue) lines run across the floor, as the log describes.
        t.fill((x0 + 1, 0, mid - 1), (x1 - 1, 0, mid - 1), "minecraft:orange_concrete")
        t.fill((x0 + 1, 0, mid + 2), (x1 - 1, 0, mid + 2), "minecraft:light_blue_concrete")
    if role in ("school", "records"):
        # The repeated symbol sequence the log mentions, copied along the far wall.
        wall_x = x0 if door_side == "east" else x1
        for i, z in enumerate(range(z0 + 2, z1 - 1)):
            t.put((wall_x, 3, z), state("neroruins:glyph_stone", glyph=(i * 5 + variant * 3) % 16))


def flag(t, x, z, colour):
    t.fill((x, 1, z), (x, 2, z), POST)
    t.put((x, 3, z), colour)


# --- conduit gate, authored solved; the puzzle_seed processor turns the pieces at generation -------------------
CONDUIT = [  # (dx, dz, block) relative to the source's row, copied from the conduit fixture room
    (0, 2, state("neroruins:conduit_source", facing="east")),
    (1, 2, state("neroruins:conduit", facing="east", corner=False, powered=False)),
    (2, 2, state("neroruins:conduit", facing="west", corner=True, powered=False)),
    (2, 1, state("neroruins:conduit", facing="north", corner=False, powered=False)),
    (2, 0, state("neroruins:conduit", facing="east", corner=True, powered=False)),
    (3, 0, state("neroruins:conduit", facing="east", corner=False, powered=False)),
    (4, 0, state("neroruins:conduit", facing="east", corner=False, powered=False)),
    (5, 0, state("neroruins:conduit", facing="south", corner=True, powered=False)),
    (5, 1, state("neroruins:conduit", facing="north", corner=False, powered=False)),
    (5, 2, state("neroruins:conduit", facing="north", corner=False, powered=False)),
    (5, 3, state("neroruins:conduit", facing="north", corner=False, powered=False)),
    (5, 4, state("neroruins:conduit", facing="north", corner=True, powered=False)),
    (6, 4, state("neroruins:conduit", facing="east", corner=False, powered=False)),
    (7, 4, state("neroruins:conduit_lock", powered=False)),
    (3, 2, state("neroruins:conduit", facing="north", corner=True, powered=False)),
    (3, 3, state("neroruins:conduit", facing="east", corner=False, powered=False)),
    (1, 4, state("neroruins:conduit", facing="south", corner=True, powered=False)),
    (6, 1, state("neroruins:conduit", facing="east", corner=True, powered=False)),
    (7, 2, state("neroruins:conduit", facing="north", corner=False, powered=False)),
]


def door(t, x, z, facing):
    t.put((x, 1, z), state("neroruins:sealed_door", facing=facing, half="lower", open=False))
    t.put((x, 2, z), state("neroruins:sealed_door", facing=facing, half="upper", open=False))


# --- citadel: the Frozen Reach ---------------------------------------------------------------------------------
CITADEL_SIZE = (48, 8, 48)
WEST_N, WEST_S, EAST_N, EAST_S = (2, 12, 13, 23), (2, 26, 13, 37), (34, 14, 45, 24), (34, 26, 45, 37)


def citadel_hub(variant):
    t = Template("colony_field_citadel_hub_" + "abc"[variant], CITADEL_SIZE)
    wall = WALLS[variant]
    structure = "neroruins:colony_field_citadel"
    t.fill((0, 0, 0), (47, 0, 47), PLAZA)
    t.fill((0, 1, 0), (47, 7, 47), AIR)
    # Landing pad and approach: log 1, the flags that link the houses to the hall, the wreck by the pad.
    t.fill((18, 0, 38), (29, 0, 38), "minecraft:yellow_concrete")
    t.fill((18, 0, 45), (29, 0, 45), "minecraft:yellow_concrete")
    t.fill((23, 0, 39), (24, 0, 44), "minecraft:yellow_concrete")
    t.fill((28, 1, 41), (30, 1, 43), RUST)
    t.fill((29, 2, 42), (30, 2, 43), HULL)
    t.terminal((19, 1, 40), "glacira.1", "east")
    for i, z in enumerate((14, 20, 26, 32)):
        flag(t, 21, z, FLAGS[(i + variant) % 3])
        flag(t, 26, z, FLAGS[(i + variant + 1) % 3])
    # Outer rooms: hab (log 2), generator (log 3), school (log 4), store.
    hab, school, generator, store = (WEST_S, WEST_N, EAST_S, EAST_N) if variant != 2 else (WEST_N, WEST_S, EAST_N, EAST_S)
    room(t, hab, "hab", variant, wall, "east", log="glacira.2", chest="neroruins:chests/colony_field_supplies")
    room(t, school, "school", variant, wall, "east", log="glacira.4")
    room(t, generator, "generator", variant, wall, "west", log="glacira.3")
    room(t, store, "store", variant, wall, "west", chest="neroruins:chests/colony_field_store")
    # The hall's dead field beside the hall.
    t.fill((2, 0, 1), (12, 0, 9), SOIL)
    for x in range(3, 12, 2):
        for z in range(2, 9, 2):
            t.put((x, 1, z), DEAD)
    # Council hall: guardian zone, conduit gate (log 5), council vault, and the sanctum (log 6, relic socket).
    building(t, 16, 1, 31, 11, HULL, [(28, 11), (29, 11)], windows=False)
    t.fill((17, 1, 5), (30, 4, 5), HULL)
    door(t, 24, 5, "south")
    for dx, dz, block in CONDUIT:
        t.put((18 + dx, 1, 6 + dz), block)
    t.spawner((27, 0, 8))
    t.terminal((30, 1, 7), "glacira.5", "west")
    t.vault((24, 1, 2), "neroruins:chests/colony_field_citadel_vault", structure)
    t.fill((31, 0, 2), (34, 5, 4), HULL)
    t.fill((31, 1, 3), (34, 2, 3), AIR)
    building(t, 34, 1, 45, 11, HULL, [(34, 3)], windows=False)
    t.fill((41, 1, 2), (41, 4, 10), HULL)
    door(t, 41, 6, "west")
    t.put((41, 2, 8), state("neroruins:relic_socket", facing="west", chapter=6, lit=False))
    t.vault((43, 1, 6), "neroruins:chests/colony_field_reliquary", structure)
    t.terminal((36, 1, 9), "glacira.6", "east")
    for i, z in enumerate(range(3, 10)):
        t.put((45, 3, z) if False else (35 + i, 3, 1), state("neroruins:glyph_stone", glyph=(i * 3 + variant) % 16))
    # Exits into the sprawl; the first is the pad entrance.
    t.exit((24, 1, 47), "south_up", PATHS)
    t.exit((0, 1, 30), "west_up", PATHS)
    t.exit((47, 1, 30), "east_up", PATHS)
    t.exit((1, 1, 0), "north_up", PATHS)
    t.exit((47, 1, 12), "east_up", PATHS)
    spec = t.spec()
    return mirror_x(spec) if variant == 1 else spec


# --- complex: Roots in the Hull ---------------------------------------------------------------------------------
COMPLEX_SIZE = (32, 8, 32)


def complex_hub(variant):
    t = Template("colony_field_complex_hub_" + "abc"[variant], COMPLEX_SIZE)
    wall = WALLS[(variant + 1) % 3]
    t.fill((0, 0, 0), (31, 0, 31), SOIL)
    t.fill((0, 1, 0), (31, 7, 31), AIR)
    # Clear grating marks the path from the pad (log 1).
    t.fill((14, 0, 0), (17, 0, 31), "minecraft:weathered_copper_grate")
    t.fill((10, 0, 26), (21, 0, 31), PLAZA)
    t.fill((14, 0, 28), (17, 0, 28), "minecraft:yellow_concrete")
    t.terminal((11, 1, 29), "greenxertz.1", "east")
    for x, z in ((12, 24), (19, 25), (13, 16), (19, 15)):
        t.fill((x, 1, z), (x, 2, z), "minecraft:moss_block")
    farm, records = ((1, 17, 11, 27), (1, 3, 11, 13)) if variant != 2 else ((1, 3, 11, 13), (1, 17, 11, 27))
    room(t, farm, "farm", variant, GLASS if variant == 0 else wall, "east", log="greenxertz.2",
         chest="neroruins:chests/colony_field_supplies")
    room(t, (20, 17, 30, 27), "hab", variant, wall, "west", log="greenxertz.3")
    room(t, records, "records", variant, wall, "east", log="greenxertz.4")
    # Gate room: guardian zone, Ballast Plates (counts shown on the plates), the council vault and log 6.
    building(t, 18, 1, 30, 13, HULL, [(18, 9), (18, 10)], windows=False)
    t.spawner((23, 0, 8))
    t.vault((28, 1, 3), "neroruins:chests/colony_field_vault", "neroruins:colony_field_complex")
    for x, z in ((24, 3), (28, 7), (22, 6), (26, 9)):
        t.put((x, 1, z), "neroruins:ballast_plate")
    t.terminal((20, 1, 3), "greenxertz.5", "east")
    t.terminal((29, 1, 9), "greenxertz.6", "west")
    t.exit((16, 1, 31), "south_up", PATHS)
    t.exit((0, 1, 15), "west_up", PATHS)
    t.exit((31, 1, 15), "east_up", PATHS)
    t.exit((15, 1, 0), "north_up", PATHS)
    spec = t.spec()
    return mirror_x(spec) if variant == 1 else spec


# --- outpost ------------------------------------------------------------------------------------------------------
OUTPOST_SIZE = (20, 8, 20)


def outpost_hub(variant):
    t = Template("colony_field_outpost_hub_" + "abc"[variant], OUTPOST_SIZE)
    wall = WALLS[(variant + 2) % 3]
    t.fill((0, 0, 0), (19, 0, 19), PLAZA)
    t.fill((0, 1, 0), (19, 7, 19), AIR)
    # Storehouse: guardian zone, Survey keycard reader, sealed door, vault.
    building(t, 10, 2, 18, 12, HULL, [(10, 9), (10, 10)], windows=False)
    t.fill((11, 1, 6), (17, 4, 6), HULL)
    door(t, 14, 6, "south")
    t.put((12, 2, 6), state("neroruins:card_reader", facing="south", tier=1, accepted=False))
    t.vault((14, 1, 4), "neroruins:chests/colony_field_outpost_vault", "neroruins:colony_field_outpost")
    t.spawner((14, 0, 9))
    keycard_hut, bunk_hut = ((1, 2, 7, 8), (1, 11, 7, 17)) if variant != 2 else ((1, 11, 7, 17), (1, 2, 7, 8))
    x0, z0, x1, z1 = keycard_hut
    building(t, x0, z0, x1, z1, wall, [(x1, (z0 + z1) // 2)])
    t.chest((x0 + 1, 1, z0 + 1), "neroruins:chests/colony_field_keycard")
    t.put((x0 + 1, 1, z1 - 1), "minecraft:barrel")
    x0, z0, x1, z1 = bunk_hut
    building(t, x0, z0, x1, z1, wall, [(x1, (z0 + z1) // 2)])
    t.fill((x0 + 1, 1, z0 + 1), (x0 + 1, 1, z1 - 1), "minecraft:white_wool")
    flag(t, 9, 16, FLAGS[variant])
    t.exit((10, 1, 19), "south_up", PATHS)
    t.exit((19, 1, 16), "east_up", PATHS)
    t.exit((0, 1, 9), "west_up", PATHS)
    spec = t.spec()
    return mirror_x(spec) if variant == 1 else spec


# --- sprawl: paths, lots and caps shared by every size class ------------------------------------------------------
def path_straight(variant):
    t = Template("colony_field_path_straight_" + "ab"[variant], (5, 6, 13))
    t.fill((0, 0, 0), (4, 0, 12), SOIL)
    t.fill((1, 0, 0), (3, 0, 12), PLAZA)
    t.fill((0, 1, 0), (4, 5, 12), AIR)
    if variant == 0:
        t.fill((4, 1, 6), (4, 2, 6), POST)
        t.put((4, 3, 6), "minecraft:lantern")
    else:
        flag(t, 0, 4, FLAGS[0])
        flag(t, 4, 9, FLAGS[2])
    t.entry((2, 1, 12), "south_up")
    t.exit((2, 1, 0), "north_up", LOTS)
    return t.spec()


def path_turn(right):
    t = Template("colony_field_path_turn_" + ("right" if right else "left"), (9, 6, 9))
    t.fill((0, 0, 0), (8, 0, 8), SOIL)
    t.fill((0, 1, 0), (8, 5, 8), AIR)
    if right:
        t.fill((1, 0, 1), (3, 0, 8), PLAZA)
        t.fill((1, 0, 1), (8, 0, 3), PLAZA)
        t.entry((2, 1, 8), "south_up")
        t.exit((8, 1, 2), "east_up", LOTS)
        flag(t, 5, 5, FLAGS[1])
    else:
        t.fill((5, 0, 1), (7, 0, 8), PLAZA)
        t.fill((0, 0, 1), (7, 0, 3), PLAZA)
        t.entry((6, 1, 8), "south_up")
        t.exit((0, 1, 2), "west_up", LOTS)
        flag(t, 3, 5, FLAGS[1])
    return t.spec()


def path_fork():
    t = Template("colony_field_path_fork", (9, 6, 9))
    t.fill((0, 0, 0), (8, 0, 8), SOIL)
    t.fill((0, 1, 0), (8, 5, 8), AIR)
    t.fill((3, 0, 1), (5, 0, 8), PLAZA)
    t.fill((0, 0, 1), (8, 0, 3), PLAZA)
    flag(t, 4, 0, FLAGS[0])
    t.entry((4, 1, 8), "south_up")
    t.exit((0, 1, 2), "west_up", LOTS)
    t.exit((8, 1, 2), "east_up", LOTS)
    return t.spec()


def lot(role, variant):
    t = Template("colony_field_%s_%s" % (role, "abc"[variant]), (11, 7, 11))
    t.fill((0, 0, 0), (10, 0, 10), SOIL)
    t.fill((0, 1, 0), (10, 6, 10), AIR)
    wall = WALLS[variant]
    if role == "farm":
        t.fill((1, 0, 1), (9, 0, 8), "minecraft:farmland" if variant == 1 else SOIL)
        for x in range(2, 9, 2):
            for z in range(2, 8, 2):
                t.put((x, 1, z), DEAD)
        t.fill((0, 1, 0), (10, 1, 0), "minecraft:spruce_fence")
        t.fill((0, 1, 0), (0, 1, 9), "minecraft:spruce_fence")
        t.fill((10, 1, 0), (10, 1, 9), "minecraft:spruce_fence")
        t.fill((5, 0, 8), (5, 0, 10), PLAZA)
        t.chest((9, 1, 9), "neroruins:chests/colony_field_supplies")
    else:
        building(t, 1, 1, 9, 9, wall, [(5, 9)])
        furnish(t, 1, 1, 9, 9, ROOMS["hab" if role == "house" else "store"][variant])
        t.chest((2, 1, 5), "neroruins:chests/colony_field_" + ("supplies" if role == "house" else "store"))
        t.put((5, 0, 10), PLAZA)
    t.entry((5, 1, 10), "south_up")
    return t.spec()


def cap(variant):
    t = Template("colony_field_cap_" + "ab"[variant], (3, 5, 3))
    t.fill((0, 0, 0), (2, 0, 2), PLAZA)
    t.fill((0, 1, 0), (2, 4, 2), AIR)
    t.fill((1, 1, 0), (1, 2, 0), POST)
    t.put((1, 3, 0), "minecraft:lantern" if variant == 0 else FLAGS[0])
    t.entry((1, 1, 2), "south_up")
    return t.spec()


# --- satellites ---------------------------------------------------------------------------------------------------
def silo(variant):
    t = Template("colony_field_silo_" + "ab"[variant], (11, 16, 11))
    t.fill((0, 0, 0), (10, 0, 10), PLAZA)
    towers = [(2, 2, 8, 8)] if variant == 0 else [(0, 2, 4, 6), (6, 4, 10, 8)]
    for i, (x0, z0, x1, z1) in enumerate(towers):
        top = 13 if variant == 0 else 10 + i
        t.fill((x0, 1, z0), (x1, top, z1), HULL)
        t.fill((x0 + 1, 1, z0 + 1), (x1 - 1, top - 1, z1 - 1), AIR)
        t.fill((x0 + 1, top + 1, z0 + 1), (x1 - 1, top + 1, z1 - 1), RUST)
        door_x = (x0 + x1) // 2
        t.fill((door_x, 1, z1), (door_x, 2, z1), AIR)
        t.chest((x0 + 1, 1, z0 + 1), "neroruins:chests/colony_field_store")
    return t.spec()


def cemetery(variant):
    t = Template("colony_field_cemetery_" + "ab"[variant], (13, 4, 13))
    t.fill((0, 0, 0), (12, 0, 12), SOIL)
    t.fill((0, 1, 0), (12, 3, 12), AIR)
    t.fill((0, 1, 0), (12, 1, 0), "minecraft:iron_bars")
    t.fill((0, 1, 12), (12, 1, 12), "minecraft:iron_bars")
    t.fill((0, 1, 0), (0, 1, 12), "minecraft:iron_bars")
    t.fill((12, 1, 0), (12, 1, 12), "minecraft:iron_bars")
    t.fill((6, 1, 12), (6, 1, 12), AIR)
    rows = range(2, 11, 2) if variant == 0 else range(3, 10, 3)
    for z in rows:
        for x in range(2, 11, 2 if variant == 0 else 3):
            if x == 6:
                continue
            t.put((x, 1, z), POST)
            t.put((x, 2, z), "minecraft:stone_brick_slab")
    t.put((6, 1, 2), state("neroruins:glyph_stone", glyph=4 + variant))
    return t.spec()


def wreck(variant):
    t = Template("colony_field_wreck_" + "ab"[variant], (15, 6, 15))
    t.fill((0, 0, 0), (14, 0, 14), PLAZA)
    t.fill((0, 1, 0), (14, 5, 14), AIR)
    t.fill((2, 0, 2), (12, 0, 2), "minecraft:yellow_concrete")
    t.fill((2, 0, 12), (12, 0, 12), "minecraft:yellow_concrete")
    for step in range(6):
        x = 3 + step if variant == 0 else 11 - step
        t.fill((x, 1, 5 + step // 2), (x + 1, 1 + step // 2, 8), HULL if step % 2 else RUST)
    t.chest((7, 1, 10), "neroruins:chests/colony_field_salvage")
    return t.spec()


def habring(variant):
    t = Template("colony_field_habring_" + "ab"[variant], (17, 7, 17))
    t.fill((0, 0, 0), (16, 0, 16), PLAZA)
    t.fill((0, 1, 0), (16, 6, 16), AIR)
    huts = [(0, 0, 5, 5), (11, 0, 16, 5), (0, 11, 5, 16), (11, 11, 16, 16)] if variant == 0 \
        else [(0, 0, 6, 6), (10, 5, 16, 11), (0, 10, 6, 16)]
    for i, (x0, z0, x1, z1) in enumerate(huts):
        door = (x1, (z0 + z1) // 2) if x0 == 0 else (x0, (z0 + z1) // 2)
        building(t, x0, z0, x1, z1, WALLS[(i + variant) % 3], [door], height=3)
        if i == 0:
            t.chest((x0 + 1, 1, z0 + 1), "neroruins:chests/colony_field_supplies")
    if variant == 1:
        t.fill((7, 1, 6), (9, 3, 10), GLASS)
        t.fill((8, 1, 7), (8, 2, 9), AIR)
        t.fill((8, 1, 6), (8, 2, 6), AIR)
    flag(t, 8, 8 if variant == 0 else 3, FLAGS[variant])
    return t.spec()


def sat_farm(variant):
    t = Template("colony_field_fields_" + "ab"[variant], (15, 6, 15))
    t.fill((0, 0, 0), (14, 0, 14), SOIL)
    t.fill((0, 1, 0), (14, 5, 14), AIR)
    for z in range(1, 14, 3 if variant == 0 else 4):
        t.fill((1, 0, z), (13, 0, z), "minecraft:farmland")
        for x in range(1, 14, 2):
            t.put((x, 1, z), DEAD)
    building(t, 10, 10, 14, 14, HULL, [(10, 12)], windows=False, height=3)
    t.chest((13, 1, 13), "neroruins:chests/colony_field_supplies")
    return t.spec()


SATELLITE_ROLES = {"habring": habring, "fields": sat_farm, "silo": silo, "cemetery": cemetery, "wreck": wreck}
# (role, chance percent, min radius, max radius) per slot; the slot index is the list position.
SATELLITES = {
    "citadel": [("habring", 80, 3, 6), ("habring", 80, 4, 8), ("habring", 75, 5, 10), ("habring", 70, 8, 13),
                ("fields", 80, 3, 7), ("fields", 75, 4, 9), ("fields", 75, 6, 11), ("fields", 70, 8, 14),
                ("silo", 75, 3, 6), ("silo", 65, 6, 12), ("cemetery", 70, 4, 9),
                ("wreck", 65, 5, 10), ("wreck", 60, 8, 14), ("habring", 70, 9, 14)],
    "complex": [("habring", 75, 2, 5), ("habring", 65, 4, 8), ("fields", 80, 2, 5), ("fields", 70, 3, 7),
                ("fields", 60, 5, 8), ("silo", 65, 3, 6), ("cemetery", 60, 3, 7), ("wreck", 55, 4, 8)],
    "outpost": [("fields", 60, 2, 3), ("habring", 50, 2, 4)],
}

CLASSES = {
    # class: biome tag values, spacing, separation, salt, layouts [(weight, depth)], max distance, ground check
    "citadel": {
        "biomes": ["nerospace:glacira", "nerospace:glacira_spires", "nerospace:glacira_methane_shore"],
        "spacing": 48, "separation": 20, "salt": 1593017264, "layouts": [(2, 6), (1, 5)], "distance": 112,
        "ground": {"radius": 32, "max_height_delta": 10, "require_dry": True, "min_depth": 8, "headroom": 12},
        "processor": "colony_field_frost"},
    "complex": {
        "biomes": ["nerospace:greenxertz", "nerospace:greenxertz_spore_marsh", "nerospace:greenxertz_fungal_depths"],
        "spacing": 36, "separation": 14, "salt": 1593017265, "layouts": [(2, 4), (1, 3)], "distance": 80,
        "ground": {"radius": 24, "max_height_delta": 8, "require_dry": True, "min_depth": 8, "headroom": 10},
        "processor": "colony_field_overgrowth"},
    "outpost": {
        "biomes": ["nerospace:mars_canyons", "nerospace:mars_dunes", "nerospace:mars_polar", "nerospace:glacira",
                   "nerospace:glacira_spires", "nerospace:greenxertz", "nerospace:greenxertz_spore_marsh",
                   "#neroland:space/planet_biomes"],
        "spacing": 24, "separation": 9, "salt": 1593017266, "layouts": [(2, 2), (1, 1)], "distance": 48,
        "ground": {"radius": 12, "max_height_delta": 6, "require_dry": True, "min_depth": 8, "headroom": 10},
        "processor": "colony_field_dust"},
}
HUBS = {"citadel": citadel_hub, "complex": complex_hub, "outpost": outpost_hub}
SATELLITE_SALT_BASE = 1593017300

PROCESSORS = {
    "colony_field_frost": [(HULL, RUST, 0.15), (PLAZA, "minecraft:snow_block", 0.2), (SOIL, "minecraft:snow_block", 0.3)],
    "colony_field_overgrowth": [(HULL, RUST, 0.25), (PLAZA, "minecraft:moss_block", 0.25),
                                (SOIL, "minecraft:rooted_dirt", 0.3)],
    "colony_field_dust": [(HULL, RUST, 0.3), (PLAZA, "minecraft:smooth_red_sandstone", 0.2)],
    "colony_field_weathering": [(HULL, RUST, 0.2)],
}


def templates():
    specs = [citadel_hub(v) for v in range(3)] + [complex_hub(v) for v in range(3)] + [outpost_hub(v) for v in range(3)]
    specs += [path_straight(0), path_straight(1), path_turn(False), path_turn(True), path_fork()]
    specs += [lot(role, v) for role in ("house", "farm", "store") for v in range(3)]
    specs += [cap(0), cap(1)]
    specs += [build(v) for build in SATELLITE_ROLES.values() for v in range(2)]
    return specs


def element(location, processor, weight=1):
    return {"weight": weight, "element": {"element_type": "minecraft:single_pool_element",
                                          "location": "neroruins:" + location,
                                          "processors": "neroruins:" + processor, "projection": "rigid"}}


def pool(elements, fallback=EMPTY):
    return {"fallback": fallback, "elements": elements}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def data_files():
    files = {}
    sprawl = "colony_field_weathering"
    files["worldgen/template_pool/colony_field_paths.json"] = pool(
        [element("colony_field_path_straight_a", sprawl, 3), element("colony_field_path_straight_b", sprawl, 3),
         element("colony_field_path_turn_left", sprawl, 2), element("colony_field_path_turn_right", sprawl, 2),
         element("colony_field_path_fork", sprawl, 2)], CAPS)
    files["worldgen/template_pool/colony_field_lots.json"] = pool(
        [element("colony_field_%s_%s" % (role, v), sprawl, weight)
         for role, weight in (("house", 3), ("farm", 2), ("store", 1)) for v in "abc"]
        + [element("colony_field_path_straight_a", sprawl, 2), element("colony_field_path_straight_b", sprawl, 2),
           element("colony_field_path_turn_left", sprawl, 1), element("colony_field_path_turn_right", sprawl, 1)],
        CAPS)
    files["worldgen/template_pool/colony_field_caps.json"] = pool(
        [element("colony_field_cap_a", sprawl), element("colony_field_cap_b", sprawl)])
    for name, rules in PROCESSORS.items():
        for overlay in (False, True):
            processors = [{"processor_type": "minecraft:rule", "rules": [{
                "input_predicate": {"predicate_type": "minecraft:random_block_match", "block": source,
                                    "probability": chance},
                "location_predicate": {"predicate_type": "minecraft:always_true"},
                "output_state": target if overlay else {"Name": target}} for source, target, chance in rules]},
                {"processor_type": "neroruins:puzzle_seed"}]
            files[("26.3:" if overlay else "") + "worldgen/processor_list/%s.json" % name] = {"processors": processors}
    salt = SATELLITE_SALT_BASE
    for size_class, config in CLASSES.items():
        name = "colony_field_" + size_class
        tag = "#neroruins:has_structure/" + name
        files["tags/worldgen/biome/has_structure/%s.json" % name] = {
            "replace": False, "values": [{"id": biome, "required": False} for biome in config["biomes"]]}
        files["worldgen/template_pool/%s_hubs.json" % name] = pool(
            [element("%s_hub_%s" % (name, v), config["processor"]) for v in "abc"])

        def jigsaw(depth, start_pool, distance, adaptation):
            return {"biomes": tag, "spawn_overrides": {}, "step": "surface_structures",
                    "terrain_adaptation": adaptation, "max_distance_from_center": distance,
                    "project_start_to_heightmap": "WORLD_SURFACE_WG", "size": depth,
                    "start_height": {"absolute": -1}, "start_pool": start_pool, "use_expansion_hack": False}

        files["worldgen/structure/%s.json" % name] = {
            "type": "neroruins:complex_hub", "biomes": tag, "spawn_overrides": {}, "step": "surface_structures",
            "terrain_adaptation": "beard_box", "family": FAMILY, "size_class": size_class,
            "layouts": [{"weight": weight, "layout": jigsaw(depth, "neroruins:%s_hubs" % name, config["distance"],
                                                             "beard_box")}
                        for weight, depth in config["layouts"]],
            "ground_check": config["ground"]}
        files["worldgen/structure_set/%s.json" % name] = {
            "structures": [{"structure": "neroruins:" + name, "weight": 1}],
            "placement": {"type": "minecraft:random_spread", "spacing": config["spacing"],
                          "separation": config["separation"], "salt": config["salt"]}}
        for role in SATELLITE_ROLES:
            files["worldgen/template_pool/%s_sat_%s.json" % (name, role)] = pool(
                [element("colony_field_%s_%s" % (role, v), config["processor"]) for v in "ab"])
        counters = {}
        for index, (role, chance, low, high) in enumerate(SATELLITES[size_class]):
            counters[role] = counters.get(role, 0) + 1
            satellite = "%s_%s_%d" % (name, role, counters[role])
            placement = {"hub_set": "neroruins:" + name, "family": FAMILY, "index": index,
                         "min_radius": low, "max_radius": high, "chance_percent": chance}
            files["worldgen/structure/%s.json" % satellite] = {
                "type": "neroruins:satellite", "biomes": tag, "spawn_overrides": {}, "step": "surface_structures",
                "terrain_adaptation": "beard_thin", "placement": placement,
                "layout": jigsaw(1, "neroruins:%s_sat_%s" % (name, role), 32, "beard_thin")}
            files["worldgen/structure_set/%s.json" % satellite] = {
                "structures": [{"structure": "neroruins:" + satellite, "weight": 1}],
                "placement": dict(placement, type="neroruins:satellite")}
            salt += 1
    files["spawn_tables/colony_field.json"] = {"entries": [
        {"entity": "neroruins:sentinel_mk1", "weight": 8, "min_tier": 1, "max_tier": 2},
        {"entity": "neroruins:sentinel_mk2", "weight": 2, "min_tier": 2, "max_tier": 3},
        {"entity": "nerocreatures:glacite_wisp", "weight": 3, "min_tier": 1, "max_tier": 2, "required": False},
        {"entity": "nerocreatures:rogue_drone", "weight": 2, "min_tier": 1, "max_tier": 3, "required": False},
        {"entity": "nerocreatures:space_pirate", "weight": 2, "min_tier": 1, "max_tier": 3, "required": False},
        {"entity": "minecraft:zombie", "weight": 2, "min_tier": 1, "max_tier": 1},
        {"entity": "minecraft:skeleton", "weight": 1, "min_tier": 1, "max_tier": 2}]}

    def chest(*pools):
        return {"type": "minecraft:chest", "pools": [
            {"rolls": rolls, "entries": [{"type": "minecraft:loot_table", "value": "neroruins:tier/%d" % tier}
                                         if isinstance(tier, int) else {"type": "minecraft:item", "name": tier}]}
            for tier, rolls in pools]}

    loot = {
        "supplies": chest((0, 2), (1, 1)),
        "store": chest((0, 1), (1, 2)),
        "salvage": chest((0, 2), (1, 1)),
        "keycard": chest(("neroruins:survey_keycard", 1), (0, 1)),
        "outpost_vault": chest((1, 2), (2, 1)),
        "vault": chest((1, 2), (2, 1), (3, 1)),
        "citadel_vault": chest((1, 2), (2, 1), (3, 1), ("neroruins:reach_thermal_core", 1)),
        "reliquary": chest((3, 1), (4, 1)),
    }
    for name, table in loot.items():
        files["loot_table/chests/colony_field_%s.json" % name] = table
    return files


def main():
    write(SPEC_FILE, {"templates": templates()})
    for relative, content in data_files().items():
        if relative.startswith("26.3:"):
            write(DATA_263 / relative[len("26.3:"):], content)
        else:
            write(DATA / relative, content)


if __name__ == "__main__":
    main()
