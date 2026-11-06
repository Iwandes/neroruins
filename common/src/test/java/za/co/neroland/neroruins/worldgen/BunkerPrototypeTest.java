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

    @Test
    void incompletePrototypeIsNotNaturallyPlaced() {
        assertNull(getClass().getResource("/data/neroruins/worldgen/structure_set/bunker_prototype.json"));
    }

    @Test
    void prototypeAvoidsBurialMoundsAndHasBoundedLayout() throws Exception {
        try (var input = getClass().getResourceAsStream(
                "/data/neroruins/worldgen/structure/bunker_prototype.json")) {
            assertNotNull(input);
            var structure = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("none", structure.get("terrain_adaptation").getAsString());
            var layout = structure.getAsJsonObject("layout");
            assertEquals("none", layout.get("terrain_adaptation").getAsString());
            assertEquals(2, layout.get("size").getAsInt());
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
