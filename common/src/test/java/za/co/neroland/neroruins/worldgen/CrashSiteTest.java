package za.co.neroland.neroruins.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.Test;
import za.co.neroland.neroruins.lore.LogCatalog;
import za.co.neroland.neroruins.worldgen.placement.SatelliteLayout;
import za.co.neroland.neroruins.worldgen.structure.GroundCheck;

class CrashSiteTest {
    private static final String[][] HUBS = {
            { "crash_site_meridian", "citadel", "14", "8" },
            { "crash_site_complex", "complex", "6", "4" },
            { "crash_site_outpost", "outpost", "2", "0" } };

    private JsonObject json(String path) throws Exception {
        try (var input = getClass().getResourceAsStream(path)) {
            assertNotNull(input, path);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void hubsAreCrashSiteComplexesWithinTheJigsawLimits() throws Exception {
        for (String[] hub : HUBS) {
            var structure = json("/data/neroruins/worldgen/structure/" + hub[0] + ".json");
            assertEquals("neroruins:complex_hub", structure.get("type").getAsString());
            assertEquals("crash_site", structure.get("family").getAsString());
            assertEquals(hub[1], structure.get("size_class").getAsString());
            GroundCheck check = GroundCheck.CODEC.parse(JsonOps.INSTANCE, structure.get("ground_check")).getOrThrow();
            for (var value : structure.getAsJsonArray("layouts")) {
                var layout = value.getAsJsonObject().getAsJsonObject("layout");
                assertTrue(layout.get("size").getAsInt() <= 5, "crash-site depth cap");
                assertTrue(layout.get("max_distance_from_center").getAsInt() <= 128);
                // The buried keel is as deep as the hub is sunk, so the ground check never exposes it.
                assertTrue(-layout.getAsJsonObject("start_height").get("absolute").getAsInt() >= check.maxHeightDelta(),
                        hub[0] + " keel shorter than the slope it accepts");
                var pool = json("/data/neroruins/worldgen/template_pool/"
                        + layout.get("start_pool").getAsString().substring("neroruins:".length()) + ".json");
                var seen = new HashSet<String>();
                for (var entry : pool.getAsJsonArray("elements")) {
                    String location = entry.getAsJsonObject().getAsJsonObject("element").get("location").getAsString();
                    assertTrue(seen.add(location));
                    try (var template = getClass().getResourceAsStream("/data/neroruins/structure/"
                            + location.substring("neroruins:".length()) + ".nbt")) {
                        assertNotNull(template, location);
                        var nbt = NbtIo.readCompressed(template, NbtAccounter.unlimitedHeap());
                        assertTrue(nbt.get("blocks").toString().contains("neroruins:vault_cache"));
                    }
                }
                assertEquals(3, seen.size(), "three hull variants per size class");
            }
        }
    }

    @Test
    void theMeridianDebrisFieldReachesThreeHundredBlocksAndVariesByHub() throws Exception {
        for (String[] hub : HUBS) {
            int count = Integer.parseInt(hub[2]);
            int always = Integer.parseInt(hub[3]);
            for (long seed = 0; seed < 64; seed++) {
                var offsets = new HashSet<SatelliteLayout.Offset>();
                int present = 0;
                for (int index = 0; index < count; index++) {
                    var placement = json("/data/neroruins/worldgen/structure_set/" + hub[0] + "_debris_" + index
                            + ".json").getAsJsonObject("placement");
                    int min = placement.get("min_radius").getAsInt();
                    int max = placement.get("max_radius").getAsInt();
                    int chance = placement.get("chance_percent").getAsInt();
                    var offset = SatelliteLayout.offset(seed, 7, -3, "crash_site", index, min, max);
                    int ring = Math.max(Math.abs(offset.x()), Math.abs(offset.z()));
                    assertTrue(ring >= min && ring <= max);
                    assertTrue(offsets.add(offset), "satellite slots never share a chunk");
                    if (SatelliteLayout.present(seed, 7, -3, "crash_site", index, chance)) present++;
                }
                assertTrue(present >= always && present <= count);
            }
        }
        var outer = json("/data/neroruins/worldgen/structure_set/crash_site_meridian_debris_0.json")
                .getAsJsonObject("placement");
        assertTrue(outer.get("max_radius").getAsInt() * 16 >= 300);
        assertThrows(IllegalArgumentException.class,
                () -> SatelliteLayout.offset(1L, 0, 0, "crash_site", 0, 3, 33), "radius beyond 32 chunks is rejected");
    }

    @Test
    void satellitesPointAtTheirHubSetAndDebrisPool() throws Exception {
        for (String[] hub : HUBS) {
            int spacing = json("/data/neroruins/worldgen/structure_set/" + hub[0] + ".json")
                    .getAsJsonObject("placement").get("spacing").getAsInt();
            assertTrue(spacing >= 16, "satellite hubs need random spread of at least 16 chunks");
            for (int index = 0; index < Integer.parseInt(hub[2]); index++) {
                var structure = json("/data/neroruins/worldgen/structure/" + hub[0] + "_debris_" + index + ".json");
                assertEquals("neroruins:satellite", structure.get("type").getAsString());
                assertEquals("neroruins:" + hub[0], structure.getAsJsonObject("placement").get("hub_set").getAsString());
                assertTrue(structure.getAsJsonObject("placement").get("max_radius").getAsInt() < spacing);
                assertEquals("neroruins:crash_site_debris",
                        structure.getAsJsonObject("layout").get("start_pool").getAsString());
            }
            assertNotNullOrMissing("/data/neroruins/worldgen/structure_set/" + hub[0] + "_debris_"
                    + Integer.parseInt(hub[2]) + ".json");
        }
    }

    private void assertNotNullOrMissing(String path) throws Exception {
        try (var input = getClass().getResourceAsStream(path)) {
            assertEquals(null, input, "no satellite beyond the configured count: " + path);
        }
    }

    @Test
    void theMeridianCarriesTheWholeCindaraChapterAndTheKeystone() throws Exception {
        var logs = new HashSet<String>();
        for (String variant : new String[] { "a", "b", "c" }) {
            try (var template = getClass().getResourceAsStream("/data/neroruins/structure/crash_site_meridian_"
                    + variant + ".nbt")) {
                assertNotNull(template);
                String blocks = NbtIo.readCompressed(template, NbtAccounter.unlimitedHeap()).get("blocks").toString();
                for (int n = 1; n <= 6; n++) {
                    String id = "neroruins:cindara." + n;
                    assertTrue(blocks.contains(id), variant + " is missing " + id);
                    logs.add(id);
                }
                assertTrue(blocks.contains("neroruins:chests/crash_site_keystone"));
                assertFalse(blocks.contains("neroruins:cindara.7"));
            }
        }
        for (String id : logs) assertTrue(LogCatalog.translationPrefix(id).isPresent(), id);
        var keystone = json("/data/neroruins/loot_table/chests/crash_site_keystone.json");
        assertEquals("neroruins:meridian_keystone", keystone.getAsJsonArray("pools").get(0).getAsJsonObject()
                .getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString());
    }
}
