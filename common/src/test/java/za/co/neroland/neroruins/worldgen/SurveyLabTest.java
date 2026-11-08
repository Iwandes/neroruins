package za.co.neroland.neroruins.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.Test;
import za.co.neroland.neroruins.lore.LogCatalog;
import za.co.neroland.neroruins.worldgen.structure.GroundCheck;

/** The Survey Lab family as data: hubs, budgets, packaged templates and placement (decision 036). */
class SurveyLabTest {
    /** Size class to (depth cap, levels below the surface). */
    private static final Map<String, int[]> CLASSES = Map.of(
            "survey_lab", new int[] { 10, 6 },
            "survey_lab_complex", new int[] { 5, 3 },
            "survey_lab_outpost", new int[] { 3, 1 });

    private JsonObject json(String path) throws Exception {
        try (var input = getClass().getResourceAsStream(path)) {
            assertNotNull(input, path);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private CompoundTag template(String location) throws Exception {
        try (var input = getClass().getResourceAsStream("/data/neroruins/structure/"
                + location.substring("neroruins:".length()) + ".nbt")) {
            assertNotNull(input, location);
            return NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
        }
    }

    private static final Pattern COMPOUND = Pattern.compile("\\{[^{}]*\\}");
    private static final Pattern FIELD = Pattern.compile("([a-z_]+):\"([^\"]*)\"");

    /** The block-entity compounds of one type in a template, read from its SNBT so every version parses alike. */
    private List<Map<String, String>> blockEntities(String location, String id) throws Exception {
        var found = new ArrayList<Map<String, String>>();
        var compounds = COMPOUND.matcher(template(location).get("blocks").toString());
        while (compounds.find()) {
            var fields = new HashMap<String, String>();
            var field = FIELD.matcher(compounds.group());
            while (field.find()) fields.put(field.group(1), field.group(2));
            if (id.equals(fields.get("id"))) found.add(fields);
        }
        return found;
    }

    private List<String> pool(String id) throws Exception {
        var locations = new ArrayList<String>();
        for (var element : json("/data/neroruins/worldgen/template_pool/" + id.substring("neroruins:".length())
                + ".json").getAsJsonArray("elements")) {
            locations.add(element.getAsJsonObject().getAsJsonObject("element").get("location").getAsString());
        }
        return locations;
    }

    @Test
    void hubsDescendToTheirDepthWithinBudget() throws Exception {
        for (var entry : CLASSES.entrySet()) {
            var hub = json("/data/neroruins/worldgen/structure/" + entry.getKey() + ".json");
            assertEquals("neroruins:complex_hub", hub.get("type").getAsString());
            assertEquals("survey_lab", hub.get("family").getAsString());
            var layout = hub.getAsJsonArray("layouts").get(0).getAsJsonObject().getAsJsonObject("layout");
            int size = layout.get("size").getAsInt();
            assertTrue(size <= entry.getValue()[0], entry.getKey() + " jigsaw depth over budget");
            assertTrue(layout.get("max_distance_from_center").getAsInt() <= 128);
            var check = GroundCheck.CODEC.parse(JsonOps.INSTANCE, hub.get("ground_check"));
            assertTrue(check.result().isPresent(), entry.getKey());
            int start = layout.getAsJsonObject("start_height").get("absolute").getAsInt();
            assertTrue(start - 1 < -1 - check.result().get().maxHeightDelta(),
                    entry.getKey() + " first floor would surface on the steepest accepted slope");

            // Follow the down connectors from the surface: every variant of a level leads to the same next level.
            int levels = 0;
            String current = layout.get("start_pool").getAsString();
            while (current != null) {
                String next = null;
                for (String location : pool(current)) {
                    String down = null;
                    for (var jigsaw : blockEntities(location, "minecraft:jigsaw")) {
                        if ("neroruins:survey_lab_up".equals(jigsaw.get("target"))) down = jigsaw.get("pool");
                    }
                    if (next == null) next = down;
                    assertEquals(next, down, location + " leads somewhere its siblings do not");
                }
                if (next != null) levels++;
                current = next;
            }
            assertEquals(entry.getValue()[1], levels, entry.getKey());
        }
    }

    @Test
    void everyLevelHoldsAGateAVaultAndAGuardian() throws Exception {
        int levels = 0;
        for (String structure : CLASSES.keySet()) {
            for (String location : allTemplates(structure)) {
                if (!location.contains("_level_")) continue;
                levels++;
                String blocks = template(location).get("blocks").toString();
                assertTrue(blocks.contains("neroruins:vault_cache"), location);
                assertTrue(blocks.contains("neroruins:ruin_spawner"), location);
                assertTrue(blocks.contains("neroruins:survey_lab"), location + " spawner must use the family table");
            }
        }
        assertEquals(30, levels);
        String palette = template("neroruins:survey_lab_level_1_a").get("palette").toString();
        assertTrue(palette.contains("neroruins:sealed_door"));
        assertTrue(palette.contains("neroruins:card_reader"));
        assertFalse(template("neroruins:survey_lab_room_storage").get("palette").toString()
                .contains("neroruins:sealed_door"), "side rooms carry no gates");
    }

    private List<String> allTemplates(String structure) throws Exception {
        var result = new ArrayList<String>();
        var seen = new HashSet<String>();
        var queue = new ArrayList<String>();
        queue.add(json("/data/neroruins/worldgen/structure/" + structure + ".json").getAsJsonArray("layouts").get(0)
                .getAsJsonObject().getAsJsonObject("layout").get("start_pool").getAsString());
        while (!queue.isEmpty()) {
            String current = queue.remove(queue.size() - 1);
            if (!seen.add(current) || "minecraft:empty".equals(current)) continue;
            for (String location : pool(current)) {
                result.add(location);
                for (var jigsaw : blockEntities(location, "minecraft:jigsaw")) queue.add(jigsaw.get("pool"));
            }
        }
        return result;
    }

    @Test
    void citadelLogsAreTheMarsChapterAndExist() throws Exception {
        var found = new HashSet<String>();
        for (String location : allTemplates("survey_lab")) {
            for (var terminal : blockEntities(location, "neroruins:log_terminal")) {
                String id = terminal.get("log_id");
                assertTrue(id.startsWith("neroruins:mars."), id);
                assertTrue(LogCatalog.translationPrefix(id).isPresent(), id);
                found.add(id);
            }
        }
        assertEquals(6, found.size());
        for (String structure : new String[] { "survey_lab_complex", "survey_lab_outpost" }) {
            for (String location : allTemplates(structure)) {
                assertFalse(template(location).get("blocks").toString().contains("neroruins:log_terminal"),
                        location + " must not repeat chapter four");
            }
        }
    }

    @Test
    void setsAreRarerTheLargerTheClassAndSatellitesFollowTheirHub() throws Exception {
        int citadel = json("/data/neroruins/worldgen/structure_set/survey_lab.json").getAsJsonObject("placement")
                .get("spacing").getAsInt();
        int complex = json("/data/neroruins/worldgen/structure_set/survey_lab_complex.json")
                .getAsJsonObject("placement").get("spacing").getAsInt();
        int outpost = json("/data/neroruins/worldgen/structure_set/survey_lab_outpost.json")
                .getAsJsonObject("placement").get("spacing").getAsInt();
        assertTrue(citadel > complex && complex > outpost && outpost >= 16);
        for (String satellite : new String[] { "survey_lab_antenna", "survey_lab_motor_pool", "survey_lab_reactor_annex",
                "survey_lab_complex_antenna", "survey_lab_complex_motor_pool", "survey_lab_outpost_antenna" }) {
            var structure = json("/data/neroruins/worldgen/structure/" + satellite + ".json");
            assertEquals("neroruins:satellite", structure.get("type").getAsString());
            String hubSet = structure.getAsJsonObject("placement").get("hub_set").getAsString();
            var hub = json("/data/neroruins/worldgen/structure/" + hubSet.substring("neroruins:".length()) + ".json");
            assertEquals(hub.get("biomes").getAsString(), structure.get("biomes").getAsString());
            for (String location : pool(structure.getAsJsonObject("layout").get("start_pool").getAsString())) {
                assertNotNull(template(location));
            }
        }
    }
}
