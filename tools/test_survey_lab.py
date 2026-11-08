"""Survey Lab family: pool graph, budgets, and per-template gating rules (decision 036)."""
import copy
import json
import unittest
from collections import deque
from functools import lru_cache

from build_templates import ROOT, SPEC_DIRECTORY
import gated_enclosure

DATA = ROOT / "common" / "src" / "main" / "resources" / "data" / "neroruins"
SPECS = {spec["name"]: spec for spec in json.loads(
    (SPEC_DIRECTORY / "survey_lab.json").read_text(encoding="utf-8"))["templates"]}
PASSABLE = {"minecraft:air", "minecraft:ladder", "neroruins:conduit", "neroruins:ballast_plate"}
LOCKS = {"neroruins:card_reader", "neroruins:glyph_lock", "neroruins:conduit_lock", "neroruins:relic_socket"}
CARDS = {"survey": 1, "security": 2, "command": 3}
UNLOCK_REACH, UNLOCK_VERTICAL = 8, 4  # PuzzleUnlock
PLATE_RADIUS, PLATE_VERTICAL = 6, 3  # VaultRewardService.puzzleSolved
SPAWNER_RANGE = 8
BUDGETS = {"survey_lab": (10, 120), "survey_lab_complex": (5, 66), "survey_lab_outpost": (3, 30)}


@lru_cache(maxsize=None)
def json_file(*parts):
    return json.loads(DATA.joinpath(*parts).read_text(encoding="utf-8"))


def block_id(block):
    if isinstance(block, dict):
        return block["id"]
    return block.split("[")[0] if block else block


def props(block):
    return block.get("properties", {}) if isinstance(block, dict) else {}


def blocks_of(spec):
    blocks = {}
    for fill in spec["fills"]:
        (x0, y0, z0), (x1, y1, z1) = fill["from"], fill["to"]
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    blocks[(x, y, z)] = fill["block"]
    for connector in spec["connectors"]:
        blocks[tuple(connector["pos"])] = connector["final_state"]
    for kind, block in (("containers", "minecraft:chest"), ("vaults", "neroruins:vault_cache"),
                        ("spawners", "neroruins:ruin_spawner"), ("terminals", "neroruins:log_terminal")):
        for entry in spec.get(kind, []):
            blocks[tuple(entry["pos"])] = block
    return blocks


def at(blocks, *ids):
    return [pos for pos, block in blocks.items() if block_id(block) in ids]


def walk(blocks, start, open_doors=frozenset()):
    """Cells a player's feet can occupy: walking, falling, one-block steps and ladders. Closed doors block."""
    def passable(pos):
        block = blocks.get(pos)
        if block_id(block) == "neroruins:sealed_door":
            lower = pos if props(block).get("half") == "lower" else (pos[0], pos[1] - 1, pos[2])
            return lower in open_doors
        return block_id(block) in PASSABLE

    def ladder(pos):
        return block_id(blocks.get(pos)) == "minecraft:ladder"

    def supported(pos):
        below = (pos[0], pos[1] - 1, pos[2])
        return ladder(pos) or ladder(below) or (blocks.get(below) is not None and not passable(below))

    def settle(pos):
        while not supported(pos) and passable((pos[0], pos[1] - 1, pos[2])):
            pos = (pos[0], pos[1] - 1, pos[2])
        return pos

    seen = {start}
    queue = deque([start])
    while queue:
        x, y, z = queue.popleft()
        options = []
        for nx, nz in ((x - 1, z), (x + 1, z), (x, z - 1), (x, z + 1)):
            if passable((nx, y, nz)) and passable((nx, y + 1, nz)):
                options.append(settle((nx, y, nz)))
            elif passable((nx, y + 1, nz)) and passable((nx, y + 2, nz)) and passable((x, y + 2, z)):
                options.append((nx, y + 1, nz))
        if ladder((x, y, z)) and passable((x, y + 1, z)):
            options.append((x, y + 1, z))
        if (ladder((x, y, z)) or ladder((x, y - 1, z))) and passable((x, y - 1, z)):
            options.append((x, y - 1, z))
        for option in options:
            if option not in seen:
                seen.add(option)
                queue.append(option)
    return seen


def usable(cells, pos):
    x, y, z = pos
    return any((nx, stand, nz) in cells for nx, ny, nz in ((x - 1, y, z), (x + 1, y, z), (x, y, z - 1), (x, y, z + 1))
               for stand in range(y - 3, y + 1))


def within_unlock(lock, pos):
    return (abs(lock[0] - pos[0]) <= UNLOCK_REACH and abs(lock[2] - pos[2]) <= UNLOCK_REACH
            and abs(lock[1] - pos[1]) <= UNLOCK_VERTICAL)


def within_plates(vault, pos):
    return (abs(vault[0] - pos[0]) <= PLATE_RADIUS and abs(vault[2] - pos[2]) <= PLATE_RADIUS
            and abs(vault[1] - pos[1]) <= PLATE_VERTICAL)


def guarded(spawners, pos):
    return any(sum((a - b) ** 2 for a, b in zip(spawner, pos)) <= SPAWNER_RANGE ** 2 for spawner in spawners)


def lower_doors(blocks):
    return [pos for pos in at(blocks, "neroruins:sealed_door") if props(blocks[pos]).get("half") == "lower"]


def gate_doors(blocks):
    return [pos for pos in lower_doors(blocks) if pos[1] >= 7]


def levels():
    return {name: spec for name, spec in SPECS.items() if "_level_" in name}


def surfaces():
    return {name: spec for name, spec in SPECS.items() if "_surface_" in name}


def start_of(spec):
    return (16, 12, 16)


def gating_holds(spec):
    """Before the gate: the lock is usable, the vault and the floor below are not. After: everything is."""
    blocks = blocks_of(spec)
    closed = walk(blocks, start_of(spec))
    gates = gate_doors(blocks)
    upper_locks = [pos for pos in at(blocks, *LOCKS) if pos[1] >= 7]
    upper_vaults = [pos for pos in at(blocks, "neroruins:vault_cache") if pos[1] >= 7]
    if len(gates) != 1 or not upper_locks or not upper_vaults:
        return False
    if not all(usable(closed, lock) for lock in upper_locks):
        return False
    if any(usable(closed, vault) for vault in at(blocks, "neroruins:vault_cache")):
        return False
    if any(cell[1] < 7 for cell in closed):
        return False
    opened = walk(blocks, start_of(spec), frozenset(gates))
    all_open = walk(blocks, start_of(spec), frozenset(lower_doors(blocks)))
    sanctum = [pos for pos in lower_doors(blocks) if pos[1] < 7]
    for vault in at(blocks, "neroruins:vault_cache"):
        behind_sanctum = sanctum and vault[1] < 7 and vault[2] < sanctum[0][2]
        if behind_sanctum and usable(opened, vault):
            return False
        if not usable(all_open, vault):
            return False
    for kind in ("neroruins:card_reader", "neroruins:glyph_lock", "neroruins:conduit_lock", "neroruins:relic_socket",
                 "neroruins:conduit", "neroruins:conduit_source", "minecraft:chest", "neroruins:log_terminal",
                 "neroruins:glyph_clue"):
        if not all(usable(all_open, pos) for pos in at(blocks, kind)):
            return False
    exits = [tuple(c["pos"]) for c in spec["connectors"] if c["orientation"].startswith("down")]
    if not all(pos in all_open for pos in exits):
        return False
    sides = [tuple(c["pos"]) for c in spec["connectors"] if c["orientation"].endswith("_up")
             and not c["orientation"].startswith(("up", "down"))]
    return all(pos in all_open for pos in sides)


def unlocks_hold(spec):
    blocks = blocks_of(spec)
    locks = at(blocks, *LOCKS)
    plates = at(blocks, "neroruins:ballast_plate")
    for door in lower_doors(blocks):
        if sum(1 for lock in locks if within_unlock(lock, door)) != 1:
            return False
    for vault in at(blocks, "neroruins:vault_cache"):
        by_lock = [lock for lock in locks if within_unlock(lock, vault)]
        by_plate = [plate for plate in plates if within_plates(vault, plate)]
        if len(by_lock) + (1 if by_plate else 0) != 1:
            return False
    return True


def guardian_zone_holds(spec):
    blocks = blocks_of(spec)
    spawners = [tuple(s["pos"]) for s in spec.get("spawners", [])]
    return all(guarded(spawners, pos) for pos in lower_doors(blocks) + at(blocks, *LOCKS))


def pool(name):
    return json_file("worldgen", "template_pool", name.split(":")[1] + ".json")


def pool_templates(name):
    return [element["element"]["location"].split(":")[1] for element in pool(name)["elements"]]


def chain(first_pool):
    """The spine below a surface: a list of pool names, one per level, as the down connectors name them."""
    order = []
    current = first_pool
    while current:
        order.append(current)
        downs = {c["pool"] for t in pool_templates(current) for c in SPECS[t]["connectors"]
                 if c["orientation"].startswith("down")}
        if len(downs) > 1:
            raise AssertionError("variants of one level must lead to the same next level")
        current = next(iter(downs)) if downs else None
    return order


def card_tiers(names):
    tiers = None
    for name in names:
        found = {CARDS[c["loot_table"].rsplit("_", 1)[1]] for c in SPECS[name].get("containers", [])
                 if "_keycard_" in c["loot_table"]}
        tiers = found if tiers is None else tiers & found
    return tiers or set()


def keycards_obtainable(surface_pool):
    owned = card_tiers(pool_templates(surface_pool))
    first = {c["pool"] for t in pool_templates(surface_pool) for c in SPECS[t]["connectors"]}
    for level_pool in chain(next(iter(first))):
        names = pool_templates(level_pool)
        for name in names:
            readers = at(blocks_of(SPECS[name]), "neroruins:card_reader")
            for reader in readers:
                tier = int(props(blocks_of(SPECS[name])[reader])["tier"])
                if not any(card >= tier for card in owned):
                    return False
        owned |= card_tiers(names)
    return True


def logs_in_order(surface_pool):
    sequence = []
    first = {c["pool"] for t in pool_templates(surface_pool) for c in SPECS[t]["connectors"]}
    for group in [surface_pool] + chain(next(iter(first))):
        per_variant = []
        for name in pool_templates(group):
            terminals = sorted(SPECS[name].get("terminals", []), key=lambda t: -t["pos"][1])
            per_variant.append([t["log_id"] for t in terminals])
        if any(variant != per_variant[0] for variant in per_variant):
            return None
        sequence += per_variant[0]
    return sequence


def budget(structure):
    """Deepest piece depth and an upper bound on pieces, walking every connector of every variant."""
    layout = json_file("worldgen", "structure", structure + ".json")["layouts"][0]["layout"]
    limit = layout["size"]

    @lru_cache(maxsize=None)
    def expand(pool_name, depth):
        if pool_name == "minecraft:empty" or depth > limit:
            return depth - 1, 0
        best_depth, best_count = depth, 0
        for name in pool_templates(pool_name):
            deepest, count = depth, 1
            for connector in SPECS[name]["connectors"]:
                if connector["pool"] == "minecraft:empty" or depth == limit:
                    continue
                child_depth, child_count = expand(connector["pool"], depth + 1)
                deepest = max(deepest, child_depth)
                count += child_count
            best_depth, best_count = max(best_depth, deepest), max(best_count, count)
        return best_depth, best_count

    return expand(layout["start_pool"], 0), limit


FACING_STEP = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}


def sealed_where_joined(spec):
    """The piece's blocks with every connector doorway shut: a joined doorway leads only into another plated piece
    (a level, corridor, room or cap), so the enclosure check stops there."""
    blocks = blocks_of(spec)
    for c in spec["connectors"]:
        x, y, z = c["pos"]
        blocks[(x, y, z)] = "neroruins:reinforced_survey_hull"
        if not c["orientation"].startswith(("up", "down")):
            blocks[(x, y + 1, z)] = "neroruins:reinforced_survey_hull"
    return blocks


def level_breaches(spec):
    """Decision 039: from behind each sealed door, mining reaches neither the world nor the door's near side."""
    blocks, found = sealed_where_joined(spec), []
    for pos in lower_doors(blocks):
        dx, dz = FACING_STEP[props(blocks[pos])["facing"]]
        near, far = (pos[0] + dx, pos[1], pos[2] + dz), (pos[0] - dx, pos[1], pos[2] - dz)
        hit = gated_enclosure.breach(blocks, far, forbidden={near})
        if hit is not None:
            found.append((pos, hit))
    return found


def branch_breaches(spec):
    """A corridor, room or cap off a gated floor: no interior cell can be mined out to the world."""
    blocks = sealed_where_joined(spec)
    for pos, block in blocks.items():
        if block_id(block) == "minecraft:air":
            hit = gated_enclosure.breach(blocks, pos)
            if hit is not None:
                return [(pos, hit)]
    return []


def branch_templates():
    names, queue = set(), [c["pool"] for spec in levels().values() for c in spec["connectors"]
                           if not c["orientation"].startswith(("up", "down"))]
    visited = set()
    while queue:
        current = queue.pop()
        if current == "minecraft:empty" or current in visited:
            continue
        visited.add(current)
        document = pool(current)
        for name in pool_templates(current):
            if name not in names:
                names.add(name)
                queue += [c["pool"] for c in SPECS[name]["connectors"]]
        fallback = document.get("fallback", "minecraft:empty")
        if fallback != "minecraft:empty" and fallback != current:
            queue.append(fallback)
    return sorted(names)


class SurveyLabTest(unittest.TestCase):
    def test_gated_floors_cannot_be_mined_into(self):
        for name, spec in levels().items():
            with self.subTest(level=name):
                self.assertTrue(lower_doors(blocks_of(spec)))
                self.assertEqual([], level_breaches(spec))
        for name in branch_templates():
            with self.subTest(branch=name):
                self.assertEqual([], branch_breaches(SPECS[name]))
        self.assertIn("survey_lab_corridor_cap", branch_templates())

    def test_one_mineable_panel_under_a_gated_floor_is_caught(self):
        weakened = copy.deepcopy(SPECS["survey_lab_level_1_a"])
        weakened["fills"].append({"from": [5, 0, 5], "to": [5, 0, 5], "block": "neroruins:survey_hull"})
        self.assertTrue(level_breaches(weakened))
        corridor = copy.deepcopy(SPECS["survey_lab_corridor_short"])
        corridor["fills"].append({"from": [4, 4, 2], "to": [4, 4, 2], "block": "neroruins:survey_hull"})
        self.assertTrue(branch_breaches(corridor))

    def test_every_level_gates_its_vault_and_descent_behind_its_lock(self):
        self.assertEqual(30, len(levels()))
        for name, spec in levels().items():
            with self.subTest(name=name):
                self.assertTrue(gating_holds(spec))
        broken = copy.deepcopy(SPECS["survey_lab_level_1_a"])
        broken["fills"].append({"from": [20, 8, 24], "to": [20, 9, 24], "block": "minecraft:air"})
        self.assertFalse(gating_holds(broken), "a hole in the gate wall must be caught")

    def test_every_door_and_vault_is_opened_by_exactly_one_mechanism(self):
        for name, spec in levels().items():
            with self.subTest(name=name):
                self.assertTrue(unlocks_hold(spec))
        far = copy.deepcopy(SPECS["survey_lab_level_2_a"])
        far["vaults"][0]["pos"] = [16, 8, 31]
        far["fills"].append({"from": [16, 8, 31], "to": [16, 8, 31], "block": "minecraft:air"})
        far["vaults"][0]["pos"] = [2, 8, 30]
        self.assertFalse(unlocks_hold(far), "a vault no lock or plate can reach must be caught")
        shared = copy.deepcopy(SPECS["survey_lab_level_6_a"])
        shared["fills"].append({"from": [17, 5, 10], "to": [17, 5, 10], "block": {
            "id": "neroruins:card_reader", "properties": {"facing": "north", "tier": "1", "accepted": "false"}}})
        self.assertFalse(unlocks_hold(shared), "a lock that also reaches the sanctum door must be caught")

    def test_every_gate_lies_inside_a_guardian_zone(self):
        for name, spec in levels().items():
            with self.subTest(name=name):
                self.assertTrue(guardian_zone_holds(spec))
        unguarded = copy.deepcopy(SPECS["survey_lab_level_4_b"])
        unguarded["spawners"] = []
        self.assertFalse(guardian_zone_holds(unguarded))

    def test_each_keycard_gate_has_its_card_higher_up(self):
        for surface in ("survey_lab_surfaces", "survey_lab_complex_surfaces", "survey_lab_outpost_surfaces"):
            with self.subTest(surface=surface):
                self.assertTrue(keycards_obtainable("neroruins:" + surface))
        saved = copy.deepcopy(SPECS["survey_lab_level_1_b"])
        try:
            SPECS["survey_lab_level_1_b"]["containers"] = [
                c for c in saved["containers"] if "_keycard_" not in c["loot_table"]]
            self.assertFalse(keycards_obtainable("neroruins:survey_lab_surfaces"),
                             "one variant without the card strands the level below")
        finally:
            SPECS["survey_lab_level_1_b"] = saved

    def test_chapter_four_logs_appear_once_in_bible_order_only_in_the_citadel(self):
        self.assertEqual(["neroruins:mars.%d" % i for i in range(1, 7)], logs_in_order("neroruins:survey_lab_surfaces"))
        self.assertEqual([], logs_in_order("neroruins:survey_lab_complex_surfaces"))
        self.assertEqual([], logs_in_order("neroruins:survey_lab_outpost_surfaces"))

    def test_spines_reach_full_depth_within_budget(self):
        expected_levels = {"survey_lab": 6, "survey_lab_complex": 3, "survey_lab_outpost": 1}
        for structure, (depth_cap, piece_cap) in BUDGETS.items():
            with self.subTest(structure=structure):
                (deepest, pieces), limit = budget(structure)
                self.assertLessEqual(limit, depth_cap)
                self.assertLessEqual(pieces, piece_cap)
                self.assertEqual(limit, deepest, "the jigsaw size must reach the deepest room, no further")
                start = json_file("worldgen", "structure", structure + ".json")["layouts"][0]["layout"]["start_pool"]
                first = {c["pool"] for t in pool_templates(start) for c in SPECS[t]["connectors"]}
                self.assertEqual(expected_levels[structure], len(chain(next(iter(first)))))
                self.assertLessEqual(expected_levels[structure], limit - 2, "every level keeps its rooms")

    def test_pools_have_variants_and_dead_end_caps(self):
        names = [p.stem for p in (DATA / "worldgen" / "template_pool").glob("survey_lab*.json")]
        self.assertTrue(names)
        for name in names:
            document = pool("neroruins:" + name)
            templates = pool_templates("neroruins:" + name)
            with self.subTest(pool=name):
                self.assertTrue(all(t in SPECS for t in templates))
                if "_level" in name or "surfaces" in name or name.endswith("_rooms"):
                    self.assertGreaterEqual(len(templates), 3)
                if name.endswith("corridors") or "satellite" in name:
                    self.assertGreaterEqual(len(templates), 2)
                outgoing = {c["pool"] for t in templates for c in SPECS[t]["connectors"]} - {"minecraft:empty"}
                for child in outgoing:
                    child_pool = pool(child)
                    if "surfaces" not in name:
                        self.assertNotEqual("minecraft:empty", child_pool["fallback"], child)
                        caps = pool_templates(child_pool["fallback"])
                        self.assertTrue(all(not any(c["pool"] != "minecraft:empty" for c in SPECS[t]["connectors"])
                                            for t in caps), "caps end the branch")
                if "caps" in name:
                    self.assertEqual("minecraft:empty", document["fallback"])

    def test_connectors_pair_up(self):
        for name, spec in SPECS.items():
            for connector in spec["connectors"]:
                if connector["pool"] == "minecraft:empty":
                    continue
                with self.subTest(template=name, pos=connector["pos"]):
                    vertical = connector["orientation"].startswith("down")
                    for child in pool_templates(connector["pool"]):
                        names = [c["name"] for c in SPECS[child]["connectors"]
                                 if c["orientation"].startswith("up") == vertical]
                        self.assertIn(connector["target"], names)

    def test_conduit_levels_are_solvable_as_authored(self):
        from test_build_templates import conduit_reaches_lock
        conduit_levels = [n for n, spec in levels().items() if at(blocks_of(spec), "neroruins:conduit_lock")]
        self.assertEqual(6, len(conduit_levels))
        for name in conduit_levels:
            with self.subTest(name=name):
                self.assertTrue(conduit_reaches_lock(blocks_of(SPECS[name])))
        turned = blocks_of(SPECS[conduit_levels[0]])
        source = at(turned, "neroruins:conduit_source")[0]
        first = (source[0] + 1, source[1], source[2])
        turned[first] = {"id": "neroruins:conduit", "properties": dict(props(turned[first]), facing="north")}
        self.assertFalse(conduit_reaches_lock(turned))

    def test_ladder_spine_is_continuous_across_levels(self):
        for name, spec in {**levels(), **surfaces()}.items():
            blocks = blocks_of(spec)
            with self.subTest(name=name):
                for connector in spec["connectors"]:
                    if connector["orientation"][:2] in ("up", "do"):
                        self.assertEqual([16, 16], [connector["pos"][0], connector["pos"][2]])
                        self.assertEqual("minecraft:ladder", block_id(connector["final_state"]))
                        x, y, z = connector["pos"]
                        for nx, nz in ((x - 1, z), (x + 1, z), (x, z - 1), (x, z + 1)):
                            self.assertNotIn(block_id(blocks.get((nx, y, nz))), PASSABLE,
                                             "a rotated ladder still needs a wall on every side")
                if name in levels():
                    self.assertTrue(all(block_id(blocks[(16, y, 16)]) == "minecraft:ladder" for y in range(8, 13)))

    def test_shallowest_rooms_stay_covered_on_the_steepest_allowed_slope(self):
        for structure in BUDGETS:
            document = json_file("worldgen", "structure", structure + ".json")
            start = document["layouts"][0]["layout"]["start_height"]["absolute"]
            drop = document["ground_check"]["max_height_delta"]
            with self.subTest(structure=structure):
                # The first level's roof sits one block below the surface piece's origin; the ground block at the
                # centre is one below the heightmap, and the check allows the ground to fall by `drop` from there.
                self.assertLess(start - 1, -1 - drop)
        self.assertFalse(-7 - 1 < -1 - 7, "a looser check would expose the first floor's roof")

    def test_sets_spacing_salts_and_satellites(self):
        sets = {p.stem: json.loads(p.read_text(encoding="utf-8"))
                for p in (DATA / "worldgen" / "structure_set").glob("*.json")}
        salts = [s["placement"]["salt"] for s in sets.values() if "salt" in s["placement"]]
        self.assertEqual(len(salts), len(set(salts)), "every structure set needs its own salt")
        spacing = {k: sets[k]["placement"]["spacing"] for k in ("survey_lab", "survey_lab_complex", "survey_lab_outpost")}
        self.assertGreater(spacing["survey_lab"], spacing["survey_lab_complex"])
        self.assertGreater(spacing["survey_lab_complex"], spacing["survey_lab_outpost"])
        self.assertGreaterEqual(spacing["survey_lab_outpost"], 16)
        satellites = [k for k, s in sets.items() if k.startswith("survey_lab")
                      and s["placement"]["type"] == "neroruins:satellite"]
        self.assertEqual(6, len(satellites))
        for name in satellites:
            placement = sets[name]["placement"]
            with self.subTest(satellite=name):
                self.assertIn(placement["hub_set"].split(":")[1], sets)
                structure = json_file("worldgen", "structure", name + ".json")
                self.assertEqual(placement, dict(structure["placement"], type="neroruins:satellite"))
                self.assertTrue(pool_templates(structure["layout"]["start_pool"]))
                hub = json_file("worldgen", "structure", placement["hub_set"].split(":")[1] + ".json")
                self.assertEqual(hub["biomes"], structure["biomes"])
        by_hub = {}
        for name in satellites:
            by_hub.setdefault(sets[name]["placement"]["hub_set"], []).append(sets[name]["placement"]["index"])
        for hub, indices in by_hub.items():
            self.assertEqual(sorted(indices), list(range(len(indices))), hub)
        self.assertTrue(2 <= len(by_hub["neroruins:survey_lab"]) <= 4)

    def test_family_loot_and_spawns_exist(self):
        tables = set()
        for spec in SPECS.values():
            tables |= {c["loot_table"] for c in spec.get("containers", []) + spec.get("vaults", [])}
            for spawner in spec.get("spawners", []):
                self.assertEqual("neroruins:survey_lab", spawner["table"])
        for table in tables:
            with self.subTest(table=table):
                self.assertTrue((DATA / "loot_table" / (table.split(":")[1] + ".json")).exists())
        entries = json_file("spawn_tables", "survey_lab.json")["entries"]
        self.assertEqual("nerocreatures:rogue_android", entries[0]["entity"], "Rogue Androids come first")
        self.assertTrue(all(e.get("required", True) is False for e in entries if not e["entity"].startswith(
            ("neroruins:", "minecraft:"))), "sibling mobs are optional")
        for tier in (1, 2, 3):
            self.assertTrue(any(e["entity"].startswith(("neroruins:", "minecraft:"))
                                and e["min_tier"] <= tier <= e["max_tier"] for e in entries))


if __name__ == "__main__":
    unittest.main()
