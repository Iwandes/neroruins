package za.co.neroland.neroruins.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

/**
 * Guards how NeroRuins items may be obtained. Loot, puzzle and vault items must never become craftable, and the
 * unbreakable fixtures must never drop themselves. Reads the shared resources and every {@code resources-<mc>}
 * overlay, so a later recipe in either set cannot make a relic craftable by accident.
 */
class ObtainabilityTest {
    /** Puzzle, vault and guardian fixtures plus the reinforced shells: unbreakable in survival, no block loot. */
    private static final Set<String> UNBREAKABLE = Set.of(
            "vault_cache", "ballast_plate", "ruin_spawner", "sealed_door", "glyph_clue", "glyph_lock", "conduit",
            "conduit_source", "conduit_lock", "resonance_pylon", "resonance_clue", "card_reader", "relic_socket",
            "reinforced_survey_hull", "reinforced_rusted_survey_hull", "reinforced_latticework",
            "reinforced_lattice_floor");
    /** Found in ruins or creative only. A recipe for any of these would bypass a puzzle, a vault or a guardian. */
    private static final Set<String> NEVER_CRAFTED;
    static {
        Set<String> ids = new TreeSet<>(UNBREAKABLE);
        ids.addAll(Set.of("data_shard", "survey_keycard", "security_keycard", "command_keycard",
                "surveyors_compass", "pale_survey_lens", "directors_seal", "field_recorder", "meridian_keystone",
                "reach_thermal_core", "rootglass_vessel", "twilight_prism", "resonance_heart",
                "sentinel_mk1_spawn_egg", "sentinel_mk2_spawn_egg", "sentinel_mk3_spawn_egg"));
        NEVER_CRAFTED = Set.copyOf(ids);
    }

    @Test
    void everyGuardedIdIsARealItem() {
        for (String path : NEVER_CRAFTED) {
            assertTrue(Files.exists(shared().resolve("assets/neroruins/items/" + path + ".json")),
                    "not a NeroRuins item: " + path);
        }
    }

    @Test
    void noRecipeMakesALootOnlyItem() throws IOException {
        List<String> problems = new ArrayList<>();
        for (Path base : resourceSets()) {
            for (Path file : recipeFiles(base)) {
                JsonObject recipe = read(file);
                String result = resultId(recipe);
                if (result.startsWith("neroruins:")) {
                    String path = result.substring("neroruins:".length());
                    if (NEVER_CRAFTED.contains(path)) {
                        problems.add(base.relativize(file) + " makes loot-only " + result);
                    } else if (!Files.exists(shared().resolve("assets/neroruins/items/" + path + ".json"))) {
                        problems.add(base.relativize(file) + " makes unknown " + result);
                    }
                }
            }
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void unbreakableFixturesHaveNoBlockLoot() throws IOException {
        for (Path base : resourceSets()) {
            for (String block : UNBREAKABLE) {
                assertFalse(Files.exists(base.resolve("data/neroruins/loot_table/blocks/" + block + ".json")),
                        base + ": " + block + " must not drop itself");
            }
        }
    }

    @Test
    void minedLogTerminalDropsSurveyHull() throws IOException {
        for (Path base : resourceSets()) {
            Path table = base.resolve("data/neroruins/loot_table/blocks/log_terminal.json");
            assertTrue(Files.exists(table), base + ": log_terminal needs a block loot table");
            String text = Files.readString(table, StandardCharsets.UTF_8);
            assertTrue(text.contains("\"neroruins:survey_hull\""), base + ": a mined terminal drops Survey Hull");
            assertFalse(text.contains("\"neroruins:log_terminal\""), base + ": a mined terminal must not drop itself");
        }
    }

    @Test
    void relicsShardsAndKeycardsStayOutOfTrade() throws IOException {
        Set<String> never = tagValues("data/neroeconomy/tags/item/never_commodity.json");
        for (String id : List.of("surveyors_compass", "pale_survey_lens", "directors_seal", "field_recorder",
                "meridian_keystone", "reach_thermal_core", "rootglass_vessel", "twilight_prism", "resonance_heart",
                "data_shard")) {
            assertTrue(never.contains("neroruins:" + id), id + " missing from never_commodity");
        }
        assertTrue(tagValues("data/neroeconomy/tags/item/untradeable.json").contains("#neroruins:keycards"));
    }

    // ---- helpers ---------------------------------------------------------------------------------

    private static Set<String> tagValues(String rel) throws IOException {
        Set<String> out = new TreeSet<>();
        for (JsonElement value : read(shared().resolve(rel)).getAsJsonArray("values")) {
            out.add(value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString());
        }
        return out;
    }

    private static String resultId(JsonObject recipe) {
        if (!recipe.has("result")) {
            return "";
        }
        JsonElement result = recipe.get("result");
        if (result.isJsonPrimitive()) {
            return result.getAsString();
        }
        JsonObject object = result.getAsJsonObject();
        JsonElement id = object.has("id") ? object.get("id") : object.get("item");
        return id == null ? "" : id.getAsString();
    }

    private static List<Path> recipeFiles(Path base) throws IOException {
        List<Path> out = new ArrayList<>();
        Path data = base.resolve("data");
        if (!Files.isDirectory(data)) {
            return out;
        }
        try (Stream<Path> files = Files.walk(data)) {
            files.filter(p -> p.toString().endsWith(".json"))
                    .filter(p -> data.relativize(p).getNameCount() > 2
                            && data.relativize(p).getName(1).toString().equals("recipe"))
                    .sorted()
                    .forEach(out::add);
        }
        return out;
    }

    private static JsonObject read(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static List<Path> resourceSets() throws IOException {
        List<Path> out = new ArrayList<>();
        out.add(shared());
        try (Stream<Path> dirs = Files.list(shared().getParent())) {
            dirs.filter(p -> p.getFileName().toString().startsWith("resources-")).sorted().forEach(out::add);
        }
        return out;
    }

    private static Path shared() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            Path candidate = dir.resolve("common/src/main/resources");
            if (Files.isDirectory(candidate.resolve("data/neroruins"))) {
                return candidate;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("common/src/main/resources not found above " + Path.of("").toAbsolutePath());
    }
}
