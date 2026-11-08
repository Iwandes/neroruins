import gzip
import json
import re
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SPEC_DIRECTORY = ROOT / "tools" / "templates"
DESTINATION = ROOT / "common" / "src" / "main" / "resources" / "data" / "neroruins" / "structure"
ID = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")
BLOCK_STATE = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+(?:\[[a-z0-9_]+=[a-z0-9_]+(?:,[a-z0-9_]+=[a-z0-9_]+)*\])?$")
NAME = re.compile(r"^[a-z0-9_]+$")
ORIENTATIONS = {"north_up", "south_up", "east_up", "west_up", "up_north", "up_south",
                "up_east", "up_west", "down_north", "down_south", "down_east", "down_west"}


def nbt_string(value):
    raw = value.encode("utf-8")
    if len(raw) > 65535:
        raise ValueError("NBT string is too long")
    return struct.pack(">H", len(raw)) + raw


def tag(kind, name, payload):
    return bytes([kind]) + nbt_string(name) + payload


def integer(value):
    return struct.pack(">i", value)


def compound(*fields):
    return b"".join(fields) + b"\x00"


def nbt_list(kind, entries):
    return bytes([kind]) + integer(len(entries)) + b"".join(entries)


def position(values):
    return nbt_list(3, [integer(value) for value in values])


def resource(value):
    if not isinstance(value, str) or not ID.fullmatch(value):
        raise ValueError("Invalid resource identifier")
    return value


def point(value, size):
    if (not isinstance(value, list) or len(value) != 3
            or any(type(n) is not int or n < 0 or n >= limit for n, limit in zip(value, size))):
        raise ValueError("Template coordinate is outside its bounds")
    return tuple(value)


def build_template(spec):
    size = spec["size"]
    if (not isinstance(size, list) or len(size) != 3
            or any(type(n) is not int or n < 1 or n > 128 for n in size)):
        raise ValueError("Invalid template size")
    if size[0] * size[1] * size[2] > 262144:
        raise ValueError("Template exceeds the block budget")
    palette = []
    states = {}
    blocks = {}

    def state(block, orientation=None):
        properties = {}
        if isinstance(block, dict):
            properties = block.get("properties", {})
            block = block.get("id")
            if (not isinstance(properties, dict) or any(not isinstance(k, str) or not NAME.fullmatch(k)
                    or not isinstance(v, str) or not NAME.fullmatch(v) for k, v in properties.items())):
                raise ValueError("Invalid block-state properties")
        resource(block)
        if orientation is not None and (block != "minecraft:jigsaw" or orientation not in ORIENTATIONS):
            raise ValueError("Invalid jigsaw orientation")
        if orientation is not None:
            properties = {"orientation": orientation}
        key = (block, tuple(sorted(properties.items())))
        if key not in states:
            states[key] = len(palette)
            fields = [tag(8, "Name", nbt_string(block))]
            if properties:
                fields.append(tag(10, "Properties", compound(*[
                    tag(8, name, nbt_string(value)) for name, value in sorted(properties.items())
                ])))
            palette.append(compound(*fields))
        return states[key]

    for fill in spec.get("fills", []):
        start, end = point(fill["from"], size), point(fill["to"], size)
        if any(a > b for a, b in zip(start, end)):
            raise ValueError("Fill bounds are reversed")
        index = state(fill["block"])
        for x in range(start[0], end[0] + 1):
            for y in range(start[1], end[1] + 1):
                for z in range(start[2], end[2] + 1):
                    blocks[(x, y, z)] = (index, None)
    occupied_containers = set()
    for container in spec.get("containers", []):
        pos = point(container["pos"], size)
        if pos in occupied_containers:
            raise ValueError("Multiple containers occupy the same position")
        occupied_containers.add(pos)
        loot_table = resource(container["loot_table"])
        index = state("minecraft:chest")
        blocks[pos] = (index, compound(tag(8, "id", nbt_string("minecraft:chest")),
                                       tag(8, "LootTable", nbt_string(loot_table))))
    for vault in spec.get("vaults", []):
        pos = point(vault["pos"], size)
        if pos in occupied_containers:
            raise ValueError("Multiple block entities occupy the same position")
        occupied_containers.add(pos)
        loot_table = resource(vault["loot_table"])
        structure = resource(vault["structure"])
        blocks[pos] = (state({"id": "neroruins:vault_cache", "properties": {"sealed": "true"}}), compound(
            tag(8, "id", nbt_string("neroruins:vault_cache")), tag(8, "loot_table", nbt_string(loot_table)),
            tag(1, "puzzle", b"\x01"), tag(8, "structure", nbt_string(structure))))
    for spawner in spec.get("spawners", []):
        pos = point(spawner["pos"], size)
        if pos in occupied_containers:
            raise ValueError("Multiple block entities occupy the same position")
        occupied_containers.add(pos)
        table = resource(spawner["table"])
        tier = spawner.get("tier", 1)
        if type(tier) is not int or not 1 <= tier <= 3:
            raise ValueError("Invalid spawner tier")
        blocks[pos] = (state("neroruins:ruin_spawner"), compound(
            tag(8, "id", nbt_string("neroruins:ruin_spawner")), tag(8, "table", nbt_string(table)),
            tag(3, "tier", integer(tier))))
    occupied_connectors = set()
    for terminal in spec.get("terminals", []):
        pos = point(terminal["pos"], size)
        if pos in occupied_containers:
            raise ValueError("Multiple block entities occupy the same position")
        log_id = terminal["log_id"]
        if not isinstance(log_id, str) or not re.fullmatch(
                r"neroruins:(earth|luna|ark|mars|cindara|glacira|greenxertz|nyxara|lattice)\.[1-6]", log_id):
            raise ValueError("Invalid terminal log")
        occupied_containers.add(pos)
        facing = terminal.get("facing", "north")
        if facing not in ("north", "south", "east", "west"):
            raise ValueError("Invalid terminal facing")
        blocks[pos] = (state({"id": "neroruins:log_terminal", "properties": {"facing": facing}}), compound(
            tag(8, "id", nbt_string("neroruins:log_terminal")), tag(8, "log_id", nbt_string(log_id))))
    for connector in spec.get("connectors", []):
        pos = point(connector["pos"], size)
        if pos in occupied_connectors or pos in occupied_containers:
            raise ValueError("Multiple block entities occupy the same position")
        occupied_connectors.add(pos)
        index = state("minecraft:jigsaw", connector["orientation"])
        fields = [tag(8, "id", nbt_string("minecraft:jigsaw"))]
        for key in ("name", "target", "pool"):
            fields.append(tag(8, key, nbt_string(resource(connector[key]))))
        final_state = connector["final_state"]
        if not isinstance(final_state, str) or len(final_state) > 256 or not BLOCK_STATE.fullmatch(final_state):
            raise ValueError("Invalid connector final state")
        fields.append(tag(8, "final_state", nbt_string(final_state)))
        fields.append(tag(8, "joint", nbt_string("rollable")))
        priority = connector.get("priority", 0)
        if type(priority) is not int or not 0 <= priority <= 100:
            raise ValueError("Invalid connector priority")
        if priority:
            fields.append(tag(3, "placement_priority", integer(priority)))
            fields.append(tag(3, "selection_priority", integer(priority)))
        blocks[pos] = (index, compound(*fields))
    if not blocks:
        raise ValueError("Template must contain blocks")
    encoded_blocks = []
    for pos, (index, block_entity) in sorted(blocks.items()):
        fields = [tag(9, "pos", position(pos)), tag(3, "state", integer(index))]
        if block_entity:
            fields.append(tag(10, "nbt", block_entity))
        encoded_blocks.append(compound(*fields))
    payload = compound(tag(3, "DataVersion", integer(3953)), tag(9, "size", position(size)),
                       tag(9, "palette", nbt_list(10, palette)),
                       tag(9, "blocks", nbt_list(10, encoded_blocks)), tag(9, "entities", nbt_list(10, [])))
    return gzip.compress(b"\x0a" + nbt_string("") + payload, mtime=0)


def main():
    templates = []
    for path in sorted(SPEC_DIRECTORY.glob("*.json")):
        document = json.loads(path.read_text(encoding="utf-8"))
        templates.extend(document["templates"])
    if not templates:
        raise ValueError("No template descriptions found")
    names = [entry["name"] for entry in templates]
    if len(set(names)) != len(names) or any(not NAME.fullmatch(name) for name in names):
        raise ValueError("Template names must be unique simple identifiers")
    generated = [(name, build_template(spec)) for name, spec in zip(names, templates)]
    DESTINATION.mkdir(parents=True, exist_ok=True)
    for name, content in generated:
        (DESTINATION / (name + ".nbt")).write_bytes(content)


if __name__ == "__main__":
    main()
