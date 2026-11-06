import copy
import gzip
import json
import struct
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

    def test_junction_has_clearance_for_two_adjacent_rooms(self):
        specs = json.loads((SPEC_DIRECTORY / "bunker.json").read_text(encoding="utf-8"))["templates"]
        junction = next(spec for spec in specs if spec["name"] == "bunker_corridor_junction")
        rooms = [spec for spec in specs if spec["name"].startswith("bunker_room_")
                 and not spec["name"].endswith("_cap")]
        max_width = max(max(room["size"][0], room["size"][2]) for room in rooms)
        self.assertGreaterEqual(junction["size"][0], max_width + 2)
        self.assertGreaterEqual(junction["size"][2], max_width + 2)

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
        data = (assets / "textures/item/data_shard.png").read_bytes()
        self.assertEqual(b"\x89PNG\r\n\x1a\n", data[:8])
        self.assertEqual((16, 16), struct.unpack(">II", data[16:24]))

    def test_terminal_assets_and_both_loot_formats_exist(self):
        resources = ROOT / "common/src/main/resources"
        assets = resources / "assets/neroruins"
        language = json.loads((assets / "lang/en_us.json").read_text(encoding="utf-8"))
        self.assertEqual(language["block.neroruins.log_terminal"], language["item.neroruins.log_terminal"])
        model = json.loads((assets / "models/block/log_terminal.json").read_text())
        self.assertEqual("neroruins:block/log_display", model["textures"]["display"])
        self.assertTrue((assets / "textures/block/log_display.png").is_file())
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


if __name__ == "__main__":
    unittest.main()
