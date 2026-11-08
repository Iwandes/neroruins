package za.co.neroland.neroruins.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import za.co.neroland.neroruins.guardian.SpawnTable;

/** The Colony Field family as packaged: hubs, satellites, biome tags, guardians and loot (decision 037). */
class ColonyFieldTest {
    private static final String[] CLASSES = { "citadel", "complex", "outpost" };
    private static final String[] SATELLITE_ROLES = { "habring", "fields", "silo", "cemetery", "wreck" };

    private JsonObject json(String path) throws Exception {
        try (var input = getClass().getResourceAsStream(path)) {
            assertNotNull(input, path);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private boolean exists(String path) {
        return getClass().getResource(path) != null;
    }

    private static boolean allOptional(JsonArray values) {
        for (var value : values) {
            if (!value.isJsonObject() || value.getAsJsonObject().get("required").getAsBoolean()) return false;
        }
        return true;
    }

    @Test
    void eachSizeClassIsASeededComplexInsideTheJigsawBoundary() throws Exception {
        for (String size : CLASSES) {
            String name = "colony_field_" + size;
            var structure = json("/data/neroruins/worldgen/structure/" + name + ".json");
            assertEquals("neroruins:complex_hub", structure.get("type").getAsString());
            assertEquals("colony_field", structure.get("family").getAsString());
            assertEquals(size, structure.get("size_class").getAsString());
            assertEquals("beard_box", structure.get("terrain_adaptation").getAsString(),
                    "surface sprawl is bedded into the ground rather than left floating");
            assertTrue(structure.has("ground_check"));
            var layouts = structure.getAsJsonArray("layouts");
            assertTrue(layouts.size() >= 2, "an instance must be able to pick between layouts");
            for (var value : layouts) {
                var layout = value.getAsJsonObject().getAsJsonObject("layout");
                assertTrue(layout.get("max_distance_from_center").getAsInt() <= 128);
                assertEquals("neroruins:" + name + "_hubs", layout.get("start_pool").getAsString());
            }
            var hubs = json("/data/neroruins/worldgen/template_pool/" + name + "_hubs.json").getAsJsonArray("elements");
            assertEquals(3, hubs.size(), "three hub variants per size class");
            for (var element : hubs) {
                String location = element.getAsJsonObject().getAsJsonObject("element").get("location").getAsString();
                assertTrue(exists("/data/neroruins/structure/" + location.substring("neroruins:".length()) + ".nbt"),
                        location);
            }
            var set = json("/data/neroruins/worldgen/structure_set/" + name + ".json").getAsJsonObject("placement");
            assertTrue(set.get("separation").getAsInt() < set.get("spacing").getAsInt());
        }
    }

    @Test
    void satellitesFollowTheirOwnHubWithDistinctSlots() throws Exception {
        for (String size : CLASSES) {
            String hub = "colony_field_" + size;
            var indices = new HashSet<Integer>();
            int spacing = json("/data/neroruins/worldgen/structure_set/" + hub + ".json")
                    .getAsJsonObject("placement").get("spacing").getAsInt();
            for (String role : SATELLITE_ROLES) {
                for (int n = 1; exists("/data/neroruins/worldgen/structure/" + hub + "_" + role + "_" + n + ".json"); n++) {
                    String name = hub + "_" + role + "_" + n;
                    var structure = json("/data/neroruins/worldgen/structure/" + name + ".json");
                    var placement = structure.getAsJsonObject("placement");
                    assertEquals("neroruins:" + hub, placement.get("hub_set").getAsString());
                    assertTrue(indices.add(placement.get("index").getAsInt()), name + " reuses a slot");
                    assertTrue(placement.get("max_radius").getAsInt() < spacing);
                    var setPlacement = json("/data/neroruins/worldgen/structure_set/" + name + ".json")
                            .getAsJsonObject("placement").deepCopy();
                    assertEquals("neroruins:satellite", setPlacement.remove("type").getAsString());
                    assertEquals(placement, setPlacement, "the set and structure must describe the same slot");
                }
            }
            assertTrue(indices.size() >= (size.equals("citadel") ? 10 : size.equals("complex") ? 5 : 1),
                    hub + " has too few satellite slots");
        }
    }

    @Test
    void biomeTagsKeepEveryPlanetEntryOptional() throws Exception {
        for (String size : CLASSES) {
            var values = json("/data/neroruins/tags/worldgen/biome/has_structure/colony_field_" + size + ".json")
                    .getAsJsonArray("values");
            assertTrue(allOptional(values), size);
        }
        var required = JsonParser.parseString("[{\"id\":\"nerospace:glacira\",\"required\":true}]").getAsJsonArray();
        assertFalse(allOptional(required), "a required sibling biome would break Core-only worlds");
    }

    @Test
    void guardiansWorkCoreOnlyAndWelcomeNeroCreatures() throws Exception {
        SpawnTable table;
        try (var input = getClass().getResourceAsStream("/data/neroruins/spawn_tables/colony_field.json")) {
            assertNotNull(input);
            table = SpawnTable.parse(new InputStreamReader(input, StandardCharsets.UTF_8)).getOrThrow();
        }
        var heaviest = table.entries().stream().max((a, b) -> Integer.compare(a.weight(), b.weight())).orElseThrow();
        assertEquals("neroruins:sentinel_mk1", heaviest.entity(), "colony guardians are mostly light Sentinels");
        for (var entry : table.entries()) {
            if (!entry.entity().startsWith("neroruins:") && !entry.entity().startsWith("minecraft:")) {
                assertFalse(entry.required(), entry.entity() + " must stay optional");
            }
        }
        var coreOnly = table.available(1, id -> id.startsWith("neroruins:") || id.startsWith("minecraft:"));
        assertFalse(coreOnly.isEmpty(), "a tier-1 spawner must have guardians without sibling mods");
        assertTrue(coreOnly.stream().noneMatch(entry -> entry.entity().startsWith("nerocreatures:")));
        assertTrue(table.available(1, id -> true).stream()
                .anyMatch(entry -> entry.entity().equals("nerocreatures:glacite_wisp")));
    }

    @Test
    void vaultsAndKeycardChestsCarryWhatTheirGatesNeed() throws Exception {
        String citadel = json("/data/neroruins/loot_table/chests/colony_field_citadel_vault.json").toString();
        assertTrue(citadel.contains("neroruins:reach_thermal_core"), "the reliquary socket's relic comes from here");
        String keycard = json("/data/neroruins/loot_table/chests/colony_field_keycard.json").toString();
        assertTrue(keycard.contains("neroruins:survey_keycard"), "outpost readers take a Survey keycard");
        for (String table : new String[] { "supplies", "store", "salvage", "vault", "outpost_vault", "reliquary" }) {
            var pools = json("/data/neroruins/loot_table/chests/colony_field_" + table + ".json").getAsJsonArray("pools");
            assertFalse(pools.isEmpty(), table);
        }
    }
}
