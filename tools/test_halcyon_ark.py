"""Halcyon Ark family checks (decision 035).

The spine is assembled end to end exactly as jigsaw joins it: each room's exit connector sits against the
next room's entrance connector, one block further along +Z. All variants of a role share their size and
connector positions, so the assembled corridor is the same shape for every mix of variants; the tests walk
every all-same-variant chain plus a fixed set of seeded mixes.
"""
import copy
import json
import random
import unittest
from collections import deque

import gated_enclosure
import halcyon_ark_spec as ark
from build_templates import build_template, ROOT, SPEC_DIRECTORY

DATA = ROOT / "common" / "src" / "main" / "resources" / "data" / "neroruins"
PASSABLE = ("minecraft:air", "neroruins:conduit")
LOCKS = ("neroruins:conduit_lock", "neroruins:card_reader", "neroruins:relic_socket", "neroruins:glyph_lock")
SPAWNER_RANGE = 8  # GuardianRules.SPAWNER_RANGE
REACH_XZ, REACH_Y = 8, 4  # PuzzleUnlock's box
STEP = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
CLOCKWISE = {"north": "east", "east": "south", "south": "west", "west": "north"}
OPPOSITE = {"north": "south", "south": "north", "east": "west", "west": "east"}


def specs():
    return {spec["name"]: spec for spec in json.loads(
        (SPEC_DIRECTORY / "halcyon_ark.json").read_text(encoding="utf-8"))["templates"]}


def block_id(block):
    return block["id"] if isinstance(block, dict) else block


def props(block):
    return block.get("properties", {}) if isinstance(block, dict) else {}


def blocks_of(spec, dz=0):
    """Every block a piece places, keyed by position, with its fixtures tagged."""
    out = {}
    for fill in spec["fills"]:
        (x0, y0, z0), (x1, y1, z1) = fill["from"], fill["to"]
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    out[(x, y, z + dz)] = fill["block"]
    for connector in spec["connectors"]:
        x, y, z = connector["pos"]
        out[(x, y, z + dz)] = connector["final_state"]
    for kind, block in (("containers", "minecraft:chest"), ("vaults", "neroruins:vault_cache"),
                        ("spawners", "neroruins:ruin_spawner"), ("terminals", "neroruins:log_terminal")):
        for entry in spec.get(kind, []):
            x, y, z = entry["pos"]
            out[(x, y, z + dz)] = block
    return out


def chain_names(prefix, chain, variants):
    return ["%s_%s_%d" % (prefix, role, v) for role, v in zip(chain, variants)]


def assemble(names, pieces=None):
    pieces = pieces or specs()
    blocks, fixtures, dz = {}, {"terminals": [], "containers": [], "vaults": [], "spawners": []}, 0
    for name in names:
        spec = pieces[name]
        blocks.update(blocks_of(spec, dz))
        for kind in fixtures:
            for entry in spec.get(kind, []):
                moved = dict(entry)
                moved["pos"] = [entry["pos"][0], entry["pos"][1], entry["pos"][2] + dz]
                moved["piece"] = name
                fixtures[kind].append(moved)
        dz += spec["size"][2]
    return blocks, fixtures


def walk(blocks, start, open_doors, blocked=frozenset()):
    def passable(pos):
        block = blocks.get(pos)
        if block_id(block) == "neroruins:sealed_door":
            return block_id(block) in open_doors or "all" in open_doors
        return block_id(block) in PASSABLE and pos not in blocked

    seen = {start}
    queue = deque([start])
    while queue:
        x, y, z = queue.popleft()
        for pos in ((x - 1, y, z), (x + 1, y, z), (x, y, z - 1), (x, y, z + 1)):
            if pos not in seen and passable(pos) and passable((pos[0], y + 1, pos[2])):
                seen.add(pos)
                queue.append(pos)
    return seen


def doors(blocks):
    return sorted({(x, z) for (x, y, z), b in blocks.items() if block_id(b) == "neroruins:sealed_door"},
                  key=lambda p: p[1])


def walk_with(blocks, start, opened_door_zs, blocked=frozenset()):
    """Walk with only the sealed doors at the given Z rows opened."""
    trial = dict(blocks)
    for (x, y, z), b in blocks.items():
        if block_id(b) == "neroruins:sealed_door" and z in opened_door_zs:
            trial[(x, y, z)] = "minecraft:air"
    return walk(trial, start, (), blocked)


def visible(cells, pos):
    x, y, z = pos
    return any((nx, stand, nz) in cells for stand in range(y - 3, y + 1)
               for nx, nz in ((x - 1, z), (x + 1, z), (x, z - 1), (x, z + 1)))


def start_of(prefix):
    return (ark.CX, 1, 1) if prefix == "halcyon_ark" else (ark.CX, 1, 0)


def gate_order_holds(blocks, fixtures, start):
    """Each gate hides everything beyond it, and the keycard is found between the two gates."""
    gates = [z for _, z in doors(blocks)]
    if len(gates) < 2:
        return False
    conduit_gate, keycard_gate = gates[0], gates[1]
    closed = walk_with(blocks, start, set())
    after_conduit = walk_with(blocks, start, {conduit_gate})
    vaults = [tuple(v["pos"]) for v in fixtures["vaults"]]
    keycards = [tuple(c["pos"]) for c in fixtures["containers"] if c["loot_table"].endswith("halcyon_ark_medbay")]
    if any(visible(closed, v) for v in vaults) or any(visible(closed, k) for k in keycards):
        return False
    if not keycards or not all(visible(after_conduit, k) for k in keycards):
        return False
    if any(visible(after_conduit, v) for v in vaults):
        return False
    everything = walk_with(blocks, start, set(gates))
    return all(visible(everything, v) for v in vaults)


def guarded(blocks, fixtures, start):
    """No lock can be reached without entering some spawner's activation sphere."""
    spawners = [tuple(s["pos"]) for s in fixtures["spawners"]]
    if not spawners:
        return False
    in_range = {pos for pos in blocks if pos[1] == 1 and any(
        sum((a - b) ** 2 for a, b in zip(pos, s)) <= SPAWNER_RANGE ** 2 for s in spawners)}
    if start in in_range:
        return False
    cells = walk_with(blocks, start, {z for _, z in doors(blocks)}, frozenset(in_range))
    locks = [pos for pos, b in blocks.items() if block_id(b) in LOCKS]
    return bool(locks) and not any(visible(cells, lock) for lock in locks)


def log_order(fixtures):
    return [int(t["log_id"].rsplit(".", 1)[1]) for t in sorted(fixtures["terminals"], key=lambda t: t["pos"][2])]


def unlock_targets(blocks):
    """What each lock would open through PuzzleUnlock's box."""
    targets = [pos for pos, b in blocks.items()
               if block_id(b) == "neroruins:vault_cache" or block_id(b) == "neroruins:sealed_door"]
    out = {}
    for pos, b in blocks.items():
        if block_id(b) in LOCKS:
            out[pos] = sorted(t for t in targets if abs(t[0] - pos[0]) <= REACH_XZ and abs(t[2] - pos[2]) <= REACH_XZ
                              and abs(t[1] - pos[1]) <= REACH_Y)
    return out


def conduit_solved(blocks):
    def openings(block):
        facing = props(block)["facing"]
        return {facing, CLOCKWISE[facing]} if props(block)["corner"] == "true" else {facing, OPPOSITE[facing]}
    for (x, y, z), source in blocks.items():
        if block_id(source) != "neroruins:conduit_source":
            continue
        out = props(source)["facing"]
        at = (x + STEP[out][0], y, z + STEP[out][1])
        for _ in range(64):
            block = blocks.get(at)
            if block_id(block) == "neroruins:conduit_lock":
                return True
            if block_id(block) != "neroruins:conduit" or OPPOSITE[out] not in openings(block):
                break
            out = next(iter(openings(block) - {OPPOSITE[out]}))
            at = (at[0] + STEP[out][0], at[1], at[2] + STEP[out][1])
    return False


def chains():
    rng = random.Random(35)
    out = []
    for prefix, chain in (("halcyon_ark", ark.CITADEL_CHAIN), ("halcyon_ark_luna", ark.LUNA_CHAIN)):
        mixes = [[v] * len(chain) for v in range(3)] + [[rng.randrange(3) for _ in chain] for _ in range(12)]
        out += [(prefix, chain, mix) for mix in mixes]
    return out


class GeneratedFilesTest(unittest.TestCase):
    def test_checked_in_files_match_the_authoring_script(self):
        self.assertEqual({"templates": ark.templates()},
                         json.loads((SPEC_DIRECTORY / "halcyon_ark.json").read_text(encoding="utf-8")))
        worldgen = DATA / "worldgen"
        expected = [(worldgen / "template_pool", ark.pools()), (worldgen / "structure", ark.structures()),
                    (worldgen / "structure_set", ark.structure_sets(ark.structures())),
                    (DATA / "loot_table" / "chests", ark.loot_tables())]
        for directory, files in expected:
            for name, data in files.items():
                with self.subTest(file=name):
                    self.assertEqual(data, json.loads((directory / (name + ".json")).read_text(encoding="utf-8")))

    def test_every_template_encodes(self):
        for spec in specs().values():
            build_template(spec)

    def test_variants_of_a_role_share_their_shape(self):
        pieces = specs()
        for prefix, chain in (("halcyon_ark", ark.CITADEL_CHAIN), ("halcyon_ark_luna", ark.LUNA_CHAIN)):
            for role in chain:
                shapes = {json.dumps([pieces["%s_%s_%d" % (prefix, role, v)]["size"],
                                      [(c["pos"], c["orientation"], c["name"], c["target"], c["pool"])
                                       for c in pieces["%s_%s_%d" % (prefix, role, v)]["connectors"]]])
                          for v in range(3)}
                with self.subTest(role=prefix + "_" + role):
                    self.assertEqual(1, len(shapes))


class PoolGraphTest(unittest.TestCase):
    def pool_files(self):
        return {path.stem: json.loads(path.read_text(encoding="utf-8"))
                for path in (DATA / "worldgen" / "template_pool").glob("halcyon_ark*.json")}

    def test_every_connector_has_a_partner_and_every_pool_is_used(self):
        pools, pieces = self.pool_files(), specs()

        def members(pool):
            return [e["element"]["location"].split(":")[1] for e in pools[pool.split(":")[1]]["elements"]]

        def problems(pools_):
            found, reached = [], set()
            starts = ["halcyon_ark_hub", "halcyon_ark_luna_hub"] + [n for n in pools_ if n in
                      ("halcyon_ark_sterns", "halcyon_ark_midsections", "halcyon_ark_pods", "halcyon_ark_debris")]
            queue = deque(starts)
            while queue:
                name = queue.popleft()
                if name in reached:
                    continue
                reached.add(name)
                if name not in pools_:
                    found.append("missing pool " + name)
                    continue
                fallback = pools_[name]["fallback"]
                if fallback != "minecraft:empty":
                    queue.append(fallback.split(":")[1])
                for element in pools_[name]["elements"]:
                    piece = pieces.get(element["element"]["location"].split(":")[1])
                    if piece is None:
                        found.append("missing template " + element["element"]["location"])
                        continue
                    for connector in piece["connectors"]:
                        if connector["pool"] == "minecraft:empty":
                            continue
                        target = connector["pool"].split(":")[1]
                        queue.append(target)
                        if target in pools_ and not any(
                                c["name"] == connector["target"]
                                for e in pools_[target]["elements"]
                                for c in pieces.get(e["element"]["location"].split(":")[1], {"connectors": []})["connectors"]):
                            found.append("no partner for %s in %s" % (piece["name"], target))
            found += ["unused pool " + n for n in pools_ if n not in reached]
            return found

        self.assertEqual([], problems(pools))
        broken = copy.deepcopy(pools)
        broken["halcyon_ark_cryo"]["elements"][0]["element"]["location"] = "neroruins:halcyon_ark_missing"
        self.assertTrue(problems(broken), "a missing template is caught")
        orphan = copy.deepcopy(pools)
        orphan["halcyon_ark_unused"] = orphan["halcyon_ark_side_caps"]
        self.assertIn("unused pool halcyon_ark_unused", problems(orphan))
        self.assertEqual(3, len(members("neroruins:halcyon_ark_side_rooms")))

    def test_every_spine_and_side_pool_ends_in_a_matching_cap(self):
        pools, pieces = self.pool_files(), specs()
        for name, pool in pools.items():
            if name.endswith(("_hub", "_caps", "sterns", "midsections", "pods", "debris")):
                continue
            with self.subTest(pool=name):
                expected = "side_caps" if name.startswith("halcyon_ark_side") else "spine_caps"
                self.assertEqual("neroruins:halcyon_ark_" + expected, pool["fallback"])
        spine, side = pieces["halcyon_ark_spine_cap"], pieces["halcyon_ark_side_cap"]
        self.assertEqual([3, 3, 1], spine["size"], "a spine cap plugs the whole 3x3 door")
        self.assertEqual([1, 2, 1], side["size"], "a side cap plugs the 1x2 side door")
        self.assertEqual(ark.SPINE, spine["connectors"][0]["name"])
        self.assertEqual(ark.SIDE, side["connectors"][0]["name"])

    def test_spine_fits_the_jigsaw_bounds_and_budget(self):
        structures = ark.structures()
        for prefix, chain, structure in (("halcyon_ark", ark.CITADEL_CHAIN, "halcyon_ark"),
                                         ("halcyon_ark_luna", ark.LUNA_CHAIN, "halcyon_ark_luna")):
            layout = structures[structure]["layouts"][0]["layout"]
            hub_center = ark.LENGTHS["hub"] / 2
            far_end = sum(ark.LENGTHS[r] for r in chain) + 1  # plus a cap
            side_reach = ark.W / 2 + 7 + 9
            with self.subTest(structure=structure):
                self.assertLessEqual(far_end - hub_center, layout["max_distance_from_center"])
                self.assertLessEqual(side_reach, layout["max_distance_from_center"])
                self.assertLessEqual(len(chain) - 1, layout["size"], "the last room must be placed")
                self.assertLessEqual(layout["size"], 8, "decision 002 caps the Ark at depth 8")
                pieces = len(chain) + sum(2 * 2 for role in chain if role in ark.SIDE_DOORS)
                self.assertLessEqual(pieces, 140)


class SpineTest(unittest.TestCase):
    def test_gates_hide_the_vaults_and_the_keycard_lies_between_them(self):
        for prefix, chain, mix in chains():
            blocks, fixtures = assemble(chain_names(prefix, chain, mix))
            with self.subTest(chain=prefix, mix=mix):
                self.assertTrue(gate_order_holds(blocks, fixtures, start_of(prefix)))
        blocks, fixtures = assemble(chain_names("halcyon_ark", ark.CITADEL_CHAIN, [0] * 8))
        engineering_gate = doors(blocks)[0][1]
        breached = dict(blocks)
        breached[(3, 1, engineering_gate)] = breached[(3, 2, engineering_gate)] = "minecraft:air"
        self.assertFalse(gate_order_holds(breached, fixtures, start_of("halcyon_ark")), "a hole in a bulkhead is caught")
        moved = copy.deepcopy(fixtures)
        moved["containers"] = [c for c in moved["containers"] if not c["loot_table"].endswith("medbay")]
        self.assertFalse(gate_order_holds(blocks, moved, start_of("halcyon_ark")), "a missing keycard is caught")

    def test_no_lock_is_reachable_without_waking_a_guardian(self):
        for prefix, chain, mix in chains():
            blocks, fixtures = assemble(chain_names(prefix, chain, mix))
            with self.subTest(chain=prefix, mix=mix):
                self.assertTrue(guarded(blocks, fixtures, start_of(prefix)))
        blocks, fixtures = assemble(chain_names("halcyon_ark", ark.CITADEL_CHAIN, [1] * 8))
        unguarded = copy.deepcopy(fixtures)
        unguarded["spawners"] = [s for s in unguarded["spawners"] if "guard" not in s["piece"]]
        self.assertFalse(guarded(blocks, unguarded, start_of("halcyon_ark")),
                         "without the guardian hall the conduit lock is open")

    def test_logs_are_placed_once_in_bible_order_and_face_open_floor(self):
        offsets = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
        for prefix, chain, mix in chains():
            blocks, fixtures = assemble(chain_names(prefix, chain, mix))
            expected = sorted((ark.CITADEL_LOGS if prefix == "halcyon_ark" else ark.LUNA_LOGS).values())
            cells = walk_with(blocks, start_of(prefix), {z for _, z in doors(blocks)})
            with self.subTest(chain=prefix, mix=mix):
                self.assertEqual(expected, log_order(fixtures))
                for t in fixtures["terminals"]:
                    x, y, z = t["pos"]
                    dx, dz = offsets[t["facing"]]
                    self.assertIn((x + dx, y, z + dz), cells, "%s screen faces a wall" % t["piece"])
        blocks, fixtures = assemble(chain_names("halcyon_ark", ark.CITADEL_CHAIN, [2] * 8))
        doubled = copy.deepcopy(fixtures)
        doubled["terminals"].append(dict(doubled["terminals"][0], pos=[1, 1, 200]))
        self.assertNotEqual(sorted(ark.CITADEL_LOGS.values()), log_order(doubled), "a repeated log is caught")
        swapped = copy.deepcopy(fixtures)
        swapped["terminals"][0]["pos"], swapped["terminals"][-1]["pos"] = (swapped["terminals"][-1]["pos"],
                                                                         swapped["terminals"][0]["pos"])
        self.assertNotEqual(sorted(log_order(swapped)), log_order(swapped), "logs out of order are caught")

    def test_each_lock_opens_only_its_own_door_and_vault(self):
        for prefix, chain, mix in chains():
            blocks, _ = assemble(chain_names(prefix, chain, mix))
            with self.subTest(chain=prefix, mix=mix):
                for lock, targets in unlock_targets(blocks).items():
                    kind = block_id(blocks[lock])
                    doors_ = [t for t in targets if block_id(blocks[t]) == "neroruins:sealed_door"]
                    vaults = [t for t in targets if block_id(blocks[t]) == "neroruins:vault_cache"]
                    self.assertEqual(2, len(doors_), "%s at %s opens one two-block door" % (kind, lock))
                    self.assertEqual(0 if kind == "neroruins:conduit_lock" else 1, len(vaults), kind)
        blocks, _ = assemble(chain_names("halcyon_ark", ark.CITADEL_CHAIN, [0] * 8))
        crowded = dict(blocks)
        reader = next(p for p, b in blocks.items() if block_id(b) == "neroruins:card_reader")
        crowded[(reader[0] - 2, 1, reader[2] - 3)] = "neroruins:vault_cache"
        self.assertTrue(any(len([t for t in ts if block_id(crowded[t]) == "neroruins:vault_cache"]) > 1
                            for ts in unlock_targets(crowded).values()), "a stray vault in reach is caught")

    def test_engineering_conduits_are_solvable_and_break_when_turned(self):
        pieces = specs()
        for prefix in ("halcyon_ark", "halcyon_ark_luna"):
            for v in range(3):
                spec = pieces["%s_engineering_%d" % (prefix, v)]
                blocks = blocks_of(spec)
                with self.subTest(piece=spec["name"]):
                    self.assertTrue(conduit_solved(blocks))
                    turned = dict(blocks)
                    pos = next(p for p, b in blocks.items() if block_id(b) == "neroruins:conduit"
                               and (p[0], p[2]) in ark.CONDUIT_PATHS[v])
                    state = copy.deepcopy(blocks[pos])
                    state["properties"]["facing"] = CLOCKWISE[state["properties"]["facing"]]
                    turned[pos] = state
                    self.assertFalse(conduit_solved(turned))

    def test_side_doors_and_branches_are_walkable(self):
        pieces = specs()
        for name, spec in pieces.items():
            side = [c for c in spec["connectors"] if c["pool"] == "neroruins:halcyon_ark_side_corridors"]
            if not side:
                continue
            blocks = blocks_of(spec)
            entrance = tuple(spec["connectors"][0]["pos"]) if spec["connectors"][0]["pool"] == "minecraft:empty" \
                else (ark.CX, 1, 1)
            cells = walk(blocks, entrance, ("all",))
            with self.subTest(piece=name):
                for connector in side:
                    self.assertIn(tuple(connector["pos"]), cells)
        for name in [n for n in pieces if n.startswith(("halcyon_ark_side_corridor", "halcyon_ark_side_room"))]:
            spec = pieces[name]
            cells = walk(blocks_of(spec), tuple(spec["connectors"][0]["pos"]), ())
            with self.subTest(piece=name):
                for c in spec["connectors"][1:]:
                    self.assertIn(tuple(c["pos"]), cells)
                for chest in spec.get("containers", []):
                    self.assertTrue(visible(cells, tuple(chest["pos"])))


class EnclosureTest(unittest.TestCase):
    """Decision 039: every room behind a gate is sealed in unbreakable plating; mining cannot reach it."""

    def breaches(self, blocks):
        found = []
        for x, z in doors(blocks):
            hit = gated_enclosure.breach(blocks, (x, 1, z + 1), forbidden={(x, 1, z - 1)})
            if hit is not None:
                found.append(((x, z), hit))
        return found

    def test_gated_rooms_cannot_be_mined_into(self):
        for prefix, chain, mix in chains():
            blocks, _ = assemble(chain_names(prefix, chain, mix))
            with self.subTest(chain=prefix, mix=mix):
                self.assertTrue(doors(blocks))
                self.assertEqual([], self.breaches(blocks))

    def test_one_mineable_panel_in_a_gated_wall_is_caught(self):
        blocks, _ = assemble(chain_names("halcyon_ark", ark.CITADEL_CHAIN, [0] * 8))
        _, gate_z = doors(blocks)[0]
        weakened = dict(blocks)
        weakened[(0, 3, gate_z + 2)] = ark.HULL
        self.assertTrue(self.breaches(weakened))


class DataTest(unittest.TestCase):
    def test_structures_place_in_void_and_on_luna(self):
        structures = ark.structures()
        citadel, luna = structures["halcyon_ark"], structures["halcyon_ark_luna"]
        layout = citadel["layouts"][0]["layout"]
        self.assertNotIn("ground_check", citadel, "the station has no ground to check")
        self.assertNotIn("project_start_to_heightmap", layout, "a void has no heightmap")
        self.assertEqual(63, layout["start_height"]["absolute"])
        self.assertEqual(("citadel", "complex"), (citadel["size_class"], luna["size_class"]))
        self.assertIn("ground_check", luna)
        tags = ark.biome_tags()
        self.assertEqual(["minecraft:the_void"], tags["halcyon_ark"]["values"])
        self.assertTrue(all(v["id"].startswith("nerospace:luna_") and v["required"] is False
                            for v in tags["halcyon_ark_luna"]["values"]))

    def test_satellites_belong_to_their_hub_with_distinct_indices(self):
        sets = ark.structure_sets(ark.structures())
        for hub, count in (("halcyon_ark", 6), ("halcyon_ark_luna", 2)):
            placements = [s["placement"] for s in sets.values() if s["placement"].get("hub_set") == "neroruins:" + hub]
            with self.subTest(hub=hub):
                self.assertEqual(count, len(placements))
                self.assertEqual(list(range(count)), sorted(p["index"] for p in placements))
                self.assertTrue(all(1 <= p["min_radius"] <= p["max_radius"] <= 32 and 0 < p["chance_percent"] <= 100
                                    for p in placements))
        self.assertGreaterEqual(sets["halcyon_ark"]["placement"]["spacing"], 16)

    def test_salts_are_unique_across_every_structure_set(self):
        salts = {}
        for path in (DATA / "worldgen" / "structure_set").glob("*.json"):
            placement = json.loads(path.read_text(encoding="utf-8"))["placement"]
            if "salt" in placement:
                salts.setdefault(placement["salt"], []).append(path.stem)
        self.assertEqual({}, {salt: names for salt, names in salts.items() if len(names) > 1})

    def test_medbay_always_holds_the_command_keycard_and_vaults_roll_relics(self):
        tables = ark.loot_tables()
        medbay = tables["halcyon_ark_medbay"]["pools"][0]
        self.assertEqual([{"type": "minecraft:item", "name": "neroruins:command_keycard"}], medbay["entries"])
        self.assertEqual(1, medbay["rolls"])
        for name in ("halcyon_ark_vault", "halcyon_ark_bridge"):
            values = [e["value"] for p in tables[name]["pools"] for e in p["entries"]]
            self.assertIn("neroruins:tier/4", values)

    def test_spawn_table_keeps_sibling_mobs_optional(self):
        table = ark.spawn_table()["entries"]
        for entry in table:
            namespace = entry["entity"].split(":")[0]
            with self.subTest(entity=entry["entity"]):
                if namespace in ("minecraft", "neroruins"):
                    self.assertNotIn("required", entry)
                else:
                    self.assertIs(False, entry["required"])
        self.assertTrue(any(e["entity"].startswith("neroruins:sentinel") and e["max_tier"] == 3 for e in table))


if __name__ == "__main__":
    unittest.main()
