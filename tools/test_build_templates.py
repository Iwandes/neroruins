import copy
import gzip
import json
import struct
import pathlib
import tempfile
import unittest
from collections import deque

from build_templates import build_template, DESTINATION, SPEC_DIRECTORY, ROOT


def reachable_passages(spec):
    blocks = {}
    for fill in spec["fills"]:
        start, end = fill["from"], fill["to"]
        for x in range(start[0], end[0] + 1):
            for y in range(start[1], end[1] + 1):
                for z in range(start[2], end[2] + 1):
                    blocks[(x, y, z)] = fill["block"]
    for connector in spec["connectors"]:
        blocks[tuple(connector["pos"])] = connector["final_state"]
    for container in spec.get("containers", []):
        blocks[tuple(container["pos"])] = "minecraft:chest"
    for terminal in spec.get("terminals", []):
        blocks[tuple(terminal["pos"])] = "neroruins:log_terminal"
    start = tuple(spec["connectors"][0]["pos"])
    if blocks.get(start) != "minecraft:air":
        return set()
    visited = {start}
    queue = deque([start])
    while queue:
        x, y, z = queue.popleft()
        for pos in ((x - 1, y, z), (x + 1, y, z), (x, y, z - 1), (x, y, z + 1)):
            if pos not in visited and blocks.get(pos) == "minecraft:air" and blocks.get((pos[0], y + 1, pos[2])) == "minecraft:air":
                visited.add(pos)
                queue.append(pos)
    return visited


def connected_passages(spec):
    visited = reachable_passages(spec)
    return all(tuple(connector["pos"]) in visited for connector in spec["connectors"])


class TemplateBuilderTest(unittest.TestCase):
    def setUp(self):
        self.spec = {
            "size": [3, 1, 3],
            "fills": [{"from": [0, 0, 0], "to": [2, 0, 2], "block": "minecraft:stone"}],
            "connectors": [{"pos": [2, 0, 1], "orientation": "east_up", "name": "neroruins:in",
                            "target": "neroruins:out", "pool": "neroruins:end", "final_state": "minecraft:air"}],
        }

    def test_build_is_deterministic_and_contains_connector(self):
        result = build_template(self.spec)
        self.assertEqual(result, build_template(self.spec))
        self.assertIn(b"neroruins:out", gzip.decompress(result))

    def test_rejects_out_of_bounds_connectors_and_invalid_resources(self):
        invalid = copy.deepcopy(self.spec)
        invalid["connectors"][0]["pos"] = [3, 0, 1]
        with self.assertRaises(ValueError):
            build_template(invalid)
        invalid = copy.deepcopy(self.spec)
        invalid["fills"][0]["block"] = "bad block"
        with self.assertRaises(ValueError):
            build_template(invalid)
        invalid = copy.deepcopy(self.spec)
        invalid["connectors"].append(copy.deepcopy(invalid["connectors"][0]))
        with self.assertRaises(ValueError):
            build_template(invalid)

    def test_supports_block_state_properties(self):
        self.spec["fills"][0]["block"] = {"id": "minecraft:oak_log", "properties": {"axis": "x"}}
        self.spec["connectors"][0]["final_state"] = "minecraft:oak_log[axis=x]"
        result = gzip.decompress(build_template(self.spec))
        self.assertIn(b"Properties", result)
        self.assertIn(b"minecraft:oak_log[axis=x]", result)

    def test_loot_container_is_encoded_and_rejects_collisions(self):
        self.spec["containers"] = [{"pos": [1, 0, 1], "loot_table": "neroruins:chests/bunker_logs"}]
        result = gzip.decompress(build_template(self.spec))
        self.assertIn(b"minecraft:chest", result)
        self.assertIn(b"neroruins:chests/bunker_logs", result)
        invalid = copy.deepcopy(self.spec)
        invalid["containers"][0]["loot_table"] = "invalid id"
        with self.assertRaises(ValueError):
            build_template(invalid)
        invalid = copy.deepcopy(self.spec)
        invalid["containers"][0]["pos"] = [2, 0, 1]
        with self.assertRaises(ValueError):
            build_template(invalid)

    def test_checked_in_templates_match_descriptions(self):
        for path in sorted(SPEC_DIRECTORY.glob("*.json")):
            for spec in json.loads(path.read_text(encoding="utf-8"))["templates"]:
                with self.subTest(name=spec["name"]):
                    actual = (DESTINATION / (spec["name"] + ".nbt")).read_bytes()
                    self.assertEqual(gzip.decompress(build_template(spec)), gzip.decompress(actual))

    def test_terminal_log_is_encoded_and_invalid_or_colliding_entries_fail(self):
        self.spec["terminals"] = [{"pos": [1, 0, 1], "log_id": "neroruins:earth.2"}]
        result = gzip.decompress(build_template(self.spec))
        self.assertIn(b"neroruins:log_terminal", result)
        self.assertIn(b"neroruins:earth.2", result)
        invalid = copy.deepcopy(self.spec)
        invalid["terminals"][0]["log_id"] = "neroruins:earth.7"
        with self.assertRaises(ValueError):
            build_template(invalid)
        invalid = copy.deepcopy(self.spec)
        invalid["terminals"][0]["pos"] = [2, 0, 1]
        with self.assertRaises(ValueError):
            build_template(invalid)

    def test_bunker_connectors_have_two_block_tall_passages(self):
        path = SPEC_DIRECTORY / "bunker.json"
        specs = json.loads(path.read_text(encoding="utf-8"))["templates"]
        for spec in specs:
            if spec["name"].endswith("_cap"):
                continue
            blocks = {}
            for fill in spec["fills"]:
                start, end = fill["from"], fill["to"]
                for x in range(start[0], end[0] + 1):
                    for y in range(start[1], end[1] + 1):
                        for z in range(start[2], end[2] + 1):
                            blocks[(x, y, z)] = fill["block"]
            for connector in spec["connectors"]:
                x, y, z = connector["pos"]
                with self.subTest(template=spec["name"], connector=connector["pos"]):
                    self.assertEqual("minecraft:air", connector["final_state"])
                    self.assertEqual("minecraft:air", blocks.get((x, y + 1, z)))

    def test_bunker_corridors_connect_each_exit_and_detect_a_blocked_path(self):
        specs = json.loads((SPEC_DIRECTORY / "bunker.json").read_text(encoding="utf-8"))["templates"]
        corridors = [spec for spec in specs if spec["name"].startswith("bunker_corridor_")
                     and not spec["name"].endswith("_cap")]
        self.assertGreaterEqual(len(corridors), 4)
        for corridor in corridors:
            with self.subTest(corridor=corridor["name"]):
                self.assertTrue(connected_passages(corridor))
        blocked = copy.deepcopy(next(s for s in corridors if s["name"] == "bunker_corridor_short"))
        blocked["fills"].append({"from": [1, 1, 1], "to": [1, 2, 3], "block": "minecraft:stone"})
        self.assertFalse(connected_passages(blocked))

    def test_bunker_turns_offer_both_directions(self):
        specs = json.loads((SPEC_DIRECTORY / "bunker.json").read_text(encoding="utf-8"))["templates"]
        turns = [spec for spec in specs if spec["name"] in
                 ("bunker_corridor_turn", "bunker_corridor_turn_right")]

        def directions(pieces):
            return {connector["orientation"] for piece in pieces for connector in piece["connectors"]
                    if connector["pool"] == "neroruins:bunker_rooms"}

        self.assertEqual({"north_up", "south_up"}, directions(turns))
        self.assertNotEqual({"north_up", "south_up"}, directions(turns[:1]))

    def test_junction_has_clearance_for_two_adjacent_rooms(self):
        specs = json.loads((SPEC_DIRECTORY / "bunker.json").read_text(encoding="utf-8"))["templates"]
        junction = next(spec for spec in specs if spec["name"] == "bunker_corridor_junction")
        rooms = [spec for spec in specs if spec["name"].startswith("bunker_room_")
                 and not spec["name"].endswith("_cap")]
        max_width = max(max(room["size"][0], room["size"][2]) for room in rooms)
        self.assertGreaterEqual(junction["size"][0], max_width + 2)
        self.assertGreaterEqual(junction["size"][2], max_width + 2)

    def test_bunker_hubs_have_supported_reachable_ladders(self):
        specs = json.loads((SPEC_DIRECTORY / "bunker.json").read_text(encoding="utf-8"))["templates"]
        hubs = [spec for spec in specs if spec["name"].startswith("bunker_hub_")]

        def has_accessible_shaft(hub):
            blocks = {}
            for fill in hub["fills"]:
                start, end = fill["from"], fill["to"]
                for x in range(start[0], end[0] + 1):
                    for y in range(start[1], end[1] + 1):
                        for z in range(start[2], end[2] + 1):
                            blocks[x, y, z] = fill["block"]
            ladder = {"id": "minecraft:ladder", "properties": {"facing": "north"}}
            support_blocks = {"neroruins:survey_hull", "neroruins:rusted_survey_hull", "minecraft:mossy_stone_bricks"}
            if any(blocks.get((4, y, 4)) != ladder or blocks.get((4, y, 5)) not in support_blocks
                   for y in range(1, hub["size"][1])):
                return False
            return any(pos in reachable_passages(hub) for pos in ((3, 1, 4), (5, 1, 4), (4, 1, 3)))

        self.assertTrue(hubs)
        for hub in hubs:
            with self.subTest(hub=hub["name"]):
                self.assertTrue(has_accessible_shaft(hub))
        broken = copy.deepcopy(hubs[0])
        broken["fills"].append({"from": [4, 5, 5], "to": [4, 5, 5], "block": "minecraft:air"})
        self.assertFalse(has_accessible_shaft(broken))

    def test_bunker_hatch_reaches_the_surface_from_the_sunk_hub(self):
        specs = json.loads((SPEC_DIRECTORY / "bunker.json").read_text(encoding="utf-8"))["templates"]
        hubs = [spec for spec in specs if spec["name"].startswith("bunker_hub_")]
        structures = ROOT / "common" / "src" / "main" / "resources" / "data" / "neroruins" / "worldgen" / "structure"

        def hatch_clears_ground(hub_height, depth):
            return hub_height - depth >= 1

        for name in ("bunker", "bunker_earth"):
            layouts = json.loads((structures / (name + ".json")).read_text(encoding="utf-8"))["layouts"]
            for layout in layouts:
                depth = -layout["layout"]["start_height"]["absolute"]
                for hub in hubs:
                    with self.subTest(structure=name, hub=hub["name"]):
                        self.assertTrue(hatch_clears_ground(hub["size"][1], depth))
        self.assertFalse(hatch_clears_ground(10, 10), "a shaft as tall as the hub is deep ends underground")

    def test_archive_terminal_is_reachable_from_the_entrance(self):
        specs = json.loads((SPEC_DIRECTORY / "bunker.json").read_text(encoding="utf-8"))["templates"]
        room = next(spec for spec in specs if spec["name"] == "bunker_room_archive")
        blocks = {}
        for fill in room["fills"]:
            start, end = fill["from"], fill["to"]
            for x in range(start[0], end[0] + 1):
                for y in range(start[1], end[1] + 1):
                    for z in range(start[2], end[2] + 1):
                        blocks[x, y, z] = fill["block"]
        for container in room["containers"]:
            blocks[tuple(container["pos"])] = "minecraft:chest"
        terminal = tuple(room["terminals"][0]["pos"])
        blocks[terminal] = "neroruins:log_terminal"
        entrance = tuple(room["connectors"][0]["pos"])
        queue, seen = deque([entrance]), {entrance}
        while queue:
            x, y, z = queue.popleft()
            for candidate in ((x - 1, y, z), (x + 1, y, z), (x, y, z - 1), (x, y, z + 1)):
                if candidate not in seen and blocks.get(candidate) == "minecraft:air" \
                        and blocks.get((candidate[0], y + 1, candidate[2])) == "minecraft:air":
                    seen.add(candidate)
                    queue.append(candidate)
        x, y, z = terminal
        self.assertTrue(any(pos in seen for pos in ((x - 1, y, z), (x + 1, y, z),
                                                    (x, y, z - 1), (x, y, z + 1))))

    def test_each_room_has_a_walkable_route_to_its_chest(self):
        specs = json.loads((SPEC_DIRECTORY / "bunker.json").read_text(encoding="utf-8"))["templates"]
        rooms = [spec for spec in specs if spec["name"].startswith("bunker_room_")
                 and not spec["name"].endswith("_cap")]
        for room in rooms:
            with self.subTest(room=room["name"]):
                reachable = reachable_passages(room)
                x, y, z = room["containers"][0]["pos"]
                self.assertTrue(any(pos in reachable for pos in ((x - 1, y, z), (x + 1, y, z),
                                                                 (x, y, z - 1), (x, y, z + 1))))
        blocked = copy.deepcopy(rooms[0])
        blocked["fills"].append({"from": [1, 1, 1], "to": [1, 2, 7], "block": "minecraft:stone"})
        reachable = reachable_passages(blocked)
        x, y, z = blocked["containers"][0]["pos"]
        self.assertFalse(any(pos in reachable for pos in ((x - 1, y, z), (x + 1, y, z),
                                                           (x, y, z - 1), (x, y, z + 1))))

    def test_room_chest_lids_have_clearance(self):
        specs = json.loads((SPEC_DIRECTORY / "bunker.json").read_text(encoding="utf-8"))["templates"]
        rooms = [spec for spec in specs if spec["name"].startswith("bunker_room_")
                 and not spec["name"].endswith("_cap")]

        def lid_is_clear(room):
            x, y, z = room["containers"][0]["pos"]
            above = (x, y + 1, z)
            block = None
            for fill in room["fills"]:
                if all(fill["from"][i] <= above[i] <= fill["to"][i] for i in range(3)):
                    block = fill["block"]
            return block == "minecraft:air"

        for room in rooms:
            with self.subTest(room=room["name"]):
                self.assertTrue(lid_is_clear(room))
        blocked = copy.deepcopy(rooms[0])
        x, y, z = blocked["containers"][0]["pos"]
        blocked["fills"].append({"from": [x, y + 1, z], "to": [x, y + 1, z], "block": "minecraft:stone"})
        self.assertFalse(lid_is_clear(blocked))

    def test_unattached_bunker_exits_have_matching_solid_caps(self):
        specs = json.loads((SPEC_DIRECTORY / "bunker.json").read_text(encoding="utf-8"))["templates"]
        pools = ROOT / "common/src/main/resources/data/neroruins/worldgen/template_pool"
        for role in ("corridor", "room"):
            with self.subTest(role=role):
                cap = next(spec for spec in specs if spec["name"] == "bunker_" + role + "_cap")
                connector = cap["connectors"][0]
                self.assertEqual("neroruins:bunker_" + role, connector["name"])
                self.assertEqual("neroruins:survey_hull", connector["final_state"])
                self.assertEqual([1, 2, 1], cap["size"])
                fallback = json.loads((pools / ("bunker_" + role + "s.json")).read_text())["fallback"]
                self.assertEqual("neroruins:bunker_" + role + "_caps", fallback)

    def test_bunker_pools_offer_compatible_pieces_and_fallbacks(self):
        specs = json.loads((SPEC_DIRECTORY / "bunker.json").read_text(encoding="utf-8"))["templates"]
        templates = {"neroruins:" + spec["name"]: spec for spec in specs}
        directory = ROOT / "common/src/main/resources/data/neroruins/worldgen/template_pool"

        def compatible(connector, pool):
            if not pool["elements"]:
                return False
            for weighted in pool["elements"]:
                candidate = templates.get(weighted["element"]["location"])
                if (weighted["weight"] <= 0 or candidate is None
                        or not any(entry["name"] == connector["target"]
                                   and entry["orientation"].endswith("_up")
                                   for entry in candidate["connectors"])):
                    return False
            return True

        for spec in specs:
            for connector in spec["connectors"]:
                if connector["pool"] == "minecraft:empty":
                    continue
                with self.subTest(template=spec["name"], position=connector["pos"]):
                    pool_name = connector["pool"].split(":", 1)[1]
                    pool = json.loads((directory / (pool_name + ".json")).read_text())
                    fallback = json.loads((directory / (pool["fallback"].split(":", 1)[1] + ".json")).read_text())
                    self.assertTrue(compatible(connector, pool))
                    self.assertTrue(compatible(connector, fallback))
                    broken = copy.deepcopy(pool)
                    broken["elements"][0]["element"]["location"] = "neroruins:missing_piece"
                    self.assertFalse(compatible(connector, broken))

    def test_all_bunker_variants_are_reachable_from_a_hub(self):
        specs = json.loads((SPEC_DIRECTORY / "bunker.json").read_text(encoding="utf-8"))["templates"]
        templates = {"neroruins:" + spec["name"]: spec for spec in specs}
        directory = ROOT / "common/src/main/resources/data/neroruins/worldgen/template_pool"
        pools = {}
        for path in directory.glob("bunker_*.json"):
            pools["neroruins:" + path.stem] = json.loads(path.read_text())

        def reachable(pool_map):
            visited = {name for name in templates if name.startswith("neroruins:bunker_hub_")}
            pending = list(visited)
            while pending:
                for connector in templates[pending.pop()]["connectors"]:
                    pool_name = connector["pool"]
                    seen_pools = set()
                    while pool_name != "minecraft:empty":
                        if pool_name in seen_pools:
                            raise ValueError("Cyclic bunker pool fallback")
                        seen_pools.add(pool_name)
                        pool = pool_map[pool_name]
                        for entry in pool["elements"]:
                            name = entry["element"]["location"]
                            if name not in visited:
                                visited.add(name)
                                pending.append(name)
                        pool_name = pool["fallback"]
            return visited

        self.assertEqual(set(templates), reachable(pools))
        broken = copy.deepcopy(pools)
        broken["neroruins:bunker_rooms"]["elements"] = [
            entry for entry in broken["neroruins:bunker_rooms"]["elements"]
            if entry["element"]["location"] != "neroruins:bunker_room_archive"
        ]
        self.assertNotIn("neroruins:bunker_room_archive", reachable(broken))
        broken = copy.deepcopy(pools)
        broken["neroruins:bunker_rooms"]["fallback"] = "neroruins:bunker_rooms"
        with self.assertRaises(ValueError):
            reachable(broken)

    def test_bunker_hull_variants_have_models_loot_and_tool_tag(self):
        resources = ROOT / "common/src/main/resources"
        specs = json.loads((SPEC_DIRECTORY / "bunker.json").read_text(encoding="utf-8"))["templates"]
        used = {fill["block"] for spec in specs for fill in spec["fills"] if isinstance(fill["block"], str)}
        tool_tag = json.loads((resources / "data/minecraft/tags/block/mineable/pickaxe.json").read_text())
        language = json.loads((resources / "assets/neroruins/lang/en_us.json").read_text(encoding="utf-8"))
        for name in ("survey_hull", "rusted_survey_hull"):
            with self.subTest(block=name):
                self.assertIn("neroruins:" + name, used)
                self.assertIn("neroruins:" + name, tool_tag["values"])
                self.assertTrue(language["block.neroruins." + name])
                self.assertEqual(language["block.neroruins." + name], language["item.neroruins." + name])
                model = json.loads((resources / "assets/neroruins/models/block" / (name + ".json")).read_text())
                self.assertEqual("neroruins:block/" + name, model["textures"]["all"])
                self.assertTrue((resources / "assets/neroruins/textures/block" / (name + ".png")).is_file())
                loot = json.loads((resources / "data/neroruins/loot_table/blocks" / (name + ".json")).read_text())
                self.assertEqual("neroruins:" + name, loot["pools"][0]["entries"][0]["name"])

    def test_data_shard_has_its_own_pixel_texture(self):
        assets = ROOT / "common/src/main/resources/assets/neroruins"
        model = json.loads((assets / "models/item/data_shard.json").read_text(encoding="utf-8"))
        self.assertEqual("neroruins:item/data_shard", model["textures"]["layer0"])
        path = assets / "textures/item/data_shard.png"
        data = path.read_bytes()
        self.assertEqual(b"\x89PNG\r\n\x1a\n", data[:8])
        width, height = struct.unpack(">II", data[16:24])
        self.assertEqual(16, width)
        self.assertEqual(0, height % 16, "an animated texture is a vertical strip of 16x16 frames")
        if height > 16:
            meta = json.loads((path.parent / (path.name + ".mcmeta")).read_text())
            self.assertGreater(meta["animation"]["frametime"], 0)

    def test_terminal_assets_and_both_loot_formats_exist(self):
        resources = ROOT / "common/src/main/resources"
        assets = resources / "assets/neroruins"
        language = json.loads((assets / "lang/en_us.json").read_text(encoding="utf-8"))
        self.assertEqual(language["block.neroruins.log_terminal"], language["item.neroruins.log_terminal"])
        model = json.loads((assets / "models/block/log_terminal.json").read_text())
        self.assertEqual("minecraft:block/orientable_with_bottom", model["parent"])
        expected = {"front": "log_terminal_side", "top": "log_terminal_top", "bottom": "log_terminal_bottom",
                    "side": "survey_hull"}
        for face, name in expected.items():
            self.assertEqual("neroruins:block/" + name, model["textures"][face])
            self.assertTrue((assets / "textures/block" / (name + ".png")).is_file())
        states = json.loads((assets / "blockstates/log_terminal.json").read_text())["variants"]
        self.assertEqual({"facing=north", "facing=east", "facing=south", "facing=west"}, set(states))
        meta = json.loads((assets / "textures/block/log_terminal_side.png.mcmeta").read_text())
        self.assertGreater(meta["animation"]["frametime"], 0)
        old_loot = json.loads((resources / "data/neroruins/loot_table/blocks/log_terminal.json").read_text())
        modern_loot = json.loads((ROOT / "common/src/main/resources-26.3/data/neroruins/loot_table/blocks/log_terminal.json").read_text())
        self.assertEqual("minecraft:survives_explosion", old_loot["pools"][0]["conditions"][0]["condition"])
        self.assertEqual("minecraft:survives_explosion", modern_loot["pools"][0]["condition"]["type"])

    def test_each_bunker_room_can_yield_an_earth_log(self):
        resources = ROOT / "common/src/main/resources"
        specs = json.loads((SPEC_DIRECTORY / "bunker.json").read_text(encoding="utf-8"))["templates"]
        rooms = [spec for spec in specs if spec["name"].startswith("bunker_room_")
                 and not spec["name"].endswith("_cap")]
        self.assertTrue(rooms)
        for room in rooms:
            self.assertEqual("neroruins:chests/bunker_logs", room["containers"][0]["loot_table"])
        loot = json.loads((resources / "data/neroruins/loot_table/chests/bunker_logs.json").read_text())
        entries = loot["pools"][0]["entries"]
        self.assertEqual({"neroruins:earth." + str(n) for n in range(1, 7)}, {
            entry["functions"][0]["components"]["neroruins:log_id"] for entry in entries
        })
        self.assertTrue(all(entry["name"] == "neroruins:data_shard" for entry in entries))
        newer = ROOT / "common/src/main/resources-26.3/data/neroruins/loot_table"
        modern_loot = json.loads((newer / "chests/bunker_logs.json").read_text())
        modern_entries = modern_loot["pools"][0]["entries"]
        self.assertEqual({entry["functions"][0]["components"]["neroruins:log_id"] for entry in entries}, {
            entry["modifier"]["components"]["neroruins:log_id"] for entry in modern_entries
        })
        for name in ("survey_hull", "rusted_survey_hull"):
            modern_block = json.loads((newer / "blocks" / (name + ".json")).read_text())
            self.assertEqual("minecraft:survives_explosion", modern_block["pools"][0]["condition"]["type"])

    def test_bunker_salvage_has_core_only_fallback_and_optional_sibling_entries(self):
        resources = ROOT / "common/src/main/resources"
        tag = json.loads((resources / "data/neroruins/tags/item/loot/salvage_metal.json").read_text())

        def valid_tag(values):
            return ("minecraft:iron_nugget" in values
                    and all(isinstance(value, str) and value.startswith("minecraft:")
                            or isinstance(value, dict) and value.get("required") is False
                            for value in values))

        self.assertTrue(valid_tag(tag["values"]))
        self.assertFalse(valid_tag(["minecraft:iron_nugget", {"id": "missing:metal", "required": True}]))
        self.assertIn({"id": "nerospace:alien_tech_scrap", "required": False}, tag["values"])
        for base, tag_field in ((resources, "name"), (ROOT / "common/src/main/resources-26.3", "items")):
            table = json.loads((base / "data/neroruins/loot_table/chests/bunker_logs.json").read_text())
            salvage = table["pools"][1]["entries"]
            tag_name = "neroruins:loot/salvage_metal"
            if tag_field == "items":
                tag_name = "#" + tag_name
            self.assertIn({"type": "minecraft:tag", tag_field: tag_name, "expand": True}, salvage)
            self.assertIn({"type": "minecraft:item", "name": "neroruins:survey_hull"}, salvage)


    def test_sealed_vault_is_encoded_and_rejects_bad_input(self):
        self.spec["vaults"] = [{"pos": [1, 0, 1], "loot_table": "neroruins:chests/bunker_vault",
                                "structure": "neroruins:bunker"}]
        data = gzip.decompress(build_template(self.spec))
        self.assertIn(b"neroruins:vault_cache", data)
        self.assertIn(b"puzzle", data)
        self.assertIn(b"neroruins:bunker", data)
        for key, value in (("loot_table", "bad id"), ("structure", "Bad:Id")):
            broken = copy.deepcopy(self.spec)
            broken["vaults"][0][key] = value
            with self.assertRaises(ValueError):
                build_template(broken)
        colliding = copy.deepcopy(self.spec)
        colliding["containers"] = [{"pos": [1, 0, 1], "loot_table": "neroruins:chests/bunker_logs"}]
        with self.assertRaises(ValueError):
            build_template(colliding)

    def test_every_bunker_hub_has_one_sealed_vault_with_nearby_plates(self):
        specs = json.loads((SPEC_DIRECTORY / "bunker.json").read_text(encoding="utf-8"))["templates"]

        def gated(spec):
            vaults = spec.get("vaults", [])
            if len(vaults) != 1:
                return False
            vx, vy, vz = vaults[0]["pos"]
            plates = [fill["from"] for fill in spec["fills"] if fill["block"] == "neroruins:ballast_plate"]
            if not 1 <= len(plates) <= 8:
                return False
            return all(abs(x - vx) <= 6 and abs(y - vy) <= 3 and abs(z - vz) <= 6 for x, y, z in plates)

        hubs = [spec for spec in specs if spec["name"].startswith("bunker_hub_")]
        self.assertEqual(3, len(hubs))
        for hub in hubs:
            with self.subTest(hub=hub["name"]):
                self.assertTrue(gated(hub))
                floor = {tuple(fill["from"]) for fill in hub["fills"] if fill["block"] == "neroruins:ballast_plate"}
                self.assertEqual(len(floor), len([f for f in hub["fills"] if f["block"] == "neroruins:ballast_plate"]))
        far = copy.deepcopy(hubs[0])
        for fill in far["fills"]:
            if fill["block"] == "neroruins:ballast_plate":
                fill["from"] = fill["to"] = [8, 9, 8]
        self.assertFalse(gated(far))
        ungated = copy.deepcopy(hubs[0])
        ungated["vaults"] = []
        self.assertFalse(gated(ungated))
        for room in specs:
            if not room["name"].startswith("bunker_hub_"):
                self.assertFalse(room.get("vaults"), room["name"] + " must not hold an ungated vault")


    def test_terminal_screens_face_open_floor(self):
        offsets = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0)}

        def screen_clear(spec):
            blocks = {}
            for fill in spec["fills"]:
                start, end = fill["from"], fill["to"]
                for x in range(start[0], end[0] + 1):
                    for y in range(start[1], end[1] + 1):
                        for z in range(start[2], end[2] + 1):
                            blocks[(x, y, z)] = fill["block"]
            for terminal in spec.get("terminals", []):
                dx, dy, dz = offsets[terminal.get("facing", "north")]
                x, y, z = terminal["pos"]
                if blocks.get((x + dx, y + dy, z + dz)) != "minecraft:air":
                    return False
            return True

        specs = json.loads((SPEC_DIRECTORY / "bunker.json").read_text(encoding="utf-8"))["templates"]
        with_terminals = [spec for spec in specs if spec.get("terminals")]
        self.assertTrue(with_terminals)
        for spec in with_terminals:
            with self.subTest(name=spec["name"]):
                self.assertTrue(screen_clear(spec))
        turned = copy.deepcopy(with_terminals[0])
        turned["terminals"][0]["facing"] = "east"
        self.assertFalse(screen_clear(turned), "a screen facing the wall must be caught")
        bad = copy.deepcopy(self.spec)
        bad["terminals"] = [{"pos": [1, 0, 1], "log_id": "neroruins:earth.1", "facing": "up"}]
        with self.assertRaises(ValueError):
            build_template(bad)


    def test_every_block_has_models_textures_lang_and_drops(self):
        resources = ROOT / "common/src/main/resources"
        assets = resources / "assets/neroruins"
        language = json.loads((assets / "lang/en_us.json").read_text(encoding="utf-8"))
        mineable = json.loads((resources / "data/minecraft/tags/block/mineable/pickaxe.json").read_text())["values"]

        def problems(blockstate_dir):
            found = []
            for state_file in sorted(blockstate_dir.glob("*.json")):
                name = state_file.stem
                if "block.neroruins." + name not in language:
                    found.append(name + ": no English name")
                for variant in json.loads(state_file.read_text())["variants"].values():
                    model_id = variant["model"].split(":", 1)[1]
                    model_file = assets / "models" / (model_id + ".json")
                    if not model_file.is_file():
                        found.append(name + ": missing model " + model_id)
                        continue
                    for texture in json.loads(model_file.read_text()).get("textures", {}).values():
                        if texture.startswith("neroruins:") and not (
                                assets / "textures" / (texture.split(":", 1)[1] + ".png")).is_file():
                            found.append(name + ": missing texture " + texture)
                if "neroruins:" + name in mineable:
                    for loot_root in (resources, ROOT / "common/src/main/resources-26.3"):
                        if not (loot_root / "data/neroruins/loot_table/blocks" / (name + ".json")).is_file():
                            found.append(name + ": no drop in " + loot_root.name)
            return found

        self.assertEqual([], problems(assets / "blockstates"))
        with tempfile.TemporaryDirectory() as scratch:
            broken = pathlib.Path(scratch)
            (broken / "ghost.json").write_text('{"variants":{"":{"model":"neroruins:block/ghost"}}}')
            self.assertEqual(2, len(problems(broken)), "an unnamed block with a missing model is caught")


if __name__ == "__main__":
    unittest.main()
