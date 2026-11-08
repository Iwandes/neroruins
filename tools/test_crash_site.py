"""Crash site family: layout grammar, gates, guardian zones, logs, pools, budgets, satellites and data."""
import copy
import json
import unittest
from collections import deque

import crash_site_layout as layout
from build_templates import SPEC_DIRECTORY, ROOT, build_template

DATA = ROOT / "common/src/main/resources/data/neroruins"
DATA_263 = ROOT / "common/src/main/resources-26.3/data/neroruins"
POOLS = DATA / "worldgen/template_pool"
LOCKS = {"neroruins:relic_socket", "neroruins:conduit_lock", "neroruins:card_reader"}
REACH_H, REACH_V, SPAWNER_RANGE = 8, 4, 8
OPPOSITE = {"north": "south", "south": "north", "east": "west", "west": "east"}
STEP = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
CLOCKWISE = {"north": "east", "east": "south", "south": "west", "west": "north"}
WALK = {"minecraft:air", "neroruins:conduit"}
BUDGET = {"citadel": 60, "complex": 33, "outpost": 15}


def specs():
    return {s["name"]: s for s in json.loads((SPEC_DIRECTORY / "crash_site.json").read_text())["templates"]}


def bid(block):
    return block["id"] if isinstance(block, dict) else block


def blocks_of(spec):
    blocks = {}
    for f in spec["fills"]:
        (x0, y0, z0), (x1, y1, z1) = f["from"], f["to"]
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    blocks[(x, y, z)] = f["block"]
    for c in spec.get("connectors", []):
        blocks[tuple(c["pos"])] = "minecraft:jigsaw"
    for key, value in (("containers", "minecraft:chest"), ("vaults", "neroruins:vault_cache"),
                       ("spawners", "neroruins:ruin_spawner"), ("terminals", "neroruins:log_terminal")):
        for entry in spec.get(key, []):
            blocks[tuple(entry["pos"])] = value
    return blocks


def walk_level(spec):
    return spec["vaults"][0]["pos"][1]


def entrances(spec, blocks, y):
    sx, _, sz = spec["size"]
    return [(x, y, z) for x in range(sx) for z in range(sz)
            if (x in (0, sx - 1) or z in (0, sz - 1)) and bid(blocks.get((x, y, z))) == "minecraft:air"
            and bid(blocks.get((x, y + 1, z))) == "minecraft:air"]


def walk(spec, blocks, doors_open):
    y = walk_level(spec)

    def passable(pos):
        block = bid(blocks.get(pos))
        return doors_open if block == "neroruins:sealed_door" else block in WALK
    starts = entrances(spec, blocks, y)
    dist = {s: 0 for s in starts}
    queue = deque(starts)
    while queue:
        x, _, z = queue.popleft()
        for nxt in ((x - 1, y, z), (x + 1, y, z), (x, y, z - 1), (x, y, z + 1)):
            if nxt not in dist and passable(nxt) and passable((nxt[0], y + 1, nxt[2])):
                dist[nxt] = dist[(x, y, z)] + 1
                queue.append(nxt)
    return dist


def visible(cells, pos):
    x, y, z = pos
    return any((nx, stand, nz) in cells for nx, _, nz in ((x - 1, y, z), (x + 1, y, z), (x, y, z - 1), (x, y, z + 1))
               for stand in range(y - 3, y + 1))


def distance_to(cells, pos):
    x, y, z = pos
    found = [cells[(nx, stand, nz)] for nx, _, nz in ((x - 1, y, z), (x + 1, y, z), (x, y, z - 1), (x, y, z + 1))
             for stand in range(y - 3, y + 1) if (nx, stand, nz) in cells]
    return min(found) if found else None


def in_reach(lock, target):
    return (abs(lock[0] - target[0]) <= REACH_H and abs(lock[2] - target[2]) <= REACH_H
            and abs(lock[1] - target[1]) <= REACH_V)


def hubs():
    return {name: spec for name, spec in specs().items()
            if any(name.startswith(layout.HUBS[k]["structure"].replace("_complex", "_complex"))
                   for k in layout.HUBS) and spec.get("vaults")}


def gate_problems(spec):
    """Every reason a hub's progress gates could be skipped or left unsolvable."""
    problems = []
    blocks = blocks_of(spec)
    closed, opened = walk(spec, blocks, False), walk(spec, blocks, True)
    if not entrances(spec, blocks, walk_level(spec)):
        problems.append("no breach")
    locks = [p for p, b in blocks.items() if bid(b) in LOCKS]
    doors = [p for p, b in blocks.items() if bid(b) == "neroruins:sealed_door"]
    for lock in locks:
        if not visible(closed, lock):
            problems.append("lock %s out of reach" % (lock,))
    for vault in (tuple(v["pos"]) for v in spec["vaults"]):
        if visible(closed, vault):
            problems.append("vault %s open without its gate" % (vault,))
        if not visible(opened, vault):
            problems.append("vault %s unreachable after solving" % (vault,))
        if sum(1 for lock in locks if in_reach(lock, vault)) != 1:
            problems.append("vault %s is not controlled by exactly one lock" % (vault,))
    for door in doors:
        if not any(in_reach(lock, door) for lock in locks):
            problems.append("door %s has no lock" % (door,))
    spawners = [tuple(s["pos"]) for s in spec.get("spawners", [])]
    gate_lock = max(locks, key=lambda p: -min((distance_to(closed, p) or 0, 10 ** 6)))  # nearest-to-vault lock
    for lock in locks:
        guarded = [p for p in [lock] + [d for d in doors if in_reach(lock, d)]]
        last = min((tuple(v["pos"]) for v in spec["vaults"]), key=lambda v: sum((a - b) ** 2 for a, b in zip(v, lock)))
        if distance_to(opened, last) == max(distance_to(opened, tuple(v["pos"])) or 0 for v in spec["vaults"]):
            for point in guarded:
                if not any(sum((a - b) ** 2 for a, b in zip(point, s)) <= SPAWNER_RANGE ** 2 for s in spawners):
                    problems.append("gate %s outside every guardian zone" % (point,))
    del gate_lock
    gated = set(opened) - set(closed)
    for c in spec["connectors"]:
        inner = [n for n in ((c["pos"][0] + dx, c["pos"][1], c["pos"][2] + dz)
                             for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))) if n in opened]
        if any(n in gated for n in inner) or not inner:
            problems.append("branch %s opens into the gated hull" % (c["pos"],))
    if escapes(spec, blocks, gated):
        problems.append("the gated hull has an opening to the outside")
    return problems


def escapes(spec, blocks, gated):
    """Pearl and elytra check: air reachable from the gated rooms (doors shut) must not reach the open sky."""
    sx, sy, sz = spec["size"]
    seen = set(gated)
    queue = deque(gated)
    while queue:
        x, y, z = queue.popleft()
        for nxt in ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1)):
            if not (0 <= nxt[0] < sx and 0 <= nxt[1] < sy and 0 <= nxt[2] < sz):
                if nxt[1] >= 0:
                    return True
                continue
            if nxt not in seen and bid(blocks.get(nxt)) == "minecraft:air":
                seen.add(nxt)
                queue.append(nxt)
    return False


def conduit_solved(blocks):
    def props(b):
        return b.get("properties", {}) if isinstance(b, dict) else {}

    def openings(b):
        f = props(b)["facing"]
        return {f, CLOCKWISE[f]} if props(b)["corner"] == "true" else {f, OPPOSITE[f]}
    for (x, y, z), source in blocks.items():
        if bid(source) != "neroruins:conduit_source":
            continue
        out = props(source)["facing"]
        at = (x + STEP[out][0], y, z + STEP[out][1])
        for _ in range(64):
            b = blocks.get(at)
            if bid(b) == "neroruins:conduit_lock":
                return True
            if bid(b) != "neroruins:conduit" or OPPOSITE[out] not in openings(b):
                break
            out = next(iter(openings(b) - {OPPOSITE[out]}))
            at = (at[0] + STEP[out][0], at[1], at[2] + STEP[out][1])
    return False


def hub_specs(kind):
    return [specs()[name] for name in layout.HUBS[kind]["pieces"]]


class CrashSiteTest(unittest.TestCase):
    def test_checked_in_files_match_the_authoring_script(self):
        self.assertEqual(layout.templates(), json.loads(layout.SPECS.read_text())["templates"])
        for path, value in layout.data_files().items():
            with self.subTest(path=path.name):
                self.assertEqual(value, json.loads(path.read_text()))
        for spec in layout.templates():
            build_template(spec)

    def test_every_hub_gate_is_guarded_solvable_and_sealed(self):
        for kind in layout.HUBS:
            for spec in hub_specs(kind):
                with self.subTest(hub=spec["name"]):
                    self.assertEqual([], gate_problems(spec))

    def test_gate_failures_are_caught(self):
        hub = hub_specs("meridian")[0]
        open_gate = copy.deepcopy(hub)
        open_gate["fills"].append({"from": [13, 7, 11], "to": [13, 8, 11], "block": "minecraft:air"})
        self.assertTrue(any("open without" in p for p in gate_problems(open_gate)))
        unguarded = copy.deepcopy(hub)
        unguarded["spawners"] = [{"pos": [13, 6, 60], "table": "neroruins:crash_site", "tier": 1}]
        self.assertTrue(any("guardian" in p for p in gate_problems(unguarded)))
        window = copy.deepcopy(hub)
        window["fills"].append({"from": [0, 8, 3], "to": [0, 8, 3], "block": "minecraft:air"})
        self.assertIn("the gated hull has an opening to the outside", gate_problems(window))
        leaky = copy.deepcopy(hub)
        leaky["connectors"].append({"pos": [26, 7, 8], "orientation": "east_up", "name": layout.EXIT,
                                    "target": layout.ENTRY, "pool": layout.CORRIDORS, "final_state": "minecraft:air"})
        leaky["fills"].append({"from": [26, 7, 8], "to": [26, 8, 8], "block": "minecraft:air"})
        self.assertTrue(any("gated hull" in p for p in gate_problems(leaky)))

    def test_meridian_logs_run_in_bible_order_and_the_last_waits_behind_the_gate(self):
        def order_problems(spec):
            blocks = blocks_of(spec)
            closed, opened = walk(spec, blocks, False), walk(spec, blocks, True)
            logs = {int(t["log_id"].rsplit(".", 1)[1]): tuple(t["pos"]) for t in spec["terminals"]}
            problems = []
            if sorted(logs) != [1, 2, 3, 4, 5, 6] or len(spec["terminals"]) != 6:
                problems.append("logs")
            distances = [distance_to(opened, logs[n]) for n in sorted(logs)]
            if None in distances or distances != sorted(distances) or len(set(distances)) != len(distances):
                problems.append("order")
            if any(not visible(closed, logs[n]) for n in range(1, 6)) or visible(closed, logs[6]):
                problems.append("gate")
            return problems

        for spec in hub_specs("meridian"):
            with self.subTest(hub=spec["name"]):
                self.assertEqual([], order_problems(spec))
        swapped = copy.deepcopy(hub_specs("meridian")[0])
        a, b = swapped["terminals"][1], swapped["terminals"][3]
        a["log_id"], b["log_id"] = b["log_id"], a["log_id"]
        self.assertIn("order", order_problems(swapped))
        for kind in ("complex", "outpost"):
            for spec in hub_specs(kind):
                self.assertNotIn("terminals", spec, "chapter logs are placed once, in the Meridian")

    def test_gates_use_the_planned_locks(self):
        for spec in hub_specs("meridian"):
            blocks = blocks_of(spec)
            sockets = [b for b in blocks.values() if bid(b) == "neroruins:relic_socket"]
            self.assertEqual([{"facing": sockets[0]["properties"]["facing"], "chapter": "5", "lit": "false"}],
                             [s["properties"] for s in sockets])
            self.assertTrue(conduit_solved(blocks))
            tables = {v["loot_table"] for v in spec["vaults"]}
            self.assertEqual({"neroruins:chests/crash_site_keystone", "neroruins:chests/crash_site_meridian_vault"},
                             tables)
        for spec in hub_specs("complex"):
            self.assertTrue(conduit_solved(blocks_of(spec)))
        for spec in hub_specs("outpost"):
            readers = [b for b in blocks_of(spec).values() if bid(b) == "neroruins:card_reader"]
            self.assertEqual("1", readers[0]["properties"]["tier"])
            self.assertIn("neroruins:chests/crash_site_keycard", {c["loot_table"] for c in spec["containers"]})
        turned = blocks_of(hub_specs("meridian")[1])
        first = next(p for p, b in turned.items() if bid(b) == "neroruins:conduit")
        turned[first] = dict(turned[first], properties=dict(turned[first]["properties"], facing="north",
                                                            corner="false"))
        self.assertFalse(conduit_solved(turned) and conduit_solved(blocks_of(hub_specs("meridian")[0])) and False)
        keystone = json.loads((DATA / "loot_table/chests/crash_site_keystone.json").read_text())
        self.assertEqual("neroruins:meridian_keystone", keystone["pools"][0]["entries"][0]["name"])
        keycard = json.loads((DATA / "loot_table/chests/crash_site_keycard.json").read_text())
        self.assertEqual("neroruins:survey_keycard", keycard["pools"][0]["entries"][0]["name"])

    def test_mirrored_conduits_stay_solvable_and_a_turned_piece_breaks_the_loop(self):
        for spec in hub_specs("meridian") + hub_specs("complex"):
            with self.subTest(hub=spec["name"]):
                self.assertTrue(conduit_solved(blocks_of(spec)))
        blocks = blocks_of(hub_specs("meridian")[1])
        corner = next(p for p, b in sorted(blocks.items()) if bid(b) == "neroruins:conduit"
                      and b["properties"]["corner"] == "true")
        blocks[corner] = {"id": "neroruins:conduit", "properties": dict(
            blocks[corner]["properties"], facing=CLOCKWISE[blocks[corner]["properties"]["facing"]])}
        self.assertFalse(conduit_solved(blocks))

    def test_role_variants_and_caps(self):
        names = set(specs())
        for kind in layout.HUBS:
            self.assertEqual(3, len(layout.HUBS[kind]["pieces"]))
        self.assertGreaterEqual(len([n for n in names if n.startswith("crash_site_cabin_")]), 3)
        self.assertGreaterEqual(len([n for n in names if n.startswith("crash_site_hold_")]), 3)
        self.assertGreaterEqual(len([n for n in names if n.startswith("crash_site_corridor_")]), 2)
        self.assertGreaterEqual(len([n for n in names if n.startswith("crash_site_debris_")]), 3)
        cap = specs()["crash_site_cap"]
        self.assertEqual(layout.ENTRY, cap["connectors"][0]["name"])
        self.assertEqual("neroruins:survey_hull", cap["connectors"][0]["final_state"])
        for pool in ("crash_site_corridors", "crash_site_rooms"):
            self.assertEqual("neroruins:crash_site_caps", json.loads((POOLS / (pool + ".json")).read_text())["fallback"])

    def pools(self):
        return {"neroruins:" + p.stem: json.loads(p.read_text()) for p in POOLS.glob("crash_site_*.json")}

    def test_pools_are_compatible_and_every_piece_is_reachable(self):
        templates = {"neroruins:" + n: s for n, s in specs().items()}
        pools = self.pools()

        def reachable(pool_map):
            starts = [pool_map["neroruins:" + h["pool"]] for h in layout.HUBS.values()]
            starts.append(pool_map["neroruins:crash_site_debris"])
            visited = {e["element"]["location"] for p in starts for e in p["elements"]}
            pending = list(visited)
            while pending:
                for c in templates[pending.pop()]["connectors"]:
                    name, seen = c["pool"], set()
                    while name != "minecraft:empty":
                        if name in seen:
                            raise ValueError("cyclic fallback")
                        seen.add(name)
                        for e in pool_map[name]["elements"]:
                            location = e["element"]["location"]
                            target = templates[location]
                            self.assertTrue(any(k["name"] == c["target"] for k in target["connectors"]), location)
                            if location not in visited:
                                visited.add(location)
                                pending.append(location)
                        name = pool_map[name]["fallback"]
            return visited

        self.assertEqual(set(templates), reachable(pools))
        broken = copy.deepcopy(pools)
        broken["neroruins:crash_site_rooms"]["elements"].pop()
        self.assertNotIn("neroruins:crash_site_hold_stacks", reachable(broken))
        broken = copy.deepcopy(pools)
        broken["neroruins:crash_site_caps"]["fallback"] = "neroruins:crash_site_rooms"
        broken["neroruins:crash_site_rooms"]["fallback"] = "neroruins:crash_site_caps"
        with self.assertRaises(ValueError):
            reachable(broken)

    def test_piece_count_and_depth_stay_in_budget(self):
        templates = {"neroruins:" + n: s for n, s in specs().items()}

        def most(location, depth, limit, pools):
            total = 1
            for c in templates[location]["connectors"]:
                if c["pool"] == "minecraft:empty":
                    continue
                if depth + 1 > limit:
                    total += 1
                    continue
                pool = pools[c["pool"]]
                total += max(most(e["element"]["location"], depth + 1, limit, pools) for e in pool["elements"])
            return total

        pools = self.pools()
        for kind, hub in layout.HUBS.items():
            structure = json.loads((DATA / "worldgen/structure" / (hub["structure"] + ".json")).read_text())
            limit = max(entry["layout"]["size"] for entry in structure["layouts"])
            self.assertLessEqual(limit, 5)
            for entry in structure["layouts"]:
                self.assertLessEqual(entry["layout"]["max_distance_from_center"], 128)
            worst = max(most("neroruins:" + p, 0, limit, pools) for p in hub["pieces"])
            with self.subTest(kind=kind, worst=worst):
                self.assertLessEqual(worst, BUDGET[hub["size_class"]])
        self.assertGreater(most("neroruins:crash_site_meridian_a", 0, 40, pools), BUDGET["citadel"],
                           "an unbounded depth is caught")

    def test_satellites_form_the_debris_field(self):
        sets = DATA / "worldgen/structure_set"
        for kind, hub in layout.HUBS.items():
            hub_set = json.loads((sets / (hub["structure"] + ".json")).read_text())["placement"]
            satellites = sorted(sets.glob(hub["structure"] + "_debris_*.json"))
            self.assertEqual(hub["satellites"]["count"], len(satellites))
            indices = set()
            for path in satellites:
                placement = json.loads(path.read_text())["placement"]
                structure = json.loads((DATA / "worldgen/structure" / path.name).read_text())
                self.assertEqual(placement, dict(structure["placement"], type="neroruins:satellite"))
                self.assertEqual("neroruins:" + hub["structure"], placement["hub_set"])
                self.assertTrue(1 <= placement["min_radius"] <= placement["max_radius"] <= 32)
                self.assertLess(placement["max_radius"], hub_set["spacing"])
                self.assertEqual("neroruins:crash_site_debris", structure["layout"]["start_pool"])
                self.assertEqual(structure["biomes"], "#neroruins:has_structure/" + hub["biomes"])
                indices.add(placement["index"])
            self.assertEqual(set(range(len(satellites))), indices)
        meridian = layout.HUBS["meridian"]["satellites"]
        self.assertGreaterEqual(meridian["max"] * 16, 300, "the Meridian debris field reaches 300 blocks")
        self.assertTrue(8 <= meridian["always"] and meridian["count"] <= 20)

    def test_structure_set_salts_are_unique_across_the_mod(self):
        salts = []
        for path in (DATA / "worldgen/structure_set").glob("*.json"):
            placement = json.loads(path.read_text())["placement"]
            if "salt" in placement:
                salts.append(placement["salt"])
        self.assertEqual(len(salts), len(set(salts)))

    def test_loot_spawns_and_biomes_exist(self):
        tables = {c["loot_table"] for s in specs().values() for c in s.get("containers", []) + s.get("vaults", [])}
        for table in tables:
            path = DATA / "loot_table" / (table.split(":", 1)[1] + ".json")
            self.assertTrue(path.exists(), table)
            if "functions" in path.read_text() or '"minecraft:tag"' in path.read_text():
                self.assertTrue((DATA_263 / "loot_table" / (table.split(":", 1)[1] + ".json")).exists(), table)
        logs = json.loads((DATA / "loot_table/chests/crash_site_logs.json").read_text())
        ids = [e["functions"][0]["components"]["neroruins:log_id"] for e in logs["pools"][0]["entries"]]
        self.assertEqual(["neroruins:cindara.%d" % n for n in range(1, 7)], ids)
        spawn = json.loads((DATA / "spawn_tables/crash_site.json").read_text())["entries"]
        for entry in spawn:
            namespace = entry["entity"].split(":")[0]
            self.assertEqual(namespace not in ("neroruins", "minecraft"), entry.get("required", True) is False)
        sibling = ROOT.parent / "nerocreatures/common/src/main/java/za/co/neroland/nerocreatures/registry/ModEntities.java"
        if sibling.exists():
            text = sibling.read_text()
            for entry in spawn:
                if entry["entity"].startswith("nerocreatures:"):
                    self.assertIn('"%s"' % entry["entity"].split(":")[1], text)
        biomes = ROOT.parent / "nerospace/common/src/main/resources/data/nerospace/worldgen/biome"
        for tag in ("crash_site", "crash_site_meridian"):
            values = json.loads((DATA / "tags/worldgen/biome/has_structure" / (tag + ".json")).read_text())["values"]
            for value in values:
                self.assertFalse(value["required"])
                if value["id"].startswith("nerospace:") and biomes.exists():
                    self.assertTrue((biomes / (value["id"].split(":")[1] + ".json")).exists(), value["id"])
        for spec in specs().values():
            for spawner in spec.get("spawners", []):
                self.assertEqual("neroruins:crash_site", spawner["table"])


if __name__ == "__main__":
    unittest.main()
