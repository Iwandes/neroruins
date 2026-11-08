"""Graph, geometry and gating tests for the lattice_temple family (citadel, complexes, outposts, satellites)."""
import copy
import itertools
import json
import unittest
from collections import deque

from build_templates import ROOT, SPEC_DIRECTORY, build_template
from lattice_temple_layout import UNBREAKABLE, spiral_well

DATA = ROOT / "common" / "src" / "main" / "resources" / "data" / "neroruins"
WORLDGEN = DATA / "worldgen"
SPAWNER_RANGE = 8           # GuardianRules.SPAWNER_RANGE
UNLOCK_H, UNLOCK_V = 8, 4   # PuzzleUnlock reach
PYLON_REACH = 6             # ResonancePylonBlock.ROOM_REACH
ENTRY = "neroruins:lattice_temple_entry"
FACING = {"north_up": (0, -1), "south_up": (0, 1), "east_up": (1, 0), "west_up": (-1, 0)}
STRUCTURES = {
    # id: (expected spine logs in order, piece budget, depth budget)
    "lattice_temple": ([f"neroruins:lattice.{i}" for i in range(1, 7)], 160, 9),
    "lattice_temple_complex": ([f"neroruins:nyxara.{i}" for i in range(1, 7)], 88, 5),
    "lattice_temple_cindara": ([], 88, 5),
    "lattice_temple_outpost": ([], 40, 2),
}
LOCKS = ("neroruins:glyph_lock", "neroruins:relic_socket", "neroruins:resonance_pylon")
CITADEL_PLAZA = 3            # the citadel hub's plaza floor, local y
MIN_CITADEL_DEPTH = 70      # decision 039


def load_specs():
    return {t["name"]: t for t in json.loads((SPEC_DIRECTORY / "lattice_temple.json").read_text("utf-8"))["templates"]}


def load_pools():
    pools = {}
    for path in (WORLDGEN / "template_pool").glob("lattice_temple_*.json"):
        data = json.loads(path.read_text("utf-8"))
        pools["neroruins:" + path.stem] = {
            "fallback": data["fallback"],
            "items": [e["element"]["location"].split(":", 1)[1] for e in data["elements"]]}
    return pools


def load_structure(sid):
    return json.loads((WORLDGEN / "structure" / f"{sid}.json").read_text("utf-8"))


def block_id(block):
    return block["id"] if isinstance(block, dict) else block


def cells(spec):
    grid = {}
    for fill in spec["fills"]:
        (x0, y0, z0), (x1, y1, z1) = fill["from"], fill["to"]
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    grid[(x, y, z)] = block_id(fill["block"])
    for key, value in (("containers", "minecraft:chest"), ("vaults", "neroruins:vault_cache"),
                       ("spawners", "neroruins:ruin_spawner"), ("terminals", "neroruins:log_terminal")):
        for entry in spec.get(key, []):
            grid[tuple(entry["pos"])] = value
    for connector in spec.get("connectors", []):
        grid[tuple(connector["pos"])] = connector["final_state"]
    return grid


def exits(spec):
    return [c for c in spec.get("connectors", []) if c["name"] != ENTRY]


def entries(spec):
    return [c for c in spec.get("connectors", []) if c["name"] == ENTRY]


def walkable(grid, size, open_doors=False):
    def solid(pos):
        block = grid.get(pos)
        if block == "neroruins:sealed_door":
            return not open_doors
        return block not in (None, "minecraft:air")

    def clear(pos):
        return not solid(pos) and grid.get(pos) is not None

    stand = set()
    for (x, y, z) in grid:
        if 0 < y < size[1] - 1 and clear((x, y, z)) and clear((x, y + 1, z)) and solid((x, y - 1, z)):
            stand.add((x, y, z))
    return stand


def flood(stand, starts, avoid=lambda pos: False):
    seen = {s for s in starts if s in stand and not avoid(s)}
    queue = deque(seen)
    while queue:
        x, y, z = queue.popleft()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for dy in (-1, 0, 1):
                nxt = (x + dx, y + dy, z + dz)
                if nxt in stand and nxt not in seen and not avoid(nxt):
                    seen.add(nxt)
                    queue.append(nxt)
    return seen


def near_spawner(spec):
    spawners = [tuple(s["pos"]) for s in spec.get("spawners", [])]

    def inside(pos):
        x, y, z = pos
        return any((x + .5 - sx - .5) ** 2 + (y - sy - .5) ** 2 + (z + .5 - sz - .5) ** 2 <= SPAWNER_RANGE ** 2
                   for sx, sy, sz in spawners)
    return inside


def touches(seen, pos):
    x, y, z = pos
    return any((x + dx, y + dy, z + dz) in seen for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)) for dy in (-1, 0, 1))


def lock_positions(spec):
    return [tuple(f["from"]) for f in spec["fills"] if block_id(f["block"]) in LOCKS]


def gated_targets(spec):
    doors = [tuple(f["from"]) for f in spec["fills"] if block_id(f["block"]) == "neroruins:sealed_door"]
    return doors + [tuple(v["pos"]) for v in spec.get("vaults", [])]


def within_unlock(a, b):
    return abs(a[0] - b[0]) <= UNLOCK_H and abs(a[2] - b[2]) <= UNLOCK_H and abs(a[1] - b[1]) <= UNLOCK_V


# ---- jigsaw placement simulator ---------------------------------------------------------------------

def rotate(r, x, z):
    for _ in range(r % 4):
        x, z = -z, x
    return x, z


class Placed:
    def __init__(self, spec, r, offset):
        self.spec, self.r, self.offset = spec, r, offset
        sx, sy, sz = spec["size"]
        corners = [self.world((x, 0, z)) for x in (0, sx - 1) for z in (0, sz - 1)]
        self.lo = (min(c[0] for c in corners), offset[1], min(c[2] for c in corners))
        self.hi = (max(c[0] for c in corners), offset[1] + sy - 1, max(c[2] for c in corners))

    def world(self, pos):
        x, z = rotate(self.r, pos[0], pos[2])
        return (x + self.offset[0], pos[1] + self.offset[1], z + self.offset[2])

    def facing(self, orientation):
        return rotate(self.r, *FACING[orientation])

    def overlaps(self, other):
        return all(self.lo[i] <= other.hi[i] and other.lo[i] <= self.hi[i] for i in range(3))


def attach(parent, connector, child_spec):
    fx, fz = parent.facing(connector["orientation"])
    px, py, pz = parent.world(connector["pos"])
    target = (px + fx, py, pz + fz)
    entry = entries(child_spec)[0]
    for r in range(4):
        cx, cz = rotate(r, *FACING[entry["orientation"]])
        if (cx, cz) == (-fx, -fz):
            lx, lz = rotate(r, entry["pos"][0], entry["pos"][2])
            return Placed(child_spec, r, (target[0] - lx, target[1] - entry["pos"][1], target[2] - lz))
    raise AssertionError("no rotation aligns the entry")


def spine_pools(pools, specs, start_pool):
    chain = [start_pool]
    while True:
        nxt = {c["pool"] for name in pools[chain[-1]]["items"] for c in exits(specs[name]) if c.get("priority")}
        if not nxt:
            return chain
        assert len(nxt) == 1, f"{chain[-1]} branches its spine"
        chain.append(nxt.pop())


def spine_layouts(pools, specs, chain):
    """Every combination of spine variants, placed as jigsaw would place them."""
    for combo in itertools.product(*[pools[p]["items"] for p in chain]):
        hub = Placed(specs[combo[0]], 0, (0, 0, 0))
        placed = [hub]
        for name in combo[1:]:
            parent = placed[-1]
            connector = [c for c in exits(parent.spec) if c.get("priority")][0]
            placed.append(attach(parent, connector, specs[name]))
        yield placed


class LatticeTempleTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.specs = load_specs()
        cls.pools = load_pools()

    def start_pool(self, sid):
        return load_structure(sid)["layouts"][0]["layout"]["start_pool"]

    # -- encoding and connectors

    def test_every_template_builds_and_connectors_sit_on_their_faces(self):
        def misplaced(spec):
            sx, sy, sz = spec["size"]
            bad = []
            for c in spec.get("connectors", []):
                x, y, z = c["pos"]
                ok = {"north_up": z == 0, "south_up": z == sz - 1, "west_up": x == 0, "east_up": x == sx - 1}
                if not ok[c["orientation"]]:
                    bad.append(c["pos"])
            return bad

        for name, spec in self.specs.items():
            with self.subTest(template=name):
                build_template(spec)
                self.assertEqual([], misplaced(spec))
                if exits(spec) or name.endswith("cap"):
                    self.assertLessEqual(len(entries(spec)), 1)
        broken = copy.deepcopy(self.specs["lattice_temple_precinct_hall_0"])
        broken["connectors"][1]["orientation"] = "north_up"
        self.assertTrue(misplaced(broken), "an exit facing into its own piece is caught")

    def test_pools_exist_and_every_attachable_pool_falls_back_to_a_solid_cap(self):
        cap = self.specs["lattice_temple_cap"]
        self.assertEqual("neroruins:latticework", cap["connectors"][0]["final_state"])
        self.assertTrue(all(block_id(f["block"]) == "neroruins:latticework" for f in cap["fills"]))
        for name, spec in self.specs.items():
            for c in exits(spec):
                with self.subTest(template=name, pool=c["pool"]):
                    self.assertIn(c["pool"], self.pools)
                    pool = self.pools[c["pool"]]
                    self.assertEqual("neroruins:lattice_temple_caps", pool["fallback"])
                    self.assertTrue(all(len(entries(self.specs[i])) == 1 for i in pool["items"]))
        self.assertEqual({"neroruins:lattice_temple_caps"},
                         {p for p in self.pools if self.pools[p]["fallback"] == "minecraft:empty"
                          and p.endswith("caps")})

    def test_every_template_is_reachable_from_a_structure(self):
        starts = [self.start_pool(sid) for sid in STRUCTURES]
        for path in (WORLDGEN / "structure").glob("lattice_temple_*satellite_*.json"):
            starts.append(json.loads(path.read_text("utf-8"))["layout"]["start_pool"])
        seen_pools, queue = set(starts), deque(starts)
        while queue:
            for name in self.pools[queue.popleft()]["items"]:
                for c in exits(self.specs[name]):
                    for pool in (c["pool"], self.pools[c["pool"]]["fallback"]):
                        if pool not in seen_pools:
                            seen_pools.add(pool)
                            queue.append(pool)
        reached = {i for p in seen_pools for i in self.pools[p]["items"]}
        self.assertEqual(set(self.specs), reached)
        self.assertEqual(set(self.pools), seen_pools)

    def test_variety_three_per_room_two_per_corridor(self):
        for pool, data in self.pools.items():
            with self.subTest(pool=pool):
                minimum = 1 if pool.endswith("caps") else 2 if ("corridor" in pool or "stair" in pool
                                                                  or "spires" in pool) else 3
                self.assertGreaterEqual(len(data["items"]), minimum)

    # -- budgets

    def worst_case(self, pool, depth, limit, seen=()):
        best = 0
        for name in self.pools[pool]["items"]:
            total = 1
            for c in exits(self.specs[name]):
                total += self.worst_case(c["pool"], depth + 1, limit) if depth < limit else 1
            best = max(best, total)
        return best

    def test_piece_count_and_depth_stay_within_budget(self):
        for sid, (_, pieces, depth_cap) in STRUCTURES.items():
            structure = load_structure(sid)
            with self.subTest(structure=sid):
                depths = [layout["layout"]["size"] for layout in structure["layouts"]]
                self.assertLessEqual(max(depths), depth_cap)
                chain = spine_pools(self.pools, self.specs, self.start_pool(sid))
                self.assertGreaterEqual(min(depths), len(chain) - 1, "every layout reaches the end of its spine")
                worst = self.worst_case(self.start_pool(sid), 0, max(depths))
                self.assertLessEqual(worst, pieces)
        self.assertGreater(self.worst_case("neroruins:lattice_temple_citadel_hubs", 0, 9), 40,
                           "the citadel is not a token structure")

    def test_spine_fits_inside_the_jigsaw_bound_without_overlap(self):
        for sid in STRUCTURES:
            structure = load_structure(sid)
            limit = min(layout["layout"]["max_distance_from_center"] for layout in structure["layouts"])
            chain = spine_pools(self.pools, self.specs, self.start_pool(sid))
            reach = 0
            for placed in spine_layouts(self.pools, self.specs, chain):
                hub = placed[0]
                cx, cz = (hub.lo[0] + hub.hi[0]) / 2, (hub.lo[2] + hub.hi[2]) / 2
                for a, b in itertools.combinations(placed, 2):
                    self.assertFalse(a.overlaps(b), f"{a.spec['name']} overlaps {b.spec['name']}")
                for piece in placed:
                    reach = max(reach, abs(piece.lo[0] - cx), abs(piece.hi[0] - cx),
                                abs(piece.lo[2] - cz), abs(piece.hi[2] - cz))
            # Side branches leave from the hub; add their longest straight chain.
            def longest(pool):
                return max(max(self.specs[i]["size"][0], self.specs[i]["size"][2])
                           + max([longest(c["pool"]) for c in exits(self.specs[i])], default=0)
                           for i in self.pools[pool]["items"])
            longest_side = max(longest(c["pool"]) for i in self.pools[self.start_pool(sid)]["items"]
                               for c in exits(self.specs[i]) if not c.get("priority"))
            with self.subTest(structure=sid, spine_reach=reach):
                self.assertLessEqual(reach, limit)
                hub = self.specs[self.pools[self.start_pool(sid)]["items"][0]]
                self.assertLessEqual(max(hub["size"][0], hub["size"][2]) / 2 + longest_side, limit + 8)

    def test_no_lock_reaches_a_door_or_vault_in_another_spine_piece(self):
        for sid in ("lattice_temple", "lattice_temple_complex"):
            chain = spine_pools(self.pools, self.specs, self.start_pool(sid))
            for placed in spine_layouts(self.pools, self.specs, chain):
                for a, b in itertools.permutations(placed, 2):
                    for lock in lock_positions(a.spec):
                        for target in gated_targets(b.spec):
                            self.assertFalse(within_unlock(a.world(lock), b.world(target)),
                                             f"{a.spec['name']} would open {b.spec['name']}")

    # -- story

    def logs(self, pool):
        variants = [tuple(t["log_id"] for t in self.specs[i].get("terminals", [])) for i in self.pools[pool]["items"]]
        self.assertEqual(1, len(set(variants)), f"{pool} variants disagree on their logs")
        return list(variants[0])

    def check_logs(self, sid, specs):
        chain = spine_pools(self.pools, specs, self.start_pool(sid))
        spine = set(chain)
        placed = []
        for pool in chain:
            placed += self.logs(pool)
        referenced = [c["pool"] for p in self.pools for i in self.pools[p]["items"] for c in exits(specs[i])
                      if c["pool"] in spine]
        every_spine = {p for other in STRUCTURES for p in spine_pools(self.pools, specs, self.start_pool(other))}
        side = [t for p in self.pools if p not in every_spine
                for i in self.pools[p]["items"] for t in specs[i].get("terminals", [])]
        return placed, referenced, chain, side

    def test_spine_places_each_chapter_log_once_in_bible_order(self):
        for sid, (expected, _, _) in STRUCTURES.items():
            with self.subTest(structure=sid):
                placed, referenced, chain, side = self.check_logs(sid, self.specs)
                self.assertEqual(expected, placed)
                for pool in chain[1:]:
                    sources = {p for p in self.pools for i in self.pools[p]["items"]
                               for c in exits(self.specs[i]) if c["pool"] == pool}
                    self.assertEqual(1, len(sources), f"{pool} is reachable from more than one pool")
                self.assertEqual([], side, "unique logs live only on the spine")
        duplicated = copy.deepcopy(self.specs)
        duplicated["lattice_temple_precinct_hall_0"]["terminals"] = [
            {"pos": [9, 1, 5], "log_id": "neroruins:lattice.2", "facing": "south"}]
        self.assertTrue(self.check_logs("lattice_temple", duplicated)[3], "a log copied into a side hall is caught")

    def test_terminals_face_open_floor(self):
        step = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
        for name, spec in self.specs.items():
            grid = cells(spec)
            for terminal in spec.get("terminals", []):
                x, y, z = terminal["pos"]
                dx, dz = step[terminal["facing"]]
                with self.subTest(template=name):
                    self.assertEqual("minecraft:air", grid.get((x + dx, y, z + dz)))

    # -- gating

    def test_vaults_stay_behind_their_doors(self):
        gated = [n for n, s in self.specs.items() if s.get("vaults")]
        self.assertGreaterEqual(len(gated), 15)
        for name in gated:
            spec = self.specs[name]
            grid = cells(spec)
            starts = [tuple(c["pos"]) for c in entries(spec)] or [(x, 1, 0) for x in range(spec["size"][0])]
            with self.subTest(template=name):
                closed = flood(walkable(grid, spec["size"]), starts)
                opened = flood(walkable(grid, spec["size"], open_doors=True), starts)
                for vault in spec["vaults"]:
                    self.assertFalse(touches(closed, tuple(vault["pos"])), "vault reachable past a closed door")
                    self.assertTrue(touches(opened, tuple(vault["pos"])), "vault unreachable once the door opens")
                for lock in lock_positions(spec):
                    self.assertTrue(touches(closed, lock), "the lock must be usable from the near side")
                    for target in gated_targets(spec):
                        self.assertTrue(within_unlock(lock, target), f"{lock} cannot open {target}")
        leaky = copy.deepcopy(self.specs["lattice_temple_citadel_gate_0"])
        leaky["fills"].append({"from": [2, 1, 12], "to": [2, 2, 12], "block": "minecraft:air"})
        grid = cells(leaky)
        reach = flood(walkable(grid, leaky["size"]), [tuple(entries(leaky)[0]["pos"])])
        self.assertTrue(touches(reach, tuple(leaky["vaults"][0]["pos"])), "a hole in the gate wall is caught")

    def test_puzzle_clues_share_a_piece_with_their_lock(self):
        for name, spec in self.specs.items():
            ids = [block_id(f["block"]) for f in spec["fills"]]
            with self.subTest(template=name):
                if "neroruins:glyph_lock" in ids:
                    self.assertEqual(4, ids.count("neroruins:glyph_clue"))
                if "neroruins:resonance_pylon" in ids:
                    pylons = [tuple(f["from"]) for f in spec["fills"] if block_id(f["block"]) == "neroruins:resonance_pylon"]
                    clues = {tuple(f["from"]) for f in spec["fills"] if block_id(f["block"]) == "neroruins:resonance_clue"}
                    self.assertEqual(4, len(pylons))
                    for p in pylons:
                        self.assertIn((p[0], p[1] + 1, p[2]), clues)
                        self.assertTrue(all(abs(p[0] - q[0]) <= PYLON_REACH and abs(p[2] - q[2]) <= PYLON_REACH
                                            for q in pylons), "every pylon must see the whole chord")

    def guardian_blocks_path(self, spec, goal):
        grid = cells(spec)
        stand = walkable(grid, spec["size"])
        starts = [tuple(c["pos"]) for c in entries(spec)] or [
            p for p in stand if p[2] == 0 or p[0] in (0, spec["size"][0] - 1)]
        return not touches(flood(stand, starts, near_spawner(spec)), goal)

    def test_gates_are_only_reached_through_a_guardian_zone(self):
        for sid in ("lattice_temple", "lattice_temple_complex", "lattice_temple_cindara"):
            chain = spine_pools(self.pools, self.specs, self.start_pool(sid))
            gate_index = next(i for i, p in enumerate(chain) if p.endswith("_gates"))
            guardians = chain[gate_index - 1]
            self.assertTrue(guardians.endswith("_guardians"), sid)
            for name in self.pools[guardians]["items"]:
                spec = self.specs[name]
                exit_cell = tuple(exits(spec)[0]["pos"])
                with self.subTest(template=name):
                    self.assertTrue(self.guardian_blocks_path(spec, exit_cell))
                    unguarded = copy.deepcopy(spec)
                    unguarded["spawners"] = []
                    self.assertFalse(self.guardian_blocks_path(unguarded, exit_cell))
        for name in self.pools["neroruins:lattice_temple_outpost_hubs"]["items"]:
            spec = self.specs[name]
            with self.subTest(template=name):
                self.assertTrue(self.guardian_blocks_path(spec, lock_positions(spec)[0]))

    def test_sanctum_needs_the_chapter_eight_relic_and_rewards_the_heart(self):
        for name in self.pools["neroruins:lattice_temple_citadel_finals"]["items"]:
            sockets = [f["block"]["properties"]["chapter"] for f in self.specs[name]["fills"]
                       if block_id(f["block"]) == "neroruins:relic_socket"]
            self.assertEqual(["8"], sockets)
        sanctum = json.loads((DATA / "loot_table/chests/lattice_temple_sanctum.json").read_text("utf-8"))
        nyxara = json.loads((DATA / "loot_table/chests/lattice_temple_nyxara_vault.json").read_text("utf-8"))
        self.assertIn("neroruins:resonance_heart", json.dumps(sanctum))
        self.assertIn("neroruins:twilight_prism", json.dumps(nyxara))

    # -- decision 039: depth, bounds and sealed gated rooms

    def citadel_depths(self, specs):
        chain = spine_pools(self.pools, specs, self.start_pool("lattice_temple"))
        depths = []
        for placed in spine_layouts(self.pools, specs, chain):
            plaza = placed[0].offset[1] + CITADEL_PLAZA
            depths.append(plaza - placed[-1].offset[1])
        return depths

    def test_citadel_final_room_lies_at_least_seventy_blocks_below_the_plaza(self):
        depths = self.citadel_depths(self.specs)
        self.assertGreaterEqual(min(depths), MIN_CITADEL_DEPTH)
        shallow = copy.deepcopy(self.specs)
        for pool, face in (("neroruins:lattice_temple_citadel_approaches", "w"),
                           ("neroruins:lattice_temple_citadel_deep_stairs", "n")):
            for name in self.pools[pool]["items"]:
                old = shallow[name]
                well = spiral_well(name, 0, None, exits(old)[0]["pool"].split(":", 1)[1], face,
                                   16 if face == "w" else 22).spec()
                shallow[name] = well
        self.assertLess(min(self.citadel_depths(shallow)), MIN_CITADEL_DEPTH, "shallow wells are caught")
        floor = load_structure("lattice_temple")["ground_check"]["min_depth"]
        self.assertGreaterEqual(floor, max(depths) + 16, "the ground check keeps the sanctum above the world floor")

    def vertical_reach(self, specs):
        chain = spine_pools(self.pools, specs, self.start_pool("lattice_temple"))
        reach = 0
        for placed in spine_layouts(self.pools, specs, chain):
            start = placed[0].offset[1]
            reach = max([reach] + [max(abs(p.lo[1] - start), abs(p.hi[1] - start)) for p in placed])
        return reach

    def test_spiral_descent_stays_inside_the_vertical_jigsaw_bound(self):
        limit = min(layout["layout"]["max_distance_from_center"]
                    for layout in load_structure("lattice_temple")["layouts"])
        self.assertLessEqual(self.vertical_reach(self.specs), limit)
        deep = copy.deepcopy(self.specs)
        for name in self.pools["neroruins:lattice_temple_citadel_deep_stairs"]["items"]:
            deep[name] = spiral_well(name, 0, None, "lattice_temple_citadel_guardians", "n", 118).spec()
        self.assertGreater(self.vertical_reach(deep), limit, "a well past the bound is caught")

    def gated_pools(self):
        gated, queue = set(), deque(p for p in self.pools if p.endswith("_gates"))
        while queue:
            for name in self.pools[queue.popleft()]["items"]:
                for c in exits(self.specs[name]):
                    if c.get("priority") and c["pool"] not in gated:
                        gated.add(c["pool"])
                        queue.append(c["pool"])
        return gated

    def breached(self, spec, gated_entry, gated_pools):
        """Cells of the gated rooms a player tunnelling in from outside the piece could reach."""
        w, h, d = spec["size"]
        grid = cells(spec)
        inside = lambda p: 0 <= p[0] < w and 0 <= p[1] < h and 0 <= p[2] < d
        sides = ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))
        around = lambda p: [(p[0] + a, p[1] + b, p[2] + c) for a, b, c in sides]
        openings = set()
        for c in spec.get("connectors", []):
            if (c["name"] == ENTRY and gated_entry) or (c["name"] != ENTRY and c["pool"] in gated_pools):
                x, y, z = c["pos"]
                for a in (-1, 0, 1):
                    for dy in range(4):
                        openings.add({"north_up": (x + a, y + dy, 0), "south_up": (x + a, y + dy, d - 1),
                                      "west_up": (0, y + dy, z + a), "east_up": (w - 1, y + dy, z + a)}[c["orientation"]])
        air = {p for p, b in grid.items() if b == "minecraft:air"}
        on_face = lambda p: p[0] in (0, w - 1) or p[1] in (0, h - 1) or p[2] in (0, d - 1)
        open_air = set(p for p in air if on_face(p) and p not in openings)
        queue = deque(open_air)
        while queue:
            for n in around(queue.popleft()):
                if n in air and n not in open_air and inside(n):
                    open_air.add(n)
                    queue.append(n)
        gated = air - open_air
        if not gated:
            return None
        outside = [(x, y, z) for x in range(-1, w + 1) for y in range(-1, h + 1) for z in range(-1, d + 1)
                   if not inside((x, y, z))]
        seen, queue = set(outside), deque(outside)
        while queue:
            for n in around(queue.popleft()):
                if n in seen or not inside(n) or n in openings or grid.get(n) in UNBREAKABLE:
                    continue
                seen.add(n)
                queue.append(n)
        return seen & gated

    def test_gated_rooms_are_sealed_by_unbreakable_blocks(self):
        gated_pools = self.gated_pools()
        self.assertEqual({"neroruins:lattice_temple_citadel_organs", "neroruins:lattice_temple_citadel_finals"},
                         gated_pools)
        checked = 0
        for pool, data in self.pools.items():
            for name in data["items"]:
                spec = self.specs[name]
                if not (spec.get("vaults") or pool in gated_pools):
                    continue
                with self.subTest(template=name):
                    breach = self.breached(spec, pool in gated_pools, gated_pools)
                    self.assertIsNotNone(breach, "a gated piece has gated rooms")
                    self.assertEqual(set(), breach)
                    checked += 1
        self.assertGreaterEqual(checked, 17)
        # Failure case: one ordinary Latticework block in the organ's sealed wall opens a tunnel.
        organ = copy.deepcopy(self.specs["lattice_temple_citadel_organ_0"])
        organ["fills"].append({"from": [0, 5, 20], "to": [0, 5, 20], "block": "neroruins:latticework"})
        self.assertTrue(self.breached(organ, True, gated_pools), "a mineable block in the shell is caught")
        gate = copy.deepcopy(self.specs["lattice_temple_citadel_gate_0"])
        gate["fills"].append({"from": [6, 0, 18], "to": [6, 0, 18], "block": "neroruins:lattice_floor"})
        self.assertTrue(self.breached(gate, False, gated_pools), "a mineable floor block under the vault room is caught")

    def test_no_window_or_glass_lets_light_or_pearls_into_a_gated_room(self):
        for name, spec in self.specs.items():
            ids = {block_id(f["block"]) for f in spec["fills"]}
            with self.subTest(template=name):
                self.assertFalse({i for i in ids if "glass" in i or "bars" in i})

    # -- placement data

    def test_satellites_sit_outside_the_hub_bound_and_share_radii(self):
        for sid in STRUCTURES:
            hub = load_structure(sid)
            bound = max(layout["layout"]["max_distance_from_center"] for layout in hub["layouts"])
            sats = [json.loads(p.read_text("utf-8")) for p in (WORLDGEN / "structure").glob(f"{sid}_satellite_*.json")]
            with self.subTest(structure=sid):
                self.assertTrue(sats)
                radii = {(s["placement"]["min_radius"], s["placement"]["max_radius"]) for s in sats}
                self.assertEqual(1, len(radii), "slots of one hub share radii so their positions stay distinct")
                min_radius = radii.pop()[0]
                self.assertGreater(min_radius * 16, bound)
                self.assertEqual(sorted(s["placement"]["index"] for s in sats), list(range(len(sats))))
                for s in sats:
                    self.assertEqual("neroruins:" + sid, s["placement"]["hub_set"])
                    set_file = json.loads((WORLDGEN / "structure_set" / f"{sid}_satellite_{s['placement']['index']}.json")
                                          .read_text("utf-8"))
                    self.assertEqual(s["placement"], {k: v for k, v in set_file["placement"].items() if k != "type"})


if __name__ == "__main__":
    unittest.main()
