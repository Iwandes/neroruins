package za.co.neroland.neroruins.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import za.co.neroland.neroruins.guardian.SpawnTable;
import za.co.neroland.neroruins.lore.LogCatalog;
import za.co.neroland.neroruins.worldgen.placement.SatelliteLayout;

/** Resource checks for the lattice_temple family; the layout graph itself is walked in tools/test_lattice_temple.py. */
class LatticeTempleTest {
    private static final Map<String, String> SIZE_CLASSES = Map.of(
            "lattice_temple", "citadel", "lattice_temple_complex", "complex",
            "lattice_temple_cindara", "complex", "lattice_temple_outpost", "outpost");

    private static JsonObject json(String path) throws Exception {
        try (var input = LatticeTempleTest.class.getResourceAsStream("/data/neroruins/" + path)) {
            assertNotNull(input, path);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void everySizeClassIsAComplexHubWithinTheJigsawBound() throws Exception {
        for (var entry : SIZE_CLASSES.entrySet()) {
            var structure = json("worldgen/structure/" + entry.getKey() + ".json");
            assertEquals("neroruins:complex_hub", structure.get("type").getAsString());
            assertEquals("lattice_temple", structure.get("family").getAsString());
            assertEquals(entry.getValue(), structure.get("size_class").getAsString());
            assertTrue(structure.has("ground_check"), "temples need level, dry ground");
            for (var value : structure.getAsJsonArray("layouts")) {
                var layout = value.getAsJsonObject().getAsJsonObject("layout");
                assertTrue(layout.get("max_distance_from_center").getAsInt() <= 128);
                assertTrue(layout.get("size").getAsInt() <= 9);
                assertEquals(structure.get("biomes").getAsString(), layout.get("biomes").getAsString());
                json("worldgen/template_pool/" + layout.get("start_pool").getAsString().substring(10) + ".json");
            }
            var set = json("worldgen/structure_set/" + entry.getKey() + ".json").getAsJsonObject("placement");
            assertTrue(set.get("spacing").getAsInt() >= 16, "satellites need hub spacing of 16 chunks");
            assertTrue(set.get("separation").getAsInt() < set.get("spacing").getAsInt());
        }
        int citadel = json("worldgen/structure_set/lattice_temple.json").getAsJsonObject("placement").get("spacing").getAsInt();
        assertTrue(citadel * 16 >= 1400, "about one citadel per 1,500 blocks");
    }

    @Test
    void everyTemplateInALatticePoolExistsAndItsLogsAreKnown() throws Exception {
        List<Path> pools = files("data/neroruins/worldgen/template_pool", "lattice_temple_");
        assertTrue(pools.size() >= 20);
        Set<String> logs = new HashSet<>();
        for (Path pool : pools) {
            for (var element : JsonParser.parseString(Files.readString(pool)).getAsJsonObject().getAsJsonArray("elements")) {
                var definition = element.getAsJsonObject().getAsJsonObject("element");
                assertEquals("neroruins:lattice_temple", definition.get("processors").getAsString());
                String location = definition.get("location").getAsString().substring("neroruins:".length());
                try (var template = getClass().getResourceAsStream("/data/neroruins/structure/" + location + ".nbt")) {
                    assertNotNull(template, location);
                }
            }
        }
        var specs = JsonParser.parseString(Files.readString(projectRoot().resolve("tools/templates/lattice_temple.json")))
                .getAsJsonObject().getAsJsonArray("templates");
        for (var spec : specs) {
            var terminals = spec.getAsJsonObject().getAsJsonArray("terminals");
            if (terminals == null) continue;
            for (var terminal : terminals) logs.add(terminal.getAsJsonObject().get("log_id").getAsString());
        }
        for (String log : logs) assertTrue(LogCatalog.translationPrefix(log).isPresent(), log);
        for (int i = 1; i <= 6; i++) {
            assertTrue(logs.contains("neroruins:lattice." + i));
            assertTrue(logs.contains("neroruins:nyxara." + i));
        }
    }

    @Test
    void processorListOnlySeedsPuzzles() throws Exception {
        var processors = json("worldgen/processor_list/lattice_temple.json").getAsJsonArray("processors");
        assertEquals(1, processors.size());
        assertEquals("neroruins:puzzle_seed", processors.get(0).getAsJsonObject().get("processor_type").getAsString());
    }

    @Test
    void spawnTableHasASentinelPerTierAndOnlyOptionalSiblings() throws Exception {
        SpawnTable table;
        try (var input = getClass().getResourceAsStream("/data/neroruins/spawn_tables/lattice_temple.json")) {
            assertNotNull(input);
            table = SpawnTable.parse(new InputStreamReader(input, StandardCharsets.UTF_8)).getOrThrow();
        }
        assertEquals(List.of(), spawnProblems(table));
        for (int tier = 1; tier <= SpawnTable.MAX_TIER; tier++) {
            assertFalse(table.available(tier, id -> id.startsWith("minecraft:") || id.startsWith("neroruins:")).isEmpty());
        }
        var required = SpawnTable.parse(new StringReader(
                "{\"entries\":[{\"entity\":\"nerocreatures:crystal_golem\",\"weight\":1,\"min_tier\":1,\"max_tier\":3}]}"))
                .getOrThrow();
        assertFalse(spawnProblems(required).isEmpty(), "a required sibling mob and missing Sentinels are reported");
    }

    private static List<String> spawnProblems(SpawnTable table) {
        List<String> problems = new ArrayList<>();
        for (var entry : table.entries()) {
            boolean sibling = entry.entity().startsWith("nerocreatures:");
            if (sibling == entry.required()) problems.add(entry.entity() + " has the wrong required flag");
        }
        for (int tier = 1; tier <= SpawnTable.MAX_TIER; tier++) {
            int t = tier;
            if (table.entries().stream().noneMatch(e -> e.minTier() <= t && t <= e.maxTier() && e.entity().startsWith("neroruins:sentinel_"))) {
                problems.add("tier " + tier + " has no Sentinel");
            }
        }
        return problems;
    }

    @Test
    void biomeTagsNameOnlyOptionalEntries() throws Exception {
        for (String tag : SIZE_CLASSES.keySet()) {
            var values = json("tags/worldgen/biome/has_structure/" + tag + ".json").getAsJsonArray("values");
            assertTrue(values.size() > 0);
            for (var value : values) assertFalse(value.getAsJsonObject().get("required").getAsBoolean());
        }
        assertTrue(json("tags/worldgen/biome/has_structure/lattice_temple.json").toString().contains("nyxara"));
        assertTrue(json("tags/worldgen/biome/has_structure/lattice_temple_cindara.json").toString().contains("cindara"));
    }

    @Test
    void saltsAreUniqueAcrossEveryNeroRuinsSet() throws Exception {
        Map<Long, String> salts = new HashMap<>();
        List<String> clashes = new ArrayList<>();
        for (Path set : files("data/neroruins/worldgen/structure_set", "")) {
            var placement = JsonParser.parseString(Files.readString(set)).getAsJsonObject().getAsJsonObject("placement");
            if (placement.has("salt")) addSalt(salts, clashes, placement.get("salt").getAsLong(), set.getFileName().toString());
        }
        assertEquals(List.of(), clashes);
        Map<Long, String> probe = new HashMap<>();
        List<String> found = new ArrayList<>();
        addSalt(probe, found, 7L, "a");
        addSalt(probe, found, 7L, "b");
        assertEquals(1, found.size(), "a reused salt is reported");
    }

    private static void addSalt(Map<Long, String> salts, List<String> clashes, long salt, String name) {
        String previous = salts.putIfAbsent(salt, name);
        if (previous != null) clashes.add(name + " reuses the salt of " + previous);
    }

    @Test
    void citadelSatellitesAreDistinctOutsideTheHubAndAverageSixToTen() throws Exception {
        List<JsonObject> slots = new ArrayList<>();
        for (int index = 0; ; index++) {
            if (LatticeTempleTest.class.getResource("/data/neroruins/worldgen/structure/lattice_temple_satellite_" + index + ".json") == null) break;
            slots.add(json("worldgen/structure/lattice_temple_satellite_" + index + ".json").getAsJsonObject("placement"));
        }
        assertTrue(slots.size() >= 6 && slots.size() <= 10);
        int minRadius = slots.get(0).get("min_radius").getAsInt();
        int maxRadius = slots.get(0).get("max_radius").getAsInt();
        int total = 0;
        int seeds = 256;
        for (long seed = 0; seed < seeds; seed++) {
            Set<SatelliteLayout.Offset> offsets = new HashSet<>();
            for (int index = 0; index < slots.size(); index++) {
                var offset = SatelliteLayout.offset(seed * 7919L, 40, -12, "lattice_temple", index, minRadius, maxRadius);
                assertTrue(offsets.add(offset), "satellite slots never share a chunk");
                assertTrue(Math.max(Math.abs(offset.x()), Math.abs(offset.z())) >= minRadius);
                if (SatelliteLayout.present(seed * 7919L, 40, -12, "lattice_temple", index,
                        slots.get(index).get("chance_percent").getAsInt())) total++;
            }
        }
        double mean = (double) total / seeds;
        assertTrue(mean >= 6.0 && mean <= 10.0, "mean citadel satellites " + mean);
    }

    private static List<Path> files(String dir, String prefix) throws Exception {
        Path root = projectRoot().resolve("common/src/main/resources").resolve(dir);
        try (Stream<Path> list = Files.list(root)) {
            return list.filter(p -> p.getFileName().toString().startsWith(prefix) && p.toString().endsWith(".json"))
                    .sorted().toList();
        }
    }

    /** The repository root, found by walking up from the node directory Gradle runs tests in. */
    private static Path projectRoot() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.isDirectory(dir.resolve("common/src/main/resources/data/neroruins"))) return dir;
        }
        throw new IllegalStateException("NeroRuins source resources not found");
    }
}
