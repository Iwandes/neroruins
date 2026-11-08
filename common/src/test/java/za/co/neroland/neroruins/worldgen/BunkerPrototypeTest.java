package za.co.neroland.neroruins.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

import com.google.gson.JsonParser;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.Test;
import za.co.neroland.neroruins.lore.LogCatalog;

class BunkerPrototypeTest {
    @Test
    void everyPrototypePoolHasDistinctReadableTemplates() throws Exception {
        for (String name : new String[] { "bunker_hubs", "bunker_corridors", "bunker_rooms" }) {
            try (var input = getClass().getResourceAsStream("/data/neroruins/worldgen/template_pool/" + name + ".json")) {
                assertNotNull(input);
                var entries = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                        .getAsJsonObject().getAsJsonArray("elements");
                assertTrue(entries.size() >= 2);
                var seen = new HashSet<String>();
                for (var entry : entries) {
                    var definition = entry.getAsJsonObject();
                    assertTrue(definition.get("weight").getAsInt() > 0);
                    assertEquals("neroruins:bunker_weathering",
                            definition.getAsJsonObject("element").get("processors").getAsString());
                    String location = definition.getAsJsonObject("element").get("location").getAsString();
                    assertTrue(location.startsWith("neroruins:"));
                    assertTrue(seen.add(location));
                    try (var template = getClass().getResourceAsStream("/data/neroruins/structure/"
                            + location.substring("neroruins:".length()) + ".nbt")) {
                        assertNotNull(template, location);
                        var nbt = NbtIo.readCompressed(template, NbtAccounter.unlimitedHeap());
                        assertTrue(nbt.get("blocks").toString().contains("minecraft:jigsaw"));
                    }
                }
                assertEquals(entries.size(), seen.size());
            }
        }
    }

    private com.google.gson.JsonObject json(String path) throws Exception {
        try (var input = getClass().getResourceAsStream(path)) {
            assertNotNull(input, path);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void bunkerHubsGenerateNaturallyWithSeededLayoutChoices() throws Exception {
        for (String name : new String[] { "bunker", "bunker_earth" }) {
            var structure = json("/data/neroruins/worldgen/structure/" + name + ".json");
            assertEquals("neroruins:complex_hub", structure.get("type").getAsString());
            assertEquals("bunker", structure.get("family").getAsString());
            assertEquals("none", structure.get("terrain_adaptation").getAsString());
            assertTrue(structure.has("ground_check"));
            var layouts = structure.getAsJsonArray("layouts");
            assertTrue(layouts.size() >= 2, "an instance must be able to pick between layouts");
            var depths = new HashSet<Integer>();
            for (var value : layouts) {
                var layout = value.getAsJsonObject().getAsJsonObject("layout");
                assertEquals("none", layout.get("terrain_adaptation").getAsString());
                assertEquals(structure.get("biomes").getAsString(), layout.get("biomes").getAsString());
                int size = layout.get("size").getAsInt();
                // Rooms are five blocks tall with the interior ending one below their roof. A hub sunk this deep
                // keeps every room interior below the lowest ground the ground check allows on a slope.
                int depth = -layout.getAsJsonObject("start_height").get("absolute").getAsInt();
                int allowedDrop = structure.getAsJsonObject("ground_check").get("max_height_delta").getAsInt();
                assertTrue(depth - 4 >= allowedDrop, name + " rooms would be exposed on the downhill side");
                assertTrue(size >= 1 && size <= 5, "bunker jigsaw depth stays within its budget");
                depths.add(size);
            }
            assertTrue(depths.size() >= 2);
            var set = json("/data/neroruins/worldgen/structure_set/" + name + ".json");
            assertEquals("neroruins:" + name, set.getAsJsonArray("structures").get(0).getAsJsonObject()
                    .get("structure").getAsString());
            var placement = set.getAsJsonObject("placement");
            assertEquals("minecraft:random_spread", placement.get("type").getAsString());
            assertTrue(placement.get("spacing").getAsInt() >= 16, "satellites need hub spacing of 16 chunks");
            assertTrue(placement.get("separation").getAsInt() < placement.get("spacing").getAsInt());
        }
        int earth = json("/data/neroruins/worldgen/structure_set/bunker_earth.json")
                .getAsJsonObject("placement").get("spacing").getAsInt();
        int planets = json("/data/neroruins/worldgen/structure_set/bunker.json")
                .getAsJsonObject("placement").get("spacing").getAsInt();
        assertTrue(earth > planets * 2, "Earth bunkers are rare");
        assertNull(getClass().getResource("/data/neroruins/worldgen/structure_set/probe.json"),
                "the temporary probe fixture must not generate");
    }

    @Test
    void satellitesFollowTheirOwnHubAndPointAtExistingPools() throws Exception {
        for (String hub : new String[] { "bunker", "bunker_earth" }) {
            var indices = new HashSet<Integer>();
            for (String role : new String[] { "antenna", "cache" }) {
                var structure = json("/data/neroruins/worldgen/structure/" + hub + "_" + role + ".json");
                var set = json("/data/neroruins/worldgen/structure_set/" + hub + "_" + role + ".json");
                var placement = structure.getAsJsonObject("placement");
                assertEquals("neroruins:" + hub, placement.get("hub_set").getAsString());
                assertEquals("bunker", placement.get("family").getAsString());
                int chance = placement.get("chance_percent").getAsInt();
                assertTrue(chance > 0 && chance < 100, "satellite counts vary between hubs");
                assertTrue(indices.add(placement.get("index").getAsInt()), "satellite slots must be distinct");
                var setPlacement = set.getAsJsonObject("placement").deepCopy();
                assertEquals("neroruins:satellite", setPlacement.remove("type").getAsString());
                assertEquals(placement, setPlacement, "the set and structure must describe the same slot");
                var hubStructure = json("/data/neroruins/worldgen/structure/" + hub + ".json");
                assertEquals(hubStructure.get("biomes").getAsString(), structure.get("biomes").getAsString());
                String pool = structure.getAsJsonObject("layout").get("start_pool").getAsString();
                var elements = json("/data/neroruins/worldgen/template_pool/" + pool.substring("neroruins:".length())
                        + ".json").getAsJsonArray("elements");
                assertTrue(elements.size() >= 2, "every satellite role has variants");
                for (var element : elements) {
                    String location = element.getAsJsonObject().getAsJsonObject("element").get("location").getAsString();
                    assertNotNull(getClass().getResource("/data/neroruins/structure/"
                            + location.substring("neroruins:".length()) + ".nbt"), location);
                }
            }
        }
    }

    @Test
    void bunkerBiomeTagsKeepSiblingAndSpaceEntriesOptional() throws Exception {
        for (var value : json("/data/neroruins/tags/worldgen/biome/has_structure/bunker.json").getAsJsonArray("values")) {
            assertTrue(value.isJsonObject(), "planet entries must be optional");
            assertTrue(!value.getAsJsonObject().get("required").getAsBoolean());
        }
        for (var value : json("/data/neroruins/tags/worldgen/biome/has_structure/bunker_earth.json").getAsJsonArray("values")) {
            String id = value.getAsString();
            assertTrue(id.startsWith("minecraft:") || id.startsWith("#minecraft:"), id);
            assertTrue(!id.contains("ocean") && !id.contains("river"), "bunkers need dry land");
        }
    }

    @Test
    void packagedBunkerLootCarriesSixValidLogIds() throws Exception {
        try (var input = getClass().getResourceAsStream("/data/neroruins/loot_table/chests/bunker_logs.json")) {
            assertNotNull(input);
            var loot = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            var entries = loot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
            var ids = new HashSet<String>();
            for (var value : entries) {
                var entry = value.getAsJsonObject();
                //? if >=26.3 {
                var modifier = entry.getAsJsonObject("modifier");
                assertEquals("minecraft:set_components", modifier.get("type").getAsString());
                //?} else {
                /*var modifier = entry.getAsJsonArray("functions").get(0).getAsJsonObject();
                assertEquals("minecraft:set_components", modifier.get("function").getAsString());
                *///?}
                String id = modifier.getAsJsonObject("components").get("neroruins:log_id").getAsString();
                assertTrue(LogCatalog.translationPrefix(id).isPresent());
                assertTrue(ids.add(id));
            }
            assertEquals(6, ids.size());
        }
    }

    @Test
    void weatheringOnlyTargetsSurveyHull() throws Exception {
        try (var input = getClass().getResourceAsStream(
                "/data/neroruins/worldgen/processor_list/bunker_weathering.json")) {
            assertNotNull(input);
            var data = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            var rules = data.getAsJsonArray("processors").get(0).getAsJsonObject().getAsJsonArray("rules");
            assertEquals(1, rules.size());
            var rule = rules.get(0).getAsJsonObject();
            var predicate = rule.getAsJsonObject("input_predicate");
            assertEquals("minecraft:random_block_match", predicate.get("predicate_type").getAsString());
            assertEquals("neroruins:survey_hull", predicate.get("block").getAsString());
            assertTrue(predicate.get("probability").getAsFloat() > 0);
            assertTrue(predicate.get("probability").getAsFloat() < 1);
            //? if >=26.3 {
            assertEquals("neroruins:rusted_survey_hull", rule.get("output_state").getAsString());
            //?} else {
            /*assertEquals("neroruins:rusted_survey_hull", rule.getAsJsonObject("output_state").get("Name").getAsString());
            *///?}
        }
    }
}
