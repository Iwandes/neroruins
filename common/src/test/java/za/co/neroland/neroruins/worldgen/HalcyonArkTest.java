package za.co.neroland.neroruins.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.Test;
import za.co.neroland.neroruins.guardian.SpawnTable;
import za.co.neroland.neroruins.lore.LogCatalog;

/** Resource checks for the Halcyon Ark family; the layout graph itself is walked by tools/test_halcyon_ark.py. */
class HalcyonArkTest {
    private static final String[] CITADEL = { "hub", "cryo", "hydro", "guard", "engineering", "medbay", "captain", "bridge" };
    private static final String[] SECTION = { "hub", "cryo", "guard", "engineering", "medbay", "captain" };

    private JsonObject json(String path) throws Exception {
        try (var input = getClass().getResourceAsStream(path)) {
            assertNotNull(input, path);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private CompoundTag template(String name) throws Exception {
        try (var input = getClass().getResourceAsStream("/data/neroruins/structure/" + name + ".nbt")) {
            assertNotNull(input, name);
            return NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
        }
    }

    private static final Pattern LOG = Pattern.compile("log_id:\\s*\"(neroruins:[a-z]+\\.[1-6])\"");

    /** Log ids of every terminal in a template, read from the block-entity data. */
    private List<String> logs(CompoundTag template) {
        List<String> logs = new ArrayList<>();
        Matcher matcher = LOG.matcher(template.get("blocks").toString());
        while (matcher.find()) logs.add(matcher.group(1));
        return logs;
    }

    @Test
    void citadelHangsInTheVoidAndTheSectionStandsOnLuna() throws Exception {
        var citadel = json("/data/neroruins/worldgen/structure/halcyon_ark.json");
        var layout = citadel.getAsJsonArray("layouts").get(0).getAsJsonObject().getAsJsonObject("layout");
        assertEquals("neroruins:complex_hub", citadel.get("type").getAsString());
        assertEquals("halcyon_ark", citadel.get("family").getAsString());
        assertEquals("citadel", citadel.get("size_class").getAsString());
        assertFalse(citadel.has("ground_check"), "the station void has no ground to check");
        assertFalse(layout.has("project_start_to_heightmap"), "a void has no heightmap to project onto");
        assertTrue(layout.get("max_distance_from_center").getAsInt() <= 128);
        assertTrue(layout.get("size").getAsInt() >= CITADEL.length - 1, "the bridge must be within the jigsaw depth");

        var section = json("/data/neroruins/worldgen/structure/halcyon_ark_luna.json");
        assertEquals("complex", section.get("size_class").getAsString());
        assertEquals("halcyon_ark", section.get("family").getAsString());
        assertTrue(section.has("ground_check"));
        assertEquals("[\"minecraft:the_void\"]",
                json("/data/neroruins/tags/worldgen/biome/has_structure/halcyon_ark.json").getAsJsonArray("values").toString());
    }

    @Test
    void eachSpineCarriesItsChapterLogsOnceInOrder() throws Exception {
        for (int variant = 0; variant < 3; variant++) {
            List<String> citadel = new ArrayList<>();
            for (String role : CITADEL) citadel.addAll(logs(template("halcyon_ark_" + role + "_" + variant)));
            assertEquals(List.of("neroruins:ark.1", "neroruins:ark.2", "neroruins:ark.3", "neroruins:ark.4",
                    "neroruins:ark.5", "neroruins:ark.6"), citadel);
            List<String> section = new ArrayList<>();
            for (String role : SECTION) section.addAll(logs(template("halcyon_ark_luna_" + role + "_" + variant)));
            assertEquals(List.of("neroruins:ark.1", "neroruins:ark.2", "neroruins:ark.4", "neroruins:ark.5"), section);
            for (String id : citadel) assertTrue(LogCatalog.translationPrefix(id).isPresent(), id);
        }
        var sideLogs = new TreeSet<String>();
        for (int variant = 0; variant < 3; variant++) sideLogs.addAll(logs(template("halcyon_ark_side_room_" + variant)));
        assertTrue(sideLogs.isEmpty(), "optional side rooms must never duplicate a unique log");
    }

    @Test
    void satellitesPointAtTheirHubSet() throws Exception {
        String[] satellites = { "stern", "midsection", "pod_a", "pod_b", "debris_a", "debris_b" };
        for (int index = 0; index < satellites.length; index++) {
            var set = json("/data/neroruins/worldgen/structure_set/halcyon_ark_" + satellites[index] + ".json")
                    .getAsJsonObject("placement");
            assertEquals("neroruins:satellite", set.get("type").getAsString());
            assertEquals("neroruins:halcyon_ark", set.get("hub_set").getAsString());
            assertEquals("halcyon_ark", set.get("family").getAsString());
            assertEquals(index, set.get("index").getAsInt());
        }
        assertNotNull(json("/data/neroruins/worldgen/structure_set/halcyon_ark.json"));
    }

    @Test
    void spawnTableParsesAndRejectsAMalformedCopy() throws Exception {
        try (var input = getClass().getResourceAsStream("/data/neroruins/spawn_tables/halcyon_ark.json")) {
            assertNotNull(input);
            SpawnTable table = SpawnTable.parse(new InputStreamReader(input, StandardCharsets.UTF_8)).getOrThrow();
            for (int tier = 1; tier <= 3; tier++) {
                int t = tier;
                assertFalse(table.available(t, id -> id.startsWith("neroruins:") || id.startsWith("minecraft:")).isEmpty(),
                        "Core-only worlds still spawn guardians at tier " + t);
            }
        }
        assertTrue(SpawnTable.parse(new StringReader("{\"entries\":[{\"entity\":\"neroruins:sentinel_mk1\"}]}"))
                .error().isPresent(), "an entry without a weight is rejected");
    }
}
