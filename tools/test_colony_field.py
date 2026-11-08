import copy
import json
import math
import unittest
from collections import deque

from build_templates import build_template, DESTINATION, SPEC_DIRECTORY, ROOT
from test_build_templates import room_blocks, block_id, props, conduit_reaches_lock, HORIZONTAL_REACH, VERTICAL_REACH

DATA = ROOT / "common" / "src" / "main" / "resources" / "data" / "neroruins"
DATA_263 = ROOT / "common" / "src" / "main" / "resources-26.3" / "data" / "neroruins"
WORLDGEN = DATA / "worldgen"
CLASSES = ("citadel", "complex", "outpost")
# Hub piece caps and depth caps from decision 002: the citadel's, then about 55% and 25% of it.
PIECE_CAP = {"citadel": 100, "complex": 55, "outpost": 25}
DEPTH_CAP = {"citadel": 6, "complex": 4, "outpost": 2}
CHAPTER = {"citadel": "glacira", "complex": "greenxertz"}
WALK = ("minecraft:air", "neroruins:conduit", "neroruins:ballast_plate")
LOCKS = ("neroruins:conduit_lock", "neroruins:relic_socket", "neroruins:card_reader", "neroruins:glyph_lock")
SPAWNER_RANGE = 8
PLATE_REACH, PLATE_HEIGHT = 6, 3


def specs():
    return {spec["name"]: spec for spec in json.loads(
        (SPEC_DIRECTORY / "colony_field.json").read_text(encoding="utf-8"))["templates"]}


def pools():
    return {path.stem: json.loads(path.read_text(encoding="utf-8"))
            for path in (WORLDGEN / "template_pool").glob("colony_field*.json")}


def strip(resource):
    return resource.split(":", 1)[1]


def hubs(size_class):
    return [spec for name, spec in specs().items() if name.startswith("colony_field_%s_hub_" % size_class)]


def blocks_of(spec):
    blocks = room_blocks(spec)
    for terminal in spec.get("terminals", []):
        blocks[tuple(terminal["pos"])] = "neroruins:log_terminal"
    for spawner in spec.get("spawners", []):
        blocks[tuple(spawner["pos"])] = "neroruins:ruin_spawner"
    for connector in spec.get("connectors", []):
        blocks[tuple(connector["pos"])] = connector["final_state"]
    return blocks


def walk(spec, doors_open, blocks=None):
    """Cells at walking height a player can reach from the first connector, with sealed doors shut or open."""
    blocks = blocks if blocks is not None else blocks_of(spec)

    def passable(pos):
        block = blocks.get(pos)
        if block_id(block) == "neroruins:sealed_door":
            return doors_open
        return block_id(block) in WALK
    start = tuple(spec["connectors"][0]["pos"])
    seen = {start: 0}
    queue = deque([start])
    while queue:
        x, y, z = queue.popleft()
        for pos in ((x - 1, y, z), (x + 1, y, z), (x, y, z - 1), (x, y, z + 1)):
            if pos not in seen and passable(pos) and passable((pos[0], y + 1, pos[2])):
                seen[pos] = seen[(x, y, z)] + 1
                queue.append(pos)
    return seen


def usable(cells, pos):
    x, _, z = pos
    return any((nx, 1, nz) in cells for nx, nz in ((x - 1, z), (x + 1, z), (x, z - 1), (x, z + 1)))


def fixtures(spec, kinds):
    return [pos for pos, block in blocks_of(spec).items() if block_id(block) in kinds]


def guarded(spec, positions):
    """Every position lies within waking range of one of the template's Ruin Spawners."""
    spawners = [tuple(s["pos"]) for s in spec.get("spawners", [])]
    return bool(spawners) and all(any(math.dist(pos, s) <= SPAWNER_RANGE for s in spawners) for pos in positions)


def gate_positions(spec):
    """The outer gate: locks and plates usable before any door opens, and the vaults they answer for. A lock that
    is itself behind that gate (the citadel's sanctum socket) is guarded by the gate in front of it."""
    shut = walk(spec, False)
    locks = [lock for lock in fixtures(spec, LOCKS) if usable(shut, lock)]
    plates = fixtures(spec, ("neroruins:ballast_plate",))
    vaults = [tuple(v["pos"]) for v in spec.get("vaults", [])
              if any(in_reach(lock, v["pos"]) for lock in locks) or (plates and usable(shut, v["pos"]))]
    return locks + plates + vaults


def vault_needs_gate(spec):
    """With every sealed door shut, no vault can be used; opening them makes every vault usable."""
    shut, opened = walk(spec, False), walk(spec, True)
    vaults = [tuple(v["pos"]) for v in spec.get("vaults", [])]
    return bool(vaults) and not any(usable(shut, v) for v in vaults) and all(usable(opened, v) for v in vaults)


def in_reach(lock, pos):
    return (abs(lock[0] - pos[0]) <= HORIZONTAL_REACH and abs(lock[2] - pos[2]) <= HORIZONTAL_REACH
            and abs(lock[1] - pos[1]) <= VERTICAL_REACH)


def logs(spec):
    return [t["log_id"] for t in spec.get("terminals", [])]


def chapter_complete(spec, chapter):
    found = logs(spec)
    return sorted(found) == ["neroruins:%s.%d" % (chapter, n) for n in range(1, 7)]


def exits(spec):
    out = {}
    for connector in spec.get("connectors", []):
        if connector["pool"] != "minecraft:empty":
            out[strip(connector["pool"])] = out.get(strip(connector["pool"]), 0) + 1
    return out


def worst_case_pieces(all_specs, all_pools, start_pool, depth):
    """An upper bound on placed pieces: every connector filled by the element of its pool with the most exits."""
    def widest(pool_name):
        best = {}
        for entry in all_pools[pool_name]["elements"]:
            for target, count in exits(all_specs[strip(entry["element"]["location"])]).items():
                best[target] = max(best.get(target, 0), count)
        return best
    total, frontier = 1, {}
    for hub in all_pools[start_pool]["elements"]:
        for target, count in exits(all_specs[strip(hub["element"]["location"])]).items():
            frontier[target] = max(frontier.get(target, 0), count)
    for _ in range(depth):
        total += sum(frontier.values())
        following = {}
        for pool_name, count in frontier.items():
            for target, width in widest(pool_name).items():
                following[target] = following.get(target, 0) + count * width
        frontier = following
    return total


class ColonyFieldTemplateTest(unittest.TestCase):
    def test_every_template_is_checked_in_and_used_by_a_pool(self):
        used = {strip(e["element"]["location"]) for p in pools().values() for e in p["elements"]}
        for name, spec in specs().items():
            with self.subTest(template=name):
                self.assertEqual((DESTINATION / (name + ".nbt")).read_bytes(), build_template(spec))
                self.assertIn(name, used, "an unused template is an orphan")
        for pool_name, pool in pools().items():
            for entry in pool["elements"]:
                self.assertIn(strip(entry["element"]["location"]), specs(), pool_name)

    def test_every_open_connector_has_a_pool_with_a_dead_end_cap(self):
        all_pools, all_specs = pools(), specs()
        targets = {connector["pool"] for spec in all_specs.values() for connector in spec.get("connectors", [])}
        for target in targets - {"minecraft:empty"}:
            with self.subTest(pool=target):
                self.assertIn(strip(target), all_pools)
                self.assertEqual("neroruins:colony_field_caps", all_pools[strip(target)]["fallback"])
        for entry in all_pools["colony_field_caps"]["elements"]:
            cap = all_specs[strip(entry["element"]["location"])]
            self.assertEqual({}, exits(cap), "a cap must end its branch")
            self.assertEqual(1, len(cap["connectors"]))

    def test_piece_budget_holds_for_the_widest_possible_growth(self):
        all_pools, all_specs = pools(), specs()
        for size_class in CLASSES:
            structure = json.loads((WORLDGEN / "structure" / ("colony_field_%s.json" % size_class))
                                   .read_text(encoding="utf-8"))
            depth = max(layout["layout"]["size"] for layout in structure["layouts"])
            with self.subTest(size_class=size_class):
                self.assertLessEqual(depth, DEPTH_CAP[size_class])
                pieces = worst_case_pieces(all_specs, all_pools, "colony_field_%s_hubs" % size_class, depth)
                self.assertLessEqual(pieces, PIECE_CAP[size_class])
                for layout in structure["layouts"]:
                    self.assertLessEqual(layout["layout"]["max_distance_from_center"], 128)
        forking = copy.deepcopy(all_pools)
        forking["colony_field_lots"]["elements"].append(
            {"weight": 1, "element": {"location": "neroruins:colony_field_path_fork"}})
        self.assertGreater(worst_case_pieces(all_specs, forking, "colony_field_citadel_hubs", 6), PIECE_CAP["citadel"],
                           "a forking piece among the lots would let the citadel outgrow its cap")

    def test_sprawl_pieces_connect_every_exit(self):
        for name, spec in specs().items():
            if "_hub_" in name or "connectors" not in spec:
                continue
            cells = walk(spec, False)
            with self.subTest(template=name):
                for connector in spec["connectors"]:
                    self.assertIn(tuple(connector["pos"]), cells)
                for container in spec.get("containers", []):
                    self.assertTrue(usable(cells, container["pos"]), "chest at %s" % container["pos"])

    def test_hubs_reach_every_room_exit_and_chest_without_the_gate(self):
        for size_class in CLASSES:
            for spec in hubs(size_class):
                cells = walk(spec, False)
                with self.subTest(hub=spec["name"]):
                    for connector in spec["connectors"]:
                        self.assertIn(tuple(connector["pos"]), cells, "exit %s" % connector["pos"])
                    for container in spec.get("containers", []):
                        self.assertTrue(usable(cells, container["pos"]), "chest at %s" % container["pos"])
                    opened = walk(spec, True)
                    for lock in fixtures(spec, LOCKS):
                        self.assertTrue(usable(opened, lock), "lock at %s" % (lock,))
                    self.assertTrue(any(usable(cells, lock) for lock in fixtures(spec, LOCKS))
                                    or fixtures(spec, ("neroruins:ballast_plate",)), "the gate faces the approach")

    def test_no_vault_is_reachable_without_passing_its_gate(self):
        for size_class in ("citadel", "outpost"):
            for spec in hubs(size_class):
                with self.subTest(hub=spec["name"]):
                    self.assertTrue(vault_needs_gate(spec))
                    opened = copy.deepcopy(spec)
                    for fill in opened["fills"]:
                        if block_id(fill["block"]) == "neroruins:sealed_door":
                            fill["block"] = "minecraft:air"
                    self.assertFalse(vault_needs_gate(opened), "a missing door must expose the vault")
        for spec in hubs("complex"):
            with self.subTest(hub=spec["name"]):
                vault = tuple(spec["vaults"][0]["pos"])
                plates = [p for p in fixtures(spec, ("neroruins:ballast_plate",))
                          if abs(p[0] - vault[0]) <= PLATE_REACH and abs(p[2] - vault[2]) <= PLATE_REACH
                          and abs(p[1] - vault[1]) <= PLATE_HEIGHT]
                self.assertTrue(3 <= len(plates) <= 8, "the vault stays sealed until its plates are loaded")

    def test_every_gate_lies_inside_a_guardian_zone(self):
        for size_class in CLASSES:
            for spec in hubs(size_class):
                with self.subTest(hub=spec["name"]):
                    self.assertTrue(all(s["tier"] == 1 for s in spec["spawners"]), "colony guardians are light")
                    self.assertTrue(guarded(spec, gate_positions(spec)))
        moved = copy.deepcopy(hubs("citadel")[0])
        moved["spawners"][0]["pos"] = [2, 0, 45]
        self.assertFalse(guarded(moved, gate_positions(moved)), "a spawner far from the gate guards nothing")

    def test_each_lock_opens_only_its_own_door_and_vault(self):
        for size_class in ("citadel", "outpost"):
            for spec in hubs(size_class):
                locks = fixtures(spec, LOCKS)
                targets = fixtures(spec, ("neroruins:sealed_door",)) + [tuple(v["pos"]) for v in spec["vaults"]]
                with self.subTest(hub=spec["name"]):
                    for target in targets:
                        self.assertEqual(1, sum(in_reach(lock, target) for lock in locks),
                                         "%s must answer to exactly one lock" % (target,))

    def test_citadel_conduit_gate_is_authored_solved_in_every_variant(self):
        for spec in hubs("citadel"):
            with self.subTest(hub=spec["name"]):
                self.assertTrue(conduit_reaches_lock(room_blocks(spec)))
        broken = copy.deepcopy(hubs("citadel")[0])
        for fill in broken["fills"]:
            if block_id(fill["block"]) == "neroruins:conduit" and fill["from"] == [20, 1, 7]:
                fill["block"]["properties"]["facing"] = "east"
        self.assertFalse(conduit_reaches_lock(room_blocks(broken)))

    def test_sanctum_socket_wants_the_frozen_reach_relic_and_outposts_a_survey_card(self):
        for spec in hubs("citadel"):
            sockets = [block for block in blocks_of(spec).values() if block_id(block) == "neroruins:relic_socket"]
            self.assertEqual(["6"], [props(s)["chapter"] for s in sockets])
        citadel_vault = json.loads((DATA / "loot_table" / "chests" / "colony_field_citadel_vault.json")
                                   .read_text(encoding="utf-8"))
        self.assertIn("neroruins:reach_thermal_core", json.dumps(citadel_vault),
                      "the council vault yields the relic its own reliquary asks for")
        for spec in hubs("outpost"):
            readers = [block for block in blocks_of(spec).values() if block_id(block) == "neroruins:card_reader"]
            self.assertEqual(["1"], [props(r)["tier"] for r in readers])
            self.assertIn("neroruins:chests/colony_field_keycard", [c["loot_table"] for c in spec["containers"]])

    def test_each_chapter_is_placed_once_in_bible_order(self):
        for size_class, chapter in CHAPTER.items():
            for spec in hubs(size_class):
                shut, opened = walk(spec, False), walk(spec, True)
                terminals = {t["log_id"]: t for t in spec["terminals"]}

                def front(log):
                    t = terminals["neroruins:%s.%d" % (chapter, log)]
                    step = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[t["facing"]]
                    return t["pos"][0] + step[0], t["pos"][1], t["pos"][2] + step[1]
                with self.subTest(hub=spec["name"]):
                    self.assertTrue(chapter_complete(spec, chapter))
                    for log in range(1, 6):
                        self.assertIn(front(log), shut, "log %d is read before the gate" % log)
                    self.assertEqual(1, min(range(1, 7), key=lambda n: opened[front(n)]),
                                     "the landing pad log is the first one met")
                    self.assertTrue(guarded(spec, [front(5)]), "the gate log lies in the guardian zone")
                    if size_class == "complex":
                        self.assertTrue(guarded(spec, [front(6)]), "the vault log lies in the guardian zone")
                    else:
                        self.assertNotIn(front(6), shut, "the revelation waits behind the gate")
                        self.assertEqual(6, max(range(1, 7), key=lambda n: opened[front(n)]))
        doubled = copy.deepcopy(hubs("citadel")[0])
        doubled["terminals"][1]["log_id"] = doubled["terminals"][0]["log_id"]
        self.assertFalse(chapter_complete(doubled, "glacira"))
        for name, spec in specs().items():
            if not name.startswith(("colony_field_citadel_hub_", "colony_field_complex_hub_")):
                self.assertEqual([], logs(spec), "%s must not repeat a unique log" % name)

    def test_terminal_screens_face_open_floor(self):
        for name, spec in specs().items():
            blocks = blocks_of(spec)
            for t in spec.get("terminals", []):
                step = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[t["facing"]]
                with self.subTest(template=name, log=t["log_id"]):
                    self.assertEqual("minecraft:air", block_id(blocks.get(
                        (t["pos"][0] + step[0], t["pos"][1], t["pos"][2] + step[1]))))

    def test_every_role_has_variants(self):
        names = specs()
        for size_class in CLASSES:
            self.assertEqual(3, len(hubs(size_class)))
        for role in ("house", "farm", "store"):
            self.assertEqual(3, sum(n.startswith("colony_field_%s_" % role) for n in names), role)
        self.assertGreaterEqual(sum(n.startswith("colony_field_path_straight_") for n in names), 2)
        self.assertEqual(2, sum(n.startswith("colony_field_path_turn_") for n in names))
        for role in ("habring", "fields", "silo", "cemetery", "wreck"):
            self.assertEqual(2, sum(n.startswith("colony_field_%s_" % role) for n in names), role)

    def test_cemeteries_have_markers(self):
        for name, spec in specs().items():
            if not name.startswith("colony_field_cemetery_"):
                continue
            blocks = blocks_of(spec)
            markers = [p for p, b in blocks.items() if b == "minecraft:andesite_wall"
                       and blocks.get((p[0], p[1] + 1, p[2])) == "minecraft:stone_brick_slab"]
            self.assertGreaterEqual(len(markers), 8, name)


class ColonyFieldWorldgenTest(unittest.TestCase):
    def test_satellites_follow_their_hub_and_fill_the_footprint(self):
        expected = {"citadel": (10, 16), "complex": (5, 9), "outpost": (1, 4)}
        for size_class in CLASSES:
            hub_set = json.loads((WORLDGEN / "structure_set" / ("colony_field_%s.json" % size_class))
                                 .read_text(encoding="utf-8"))
            slots = []
            for path in (WORLDGEN / "structure").glob("colony_field_%s_*.json" % size_class):
                structure = json.loads(path.read_text(encoding="utf-8"))
                placement = structure["placement"]
                the_set = json.loads((WORLDGEN / "structure_set" / path.name).read_text(encoding="utf-8"))
                with self.subTest(satellite=path.stem):
                    self.assertEqual("neroruins:colony_field_" + size_class, placement["hub_set"])
                    self.assertEqual(dict(placement, type="neroruins:satellite"), the_set["placement"])
                    self.assertLess(placement["max_radius"], hub_set["placement"]["spacing"])
                    self.assertTrue(0 < placement["chance_percent"] < 100)
                    pool = pools()[strip(structure["layout"]["start_pool"])]
                    self.assertGreaterEqual(len(pool["elements"]), 2)
                slots.append(placement)
            with self.subTest(size_class=size_class):
                self.assertEqual(len(slots), len({s["index"] for s in slots}), "slots must be distinct")
                low, high = expected[size_class]
                mean = sum(s["chance_percent"] for s in slots) / 100
                self.assertTrue(low <= mean <= high, "expected satellites %.1f" % mean)
        citadel = [json.loads(p.read_text(encoding="utf-8"))["placement"]
                   for p in (WORLDGEN / "structure").glob("colony_field_citadel_*.json")]
        self.assertGreaterEqual(max(s["max_radius"] for s in citadel) * 16, 200,
                                "citadel satellites carry the footprint towards 450 blocks across")

    def test_structure_set_salts_are_unique(self):
        salts = {}
        for path in (WORLDGEN / "structure_set").glob("*.json"):
            placement = json.loads(path.read_text(encoding="utf-8"))["placement"]
            if "salt" in placement:
                self.assertNotIn(placement["salt"], salts, "%s reuses %s's salt" % (path.stem, salts.get(placement["salt"])))
                salts[placement["salt"]] = path.stem

    def test_biome_tags_keep_planet_entries_optional(self):
        for path in (DATA / "tags" / "worldgen" / "biome" / "has_structure").glob("colony_field_*.json"):
            for value in json.loads(path.read_text(encoding="utf-8"))["values"]:
                self.assertFalse(value["required"], path.stem)

    def test_decay_processors_seed_puzzles_in_both_formats(self):
        for directory in (DATA, DATA_263):
            for path in (directory / "worldgen" / "processor_list").glob("colony_field_*.json"):
                processors = json.loads(path.read_text(encoding="utf-8"))["processors"]
                with self.subTest(processor=str(path.relative_to(ROOT))):
                    self.assertEqual("neroruins:puzzle_seed", processors[-1]["processor_type"])
                    for rule in processors[0]["rules"]:
                        output = rule["output_state"]
                        self.assertIsInstance(output, str if directory == DATA_263 else dict)


if __name__ == "__main__":
    unittest.main()
