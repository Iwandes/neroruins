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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

/**
 * Data checks for the tiered, tag-driven loot. Reads the source resource folders directly so the shared
 * tables and the 26.3 overlay are both checked on every node, whichever one the classpath carries.
 */
class LootDataTest {
    /** Namespaces that are always loaded alongside NeroRuins: vanilla, NeroRuins and Neroland Core. */
    private static final Set<String> ALWAYS_PRESENT = Set.of("minecraft", "neroruins", "nerolandcore", "neroland");
    private static final String LOOT_TAG_PREFIX = "neroruins:loot/";
    private static final int RELIC_COUNT = 9;

    // ---- golden paths over the shipped data --------------------------------------------------

    @Test
    void everyLootTableParsesInBothResourceSets() throws IOException {
        Map<String, JsonObject> shared = tables(sharedResources());
        Map<String, JsonObject> modern = tables(overlayResources());
        assertTrue(shared.keySet().containsAll(List.of("tier/0.json", "tier/1.json", "tier/2.json", "tier/3.json",
                "tier/4.json", "chests/bunker_salvage.json", "chests/bunker_logs.json", "chests/prototype_vault.json")),
                shared.keySet().toString());
        assertFalse(modern.isEmpty());
        for (var table : shared.values()) {
            assertTrue(table.get("pools").isJsonArray());
        }
        for (var table : modern.values()) {
            assertTrue(table.get("pools").isJsonArray());
        }
    }

    @Test
    void everyLootTagReferenceResolves() throws IOException {
        Set<String> tags = lootTagNames();
        for (Path base : List.of(sharedResources(), overlayResources())) {
            for (var entry : tables(base).entrySet()) {
                assertEquals(List.of(), tagReferenceProblems(entry.getValue(), tags), entry.getKey());
            }
        }
    }

    @Test
    void everyLootTagIsRolledBySomeTable() throws IOException {
        Set<String> rolled = new TreeSet<>();
        for (var table : tables(sharedResources()).values()) {
            rolled.addAll(tagReferences(table));
        }
        for (String tag : lootTagNames()) {
            assertTrue(rolled.contains(tag), tag);
        }
    }

    @Test
    void noPoolDependsOnlyOnTags() throws IOException {
        for (Path base : List.of(sharedResources(), overlayResources())) {
            for (var entry : tables(base).entrySet()) {
                assertEquals(List.of(), fallbackProblems(entry.getValue()), entry.getKey());
            }
        }
    }

    @Test
    void siblingItemsInTagsAreOptional() throws IOException {
        List<Path> files = new ArrayList<>();
        for (String namespace : List.of("neroruins", "neroeconomy")) {
            Path dir = sharedResources().resolve("data/" + namespace + "/tags/item");
            if (Files.isDirectory(dir)) {
                try (Stream<Path> walk = Files.walk(dir)) {
                    walk.filter(path -> path.toString().endsWith(".json")).forEach(files::add);
                }
            }
        }
        assertTrue(files.size() >= 18, "expected the loot tags, relics and never_commodity");
        for (Path file : files) {
            assertEquals(List.of(), optionalProblems(read(file)), file.toString());
        }
    }

    @Test
    void nineRelicsAreTaggedNeverCommodityAndHaveAssets() throws IOException {
        Path resources = sharedResources();
        JsonObject relics = read(resources.resolve("data/neroruins/tags/item/relics.json"));
        JsonObject neverCommodity = read(resources.resolve("data/neroeconomy/tags/item/never_commodity.json"));
        assertEquals(List.of(), relicProblems(relics, neverCommodity));
        JsonObject lang = read(resources.resolve("assets/neroruins/lang/en_us.json"));
        for (JsonElement value : relics.getAsJsonArray("values")) {
            String name = value.getAsString().substring("neroruins:".length());
            for (String key : List.of("item.neroruins." + name, "item.neroruins." + name + ".tooltip.0",
                    "item.neroruins." + name + ".tooltip.1")) {
                assertTrue(lang.has(key), key);
            }
            assertTrue(Files.isRegularFile(resources.resolve("assets/neroruins/items/" + name + ".json")), name);
            assertTrue(Files.isRegularFile(resources.resolve("assets/neroruins/models/item/" + name + ".json")), name);
            assertTrue(Files.isRegularFile(resources.resolve("assets/neroruins/textures/item/" + name + ".png")), name);
        }
    }

    @Test
    void overlayDiffersOnlyInSyntax() throws IOException {
        assertEquals(List.of(), parityProblems(tables(sharedResources()), tables(overlayResources())));
    }

    @Test
    void lootConditionsAreWellFormed() throws IOException {
        int modLoaded = 0;
        for (Path base : List.of(sharedResources(), overlayResources())) {
            for (var entry : tables(base).entrySet()) {
                assertEquals(List.of(), conditionProblems(entry.getValue()), entry.getKey());
                modLoaded += countConditions(entry.getValue(), "neroruins:mod_loaded");
            }
        }
        assertTrue(modLoaded > 0, "at least one sibling-flavour pool uses neroruins:mod_loaded");
    }

    @Test
    void tierFourRollsEveryRelicBehindTheRarityLever() throws IOException {
        JsonObject tier = read(sharedResources().resolve("data/neroruins/loot_table/tier/4.json"));
        JsonObject relicPool = tier.getAsJsonArray("pools").get(0).getAsJsonObject();
        JsonObject condition = relicPool.getAsJsonArray("conditions").get(0).getAsJsonObject();
        assertEquals("neroruins:relic_chance", condition.get("condition").getAsString());
        Set<String> rolled = new TreeSet<>();
        for (JsonElement entry : relicPool.getAsJsonArray("entries")) {
            rolled.add(entry.getAsJsonObject().get("name").getAsString());
        }
        Set<String> tagged = new TreeSet<>();
        read(sharedResources().resolve("data/neroruins/tags/item/relics.json")).getAsJsonArray("values")
                .forEach(value -> tagged.add(value.getAsString()));
        assertEquals(tagged, rolled);
    }

    // ---- each check rejects a deliberately broken fixture -----------------------------------

    @Test
    void brokenTagReferenceIsReported() {
        JsonObject table = json("{\"pools\":[{\"rolls\":1,\"entries\":["
                + "{\"type\":\"minecraft:tag\",\"name\":\"neroruins:loot/missing\",\"expand\":true},"
                + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:stick\"}]}]}");
        assertEquals(1, tagReferenceProblems(table, Set.of("neroruins:loot/salvage_metal")).size());
        JsonObject modern = json("{\"pools\":[{\"rolls\":1,\"entries\":["
                + "{\"type\":\"minecraft:tag\",\"items\":\"#neroruins:loot/missing\",\"expand\":true}]}]}");
        assertEquals(1, tagReferenceProblems(modern, Set.of()).size());
    }

    @Test
    void tagOnlyPoolIsReported() {
        JsonObject table = json("{\"pools\":[{\"rolls\":1,\"entries\":["
                + "{\"type\":\"minecraft:tag\",\"name\":\"neroruins:loot/salvage_metal\",\"expand\":true}]},"
                + "{\"rolls\":1,\"entries\":[]}]}");
        assertEquals(2, fallbackProblems(table).size());
    }

    @Test
    void requiredSiblingIdIsReported() {
        JsonObject tag = json("{\"values\":[\"minecraft:iron_nugget\",\"nerotech:circuit_board\","
                + "{\"id\":\"nerospace:alien_tech_scrap\",\"required\":true},\"#nerocolonies:colony_food/staple\","
                + "{\"id\":\"nerologistics:drone\",\"required\":false}]}");
        assertEquals(3, optionalProblems(tag).size());
    }

    @Test
    void relicMissingFromNeverCommodityIsReported() {
        JsonObject relics = json("{\"values\":[\"neroruins:a\",\"neroruins:b\",\"neroruins:c\",\"neroruins:d\","
                + "\"neroruins:e\",\"neroruins:f\",\"neroruins:g\",\"neroruins:h\",\"neroruins:i\"]}");
        JsonObject never = json("{\"values\":[\"neroruins:a\",\"neroruins:b\",\"neroruins:c\",\"neroruins:d\","
                + "\"neroruins:e\",\"neroruins:f\",\"neroruins:g\",\"neroruins:h\"]}");
        assertEquals(1, relicProblems(relics, never).size());
        assertEquals(1, relicProblems(json("{\"values\":[\"neroruins:a\"]}"), never).size());
    }

    @Test
    void overlayWithOldSyntaxOrMissingOverlayIsReported() {
        JsonObject shared = json("{\"pools\":[{\"rolls\":1,\"entries\":["
                + "{\"type\":\"minecraft:tag\",\"name\":\"neroruins:loot/salvage_metal\",\"expand\":true},"
                + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:stick\",\"functions\":"
                + "[{\"function\":\"minecraft:set_count\",\"count\":2}]}],"
                + "\"conditions\":[{\"condition\":\"neroruins:mod_loaded\",\"mod\":\"nerotech\"}]}]}");
        JsonObject correct = json("{\"pools\":[{\"rolls\":1,\"entries\":["
                + "{\"type\":\"minecraft:tag\",\"items\":\"#neroruins:loot/salvage_metal\",\"expand\":true},"
                + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:stick\",\"modifier\":"
                + "{\"type\":\"minecraft:set_count\",\"count\":2}}],"
                + "\"condition\":{\"type\":\"neroruins:mod_loaded\",\"mod\":\"nerotech\"}}]}");
        assertEquals(List.of(), parityProblems(Map.of("t.json", shared), Map.of("t.json", correct)));
        JsonObject stale = shared.deepCopy();
        assertEquals(1, parityProblems(Map.of("t.json", shared), Map.of("t.json", stale)).size());
        assertEquals(1, parityProblems(Map.of("t.json", shared), Map.of()).size());
        JsonObject drifted = correct.deepCopy();
        drifted.getAsJsonArray("pools").get(0).getAsJsonObject().addProperty("rolls", 2);
        assertEquals(1, parityProblems(Map.of("t.json", shared), Map.of("t.json", drifted)).size());
    }

    @Test
    void malformedConditionsAreReported() {
        JsonObject table = json("{\"pools\":["
                + "{\"rolls\":1,\"entries\":[],\"conditions\":[{\"condition\":\"neroruins:mod_loaded\",\"mod\":\"Not A Mod\"}]},"
                + "{\"rolls\":1,\"entries\":[],\"condition\":{\"type\":\"neroruins:relic_chance\",\"chance\":1.5}},"
                + "{\"rolls\":1,\"entries\":[],\"conditions\":[{\"condition\":\"minecraft:random_chance\",\"chance\":0.5},"
                + "{\"condition\":\"minecraft:random_chance\",\"chance\":0.5}]}]}");
        assertEquals(3, conditionProblems(table).size());
    }

    // ---- checks ----------------------------------------------------------------------------

    static List<String> tagReferenceProblems(JsonObject table, Set<String> lootTags) {
        List<String> problems = new ArrayList<>();
        for (String tag : tagReferences(table)) {
            if (tag.startsWith(LOOT_TAG_PREFIX) && !lootTags.contains(tag)) {
                problems.add("unknown loot tag " + tag);
            }
        }
        return problems;
    }

    static List<String> fallbackProblems(JsonObject table) {
        List<String> problems = new ArrayList<>();
        JsonArray pools = table.getAsJsonArray("pools");
        for (int i = 0; i < pools.size(); i++) {
            boolean fallback = false;
            for (JsonElement entry : pools.get(i).getAsJsonObject().getAsJsonArray("entries")) {
                fallback |= !"minecraft:tag".equals(entry.getAsJsonObject().get("type").getAsString());
            }
            if (!fallback) {
                problems.add("pool " + i + " has no non-tag entry");
            }
        }
        return problems;
    }

    static List<String> optionalProblems(JsonObject tag) {
        List<String> problems = new ArrayList<>();
        for (JsonElement value : tag.getAsJsonArray("values")) {
            if (value.isJsonPrimitive()) {
                String id = value.getAsString();
                String namespace = id.substring(id.startsWith("#") ? 1 : 0, id.indexOf(':'));
                if (!ALWAYS_PRESENT.contains(namespace)) {
                    problems.add("required sibling entry " + id);
                }
            } else {
                JsonObject entry = value.getAsJsonObject();
                if (!entry.has("required") || entry.get("required").getAsBoolean()) {
                    String id = entry.get("id").getAsString();
                    String namespace = id.substring(id.startsWith("#") ? 1 : 0, id.indexOf(':'));
                    if (!ALWAYS_PRESENT.contains(namespace)) {
                        problems.add("required sibling entry " + id);
                    }
                }
            }
        }
        return problems;
    }

    static List<String> relicProblems(JsonObject relics, JsonObject neverCommodity) {
        List<String> problems = new ArrayList<>();
        Set<String> ids = new TreeSet<>();
        relics.getAsJsonArray("values").forEach(value -> ids.add(value.getAsString()));
        if (ids.size() != RELIC_COUNT) {
            problems.add("expected " + RELIC_COUNT + " relics, found " + ids.size());
        }
        Set<String> never = new TreeSet<>();
        neverCommodity.getAsJsonArray("values").forEach(value -> never.add(value.getAsString()));
        for (String id : ids) {
            if (!never.contains(id)) {
                problems.add(id + " is not in neroeconomy:never_commodity");
            }
        }
        return problems;
    }

    static List<String> parityProblems(Map<String, JsonObject> shared, Map<String, JsonObject> overlay) {
        List<String> problems = new ArrayList<>();
        for (var entry : shared.entrySet()) {
            JsonElement expected = to263(entry.getValue());
            JsonObject actual = overlay.get(entry.getKey());
            if (actual == null) {
                if (!expected.equals(entry.getValue())) {
                    problems.add(entry.getKey() + " needs a 26.3 overlay");
                }
            } else if (!expected.equals(actual)) {
                problems.add(entry.getKey() + " overlay differs beyond syntax");
            }
        }
        for (String name : overlay.keySet()) {
            if (!shared.containsKey(name)) {
                problems.add(name + " overlay has no shared table");
            }
        }
        return problems;
    }

    static List<String> conditionProblems(JsonElement element) {
        List<String> problems = new ArrayList<>();
        collectConditionProblems(element, problems);
        return problems;
    }

    private static void collectConditionProblems(JsonElement element, List<String> problems) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectConditionProblems(child, problems));
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("conditions") && object.getAsJsonArray("conditions").size() > 1) {
            problems.add("more than one condition; the 26.3 form takes one (wrap in all_of)");
        }
        String type = conditionType(object);
        if ("neroruins:mod_loaded".equals(type)
                && !(object.has("mod") && LootRules.isValidModId(object.get("mod").getAsString()))) {
            problems.add("invalid mod id in neroruins:mod_loaded");
        }
        if ("neroruins:relic_chance".equals(type)) {
            float chance = object.has("chance") ? object.get("chance").getAsFloat() : -1.0F;
            if (!(chance > 0.0F && chance <= 1.0F)) {
                problems.add("relic_chance outside (0, 1]");
            }
        }
        object.entrySet().forEach(child -> collectConditionProblems(child.getValue(), problems));
    }

    private static int countConditions(JsonElement element, String type) {
        if (element.isJsonArray()) {
            int count = 0;
            for (JsonElement child : element.getAsJsonArray()) {
                count += countConditions(child, type);
            }
            return count;
        }
        if (!element.isJsonObject()) {
            return 0;
        }
        JsonObject object = element.getAsJsonObject();
        int count = type.equals(conditionType(object)) ? 1 : 0;
        for (var child : object.entrySet()) {
            count += countConditions(child.getValue(), type);
        }
        return count;
    }

    /** A condition object's type in either syntax ({@code condition} through 26.2, {@code type} in a 26.3 condition). */
    private static String conditionType(JsonObject object) {
        if (object.has("condition") && object.get("condition").isJsonPrimitive()) {
            return object.get("condition").getAsString();
        }
        if (object.has("type") && object.get("type").isJsonPrimitive() && !object.has("entries")
                && !object.has("name") && !object.has("items") && !object.has("value")) {
            return object.get("type").getAsString();
        }
        return null;
    }

    /**
     * The shared (1.21.1 to 26.2) loot syntax rewritten to 26.3's: {@code functions} lists become a
     * {@code modifier} (one object, or a list when there are several) keyed by {@code type}; a single
     * {@code conditions} entry becomes {@code condition} keyed by {@code type}; a tag entry's
     * {@code name} becomes {@code items: "#tag"}.
     */
    static JsonElement to263(JsonElement element) {
        if (element.isJsonArray()) {
            JsonArray out = new JsonArray();
            element.getAsJsonArray().forEach(child -> out.add(to263(child)));
            return out;
        }
        if (!element.isJsonObject()) {
            return element.deepCopy();
        }
        JsonObject in = element.getAsJsonObject();
        JsonObject out = new JsonObject();
        for (var entry : in.entrySet()) {
            switch (entry.getKey()) {
                case "functions" -> {
                    JsonArray modifiers = new JsonArray();
                    entry.getValue().getAsJsonArray().forEach(function -> modifiers.add(renameKey(function, "function")));
                    out.add("modifier", modifiers.size() == 1 ? modifiers.get(0) : modifiers);
                }
                case "conditions" -> {
                    JsonArray conditions = entry.getValue().getAsJsonArray();
                    out.add("condition", conditions.size() == 1 ? renameKey(conditions.get(0), "condition") : conditions.deepCopy());
                }
                default -> out.add(entry.getKey(), to263(entry.getValue()));
            }
        }
        if (out.has("type") && "minecraft:tag".equals(out.get("type").getAsString()) && out.has("name")) {
            out.addProperty("items", "#" + out.remove("name").getAsString());
        }
        return out;
    }

    private static JsonObject renameKey(JsonElement element, String key) {
        JsonObject out = new JsonObject();
        for (var entry : element.getAsJsonObject().entrySet()) {
            out.add(entry.getKey().equals(key) ? "type" : entry.getKey(), to263(entry.getValue()));
        }
        return out;
    }

    private static Set<String> tagReferences(JsonObject table) {
        Set<String> tags = new TreeSet<>();
        for (JsonElement pool : table.getAsJsonArray("pools")) {
            for (JsonElement value : pool.getAsJsonObject().getAsJsonArray("entries")) {
                JsonObject entry = value.getAsJsonObject();
                if ("minecraft:tag".equals(entry.get("type").getAsString())) {
                    tags.add(entry.has("items") ? entry.get("items").getAsString().substring(1) : entry.get("name").getAsString());
                }
            }
        }
        return tags;
    }

    // ---- resources -------------------------------------------------------------------------

    private static Set<String> lootTagNames() throws IOException {
        Set<String> names = new TreeSet<>();
        Path dir = sharedResources().resolve("data/neroruins/tags/item/loot");
        try (Stream<Path> files = Files.list(dir)) {
            files.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .forEach(name -> names.add(LOOT_TAG_PREFIX + name.substring(0, name.length() - ".json".length())));
        }
        return names;
    }

    private static Map<String, JsonObject> tables(Path resources) throws IOException {
        Map<String, JsonObject> tables = new TreeMap<>();
        Path dir = resources.resolve("data/neroruins/loot_table");
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path file : walk.filter(path -> path.toString().endsWith(".json")).toList()) {
                tables.put(dir.relativize(file).toString().replace('\\', '/'), read(file));
            }
        }
        return tables;
    }

    private static Path sharedResources() {
        return projectRoot().resolve("common/src/main/resources");
    }

    private static Path overlayResources() {
        return projectRoot().resolve("common/src/main/resources-26.3");
    }

    /** The repository root, found by walking up from the node directory Gradle runs tests in. */
    private static Path projectRoot() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.isDirectory(dir.resolve("common/src/main/resources/data/neroruins"))) {
                return dir;
            }
        }
        throw new IllegalStateException("NeroRuins source resources not found above " + Path.of("").toAbsolutePath());
    }

    private static JsonObject read(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }
}
